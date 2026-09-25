package com.ahmed9461.nawa.data

data class LocalModel(val name: String, val path: String, val sizeBytes: Long)

enum class MessageRole { USER, ASSISTANT }

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val isStreaming: Boolean = false,
    val error: String? = null,
)

data class ChatThread(
    val id: String,
    val title: String,
    val modelName: String?,
    val messages: List<ChatMessage>,
    val updatedAt: Long,
)
