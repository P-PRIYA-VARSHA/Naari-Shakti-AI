package com.example.sosalert.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AIChatbotService(private val context: Context) {
    
    companion object {
        private const val TAG = "AIChatbotService"
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
        private const val ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages"
        private const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"
        private const val DEFAULT_OLLAMA_BASE_URL = "http://10.0.2.2:11434" // Emulator host loopback
        private const val OLLAMA_CHAT_PATH = "/api/chat"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    
    // AI Provider enum
    enum class AIProvider {
        OPENAI, ANTHROPIC, GEMINI, OLLAMA, LOCAL
    }
    
    // Message data class
    data class ChatMessage(
        val role: String,
        val content: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    // Response data class
    data class AIResponse(
        val content: String,
        val provider: AIProvider,
        val confidence: Float = 1.0f,
        val suggestions: List<String> = emptyList()
    )
    
    private val conversationHistory = mutableListOf<ChatMessage>()
    
    suspend fun sendMessage(
        message: String,
        provider: AIProvider = AIProvider.OPENAI,
        apiKey: String? = null
    ): AIResponse = withContext(Dispatchers.IO) {
        try {
            // Add user message to history
            conversationHistory.add(ChatMessage("user", message))
            
            val response = when (provider) {
                AIProvider.OPENAI -> sendToOpenAI(message, apiKey)
                AIProvider.ANTHROPIC -> sendToAnthropic(message, apiKey)
                AIProvider.GEMINI -> sendToGemini(message, apiKey)
                AIProvider.OLLAMA -> sendToOllama(message)
                AIProvider.LOCAL -> generateLocalResponse(message)
            }
            
            // Add AI response to history
            conversationHistory.add(ChatMessage("assistant", response.content))
            
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}", e)
            AIResponse(
                content = "I apologize, but I'm having trouble connecting to my AI services right now. Please try again later or use the local response mode.",
                provider = AIProvider.LOCAL,
                confidence = 0.0f
            )
        }
    }
    
    private suspend fun sendToOllama(message: String): AIResponse {
        val baseUrl = getStoredOllamaBaseUrl() ?: DEFAULT_OLLAMA_BASE_URL
        val model = getStoredOllamaModel() ?: "qwen2.5:1.5b-instruct"

        // Build chat history for Ollama
        val messagesJson = JSONArray().apply {
            // System prompt to steer style and avoid templates/boilerplate
            put(JSONObject().apply {
                put("role", "system")
                put("content", "You are a concise legal information assistant for India. Answer the user's question directly in 3-6 sentences. Do not write emails, letters, or templates. Do not add headings, greetings, or sign-offs. Avoid boilerplate; focus only on the specific question. If you don't know, say you are unsure and suggest where to check.")
            })

            // Include a short recent history to keep context
            conversationHistory.takeLast(8).forEach { m ->
                put(JSONObject().apply {
                    put("role", m.role)
                    put("content", m.content)
                })
            }

            // Current user message
            put(JSONObject().apply {
                put("role", "user")
                put("content", message)
            })
        }

        val body = JSONObject().apply {
            put("model", model)
            put("messages", messagesJson)
            put("stream", false)
            // Lower temperature to reduce generic boilerplate
            put("options", JSONObject().apply {
                put("temperature", 0.25)
                put("top_p", 0.9)
                put("repeat_penalty", 1.15)
                put("num_predict", 384)
            })
        }.toString()

        val request = Request.Builder()
            .url(baseUrl + OLLAMA_CHAT_PATH)
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful || responseBody == null) {
            throw IOException("Ollama error: ${response.code} - $responseBody")
        }

        val json = JSONObject(responseBody)
        val messageObj = json.optJSONObject("message")
        val content = messageObj?.optString("content")
            ?: json.optString("response") // Fallback if using /api/generate shape

        return AIResponse(
            content = content.ifEmpty { "(No response from model)" },
            provider = AIProvider.OLLAMA,
            confidence = 0.8f
        )
    }

    private suspend fun sendToOpenAI(message: String, apiKey: String?): AIResponse {
        val key = apiKey ?: getStoredAPIKey(AIProvider.OPENAI)
        if (key.isNullOrEmpty()) {
            throw IllegalArgumentException("OpenAI API key not provided")
        }
        
        val systemPrompt = """
            You are a helpful legal assistant AI. You provide accurate, helpful legal information and guidance while being careful to:
            1. Clarify that you're not providing legal advice, just general information
            2. Encourage users to consult with qualified legal professionals for specific advice
            3. Provide helpful resources and next steps
            4. Be empathetic and supportive
            5. Keep responses concise but informative
            6. Focus on Indian legal system when relevant
        """.trimIndent()
        
        val messages = mutableListOf<JSONObject>()
        messages.add(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })
        
        // Add recent conversation history (last 10 messages to avoid token limits)
        val recentHistory = conversationHistory.takeLast(10)
        recentHistory.forEach { chatMessage ->
            messages.add(JSONObject().apply {
                put("role", chatMessage.role)
                put("content", chatMessage.content)
            })
        }
        
        val requestBody = JSONObject().apply {
            put("model", "gpt-3.5-turbo")
            put("messages", messages)
            put("max_tokens", 500)
            put("temperature", 0.7)
        }.toString()
        
        val request = Request.Builder()
            .url(OPENAI_API_URL)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody(jsonMediaType))
            .build()
        
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        
        if (!response.isSuccessful || responseBody == null) {
            throw IOException("OpenAI API error: ${response.code} - $responseBody")
        }
        
        val jsonResponse = JSONObject(responseBody)
        val choices = jsonResponse.getJSONArray("choices")
        val firstChoice = choices.getJSONObject(0)
        val messageObj = firstChoice.getJSONObject("message")
        val content = messageObj.getString("content")
        
        return AIResponse(
            content = content,
            provider = AIProvider.OPENAI,
            confidence = 0.9f
        )
    }
    
    private suspend fun sendToAnthropic(message: String, apiKey: String?): AIResponse {
        val key = apiKey ?: getStoredAPIKey(AIProvider.ANTHROPIC)
        if (key.isNullOrEmpty()) {
            throw IllegalArgumentException("Anthropic API key not provided")
        }
        
        val systemPrompt = """
            You are a helpful legal assistant AI. You provide accurate, helpful legal information and guidance while being careful to:
            1. Clarify that you're not providing legal advice, just general information
            2. Encourage users to consult with qualified legal professionals for specific advice
            3. Provide helpful resources and next steps
            4. Be empathetic and supportive
            5. Keep responses concise but informative
            6. Focus on Indian legal system when relevant
        """.trimIndent()
        
        val requestBody = JSONObject().apply {
            put("model", "claude-3-sonnet-20240229")
            put("max_tokens", 500)
            put("system", systemPrompt)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", message)
                })
            })
        }.toString()
        
        val request = Request.Builder()
            .url(ANTHROPIC_API_URL)
            .addHeader("x-api-key", key)
            .addHeader("Content-Type", "application/json")
            .addHeader("anthropic-version", "2023-06-01")
            .post(requestBody.toRequestBody(jsonMediaType))
            .build()
        
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        
        if (!response.isSuccessful || responseBody == null) {
            throw IOException("Anthropic API error: ${response.code} - $responseBody")
        }
        
        val jsonResponse = JSONObject(responseBody)
        val content = jsonResponse.getJSONArray("content").getJSONObject(0).getString("text")
        
        return AIResponse(
            content = content,
            provider = AIProvider.ANTHROPIC,
            confidence = 0.9f
        )
    }
    
    private suspend fun sendToGemini(message: String, apiKey: String?): AIResponse {
        val key = apiKey ?: getStoredAPIKey(AIProvider.GEMINI)
        if (key.isNullOrEmpty()) {
            throw IllegalArgumentException("Gemini API key not provided")
        }
        
        val systemPrompt = """
            You are a helpful legal assistant AI. You provide accurate, helpful legal information and guidance while being careful to:
            1. Clarify that you're not providing legal advice, just general information
            2. Encourage users to consult with qualified legal professionals for specific advice
            3. Provide helpful resources and next steps
            4. Be empathetic and supportive
            5. Keep responses concise but informative
            6. Focus on Indian legal system when relevant
        """.trimIndent()
        
        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "$systemPrompt\n\nUser: $message")
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 500)
                put("temperature", 0.7)
            })
        }.toString()
        
        val request = Request.Builder()
            .url("$GEMINI_API_URL?key=$key")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody(jsonMediaType))
            .build()
        
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        
        if (!response.isSuccessful || responseBody == null) {
            throw IOException("Gemini API error: ${response.code} - $responseBody")
        }
        
        val jsonResponse = JSONObject(responseBody)
        val candidates = jsonResponse.getJSONArray("candidates")
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        
        return AIResponse(
            content = content,
            provider = AIProvider.GEMINI,
            confidence = 0.9f
        )
    }
    
    private suspend fun generateLocalResponse(message: String): AIResponse {
        val input = message.lowercase()
        
        val response = when {
            // Legal advice requests
            input.contains("legal advice") || input.contains("legal help") -> {
                "I can provide general legal information, but I cannot give specific legal advice. For personalized legal advice, please consult with a qualified attorney. I can help you understand legal processes, find resources, and guide you through basic legal procedures."
            }
            
            // Criminal law
            input.contains("criminal") || input.contains("arrest") || input.contains("charge") -> {
                "For criminal matters, you have the right to legal representation. If you've been arrested, you have the right to remain silent and speak to an attorney. Contact a criminal defense lawyer immediately. I can help you understand your rights and the legal process."
            }
            
            // Civil law
            input.contains("civil") || input.contains("lawsuit") || input.contains("sue") -> {
                "Civil cases involve disputes between individuals or organizations. You may need to file a lawsuit in the appropriate court. Consider consulting with a civil attorney to understand your options and the legal process involved."
            }
            
            // Family law
            input.contains("divorce") || input.contains("custody") || input.contains("family") -> {
                "Family law matters are sensitive and complex. For divorce, custody, or other family issues, it's important to work with a family law attorney who can guide you through the process and protect your interests."
            }
            
            // Property law
            input.contains("property") || input.contains("real estate") || input.contains("land") -> {
                "Property law issues can be complex. Whether it's buying, selling, or disputes, you should consult with a real estate attorney. Document everything and keep all relevant paperwork organized."
            }
            
            // Employment law
            input.contains("employment") || input.contains("work") || input.contains("job") -> {
                "Employment law covers workplace rights and disputes. If you're facing workplace issues, document everything and consider consulting with an employment attorney. You may also contact your local labor board."
            }
            
            // General help
            else -> {
                "I'm here to help with legal information and guidance. Please let me know what specific legal issue you're facing, and I'll do my best to provide helpful information and resources. Remember, for specific legal advice, always consult with a qualified attorney."
            }
        }
        
        return AIResponse(
            content = response,
            provider = AIProvider.LOCAL,
            confidence = 0.7f,
            suggestions = listOf("Get legal consultation", "Document everything", "Know your rights")
        )
    }
    
    fun getStoredAPIKey(provider: AIProvider): String? {
        val sharedPrefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
        return when (provider) {
            AIProvider.OPENAI -> sharedPrefs.getString("openai_api_key", null)
            AIProvider.ANTHROPIC -> sharedPrefs.getString("anthropic_api_key", null)
            AIProvider.GEMINI -> sharedPrefs.getString("gemini_api_key", null)
            AIProvider.OLLAMA -> null
            AIProvider.LOCAL -> null
        }
    }
    
    fun storeAPIKey(provider: AIProvider, apiKey: String) {
        val sharedPrefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
        val key = when (provider) {
            AIProvider.OPENAI -> "openai_api_key"
            AIProvider.ANTHROPIC -> "anthropic_api_key"
            AIProvider.GEMINI -> "gemini_api_key"
            AIProvider.OLLAMA -> return
            AIProvider.LOCAL -> return
        }
        sharedPrefs.edit().putString(key, apiKey).apply()
    }

    fun storeOllamaSettings(baseUrl: String, model: String) {
        val sharedPrefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putString("ollama_base_url", baseUrl)
            .putString("ollama_model", model)
            .apply()
    }

    fun getStoredOllamaBaseUrl(): String? {
        val sharedPrefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
        return sharedPrefs.getString("ollama_base_url", null)
    }

    fun getStoredOllamaModel(): String? {
        val sharedPrefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
        return sharedPrefs.getString("ollama_model", null)
    }
    
    fun clearConversationHistory() {
        conversationHistory.clear()
    }
    
    fun getConversationHistory(): List<ChatMessage> {
        return conversationHistory.toList()
    }
} 