package com.ahmed9461.nawa.engine

data class EngineConfig(
    val contextSize: Int = 2048,
    val threads: Int = 4,
    val batchSize: Int = 256,
    val temperature: Float = 0.7f,
) {
    fun normalized(): EngineConfig = copy(
        contextSize = contextSize.coerceIn(512, 8192),
        threads = threads.coerceIn(1, 8),
        batchSize = batchSize.coerceIn(64, 512).coerceAtMost(contextSize),
        temperature = temperature.coerceIn(0.0f, 2.0f),
    )
}
