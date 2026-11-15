package com.ahmed9461.nawa.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ahmed9461.nawa.NawaUiState
import com.ahmed9461.nawa.data.ChatThread

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatScreen(thread: ChatThread?, state: NawaUiState, onBack: () -> Unit,
    onModels: () -> Unit, onSend: (String) -> Unit, onStop: () -> Unit, onRegenerate: () -> Unit) {
    if (thread == null) {
        if (!state.initializing) LaunchedEffect(Unit) { onBack() }
        return
    }
    val list = rememberLazyListState()
    var followTail by rememberSaveable(thread.id) { mutableStateOf(true) }
    val dragging by list.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(dragging) { if (dragging) followTail = false }
    LaunchedEffect(thread.messages.size) { followTail = true }
    LaunchedEffect(thread.messages.size, thread.messages.lastOrNull()?.content?.length, followTail) {
        if (followTail && thread.messages.isNotEmpty()) {
            withFrameNanos { }
            list.scrollToItem(thread.messages.lastIndex)
            withFrameNanos { }
            list.scrollBy(100000f)
        }
    }
    val generating = state.generatingThreadId == thread.id
    val enabled = state.loadedModel != null && !state.engineBusy && state.storageReady && !state.backupBusy && state.generatingThreadId == null
    Scaffold(topBar = {
        TopAppBar(title = {
            Column {
                Text(thread.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.loadedModel?.name ?: "لا يوجد موديل محمّل", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
            actions = { TextButton(onClick = onModels) { Text("الموديلات") } })
    }, bottomBar = {
        Column(Modifier.imePadding()) {
            if (thread.messages.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onRegenerate, enabled = enabled) { Text("إعادة آخر رد") }
                if (!followTail) TextButton(onClick = { followTail = true }) { Text("متابعة أحدث رد ↓") }
            }
            ChatComposer(thread.id, generating, enabled, if (state.loadedModel == null) "حمّل موديل أولًا" else "انتظر اكتمال العملية الحالية", onSend, onStop)
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            if (thread.modelName != null && state.loadedModel != null && thread.modelName != state.loadedModel.name) {
                Text("تم تغيير الموديل؛ الرد القادم سيستخدم ${state.loadedModel.name}.",
                    Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (thread.messages.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    NawaMark(78)
                    Text("كيف أساعدك؟", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(if (state.loadedModel != null) "الرد يُولد على جهازك، ومحادثتك محفوظة محليًا." else "أضف موديل GGUF وحمّله من صفحة الموديلات.")
                }
            } else LazyColumn(state = list, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(thread.messages, key = { it.id }) { MessageBubble(it) }
            }
        }
    }
}

@Composable
private fun ChatComposer(threadId: String, generating: Boolean, enabled: Boolean, unavailable: String,
    onSend: (String) -> Unit, onStop: () -> Unit) {
    var text by rememberSaveable(threadId) { mutableStateOf("") }
    val tooLong = text.length > 20000
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp), shape = RoundedCornerShape(25.dp)) {
        Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(text, { text = it }, modifier = Modifier.weight(1f), enabled = !generating,
                placeholder = { Text(if (enabled) "اكتب رسالة إلى Nawa…" else unavailable) }, minLines = 1, maxLines = 6,
                isError = tooLong, supportingText = if (tooLong) ({ Text("الحد الأقصى 20 ألف حرف.") }) else null,
                shape = RoundedCornerShape(20.dp))
            Spacer(Modifier.width(6.dp))
            FilledIconButton(onClick = {
                if (generating) onStop() else if (enabled && text.isNotBlank() && !tooLong) { onSend(text); text = "" }
            }, enabled = generating || (enabled && text.isNotBlank() && !tooLong)) {
                Icon(if (generating) Icons.Default.Clear else Icons.AutoMirrored.Filled.Send, if (generating) "إيقاف الرد" else "إرسال")
            }
        }
    }
}
