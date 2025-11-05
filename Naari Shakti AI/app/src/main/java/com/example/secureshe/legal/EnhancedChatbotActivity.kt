package com.example.secureshe.legal

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.secureshe.R
import kotlinx.coroutines.launch
import android.util.Log
import com.example.secureshe.legal.ai.LegalAiService
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.view.WindowCompat
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max

class EnhancedChatbotActivity : AppCompatActivity() {

    private lateinit var chatContainer: LinearLayout
    private lateinit var inputField: EditText
    private lateinit var scrollView: ScrollView
    private lateinit var sendBtn: Button
    private lateinit var micButton: ImageButton
    private lateinit var settingsButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var clearButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var progressBar: ProgressBar

    private val REQUEST_CODE_SPEECH_INPUT = 100

    // Mode and simple local persistence
    private lateinit var mode: String // "assistant" | "research" (default assistant)
    private lateinit var prefs: android.content.SharedPreferences
    private val messages = mutableListOf<Msg>()

    private data class Msg(val sender: String, val content: String, val isUser: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enhanced_chatbot)

        // Determine mode and setup per-mode storage
        mode = intent?.getStringExtra("mode") ?: "assistant"
        prefs = getSharedPreferences("legal_ai_chat_${mode}", MODE_PRIVATE)

        // Ensure window resizes when keyboard shows and content fits system windows
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // Also apply IME insets as bottom padding so input stays visible across OEMs
        val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                val navBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, max(imeBottom, navBottom))
                insets
            }
        }

        initializeViews()
        setupClickListeners()
        loadOrWelcome()
    }

    // Remove common disclaimer phrases from AI output per product preference
    private fun cleanDisclaimers(text: String): String {
        val removedLines = text
            .lines()
            .filter { line ->
                val l = line.lowercase()
                !(
                    l.contains("legal advice") ||
                    l.contains("i am not a lawyer") ||
                    l.contains("cannot provide") ||
                    l.contains("not a substitute for")
                )
            }
        // Collapse excessive blank lines
        val collapsed = removedLines.joinToString("\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
        return collapsed
    }

    // Build history in the format the Worker expects: [{ role, parts: [{ text }] }]
    private fun buildHistoryJson(): JSONArray {
        val arr = JSONArray()
        // Take last 20 messages to keep payload small
        val start = (messages.size - 20).coerceAtLeast(0)
        for (i in start until messages.size) {
            val m = messages[i]
            // Skip the initial disclaimer message to reduce bias if desired
            val role = if (m.isUser) "user" else "model"
            arr.put(
                JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(JSONObject().put("text", m.content)))
                }
            )
        }
        return arr
    }

    private fun initializeViews() {
        chatContainer = findViewById(R.id.chatMessagesContainer)
        inputField = findViewById(R.id.userInputEditText)
        sendBtn = findViewById(R.id.sendButton)
        scrollView = findViewById(R.id.chatScrollView)
        micButton = findViewById(R.id.micButton)
        settingsButton = findViewById(R.id.settingsButton)
        clearButton = findViewById(R.id.clearButton)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        sendBtn.setOnClickListener {
            val userInput = inputField.text.toString().trim()
            if (userInput.isNotEmpty()) {
                sendMessage(userInput)
            }
        }

        micButton.setOnClickListener { promptSpeechInput() }

        settingsButton.setOnClickListener { showSettingsDialog() }
        clearButton.setOnClickListener { showClearConversationDialog() }
        // Long press previously used for Ollama ping; removed

        inputField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                val userInput = inputField.text.toString().trim()
                if (userInput.isNotEmpty()) {
                    sendMessage(userInput)
                }
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun sendMessage(message: String) {
        setProcessingState(true)
        addMessage("👤 You", message, true)
        inputField.setText("")

        lifecycleScope.launch {
            try {
                val history = buildHistoryJson()
                val response = LegalAiService.ask(message, history, mode)
                val cleaned = cleanDisclaimers(response)
                val finalText = if (cleaned.isBlank()) {
                    val m = message.trim().lowercase()
                    if (m == "hi" || m == "hello" || m == "hey" || m == "hii" || m == "helo" || m == "yo") {
                        "Hi! How can I help you today?"
                    } else {
                        "(No response. Please try asking in a different way.)"
                    }
                } else cleaned
                addMessage("🤖 AI Assistant", finalText, false)
            } catch (e: Exception) {
                Log.e("EnhancedChatbot", "AI chat failed", e)
                addMessage("🤖 AI Assistant", "Connection failed: ${e.message ?: "Unknown error"}", false)
            } finally {
                setProcessingState(false)
            }
        }
    }

    

    private fun addWelcomeMessage() {
        val text = if (mode == "research") {
            "Hello! This is the Legal Research mode. Ask me to find acts, sections, judgments, and credible references. I’ll summarize and cite sources.\n\nPowered by Gemini (via secure Cloudflare Worker)"
        } else {
            "Hello! This is the AI Legal Assistant. Ask in natural language and I’ll provide clear, actionable information and steps you can take.\n\nPowered by Gemini (via secure Cloudflare Worker)"
        }
        addMessage(
            sender = "🤖 AI Assistant",
            content = text,
            isUser = false
        )
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("AI Legal Assistant")
            .setMessage("This chatbot uses Gemini via a secure Cloudflare Worker. Your API key is not stored on-device.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showClearConversationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Conversation")
            .setMessage("Are you sure you want to clear the conversation history?")
            .setPositiveButton("Clear") { _, _ ->
                chatContainer.removeAllViews()
                messages.clear()
                prefs.edit().remove("history").apply()
                addWelcomeMessage()
                saveConversation()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setProcessingState(processing: Boolean) {
        progressBar.visibility = if (processing) View.VISIBLE else View.GONE
        sendBtn.isEnabled = !processing
        micButton.isEnabled = !processing
        inputField.isEnabled = !processing
    }

    private fun scrollToBottom() {
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun promptSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message...")
        }
        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == Activity.RESULT_OK) {
            val spokenText = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrEmpty()) {
                inputField.setText(spokenText)
                sendMessage(spokenText)
            }
        }
    }

    // ---- Persistence helpers ----
    private fun loadOrWelcome() {
        val json = prefs.getString("history", null)
        if (json.isNullOrBlank()) {
            addWelcomeMessage()
            saveConversation()
            return
        }
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val msg = Msg(
                    sender = o.optString("sender"),
                    content = o.optString("content"),
                    isUser = o.optBoolean("isUser", false)
                )
                messages.add(msg)
                // render
                addMessage(msg.sender, msg.content, msg.isUser)
            }
            // If history existed but empty, show welcome
            if (messages.isEmpty()) {
                addWelcomeMessage()
                saveConversation()
            }
        } catch (e: Exception) {
            Log.w("EnhancedChatbot", "Failed to parse saved history, resetting", e)
            chatContainer.removeAllViews()
            messages.clear()
            addWelcomeMessage()
            saveConversation()
        }
    }

    private fun saveConversation() {
        try {
            val arr = JSONArray()
            messages.forEach { m ->
                arr.put(
                    JSONObject().apply {
                        put("sender", m.sender)
                        put("content", m.content)
                        put("isUser", m.isUser)
                    }
                )
            }
            prefs.edit().putString("history", arr.toString()).apply()
        } catch (e: Exception) {
            Log.w("EnhancedChatbot", "Failed to save conversation", e)
        }
    }

    // Extend addMessage to keep in-memory and persist
    private fun addMessage(sender: String, content: String, isUser: Boolean) {
        val messageLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(16, 8, 16, 8) }
        }

        val messageCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { weight = 1f }
            setPadding(16, 12, 16, 12)
            setBackgroundResource(if (isUser) R.drawable.user_message_bg else R.drawable.ai_message_bg)
        }

        val senderText = TextView(this).apply {
            text = sender
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 4 }
        }

        val contentText = TextView(this).apply {
            text = content
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.black, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        messageCard.addView(senderText)
        messageCard.addView(contentText)
        messageLayout.addView(messageCard)
        chatContainer.addView(messageLayout)
        scrollToBottom()

        // Save in memory and persist
        messages.add(Msg(sender, content, isUser))
        saveConversation()
    }
}
