package com.ahmed9461.nawa.engine

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Process-owned CPU backend. ViewModels never destroy a shared native runtime. */
class NawaInferenceEngine private constructor() {
    sealed interface State {
        data object Ready : State
        data object LoadingModel : State
        data object UnloadingModel : State
        data class ModelReady(val path: String, val description: String) : State
        data object Generating : State
        data class Error(val message: String) : State
    }
    companion object {
        private val instance by lazy { NawaInferenceEngine() }
        fun get(@Suppress("UNUSED_PARAMETER") context: Context): NawaInferenceEngine = instance
    }
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)
    private val operation = Mutex()
    private val _state = MutableStateFlow<State>(State.Ready)
    val state: StateFlow<State> = _state.asStateFlow()
    @Volatile private var initialized = false
    private var model: State.ModelReady? = null

    private fun initialize() {
        if (!initialized) {
            System.loadLibrary("nawa-engine")
            checkResult(nativeInit())
            initialized = true
        }
    }
    private fun checkResult(result: Int) {
        if (result != 0) error(nativeError().toString(Charsets.UTF_8).ifBlank { "Native engine failure." })
    }
    fun cancel() { if (initialized) nativeCancel() }

    suspend fun loadModel(path: String, config: EngineConfig) = withContext(dispatcher) {
        operation.withLock {
            _state.value = State.LoadingModel
            model = null
            try {
                initialize()
                nativeClearCancel()
                currentCoroutineContext().ensureActive()
                checkResult(nativeUnload())
                require(File(path).isFile && File(path).canRead()) { "ملف الموديل غير متاح." }
                val safe = config.normalized()
                checkResult(nativeLoad(path, safe.contextSize, safe.threads, safe.batchSize))
                currentCoroutineContext().ensureActive()
                val description = nativeDescription()?.toString(Charsets.UTF_8) ?: "CPU / GGUF"
                val ready = State.ModelReady(path, description)
                model = ready
                _state.value = ready
            } catch (t: Throwable) {
                if (initialized) nativeUnload()
                model = null
                _state.value = if (t is CancellationException) State.Ready else State.Error(t.message ?: "تعذر تحميل الموديل.")
                currentCoroutineContext().ensureActive()
                throw t
            }
        }
    }

    suspend fun unload() = withContext(dispatcher) {
        operation.withLock {
            _state.value = State.UnloadingModel
            try {
                if (initialized) checkResult(nativeUnload())
                model = null
                _state.value = State.Ready
            } catch (t: Throwable) {
                _state.value = State.Error(t.message ?: "تعذر تفريغ الموديل.")
                throw t
            }
        }
    }

    fun generate(history: List<EngineMessage>, systemPrompt: String, prompt: String,
                 maxTokens: Int, temperature: Float): Flow<GenerationUpdate> = flow {
        operation.withLock {
            val ready = model ?: error("حمّل موديل أولًا.")
            _state.value = State.Generating
            val decoder = Utf8StreamDecoder()
            val pending = StringBuilder()
            var lastEmission = 0L
            var visible = false
            try {
                nativeClearCancel()
                currentCoroutineContext().ensureActive()
                checkResult(nativeBegin(systemPrompt, history.map { it.role }.toTypedArray(),
                    history.map { it.content }.toTypedArray(), prompt,
                    maxTokens.coerceIn(16, 4096), if (temperature.isFinite()) temperature.coerceIn(0f, 2f) else 0.7f))
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val piece = nativeNext()
                    currentCoroutineContext().ensureActive()
                    if (piece == null) {
                        val failure = nativeError().toString(Charsets.UTF_8)
                        check(failure.isBlank()) { failure }
                        break
                    }
                    pending.append(decoder.append(piece))
                    val now = System.nanoTime()
                    if (pending.isNotEmpty() && now - lastEmission >= 50_000_000L) {
                        val text = pending.toString()
                        visible = visible || text.isNotBlank()
                        emit(GenerationUpdate(text))
                        pending.clear()
                        lastEmission = now
                    }
                }
                pending.append(decoder.finish())
                if (pending.isNotEmpty()) {
                    val text = pending.toString()
                    visible = visible || text.isNotBlank()
                    emit(GenerationUpdate(text))
                }
                check(visible) { "لم ينتج الموديل نصًا ظاهرًا. تحقق من توافقه وقالب المحادثة." }
                val s = nativeStats()
                emit(GenerationUpdate(stats = GenerationStats(s[0].toInt(), s[1].toInt(), s[2].toInt(), s[3], s[4], s[5].toInt())))
            } catch (t: Throwable) {
                currentCoroutineContext().ensureActive()
                throw t
            } finally {
                _state.value = ready
            }
        }
    }.flowOn(dispatcher)

    private external fun nativeInit(): Int
    private external fun nativeClearCancel()
    private external fun nativeCancel()
    private external fun nativeLoad(path: String, context: Int, threads: Int, batch: Int): Int
    private external fun nativeBegin(system: String, roles: Array<String>, contents: Array<String>, prompt: String, maxTokens: Int, temperature: Float): Int
    private external fun nativeNext(): ByteArray?
    private external fun nativeError(): ByteArray
    private external fun nativeDescription(): ByteArray?
    private external fun nativeStats(): DoubleArray
    private external fun nativeUnload(): Int
}
