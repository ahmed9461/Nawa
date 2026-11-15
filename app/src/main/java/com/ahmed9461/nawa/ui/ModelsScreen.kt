package com.ahmed9461.nawa.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahmed9461.nawa.NawaUiState
import com.ahmed9461.nawa.data.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModelsScreen(state: NawaUiState, onBack: () -> Unit, onImport: (Uri) -> Unit,
    onLoad: (LocalModel) -> Unit, onUnload: () -> Unit, onDelete: (LocalModel) -> Unit,
    onCancelImport: () -> Unit, onCancelLoad: () -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onImport) }
    var deletePath by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(topBar = {
        TopAppBar(title = { Column { Text("الموديلات", fontWeight = FontWeight.Bold); Text(state.engineLabel, style = MaterialTheme.typography.labelSmall) } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                FilledTonalButton(onClick = { picker.launch(arrayOf("*/*")) }, enabled = !state.importBusy && !state.engineBusy, modifier = Modifier.fillMaxWidth()) {
                    Text("إضافة ملف GGUF محلي")
                }
                Text("يُنسخ الموديل إلى مساحة Nawa الخاصة. احتفظ بالملف الأصلي خارج التطبيق؛ حذف التطبيق يزيل النسخة الداخلية.",
                    Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("الذاكرة المتاحة عند آخر فحص: ${state.availableMemoryMb} MB • CPU / ARM64", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelMedium)
            }
            if (state.importBusy) item {
                Card { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("جاري نسخ الموديل…")
                    val total = state.importProgress.total
                    if (total != null && total > 0) LinearProgressIndicator(progress = { (state.importProgress.copied.toFloat() / total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(formatBytes(state.importProgress.copied) + (total?.let { " / ${formatBytes(it)}" } ?: ""))
                    TextButton(onClick = onCancelImport) { Text("إلغاء النسخ") }
                } }
            }
            if (state.models.isEmpty() && !state.importBusy) item {
                Text("لا توجد موديلات بعد. اختر ملف GGUF نصيًا متوافقًا مع ذاكرة هاتفك.", Modifier.padding(20.dp))
            }
            items(state.models, key = { it.path }) { model ->
                val loaded = state.loadedModel?.path == model.path
                val loading = state.loadingModelPath == model.path
                Card { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(model.name, fontWeight = FontWeight.Bold)
                    Text(formatBytes(model.sizeBytes), style = MaterialTheme.typography.bodySmall)
                    if (loaded) Text(state.engineDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    if (model.sizeBytes > state.availableMemoryMb * 1024 * 1024 && state.availableMemoryMb > 0)
                        Text("الموديل كبير مقارنة بالذاكرة المتاحة. قد يفشل تحميله؛ جرّب موديلًا أصغر أو سياقًا أقل.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            TextButton(onClick = onCancelLoad) { Text("إلغاء التحميل") }
                        } else Button(onClick = { if (loaded) onUnload() else onLoad(model) }, enabled = !state.engineBusy) {
                            Text(if (loaded) "تفريغ الذاكرة" else "تحميل الموديل")
                        }
                        OutlinedButton(onClick = { deletePath = model.path }, enabled = !loaded && !state.engineBusy) { Text("حذف") }
                    }
                } }
            }
        }
    }
    deletePath?.let { path ->
        AlertDialog(onDismissRequest = { deletePath = null }, title = { Text("حذف نسخة الموديل؟") },
            text = { Text("سيُحذف الملف من مساحة Nawa فقط، وليس ملفك الأصلي. لن تُحذف المحادثات.") },
            confirmButton = { TextButton(onClick = { state.models.find { it.path == path }?.let(onDelete); deletePath = null }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { deletePath = null }) { Text("إلغاء") } })
    }
}
