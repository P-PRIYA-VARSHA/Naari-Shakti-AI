package com.example.shaktibotprobono

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.google.firebase.firestore.Source
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import com.google.firebase.firestore.ListenerRegistration

class LawyerViewModel : ViewModel() {

    // Full approved dataset kept for fast, accurate client-side filtering
    private val _allLawyers = MutableStateFlow<List<Lawyer>>(emptyList())
    // Exposed filtered list used by UI
    private val _lawyers = MutableStateFlow<List<Lawyer>>(emptyList())
    val lawyers: StateFlow<List<Lawyer>> = _lawyers

    private val db: FirebaseFirestore = FirebaseApp.getInstance("probono").let { app ->
        FirebaseFirestore.getInstance(app)
    }

    private var approvedListener: ListenerRegistration? = null

    init {
        fetchLawyers()
        startApprovedLawyersListener()
    }

    fun fetchLawyers() {
        viewModelScope.launch {
            Log.d("LawyerVM", "Querying Firestore app='${db.app.name}', project='${db.app.options.projectId}'")
            val sdkDeferred = async { sdkFetchApprovedFast() }
            val restDeferred = async { fetchLawyersViaRest() }

            val sdkFirst = withTimeoutOrNull(2500) { sdkDeferred.await() } ?: emptyList()
            if (sdkFirst.isNotEmpty()) {
                _allLawyers.value = sdkFirst
                _lawyers.value = sdkFirst
                return@launch
            }

            val rest = withTimeoutOrNull(6000) { restDeferred.await() } ?: emptyList()
            if (rest.isNotEmpty()) {
                _allLawyers.value = rest
                _lawyers.value = rest
                return@launch
            }

            val fallback = sdkFetchApprovedFast()
            _allLawyers.value = fallback
            _lawyers.value = fallback
        }
    }

    private suspend fun sdkFetchApprovedFast(): List<Lawyer> {
        return try {
            val cacheSnap = db.collection("lawyers")
                .whereEqualTo("verificationStatus", "approved")
                .get(Source.CACHE)
                .await()
            if (!cacheSnap.isEmpty) {
                cacheSnap.documents.mapNotNull { it.toObject(Lawyer::class.java) }
            } else {
                val serverSnap = db.collection("lawyers")
                    .whereEqualTo("verificationStatus", "approved")
                    .get(Source.SERVER)
                    .await()
                serverSnap.documents.mapNotNull { it.toObject(Lawyer::class.java) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun startApprovedLawyersListener() {
        try {
            approvedListener?.remove()
            approvedListener = db.collection("lawyers")
                .whereEqualTo("verificationStatus", "approved")
                .addSnapshotListener { snaps, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }
                    if (snaps == null) return@addSnapshotListener
                    val list = snaps.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Lawyer::class.java)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    _allLawyers.value = list
                    _lawyers.value = list
                }
        } catch (_: Exception) {
            // ignore listener setup issues
        }
    }

    // ✅ Improved filtering with Firestore queries for better performance
    fun applyFilters(
        state: String?,
        specialization: String?,
        language: String?,
        availability: Boolean?,
        query: String?
    ) {
        viewModelScope.launch {
            // Fast client-side filtering on full list
            var list = _allLawyers.value
            if (state != null && state != "All") list = list.filter { it.state.equals(state, ignoreCase = true) }
            if (specialization != null && specialization != "All") list = list.filter { it.specialization.equals(specialization, ignoreCase = true) }
            if (availability != null) list = list.filter { it.availability == availability }
            if (language != null && language != "All") list = list.filter { l -> l.languages.any { it.equals(language, ignoreCase = true) } }
            if (!query.isNullOrBlank()) {
                val q = query.trim()
                list = list.filter { l ->
                    l.name.contains(q, true) || l.barId.contains(q, true) ||
                    l.email.contains(q, true) || l.contactNumber.contains(q, true)
                }
            }
            _lawyers.value = list
        }
    }

    private suspend fun fetchLawyersViaRest(): List<Lawyer> = withContext(Dispatchers.IO) {
        // Build REST URL to list documents in lawyers collection
        val apiKey = db.app.options.apiKey
        val projectId = db.app.options.projectId ?: "shaktibot-probono"
        val url = "https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/lawyers?key=${apiKey}"
        val client = OkHttpClient()
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            val body = resp.body?.string() ?: throw IllegalStateException("Empty body")
            val json = JSONObject(body)
            val docs = if (json.has("documents")) json.getJSONArray("documents") else JSONArray()
            val result = mutableListOf<Lawyer>()
            for (i in 0 until docs.length()) {
                val d = docs.getJSONObject(i)
                // fields object maps Firestore value types
                val fields = d.optJSONObject("fields") ?: continue
                fun jf(name: String) = fields.optJSONObject(name)
                fun str(name: String): String {
                    val o = jf(name) ?: return ""
                    return o.optString("stringValue", "")
                }
                fun bool(name: String): Boolean {
                    val o = jf(name) ?: return false
                    return o.optBoolean("booleanValue", false)
                }
                fun strList(name: String): List<String> {
                    val o = jf(name) ?: return emptyList()
                    val arr = o.optJSONObject("arrayValue")?.optJSONArray("values") ?: return emptyList()
                    val list = mutableListOf<String>()
                    for (j in 0 until arr.length()) {
                        val v = arr.getJSONObject(j)
                        list.add(v.optString("stringValue", ""))
                    }
                    return list.filter { it.isNotBlank() }
                }
                val verification = (str("verificationStatus")).ifBlank { str("status") }
                val isApproved = verification.equals("approved", true) || bool("verified")
                result.add(
                    Lawyer(
                        id = d.optString("name").substringAfterLast("/"),
                        userId = str("userId"),
                        name = str("name"),
                        barId = str("barId"),
                        email = str("email"),
                        contactNumber = str("contactNumber"),
                        state = str("state"),
                        specialization = str("specialization"),
                        languages = strList("languages"),
                        gender = str("gender"),
                        casesHandled = str("casesHandled"),
                        verified = isApproved,
                        availability = bool("availability")
                    )
                )
            }
            // Only show approved
            return@withContext result.filter { it.verified }
        }
    }

    // Fallback method for client-side filtering when Firestore queries fail
    private fun fallbackFiltering(
        state: String?,
        specialization: String?,
        language: String?,
        availability: Boolean?,
        query: String?
    ) {
        viewModelScope.launch {
            db.collection("lawyers")
                .get()
                .addOnSuccessListener { result ->
                    var filtered = result.mapNotNull { doc ->
                        try {
                            doc.toObject(Lawyer::class.java)?.also { lawyer ->
                                println("📋 Fallback lawyer: ${lawyer.name}, Email: ${lawyer.email}, Phone: ${lawyer.contactNumber}")
                            }
                        } catch (e: Exception) {
                            println("❌ Error mapping lawyer document: ${e.message}")
                            null
                        }
                    }

                    // Apply state filter
                    if (state != null && state != "All") {
                        filtered = filtered.filter { it.state == state }
                    }

                    // Apply specialization filter
                    if (specialization != null && specialization != "All") {
                        filtered = filtered.filter { it.specialization == specialization }
                    }

                    // Apply language filter
                    if (language != null && language != "All") {
                        filtered = filtered.filter { lawyer ->
                            lawyer.languages.contains(language)
                        }
                    }

                    // Apply availability filter
                    if (availability != null) {
                        filtered = filtered.filter { it.availability == availability }
                    }

                    // Apply text search filter
                    if (!query.isNullOrBlank()) {
                        filtered = filtered.filter { lawyer ->
                            lawyer.name.contains(query, ignoreCase = true) ||
                                    lawyer.barId.contains(query, ignoreCase = true) ||
                                    lawyer.email.contains(query, ignoreCase = true) ||
                                    lawyer.contactNumber.contains(query, ignoreCase = true)
                        }
                    }

                    _lawyers.value = filtered
                }
        }
    }

    override fun onCleared() {
        try {
            approvedListener?.remove()
        } catch (_: Exception) {
        }
        approvedListener = null
        super.onCleared()
    }
}

