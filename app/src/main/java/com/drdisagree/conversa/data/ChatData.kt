package com.drdisagree.conversa.data

import android.graphics.Bitmap
import com.drdisagree.conversa.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ChatData {

    suspend fun getResponse(prompt: String, bitmap: Bitmap? = null): Chat {
        val modelName = if (bitmap != null) "gemini-pro-vision" else "gemini-pro"

        val generativeModel = GenerativeModel(
            modelName = modelName,
            apiKey = BuildConfig.apiKey
        )

        try {
            val response = withContext(Dispatchers.IO) {
                if (bitmap != null) {
                    generativeModel.generateContent(
                        content {
                            image(bitmap)
                            text(prompt)
                        }
                    )
                } else {
                    generativeModel.generateContent(prompt)
                }
            }

            return Chat(
                prompt = response.text ?: "Error generating response",
                bitmap = null,
                isFromUser = false
            )
        } catch (e: Exception) {
            return Chat(
                prompt = e.message ?: "Error generating response",
                bitmap = null,
                isFromUser = false
            )
        }
    }
}