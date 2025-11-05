package com.example.sosalert

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
import com.example.sosalert.ai.AIChatbotService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import java.util.*

class EnhancedChatbotActivity : AppCompatActivity() {

    private lateinit var chatContainer: LinearLayout
    private lateinit var inputField: EditText
    private lateinit var scrollView: ScrollView
    private lateinit var sendBtn: Button
    private lateinit var micButton: ImageButton
    private lateinit var settingsButton: FloatingActionButton
    private lateinit var clearButton: FloatingActionButton
    private lateinit var progressBar: ProgressBar

    private val REQUEST_CODE_SPEECH_INPUT = 100
    private lateinit var aiService: AIChatbotService
    private var currentAIProvider = AIChatbotService.AIProvider.LOCAL
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enhanced_chatbot)

        initializeViews()
        setupAI()
        setupClickListeners()
        addWelcomeMessage()
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

    private fun setupAI() {
        aiService = AIChatbotService(this)
        // Force Ollama as the only provider
        currentAIProvider = AIChatbotService.AIProvider.OLLAMA
    }

    private fun setupClickListeners() {
        sendBtn.setOnClickListener {
            val userInput = inputField.text.toString().trim()
            if (userInput.isNotEmpty() && !isProcessing) {
                sendMessage(userInput)
            }
        }

        micButton.setOnClickListener {
            promptSpeechInput()
        }

        settingsButton.setOnClickListener {
            showSettingsDialog()
        }

        clearButton.setOnClickListener {
            showClearConversationDialog()
        }

        // Send on Enter key
        inputField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                val userInput = inputField.text.toString().trim()
                if (userInput.isNotEmpty() && !isProcessing) {
                    sendMessage(userInput)
                }
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun sendMessage(message: String) {
        if (isProcessing) return
        
        isProcessing = true
        setProcessingState(true)
        
        // Add user message to chat
        addMessage("👤 You", message, true)
        inputField.setText("")
        
        // Send to AI service
        lifecycleScope.launch {
            try {
                val response = aiService.sendMessage(message, currentAIProvider)
                addMessage("🤖 AI Assistant", response.content, false, response.provider)
                
                // Add suggestions if available
                if (response.suggestions.isNotEmpty()) {
                    addSuggestions(response.suggestions)
                }
            } catch (e: Exception) {
                addMessage("🤖 AI Assistant", "I apologize, but I encountered an error. Please try again or check your internet connection.", false, AIChatbotService.AIProvider.LOCAL)
            } finally {
                isProcessing = false
                setProcessingState(false)
            }
        }
    }

    private fun addMessage(sender: String, content: String, isUser: Boolean, provider: AIChatbotService.AIProvider = AIChatbotService.AIProvider.LOCAL) {
        val messageLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 8, 16, 8)
            }
        }

        val messageCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
            }
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
            ).apply {
                bottomMargin = 4
            }
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

        val providerText = TextView(this).apply {
            text = "Powered by ${provider.name}"
            textSize = 10f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 4
            }
        }

        messageCard.addView(senderText)
        messageCard.addView(contentText)
        if (!isUser) {
            messageCard.addView(providerText)
        }

        messageLayout.addView(messageCard)
        chatContainer.addView(messageLayout)
        
        scrollToBottom()
    }

    private fun addSuggestions(suggestions: List<String>) {
        val suggestionsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 8, 16, 8)
            }
        }

        suggestions.forEach { suggestion ->
            val suggestionButton = Button(this).apply {
                text = suggestion
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 8
                }
                setOnClickListener {
                    inputField.setText(suggestion)
                    inputField.requestFocus()
                }
            }
            suggestionsLayout.addView(suggestionButton)
        }

        chatContainer.addView(suggestionsLayout)
        scrollToBottom()
    }

    private fun addWelcomeMessage() {
        addMessage("🤖 AI Assistant", "Hello! I'm your AI legal assistant. I can help you with legal information, guidance, and resources. What legal issue would you like to discuss today?", false, currentAIProvider)
        
        val suggestions = listOf(
            "I need legal advice",
            "Help with a criminal case",
            "Family law questions",
            "Property dispute",
            "Employment issues"
        )
        addSuggestions(suggestions)
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Ollama Settings")
            .setMessage("Configure base URL and model for your local Ollama server.")
            .setPositiveButton("Configure") { _, _ ->
                currentAIProvider = AIChatbotService.AIProvider.OLLAMA
                showOllamaSettingsDialog()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAPIKeyDialog() {
        val input = EditText(this)
        input.hint = "Enter your API key"

        AlertDialog.Builder(this)
            .setTitle("Set API Key for ${currentAIProvider.name}")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val apiKey = input.text.toString().trim()
                if (apiKey.isNotEmpty()) {
                    aiService.storeAPIKey(currentAIProvider, apiKey)
                    Toast.makeText(this, "API key saved!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showOllamaSettingsDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 0)
        }

        val baseUrlInput = EditText(this).apply {
            hint = "Base URL (e.g., http://10.0.2.2:11434)"
            setText(aiService.getStoredOllamaBaseUrl() ?: "http://10.0.2.2:11434")
        }
        val modelInput = EditText(this).apply {
            hint = "Model (e.g., tinyllama, qwen2:0.5b, llama3.2:1b)"
            setText(aiService.getStoredOllamaModel() ?: "tinyllama")
        }

        container.addView(baseUrlInput)
        container.addView(modelInput)

        AlertDialog.Builder(this)
            .setTitle("Configure Ollama")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val baseUrl = baseUrlInput.text.toString().trim()
                val model = modelInput.text.toString().trim()
                if (baseUrl.isNotEmpty() && model.isNotEmpty()) {
                    aiService.storeOllamaSettings(baseUrl, model)
                    Toast.makeText(this, "Ollama settings saved!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showClearConversationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Conversation")
            .setMessage("Are you sure you want to clear the conversation history?")
            .setPositiveButton("Clear") { _, _ ->
                chatContainer.removeAllViews()
                aiService.clearConversationHistory()
                addWelcomeMessage()
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
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
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
            Toast.makeText(this, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
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
} 