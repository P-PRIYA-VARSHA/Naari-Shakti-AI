package com.example.secureshe.legal.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object LegalAiService {
    // Set your Cloudflare Worker URL here
    private const val WORKER_URL: String = "https://naari-legal-chat.likhithadorraju.workers.dev/"
    // Use the cheaper/faster model by default to avoid quota issues
    private const val DEFAULT_MODEL: String = "gemini-1.5-flash"
    // Optional: If you secure the Worker with a bearer token, use BuildConfig to avoid hardcoding in source
    private val AUTH_TOKEN: String = com.example.secureshe.BuildConfig.WORKER_AUTH

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(false)
            .build()
    }

    suspend fun ask(prompt: String): String = ask(prompt, null, null)

    suspend fun ask(prompt: String, history: JSONArray?): String = ask(prompt, history, null)

    suspend fun ask(prompt: String, history: JSONArray?, mode: String?): String = withContext(Dispatchers.IO) {
        val bodyJson = JSONObject().apply {
            put("prompt", prompt)
            // Optional tuning
            put("modelName", DEFAULT_MODEL)
            // ChatGPT-like: balanced creativity
            put("temperature", 0.6)
            // Allow longer, well-structured replies
            put("maxOutputTokens", 2048)
            if (history != null) put("history", history)
            if (!mode.isNullOrBlank()) put("mode", mode)
        }.toString()

        val builder = Request.Builder()
            .url(WORKER_URL)
            .post(bodyJson.toRequestBody(jsonMedia))
            .addHeader("X-Debug-Client", "android")
        if (AUTH_TOKEN.isNotBlank()) {
            builder.addHeader("Authorization", "Bearer $AUTH_TOKEN")
        }
        val req = builder.build()

        // Debug log (masked): confirm token presence and length
        try {
            Log.d(
                "LegalAiService",
                "Sending request: url=$WORKER_URL, authBlank=${AUTH_TOKEN.isBlank()}, authLen=${AUTH_TOKEN.length}"
            )
        } catch (_: Throwable) { }

        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (resp.code in 300..399) {
                val loc = resp.header("Location").orEmpty()
                error("Worker redirect ${resp.code} -> ${loc}. Avoid redirects; use the exact Worker URL.")
            }
            if (!resp.isSuccessful) error("Worker HTTP ${resp.code}: ${text}")
            val json = JSONObject(text)
            if (!json.optBoolean("ok", false)) {
                val err = json.optString("error", "AI call failed")
                throw IllegalStateException(err)
            }
            // Try multiple shapes for message text
            var out = json.optString("text", "")
            if (out.isBlank()) out = json.optString("output", "")
            if (out.isBlank()) out = json.optString("reply", "")
            if (out.isBlank()) out = json.optString("message", "")
            // Gemini-like candidates/content forms
            if (out.isBlank()) {
                val choices = json.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val first = choices.optJSONObject(0)
                    if (first != null) out = first.optString("text", out)
                }
            }
            if (out.isBlank()) {
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val c0 = candidates.optJSONObject(0)
                    // Google-style: { content: { parts: [{text: "..."}] } }
                    val contentObj = c0?.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    val p0 = parts?.optJSONObject(0)
                    if (p0 != null) out = p0.optString("text", out)
                    // Some variants: content as array
                    if (out.isBlank()) {
                        val contentArr = c0?.optJSONArray("content")
                        val cfirst = contentArr?.optJSONObject(0)
                        if (cfirst != null) out = cfirst.optString("text", out)
                    }
                }
            }
            if (out.isBlank()) {
                try { Log.w("LegalAiService", "Empty AI text. Raw json=${json.toString()}") } catch (_: Throwable) {}
            }
            out
        }
    }
}
