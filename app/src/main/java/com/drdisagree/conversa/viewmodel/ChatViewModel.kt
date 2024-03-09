package com.drdisagree.conversa.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drdisagree.conversa.data.Chat
import com.drdisagree.conversa.data.ChatData
import com.drdisagree.conversa.data.ChatState
import com.drdisagree.conversa.event.ChatUiEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val _chatState = MutableStateFlow(ChatState())
    val chatState = _chatState.asStateFlow()

    fun onEvent(event: ChatUiEvent) {
        when (event) {
            is ChatUiEvent.SendPrompt -> {
                if (event.prompt.isNotEmpty()) {
                    addPrompt(
                        prompt = event.prompt,
                        bitmap = event.bitmap
                    )
                    getResponse(
                        prompt = event.prompt,
                        bitmap = event.bitmap
                    )
                }
            }

            is ChatUiEvent.UpdatePrompt -> {
                _chatState.update {
                    it.copy(
                        prompt = event.newPrompt
                    )
                }
            }

            is ChatUiEvent.Loading -> {
                _chatState.update {
                    it.copy(
                        isLoading = event.isLoading
                    )
                }
            }
        }
    }

    private fun addPrompt(prompt: String, bitmap: Bitmap?) {
        _chatState.update {
            it.copy(
                chatList = it.chatList.toMutableList().apply {
                    add(0, Chat(prompt, bitmap, true))
                },
                prompt = "",
                bitmap = null,
                isLoading = true
            )
        }
    }

    private fun getResponse(prompt: String, bitmap: Bitmap? = null) {
        viewModelScope.launch {
            val chat = ChatData.getResponse(prompt, bitmap)

            _chatState.update {
                it.copy(
                    chatList = it.chatList.toMutableList().apply {
                        add(0, chat)
                    },
                    isLoading = false
                )
            }
        }
    }
}