package com.drdisagree.conversa.data

import android.graphics.Bitmap

data class ChatState(
    val chatList: MutableList<Chat> = mutableListOf(),
    val prompt: String = "",
    val bitmap: Bitmap? = null,
    val isLoading: Boolean = false
)
