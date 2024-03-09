package com.drdisagree.conversa.event

import android.graphics.Bitmap

sealed class ChatUiEvent {
    data class Loading(val isLoading: Boolean) : ChatUiEvent()
    data class UpdatePrompt(val newPrompt: String) : ChatUiEvent()
    data class SendPrompt(
        val prompt: String,
        val bitmap: Bitmap?
    ) : ChatUiEvent()
}