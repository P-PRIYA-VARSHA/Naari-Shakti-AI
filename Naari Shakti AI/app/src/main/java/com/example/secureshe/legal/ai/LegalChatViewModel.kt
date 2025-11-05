package com.example.secureshe.legal.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class LegalChatViewModel : ViewModel() {
    data class Message(val text: String, val isUser: Boolean)

    var messages: List<Message> by mutableStateOf(emptyList())
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var error: String? by mutableStateOf(null)
        private set

    fun send(userText: String) {
        if (userText.isBlank() || isLoading) return
        messages = messages + Message(userText, true)
        isLoading = true
        error = null
        viewModelScope.launch {
            try {
                val reply = LegalAiService.ask(userText)
                messages = messages + Message(reply, false)
            } catch (e: Exception) {
                error = e.message ?: "Something went wrong"
            } finally {
                isLoading = false
            }
        }
    }
}
