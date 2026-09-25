package com.ahmed9461.nawa.engine

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

class NawaInferenceEngine private constructor(context: Context) {
    sealed interface State {
        data object Ready : State
        data object LoadingModel : State
        data class ModelReady(val path: String) : State
        data object Generating : State
        data class Error(val message: String) : State
        data object Destroyed : State
    }

    companion object {
        @Volatile private var instance: NawaInferenceEngine? = null
        fun get(context: Context): NawaInferenceEngine =
            instance ?: synchronized(this) {
                instance ?: NawaInferenceEngine(context.applicationContext).also { instance = it }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val nativeDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val _state = MutableStateFlow<State>(State.Ready)
    val state: StateFlow<State> = _state.asStateFlow()
    private var loadedPath: String? = null

    init {
        System.loadLibrary("nawa-engine")
        nativeInit(context.applicationInfo.nativeLibraryDir)
    }

    suspend fun loadModel(path: String, config: EngineConfig) = withContext(nativeDispatcher) {
        val file = File(path)
        require(file.isFile && file.canRead()) { "Model file is not readable." }

        if (loadedPath != null) {
            nativeUnload()
            loadedPath = null
        }

        val safe = config.normalized()
        _state.value = State.LoadingModel
        try {
            check(nativeLoad(path) == 0) { "llama.cpp could not load this GGUF model." }
            check(nativePrepare(safe.contextSize, safe.threads, safe.batchSize, safe.temperature) == 0) {
                "Could not allocate model context. Try a smaller context size."
            }
            loadedPath = path
            _state.value = State.ModelReady(path)
        } catch (t: Throwable) {
            runCatching { nativeUnload() }
            loadedPath = null
            _state.value = State.Error(t.message ?: "Model load failed.")
            throw t
        }
    }

    suspend fun resetConversation() = withContext(nativeDispatcher) {
        check(loadedPath != null) { "No model is loaded." }
        check(nativeResetConversation() == 0) { "Could not reset model context." }
        _state.value = State.ModelReady(loadedPath!!)
    }

    suspend fun appendHistory(role: String, content: String) = withContext(nativeDispatcher) {
        if (content.isBlank()) return@withContext
        require(role == "system" || role == "user" || role == "assistant")
        check(nativeAppendHistory(role, content) == 0) {
            "Conversation is too large for the current context."
        }
    }

    fun generate(userPrompt: String, maxTokens: Int): Flow<String> = flow {
        require(userPrompt.isNotBlank()) { "Prompt is empty." }
        val path = loadedPath ?: error("No model is loaded.")
        _state.value = State.Generating
        try {
            check(nativeProcessUserPrompt(userPrompt, maxTokens.coerceIn(1, 4096)) == 0) {
                "Prompt does not fit in the current context."
            }
            val reservedTokenPattern =
                Regex("""<unused\d+>|<bos>|<eos>|\[multimodal]""")
            var reservedStreak = 0

            while (true) {
                currentCoroutineContext().ensureActive()
                val token = nativeGenerateNextToken() ?: break
                if (token.isEmpty()) continue

                if (reservedTokenPattern.containsMatchIn(token)) {
                    reservedStreak++
                    if (reservedStreak >= 4) {
                        error(
                            "The model is producing reserved tokens instead of text. " +
                                "Its embedded chat template may be incompatible."
                        )
                    }
                    continue
                }

                reservedStreak = 0
                emit(token)
            }
            _state.value = State.ModelReady(path)
        } catch (cancelled: CancellationException) {
            _state.value = State.ModelReady(path)
            throw cancelled
        } catch (t: Throwable) {
            _state.value = State.Error(t.message ?: "Generation failed.")
            throw t
        }
    }.flowOn(nativeDispatcher)

    fun unload() {
        runBlocking(nativeDispatcher) {
            if (loadedPath != null) nativeUnload()
            loadedPath = null
            _state.value = State.Ready
        }
    }

    fun destroy() {
        runBlocking(nativeDispatcher) {
            runCatching {
                if (loadedPath != null) nativeUnload()
                nativeShutdown()
            }
            loadedPath = null
            _state.value = State.Destroyed
        }
    }

    private external fun nativeInit(nativeLibDir: String)
    private external fun nativeLoad(modelPath: String): Int
    private external fun nativePrepare(contextSize: Int, threads: Int, batchSize: Int, temperature: Float): Int
    private external fun nativeResetConversation(): Int
    private external fun nativeAppendHistory(role: String, content: String): Int
    private external fun nativeProcessUserPrompt(prompt: String, maxTokens: Int): Int
    private external fun nativeGenerateNextToken(): String?
    private external fun nativeUnload()
    private external fun nativeShutdown()
}
