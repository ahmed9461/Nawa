package com.ahmed9461.nawa.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ahmed9461.nawa.AppSettings
import com.ahmed9461.nawa.NawaUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(state: NawaUiState, onBack: () -> Unit, onSave: (AppSettings) -> Unit,
    onExport: (Uri) -> Unit, onImport: (Uri) -> Unit) {
    var context by rememberSaveable { mutableStateOf(state.settings.contextSize.toString()) }
    var threads by rememberSaveable { mutableStateOf(state.settings.threads.toString()) }
    var batch by rememberSaveable { mutableStateOf(state.settings.batchSize.toString()) }
    var temp by rememberSaveable { mutableStateOf(state.settings.temperature.toString()) }
    var max by rememberSaveable { mutableStateOf(state.settings.maxTokens.toString()) }
    var system by rememberSaveable { mutableStateOf(state.settings.systemPrompt) }
    var importUri by rememberSaveable { mutableStateOf<String?>(null) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(onExport) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { importUri = it?.toString() }
    val contextValid = context.toIntOrNull()?.let { it in 512..8192 } == true
    val threadsValid = threads.toIntOrNull()?.let { it in 1..8 } == true
    val batchValid = batch.toIntOrNull()?.let { it in 64..512 && it <= (context.toIntOrNull() ?: 0) } == true
    val tempValid = temp.toFloatOrNull()?.let { it.isFinite() && it in 0f..2f } == true
    val maxValid = max.toIntOrNull()?.let { it in 16..4096 } == true
    val valid = contextValid && threadsValid && batchValid && tempValid && maxValid && system.length <= 20000
    Scaffold(topBar = {
        TopAppBar(title = { Text("الإعدادات", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("نسختك الاحتياطية", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("التصدير ملف JSON قابل للقراءة يحتوي على المحادثات فقط. احفظه في مكان آمن. الموديلات وإعدادات التشغيل ليست ضمنه.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { exporter.launch("Nawa-chats-${System.currentTimeMillis()}.json") }, enabled = state.storageReady && !state.backupBusy, modifier = Modifier.fillMaxWidth()) { Text("تصدير المحادثات") }
            OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, enabled = !state.backupBusy && !state.initializing, modifier = Modifier.fillMaxWidth()) { Text("استعادة ودمج نسخة احتياطية") }
            if (state.backupBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("المحرك المحلي", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("السياق والخيوط وحجم الدفعة تُطبق عند إعادة تحميل الموديل. الحرارة وحد الرد وتعليمات النظام تسري على الرد القادم.", style = MaterialTheme.typography.bodySmall)
            NumberField("حجم السياق • 512–8192", context, contextValid) { context = it }
            NumberField("خيوط CPU • 1–8", threads, threadsValid) { threads = it }
            NumberField("حجم الدفعة • 64–512", batch, batchValid) { batch = it }
            NumberField("الحرارة • 0–2", temp, tempValid, decimal = true) { temp = it }
            NumberField("أقصى رموز الرد • 16–4096", max, maxValid) { max = it }
            OutlinedTextField(system, { system = it }, label = { Text("تعليمات النظام (اختياري)") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8, isError = system.length > 20000)
            Button(onClick = {
                onSave(AppSettings(context.toInt(), threads.toInt(), batch.toInt(), temp.toFloat(), max.toInt(), system)); onBack()
            }, enabled = valid, modifier = Modifier.fillMaxWidth()) { Text("حفظ الإعدادات") }
            TextButton(onClick = { context = "2048"; threads = "4"; batch = "256"; temp = "0.7"; max = "512"; system = "" }) { Text("استعادة القيم الافتراضية") }
            Text("Nawa 0.2.0 • Android 13+ • ARM64\nالتوليد محلي وعلى CPU فقط. يتوقف الرد عند مغادرة التطبيق إلى الخلفية. لا صور، ولا صوت، ولا مزامنة سحابية في هذه النسخة.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    importUri?.let { uri ->
        AlertDialog(onDismissRequest = { importUri = null }, title = { Text("استعادة المحادثات؟") },
            text = { Text("ستُدمج المحادثات مع السجل الحالي، وتُحفظ النسخ المختلفة دون حذف محادثاتك. سيتوقف أي رد جارٍ.") },
            confirmButton = { TextButton(onClick = { onImport(Uri.parse(uri)); importUri = null }) { Text("استعادة") } },
            dismissButton = { TextButton(onClick = { importUri = null }) { Text("إلغاء") } })
    }
}

@Composable
private fun NumberField(label: String, value: String, valid: Boolean, decimal: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
        isError = !valid, keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number))
}
