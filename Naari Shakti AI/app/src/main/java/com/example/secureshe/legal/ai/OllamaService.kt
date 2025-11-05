package com.example.secureshe.legal.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OllamaService(context: Context) {
    private val prefs = context.getSharedPreferences("ollama_prefs", Context.MODE_PRIVATE)
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun storeSettings(baseUrl: String, model: String) {
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
        prefs.edit()
            .putString(KEY_BASE_URL, normalizedBaseUrl)
            .putString(KEY_MODEL, model.trim())
            .apply()
    }

    fun storeAuthToken(token: String) {
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, token.trim())
            .apply()
    }

    fun getBaseUrl(): String? = prefs.getString(KEY_BASE_URL, null)
    fun getModel(): String? = prefs.getString(KEY_MODEL, null)
    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    suspend fun chat(prompt: String, history: List<Pair<String, String>> = emptyList()): String = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl() ?: DEFAULT_BASE_URL
        val model = getModel() ?: DEFAULT_MODEL

        val messagesArray = JSONArray().apply {
            history.forEach { (role, content) ->
                put(JSONObject().apply {
                    put("role", role)
                    put("content", content)
                })
            }
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }

        val payload = JSONObject().apply {
            put("model", model)
            put("prompt", prompt)
            put("stream", false)
        }

        val builder = Request.Builder()
            .url("$baseUrl/api/generate")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("ngrok-skip-browser-warning", "true")
        getAuthToken()?.takeIf { it.isNotBlank() }?.let { builder.addHeader("Authorization", "Bearer $it") }
        val req = builder.build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("Ollama error: ${resp.code}")
            val body = resp.body?.string() ?: throw IllegalStateException("Empty response from Ollama")
            val json = JSONObject(body)
            val message = json.optJSONObject("message")
            message?.optString("content") ?: json.optString("response", "")
        }
    }

    suspend fun ping(): String = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl() ?: DEFAULT_BASE_URL
        val builder = Request.Builder()
            .url("$baseUrl/api/tags")
            .addHeader("ngrok-skip-browser-warning", "true")
            .get()
        getAuthToken()?.takeIf { it.isNotBlank() }?.let { builder.addHeader("Authorization", "Bearer $it") }
        val req = builder.build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("Ping failed: ${resp.code}")
            resp.body?.string() ?: ""
        }
    }

    private fun normalizeBaseUrl(input: String): String {
        var url = input.trim().trimEnd('/')
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = if ("ngrok" in url) "https://$url" else "http://$url"
        }
        // Prefer HTTPS for ngrok domains
        if ("ngrok" in url && url.startsWith("http://")) {
            url = url.replaceFirst("http://", "https://")
        }
        return url
    }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL = "model"
        private const val KEY_AUTH_TOKEN = "auth_token"
        // Defaults requested by user; can be changed in settings inside the app
        private const val DEFAULT_BASE_URL = "http://10.0.2.2:11434"
        private const val DEFAULT_MODEL = "qwen2.5:1.5b-instruct"
    }
}


