package com.ahmed9461.nawa

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ahmed9461.nawa.data.ChatMessage
import com.ahmed9461.nawa.data.ChatStore
import com.ahmed9461.nawa.data.ChatThread
import com.ahmed9461.nawa.data.LocalModel
import com.ahmed9461.nawa.data.MessageRole
import com.ahmed9461.nawa.data.ModelStore
import com.ahmed9461.nawa.engine.EngineConfig
import com.ahmed9461.nawa.engine.NawaInferenceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class AppSettings(
    val contextSize: Int = 2048,
    val threads: Int = 4,
    val batchSize: Int = 256,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 512,
    val systemPrompt: String = "",
)

data class NawaUiState(
    val threads: List<ChatThread> = emptyList(),
    val models: List<LocalModel> = emptyList(),
    val loadedModel: LocalModel? = null,
    val engineLabel: String = "جاهز",
    val importBusy: Boolean = false,
    val loadingModelPath: String? = null,
    val generatingThreadId: String? = null,
    val settings: AppSettings = AppSettings(),
    val error: String? = null,
)

class NawaViewModel(application: Application) : AndroidViewModel(application) {
    private val chatStore = ChatStore(application)
    private val modelStore = ModelStore(application)
    private val engine = NawaInferenceEngine.get(application)
    private val preferences = application.getSharedPreferences("nawa_settings", 0)

    private val _uiState = MutableStateFlow(
        NawaUiState(models = modelStore.list(), settings = loadSettings())
    )
    val uiState: StateFlow<NawaUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null
    private var activeThreadId: String? = null

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(threads = chatStore.load())
        }
        viewModelScope.launch {
            engine.state.collect { state ->
                val label = when (state) {
                    NawaInferenceEngine.State.Ready -> "جاهز"
                    NawaInferenceEngine.State.LoadingModel -> "جاري تحميل الموديل"
                    is NawaInferenceEngine.State.ModelReady -> "الموديل جاهز"
                    NawaInferenceEngine.State.Generating -> "يولد الرد"
                    is NawaInferenceEngine.State.Error -> "خطأ"
                    NawaInferenceEngine.State.Destroyed -> "متوقف"
                }
                _uiState.value = _uiState.value.copy(engineLabel = label)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun createThread(): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val thread = ChatThread(
            id = id,
            title = "محادثة جديدة",
            modelName = _uiState.value.loadedModel?.name,
            messages = emptyList(),
            updatedAt = now,
        )
        _uiState.value = _uiState.value.copy(
            threads = listOf(thread) + _uiState.value.threads
        )
        persistChats()
        return id
    }

    fun deleteThread(id: String) {
        if (activeThreadId == id) activeThreadId = null
        _uiState.value = _uiState.value.copy(
            threads = _uiState.value.threads.filterNot { it.id == id }
        )
        persistChats()
    }

    fun importModel(uri: Uri) {
        if (_uiState.value.importBusy) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(importBusy = true, error = null)
            runCatching { modelStore.import(uri) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(models = modelStore.list())
                }
                .onFailure(::showError)
            _uiState.value = _uiState.value.copy(importBusy = false)
        }
    }

    fun deleteModel(model: LocalModel) {
        if (_uiState.value.loadedModel?.path == model.path) return
        if (modelStore.delete(model)) {
            _uiState.value = _uiState.value.copy(models = modelStore.list())
        }
    }

    fun loadModel(model: LocalModel) {
        if (_uiState.value.loadingModelPath != null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loadingModelPath = model.path,
                error = null,
            )
            val settings = _uiState.value.settings
            runCatching {
                engine.loadModel(
                    model.path,
                    EngineConfig(
                        contextSize = settings.contextSize,
                        threads = settings.threads,
                        batchSize = settings.batchSize,
                        temperature = settings.temperature,
                    )
                )
            }.onSuccess {
                activeThreadId = null
                _uiState.value = _uiState.value.copy(loadedModel = model)
            }.onFailure(::showError)
            _uiState.value = _uiState.value.copy(loadingModelPath = null)
        }
    }

    fun unloadModel() {
        generationJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { engine.unload() }
            activeThreadId = null
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    loadedModel = null,
                    generatingThreadId = null,
                )
            }
        }
    }

    fun sendMessage(threadId: String, rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || generationJob?.isActive == true) return

        val loaded = _uiState.value.loadedModel
        if (loaded == null) {
            _uiState.value = _uiState.value.copy(
                error = "حمّل موديل أولًا من صفحة الموديلات."
            )
            return
        }

        generationJob = viewModelScope.launch {
            val original = _uiState.value.threads.firstOrNull { it.id == threadId }
                ?: return@launch

            try {
                if (activeThreadId != threadId) {
                    engine.resetConversation()
                    val systemPrompt = _uiState.value.settings.systemPrompt.trim()
                    if (systemPrompt.isNotEmpty()) {
                        engine.appendHistory("system", systemPrompt)
                    }
                    original.messages
                        .filter { it.content.isNotBlank() && it.error == null }
                        .forEach { message ->
                            engine.appendHistory(
                                if (message.role == MessageRole.USER) "user" else "assistant",
                                message.content,
                            )
                        }
                    activeThreadId = threadId
                }

                val user = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.USER,
                    content = text,
                )
                val assistant = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.ASSISTANT,
                    content = "",
                    isStreaming = true,
                )

                updateThread(threadId) { thread ->
                    thread.copy(
                        title = if (thread.messages.isEmpty()) text.take(42) else thread.title,
                        modelName = loaded.name,
                        messages = thread.messages + user + assistant,
                        updatedAt = System.currentTimeMillis(),
                    )
                }
                _uiState.value = _uiState.value.copy(
                    generatingThreadId = threadId,
                    error = null,
                )

                engine.generate(text, _uiState.value.settings.maxTokens).collect { token ->
                    updateMessage(threadId, assistant.id) { message ->
                        message.copy(content = message.content + token)
                    }
                }

                updateMessage(threadId, assistant.id) {
                    it.copy(isStreaming = false)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                markLastStreamingStopped(threadId)
                throw cancelled
            } catch (t: Throwable) {
                updateLastAssistantError(threadId, t.message ?: "فشل التوليد.")
                showError(t)
            } finally {
                _uiState.value = _uiState.value.copy(generatingThreadId = null)
                persistChats()
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        generationJob = null
    }

    fun updateSettings(settings: AppSettings) {
        val safe = settings.copy(
            contextSize = settings.contextSize.coerceIn(512, 8192),
            threads = settings.threads.coerceIn(1, 8),
            batchSize = settings.batchSize.coerceIn(64, 512),
            temperature = settings.temperature.coerceIn(0f, 2f),
            maxTokens = settings.maxTokens.coerceIn(16, 4096),
        )
        preferences.edit()
            .putInt("contextSize", safe.contextSize)
            .putInt("threads", safe.threads)
            .putInt("batchSize", safe.batchSize)
            .putFloat("temperature", safe.temperature)
            .putInt("maxTokens", safe.maxTokens)
            .putString("systemPrompt", safe.systemPrompt)
            .apply()
        _uiState.value = _uiState.value.copy(settings = safe)
    }

    private fun loadSettings() = AppSettings(
        contextSize = preferences.getInt("contextSize", 2048),
        threads = preferences.getInt("threads", 4),
        batchSize = preferences.getInt("batchSize", 256),
        temperature = preferences.getFloat("temperature", 0.7f),
        maxTokens = preferences.getInt("maxTokens", 512),
        systemPrompt = preferences.getString("systemPrompt", "") ?: "",
    )

    private fun updateThread(
        id: String,
        transform: (ChatThread) -> ChatThread,
    ) {
        _uiState.value = _uiState.value.copy(
            threads = _uiState.value.threads
                .map { if (it.id == id) transform(it) else it }
                .sortedByDescending { it.updatedAt }
        )
    }

    private fun updateMessage(
        threadId: String,
        messageId: String,
        transform: (ChatMessage) -> ChatMessage,
    ) {
        updateThread(threadId) { thread ->
            thread.copy(
                messages = thread.messages.map { message ->
                    if (message.id == messageId) transform(message) else message
                },
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    private fun markLastStreamingStopped(threadId: String) {
        val thread = _uiState.value.threads.firstOrNull { it.id == threadId } ?: return
        val id = thread.messages.lastOrNull {
            it.role == MessageRole.ASSISTANT && it.isStreaming
        }?.id ?: return
        updateMessage(threadId, id) { it.copy(isStreaming = false) }
    }

    private fun updateLastAssistantError(threadId: String, error: String) {
        val thread = _uiState.value.threads.firstOrNull { it.id == threadId } ?: return
        val message = thread.messages.lastOrNull {
            it.role == MessageRole.ASSISTANT
        } ?: return
        updateMessage(threadId, message.id) {
            it.copy(isStreaming = false, error = error)
        }
    }

    private fun persistChats() {
        val snapshot = _uiState.value.threads
        viewModelScope.launch { chatStore.save(snapshot) }
    }

    private fun showError(t: Throwable) {
        _uiState.value = _uiState.value.copy(
            error = t.message?.takeIf { it.isNotBlank() }
                ?: t::class.java.simpleName
        )
    }

    override fun onCleared() {
        generationJob?.cancel()
        runCatching { engine.destroy() }
        super.onCleared()
    }
}
