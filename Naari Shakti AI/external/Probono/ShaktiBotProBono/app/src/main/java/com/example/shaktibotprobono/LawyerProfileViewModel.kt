package com.example.shaktibotprobono

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray

data class LawyerProfileState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lawyer: Lawyer? = null
)

class LawyerProfileViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance(FirebaseApp.getInstance("probono"))

    private val _state = MutableStateFlow(LawyerProfileState())
    val state: StateFlow<LawyerProfileState> = _state.asStateFlow()

    fun prefillLawyer(lawyer: Lawyer) {
        _state.value = LawyerProfileState(isLoading = false, errorMessage = null, lawyer = lawyer)
    }

    fun loadLawyer(lawyerIdOrBarId: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)

                val input = lawyerIdOrBarId.trim()
                println("🔍 loadLawyer input='$input'")
                if (input.isEmpty()) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Lawyer not found"
                    )
                    return@launch
                }

                suspend fun mapDoc(id: String, getter: suspend () -> com.google.firebase.firestore.DocumentSnapshot?): Lawyer? {
                    val doc = getter() ?: return null
                    if (!doc.exists()) return null
                    return Lawyer(
                        id = id,
                        userId = doc.getString("userId"),
                        name = doc.getString("name") ?: "",
                        barId = doc.getString("barId") ?: "",
                        email = doc.getString("email") ?: "",
                        contactNumber = doc.getString("contactNumber") ?: "",
                        state = doc.getString("state") ?: "",
                        specialization = doc.getString("specialization") ?: "",
                        languages = (doc.get("languages") as? List<String>)?.toList() ?: listOf(),
                        gender = doc.getString("gender") ?: "",
                        casesHandled = doc.getString("casesHandled") ?: "",
                        verified = (doc.getString("verificationStatus") == "approved") || (doc.getBoolean("verified") ?: false),
                        availability = doc.getBoolean("availability") ?: true,
                        emailVerified = doc.getBoolean("emailVerified") ?: false,
                        registrationDate = doc.getString("registrationDate") ?: ""
                    )
                }

                fun docRefFor(id: String) = db.collection("lawyers").document(id)

                suspend fun getServerThenCache(ref: com.google.firebase.firestore.DocumentReference): com.google.firebase.firestore.DocumentSnapshot? {
                    return try {
                        println("🌐 Fetching from server: ${ref.path}")
                        ref.get(Source.SERVER).await()
                    } catch (e: Exception) {
                        println("⚠️ Server fetch failed: ${e.message}")
                        try {
                            println("🗄️ Fetching from cache: ${ref.path}")
                            ref.get(Source.CACHE).await()
                        } catch (e2: Exception) {
                            println("❌ Cache fetch failed: ${e2.message}")
                            null
                        }
                    }
                }

                suspend fun queryByBarId(bar: String): Lawyer? {
                    val col = db.collection("lawyers").whereEqualTo("barId", bar).limit(1)
                    val snap = try { col.get(Source.SERVER).await() } catch (_: Exception) {
                        try { col.get(Source.CACHE).await() } catch (_: Exception) { null }
                    }
                    val d = snap?.documents?.firstOrNull() ?: return null
                    return mapDoc(d.id) { d }
                }

                // 1) Try as document ID (SDK)
                val byIdSdk = if (input.contains('/')) {
                    println("⏭️ Skipping by-id fetch because input looks like barId (contains '/'): '$input'")
                    null
                } else {
                    println("🔎 Trying fetch by documentId='$input'")
                    mapDoc(input) { getServerThenCache(docRefFor(input)) }
                }

                // 2) If not found, try as barId (SDK)
                val byBarSdk = if (byIdSdk == null) {
                    println("🔎 Trying fetch by barId='$input'")
                    queryByBarId(input)
                } else null

                // 3) If SDK path failed, try REST by ID
                suspend fun restById(id: String): Lawyer? {
                    return try {
                        val apiKey = db.app.options.apiKey
                        val projectId = db.app.options.projectId ?: "shaktibot-probono"
                        val url = "https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/lawyers/${id}?key=${apiKey}"
                        val client = OkHttpClient()
                        val req = Request.Builder().url(url).get().build()
                        client.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) return@use null
                            val body = resp.body?.string() ?: return@use null
                            val json = JSONObject(body)
                            val fields = json.optJSONObject("fields") ?: return@use null
                            fun str(name: String): String = fields.optJSONObject(name)?.optString("stringValue", "") ?: ""
                            fun bool(name: String): Boolean = fields.optJSONObject(name)?.optBoolean("booleanValue", false) ?: false
                            fun strList(name: String): List<String> {
                                val arr = fields.optJSONObject(name)?.optJSONObject("arrayValue")?.optJSONArray("values") ?: JSONArray()
                                return (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.optString("stringValue", null) }.filter { it.isNotBlank() }
                            }
                            Lawyer(
                                id = id,
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
                                verified = fields.optJSONObject("verificationStatus")?.optString("stringValue", "").equals("approved", true) || bool("verified"),
                                availability = bool("availability"),
                                emailVerified = bool("emailVerified"),
                                registrationDate = str("registrationDate")
                            )
                        }
                    } catch (_: Exception) { null }
                }

                // 4) If still not found, try REST by barId (structured query, limit 1)
                suspend fun restByBar(barId: String): Lawyer? {
                    return try {
                        val apiKey = db.app.options.apiKey
                        val projectId = db.app.options.projectId ?: "shaktibot-probono"
                        val url = "https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents:runQuery?key=${apiKey}"
                        val payload = """
                            {
                              "structuredQuery": {
                                "from": [{"collectionId": "lawyers"}],
                                "where": {
                                  "fieldFilter": {
                                    "field": {"fieldPath": "barId"},
                                    "op": "EQUAL",
                                    "value": {"stringValue": "${barId}"}
                                  }
                                },
                                "limit": 1
                              }
                            }
                        """.trimIndent()
                        val client = OkHttpClient()
                        val mediaType = "application/json".toMediaType()
                        val reqBody = payload.toRequestBody(mediaType)
                        val req = Request.Builder().url(url).post(reqBody).build()
                        client.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) return@use null
                            val body = resp.body?.string() ?: return@use null
                            val arr = JSONArray(body)
                            val first = (0 until arr.length()).firstNotNullOfOrNull { idx -> arr.optJSONObject(idx)?.optJSONObject("document") }
                                ?: return@use null
                            val name = first.optString("name")
                            val id = name.substringAfterLast('/')
                            val fields = first.optJSONObject("fields") ?: return@use null
                            fun str(name: String): String = fields.optJSONObject(name)?.optString("stringValue", "") ?: ""
                            fun bool(name: String): Boolean = fields.optJSONObject(name)?.optBoolean("booleanValue", false) ?: false
                            fun strList(name: String): List<String> {
                                val values = fields.optJSONObject(name)?.optJSONObject("arrayValue")?.optJSONArray("values") ?: JSONArray()
                                return (0 until values.length()).mapNotNull { i -> values.optJSONObject(i)?.optString("stringValue", null) }.filter { it.isNotBlank() }
                            }
                            Lawyer(
                                id = id,
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
                                verified = fields.optJSONObject("verificationStatus")?.optString("stringValue", "").equals("approved", true) || bool("verified"),
                                availability = bool("availability"),
                                emailVerified = bool("emailVerified"),
                                registrationDate = str("registrationDate")
                            )
                        }
                    } catch (_: Exception) { null }
                }

                val byId = byIdSdk ?: (if (!input.contains('/')) restById(input) else null)
                val byBar = byId ?: byBarSdk ?: restByBar(input)
                val resolved = byId ?: byBar
                if (resolved != null) {
                    println("✅ Resolved lawyer: id='${resolved.id}', name='${resolved.name}', barId='${resolved.barId}'")
                } else {
                    println("❌ Could not resolve lawyer for input='$input'")
                }

                if (resolved == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Lawyer not found"
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        lawyer = resolved
                    )
                }

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load lawyer: ${e.message}"
                )
            }
        }
    }
}


