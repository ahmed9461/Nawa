package com.ahmed9461.nawa

import android.app.ActivityManager
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ahmed9461.nawa.data.*
import com.ahmed9461.nawa.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.Locale
import java.util.UUID

 data class AppSettings(
    val contextSize: Int = 2048, val threads: Int = 4, val batchSize: Int = 256,
    val temperature: Float = 0.7f, val maxTokens: Int = 512, val systemPrompt: String = "",
)
data class NawaUiState(
    val threads: List<ChatThread> = emptyList(), val models: List<LocalModel> = emptyList(),
    val loadedModel: LocalModel? = null, val engineLabel: String = "جاهز",
    val engineDescription: String = "", val importBusy: Boolean = false,
    val importProgress: ImportProgress = ImportProgress(), val loadingModelPath: String? = null,
    val engineBusy: Boolean = false, val generatingThreadId: String? = null,
    val settings: AppSettings = AppSettings(), val error: String? = null, val notice: String? = null,
    val initializing: Boolean = true, val storageReady: Boolean = false, val backupBusy: Boolean = false,
    val availableMemoryMb: Long = 0,
)

class NawaViewModel(application: Application) : AndroidViewModel(application) {
    private val chatStore = ChatStore(application)
    private val modelStore = ModelStore(application)
    private val engine = NawaInferenceEngine.get(application)
    private val preferences = application.getSharedPreferences("nawa_settings", 0)
    private val _uiState = MutableStateFlow(NawaUiState(settings = readSettings()))
    val uiState: StateFlow<NawaUiState> = _uiState.asStateFlow()
    private var generationJob: Job? = null
    private var modelJob: Job? = null
    private var importJob: Job? = null
    private val writerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private sealed interface WriteTask {
        data class Snapshot(val threads: List<ChatThread>) : WriteTask
        data class Barrier(val done: CompletableDeferred<Unit>) : WriteTask
    }
    private val writes = Channel<WriteTask>(Channel.UNLIMITED)
    private val writer = writerScope.launch {
        var lastFailure: Throwable? = null
        try {
            for (task in writes) when (task) {
                is WriteTask.Snapshot -> try {
                    chatStore.save(task.threads)
                    lastFailure = null
                } catch (t: Exception) {
                    lastFailure = t
                    _uiState.update { it.copy(error = "تعذر حفظ المحادثات: ${t.message}. لا تحذف التطبيق؛ صدّر نسخة احتياطية.") }
                }
                is WriteTask.Barrier -> {
                    val failure = lastFailure
                    if (failure == null) task.done.complete(Unit) else task.done.completeExceptionally(failure)
                }
            }
        } finally { writerScope.cancel() }
    }
    companion object { private var finishingWriter: Job? = null }

    init {
        refreshMemory()
        reloadChats()
        viewModelScope.launch {
            try { modelStore.cleanInterruptedImports(); refreshModels() } catch (t: Exception) { showError(t) }
        }
        viewModelScope.launch {
            engine.state.collect { state ->
                _uiState.update { current -> when (state) {
                    NawaInferenceEngine.State.Ready -> current.copy(loadedModel = null, engineLabel = "جاهز", engineDescription = "")
                    NawaInferenceEngine.State.LoadingModel -> current.copy(loadedModel = null, engineLabel = "جاري تحميل الموديل")
                    NawaInferenceEngine.State.UnloadingModel -> current.copy(engineLabel = "جاري تفريغ الذاكرة")
                    is NawaInferenceEngine.State.ModelReady -> current.copy(
                        loadedModel = current.models.find { it.path == state.path }
                            ?: File(state.path).let { LocalModel(it.name, it.path, it.length()) },
                        engineLabel = "الموديل جاهز", engineDescription = state.description)
                    NawaInferenceEngine.State.Generating -> current.copy(engineLabel = "يولد الرد محليًا")
                    is NawaInferenceEngine.State.Error -> current.copy(engineLabel = "تعذر إكمال العملية")
                } }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun clearNotice() { _uiState.update { it.copy(notice = null) } }
    fun reloadChats() {
        if (_uiState.value.storageReady) return
        _uiState.update { it.copy(initializing = true) }
        viewModelScope.launch {
            finishingWriter?.join()
            try {
                val threads = chatStore.load()
                _uiState.update { it.copy(threads = threads, storageReady = true, initializing = false) }
            } catch (t: Exception) {
                _uiState.update { it.copy(initializing = false, storageReady = false,
                    error = "تعذر قراءة سجل المحادثات. تم الحفاظ على الملف الأصلي. أعد المحاولة أو استعد نسخة احتياطية.\n${t.message}") }
            }
        }
    }
    private fun mayEdit() = _uiState.value.storageReady && !_uiState.value.backupBusy && !_uiState.value.initializing

    fun createThread(): String? {
        if (!mayEdit()) return null
        val id = UUID.randomUUID().toString()
        val thread = ChatThread(id, "محادثة جديدة", _uiState.value.loadedModel?.name, emptyList(), System.currentTimeMillis())
        _uiState.update { it.copy(threads = listOf(thread) + it.threads) }
        persistChats()
        return id
    }
    fun renameThread(id: String, name: String) {
        if (!mayEdit() || name.isBlank()) return
        updateThread(id) { it.copy(title = name.trim().take(120)) }
        persistChats()
    }
    fun deleteThread(id: String) {
        if (!mayEdit()) return
        if (_uiState.value.generatingThreadId == id) stopGeneration()
        _uiState.update { it.copy(threads = it.threads.filterNot { t -> t.id == id }) }
        persistChats()
    }

    fun importModel(uri: Uri) {
        if (importJob?.isActive == true) return
        _uiState.update { it.copy(importBusy = true, importProgress = ImportProgress()) }
        importJob = viewModelScope.launch {
            try {
                modelStore.import(uri) { p -> _uiState.update { it.copy(importProgress = p) } }
                refreshModels()
                _uiState.update { it.copy(notice = "تم استيراد الموديل. اضغط تحميل لبدء استخدامه.") }
            } catch (c: CancellationException) { throw c }
            catch (t: Exception) { showError(t) }
            finally { _uiState.update { it.copy(importBusy = false) } }
        }
    }
    fun cancelImport() { importJob?.cancel() }
    private suspend fun refreshModels() {
        val models = modelStore.list()
        _uiState.update { it.copy(models = models) }
    }
    fun deleteModel(model: LocalModel) {
        val state = _uiState.value
        if (state.engineBusy || state.loadedModel?.path == model.path || state.loadingModelPath == model.path) return
        viewModelScope.launch {
            try {
                check(modelStore.delete(model)) { "تعذر حذف الموديل." }
                refreshModels()
            } catch (t: Exception) { showError(t) }
        }
    }
    fun loadModel(model: LocalModel) {
        if (modelJob?.isActive == true || _uiState.value.backupBusy) return
        val oldGeneration = generationJob
        stopGeneration()
        _uiState.update { it.copy(loadedModel = null, engineBusy = true, loadingModelPath = model.path, error = null) }
        modelJob = viewModelScope.launch {
            try {
                oldGeneration?.join()
                val s = _uiState.value.settings
                engine.loadModel(model.path, EngineConfig(s.contextSize, s.threads, s.batchSize, s.temperature))
            } catch (c: CancellationException) { throw c }
            catch (t: Throwable) { showError(t) }
            finally {
                _uiState.update { it.copy(engineBusy = false, loadingModelPath = null) }
                refreshMemory()
            }
        }
    }
    fun unloadModel() {
        if (modelJob?.isActive == true) return
        val oldGeneration = generationJob
        stopGeneration()
        _uiState.update { it.copy(engineBusy = true) }
        modelJob = viewModelScope.launch {
            try { oldGeneration?.join(); engine.unload() }
            catch (c: CancellationException) { throw c }
            catch (t: Throwable) { showError(t) }
            finally { _uiState.update { it.copy(engineBusy = false) }; refreshMemory() }
        }
    }
    fun cancelModelOperation() { engine.cancel(); modelJob?.cancel() }

    fun sendMessage(threadId: String, rawText: String) {
        val text = rawText.trim()
        if (!mayEdit() || text.isEmpty() || generationJob?.isActive == true || _uiState.value.engineBusy) return
        if (text.length > 20000) { showError(IllegalArgumentException("الرسالة طويلة جدًا. اختصرها إلى أقل من 20 ألف حرف.")); return }
        val loaded = _uiState.value.loadedModel ?: run { showError(IllegalStateException("حمّل موديل أولًا.")); return }
        val thread = _uiState.value.threads.find { it.id == threadId } ?: return
        val assistantId = UUID.randomUUID().toString()
        val messages = thread.messages + ChatMessage(UUID.randomUUID().toString(), MessageRole.USER, text) +
            ChatMessage(assistantId, MessageRole.ASSISTANT, "", isStreaming = true, modelName = loaded.name)
        updateThread(threadId) { it.copy(title = if (it.messages.isEmpty()) text.take(42) else it.title,
            messages = messages, modelName = loaded.name) }
        _uiState.update { it.copy(generatingThreadId = threadId, error = null) }
        persistChats() // Save the question before entering native inference.
        val settings = _uiState.value.settings
        generationJob = viewModelScope.launch(start = CoroutineStart.LAZY) {
            var lastCheckpoint = System.nanoTime()
            try {
                engine.generate(historyPairs(thread.messages), settings.systemPrompt.trim(), text,
                    settings.maxTokens, settings.temperature).collect { update ->
                    if (update.text.isNotEmpty()) updateMessage(threadId, assistantId) { it.copy(content = it.content + update.text) }
                    update.stats?.let { stats -> updateMessage(threadId, assistantId) { it.copy(notice = statsNotice(stats)) } }
                    val now = System.nanoTime()
                    if (now - lastCheckpoint >= 1_000_000_000L) { persistChats(); lastCheckpoint = now }
                }
                updateMessage(threadId, assistantId) { it.copy(isStreaming = false) }
            } catch (c: CancellationException) {
                updateMessage(threadId, assistantId) { it.copy(isStreaming = false, notice = "تم إيقاف الرد. يمكنك إعادة توليده.") }
                throw c
            } catch (t: Throwable) {
                updateMessage(threadId, assistantId) { it.copy(isStreaming = false, error = t.message ?: "فشل التوليد.") }
                showError(t)
            } finally {
                _uiState.update { if (it.generatingThreadId == threadId) it.copy(generatingThreadId = null) else it }
                persistChats()
            }
        }.also { it.start() }
    }
    fun regenerate(threadId: String) {
        if (!mayEdit() || generationJob?.isActive == true || _uiState.value.loadedModel == null || _uiState.value.engineBusy) return
        val thread = _uiState.value.threads.find { it.id == threadId } ?: return
        val index = thread.messages.indexOfLast { it.role == MessageRole.USER }
        if (index < 0) return
        val prompt = thread.messages[index].content
        updateThread(threadId) { it.copy(messages = it.messages.take(index)) }
        sendMessage(threadId, prompt)
    }
    fun stopGeneration() { engine.cancel(); generationJob?.cancel() }
    fun onBackground() { stopGeneration(); persistChats() }

    private fun historyPairs(messages: List<ChatMessage>): List<EngineMessage> = buildList {
        var user: ChatMessage? = null
        messages.forEach { m ->
            if (m.role == MessageRole.USER) user = m
            else {
                val prompt = user
                if (prompt != null && m.error == null && m.content.isNotBlank()) {
                    add(EngineMessage("user", prompt.content)); add(EngineMessage("assistant", m.content))
                }
                user = null
            }
        }
    }
    private fun statsNotice(s: GenerationStats): String = buildString {
        append(String.format(Locale.US, "%d رمز • %.1f رمز/ث • أول رمز %.2f ث", s.outputTokens, s.tokensPerSecond, s.firstTokenMs / 1000))
        if (s.droppedMessages > 0) append("\nاستُخدم أحدث جزء من المحادثة؛ السجل الكامل محفوظ (${s.droppedMessages} رسائل خارج السياق).")
        if (s.finishReason == 2 || s.finishReason == 3) append("\nوصل الرد إلى حد التوليد/السياق. يمكنك طلب المتابعة.")
    }

    fun updateSettings(settings: AppSettings) {
        val c = EngineConfig(settings.contextSize, settings.threads, settings.batchSize, settings.temperature).normalized()
        val safe = settings.copy(contextSize = c.contextSize, threads = c.threads, batchSize = c.batchSize,
            temperature = c.temperature, maxTokens = settings.maxTokens.coerceIn(16, 4096), systemPrompt = settings.systemPrompt.take(20000))
        preferences.edit().putInt("contextSize", safe.contextSize).putInt("threads", safe.threads)
            .putInt("batchSize", safe.batchSize).putFloat("temperature", safe.temperature)
            .putInt("maxTokens", safe.maxTokens).putString("systemPrompt", safe.systemPrompt).apply()
        _uiState.update { it.copy(settings = safe, notice = "تم الحفظ. الحرارة وتعليمات النظام تُطبّقان على الرد القادم؛ إعدادات الذاكرة عند إعادة تحميل الموديل.") }
    }
    private fun readSettings(): AppSettings {
        val c = EngineConfig(preferences.getInt("contextSize", 2048), preferences.getInt("threads", 4),
            preferences.getInt("batchSize", 256), preferences.getFloat("temperature", 0.7f)).normalized()
        return AppSettings(c.contextSize, c.threads, c.batchSize, c.temperature,
            preferences.getInt("maxTokens", 512).coerceIn(16, 4096), preferences.getString("systemPrompt", "") ?: "")
    }

    fun exportBackup(uri: Uri) {
        if (_uiState.value.backupBusy || !_uiState.value.storageReady) return
        val snapshot = _uiState.value.threads
        _uiState.update { it.copy(backupBusy = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val bytes = ChatCodec.encode(snapshot)
                    getApplication<Application>().contentResolver.openOutputStream(uri, "w").use { stream ->
                        requireNotNull(stream) { "تعذر فتح مكان الحفظ." }.write(bytes)
                    }
                }
                _uiState.update { it.copy(notice = "حُفظت نسخة المحادثات. لا تتضمن ملفات الموديلات أو كلمات مرور.") }
            } catch (c: CancellationException) { throw c }
            catch (t: Exception) { showError(t) }
            finally { _uiState.update { it.copy(backupBusy = false) } }
        }
    }
    fun importBackup(uri: Uri) {
        if (_uiState.value.backupBusy || _uiState.value.initializing) return
        _uiState.update { it.copy(backupBusy = true) }
        val old = generationJob
        stopGeneration()
        viewModelScope.launch {
            try {
                old?.join()
                val barrier = CompletableDeferred<Unit>()
                writes.send(WriteTask.Barrier(barrier)); barrier.await()
                val incoming = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri).use { stream ->
                        ChatCodec.decode(ChatStore.readBounded(requireNotNull(stream) { "تعذر فتح النسخة الاحتياطية." }))
                    }
                }
                val merged = ChatCodec.merge(_uiState.value.threads, incoming)
                chatStore.restoreAfterReadFailure(merged)
                _uiState.update { it.copy(threads = merged, storageReady = true,
                    notice = "تمت استعادة المحادثات ودمجها دون حذف السجل الحالي.") }
            } catch (c: CancellationException) { throw c }
            catch (t: Exception) { showError(t) }
            finally { _uiState.update { it.copy(backupBusy = false) } }
        }
    }

    private fun refreshMemory() {
        val info = ActivityManager.MemoryInfo()
        getApplication<Application>().getSystemService(ActivityManager::class.java).getMemoryInfo(info)
        _uiState.update { it.copy(availableMemoryMb = info.availMem / (1024 * 1024)) }
    }
    private fun updateThread(id: String, transform: (ChatThread) -> ChatThread) {
        _uiState.update { state -> state.copy(threads = state.threads.map { thread ->
            if (thread.id == id) transform(thread).copy(updatedAt = System.currentTimeMillis()) else thread
        }) }
    }
    private fun updateMessage(threadId: String, id: String, transform: (ChatMessage) -> ChatMessage) {
        updateThread(threadId) { it.copy(messages = it.messages.map { m -> if (m.id == id) transform(m) else m }) }
    }
    private fun persistChats() {
        if (_uiState.value.storageReady) writes.trySend(WriteTask.Snapshot(_uiState.value.threads))
    }
    private fun showError(t: Throwable) {
        _uiState.update { it.copy(error = t.message?.takeIf { m -> m.isNotBlank() } ?: "تعذر إكمال العملية (${t::class.java.simpleName}).") }
    }
    override fun onCleared() {
        stopGeneration()
        importJob?.cancel()
        modelJob?.cancel()
        persistChats()
        writes.close()
        finishingWriter = writer
        super.onCleared()
    }
}
