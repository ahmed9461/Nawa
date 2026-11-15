package com.ahmed9461.nawa.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ahmed9461.nawa.NawaUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: NawaUiState, onNewChat: () -> Unit, onOpenChat: (String) -> Unit,
    onModels: () -> Unit, onSettings: () -> Unit, onDelete: (String) -> Unit,
    onRename: (String, String) -> Unit, onRetryStorage: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }
    val editable = state.storageReady && !state.initializing && !state.backupBusy
    val filtered = remember(state.threads, query) {
        state.threads.filter { query.isBlank() || it.title.contains(query, true) || it.messages.any { m -> m.content.contains(query, true) } }
            .sortedByDescending { it.updatedAt }
    }
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NawaMark(); Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Nawa", fontWeight = FontWeight.ExtraBold)
                        Text(state.loadedModel?.name ?: "مساعدك المحلي والخاص", maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }, actions = {
                TextButton(onClick = onModels) { Text("الموديلات") }
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "الإعدادات والنسخ الاحتياطي") }
            })
        },
        floatingActionButton = {
            if (editable) ExtendedFloatingActionButton(onClick = onNewChat, icon = { Icon(Icons.Default.Add, null) }, text = { Text("محادثة جديدة") })
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp)) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("بحث في المحادثات") },
                leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(18.dp))
            when {
                state.initializing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                !state.storageReady -> Column(Modifier.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("السجل بحاجة إلى استرداد", style = MaterialTheme.typography.titleLarge)
                    Text("لم نكتب فوق ملف المحادثات. يمكنك إعادة القراءة أو استعادة نسخة احتياطية من الإعدادات.")
                    Button(onClick = onRetryStorage) { Text("إعادة المحاولة") }
                    OutlinedButton(onClick = onSettings) { Text("فتح النسخ الاحتياطي") }
                }
                filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        NawaMark(80)
                        Text(if (query.isBlank()) "مساحتك للتفكير" else "لا توجد نتائج", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(if (query.isNotBlank()) "جرّب كلمة أخرى في العنوان أو نص الرسائل." else
                            "محادثاتك وموديلاتك على جهازك. لا حساب، ولا خادم، ولا اتصال مطلوب للدردشة.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (query.isBlank()) Button(onClick = if (state.loadedModel == null) onModels else onNewChat, enabled = editable) {
                            Text(if (state.loadedModel == null) "إضافة موديل محلي" else "ابدأ محادثة")
                        }
                    }
                }
                else -> LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.id }) { thread ->
                        Card(onClick = { onOpenChat(thread.id) }) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(thread.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { renameId = thread.id; newName = thread.title }, enabled = editable) { Icon(Icons.Default.Edit, "إعادة تسمية") }
                                    IconButton(onClick = { deleteId = thread.id }, enabled = editable) { Icon(Icons.Default.Delete, "حذف المحادثة") }
                                }
                                Text(thread.messages.lastOrNull()?.content?.replace("\n", " ")?.takeIf { it.isNotBlank() } ?: "محادثة جديدة",
                                    maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (state.generatingThreadId == thread.id) Text("يولد الرد…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
    deleteId?.let { id ->
        AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("حذف المحادثة؟") },
            text = { Text("سيُحذف سجلها المحلي، وسيتوقف الرد الجاري إن وجد. صدّر نسخة احتياطية قبل حذف بيانات مهمة.") },
            confirmButton = { TextButton(onClick = { onDelete(id); deleteId = null }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("إلغاء") } })
    }
    renameId?.let { id ->
        AlertDialog(onDismissRequest = { renameId = null }, title = { Text("اسم المحادثة") },
            text = { OutlinedTextField(newName, { newName = it.take(120) }, singleLine = true) },
            confirmButton = { TextButton(onClick = { onRename(id, newName); renameId = null }, enabled = newName.isNotBlank()) { Text("حفظ") } },
            dismissButton = { TextButton(onClick = { renameId = null }) { Text("إلغاء") } })
    }
}
