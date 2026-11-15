package com.ahmed9461.nawa.engine

data class EngineConfig(
    val contextSize: Int = 2048,
    val threads: Int = 4,
    val batchSize: Int = 256,
    val temperature: Float = 0.7f,
) {
    fun normalized(): EngineConfig {
        val context = contextSize.coerceIn(512, 8192)
        return copy(contextSize = context, threads = threads.coerceIn(1, 8),
            batchSize = batchSize.coerceIn(64, 512).coerceAtMost(context),
            temperature = if (temperature.isFinite()) temperature.coerceIn(0f, 2f) else 0.7f)
    }
}
data class EngineMessage(val role: String, val content: String)
data class GenerationStats(
    val promptTokens: Int, val outputTokens: Int, val droppedMessages: Int,
    val firstTokenMs: Double, val tokensPerSecond: Double, val finishReason: Int,
)
data class GenerationUpdate(val text: String = "", val stats: GenerationStats? = null)
