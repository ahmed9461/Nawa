package com.ahmed9461.nawa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahmed9461.nawa.AppSettings
import com.ahmed9461.nawa.NawaUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: NawaUiState,
    onBack: () -> Unit,
    onSave: (AppSettings) -> Unit,
) {
    var contextSize by remember(state.settings) {
        mutableStateOf(state.settings.contextSize.toString())
    }
    var threads by remember(state.settings) {
        mutableStateOf(state.settings.threads.toString())
    }
    var batch by remember(state.settings) {
        mutableStateOf(state.settings.batchSize.toString())
    }
    var temperature by remember(state.settings) {
        mutableStateOf(state.settings.temperature.toString())
    }
    var maxTokens by remember(state.settings) {
        mutableStateOf(state.settings.maxTokens.toString())
    }
    var systemPrompt by remember(state.settings) {
        mutableStateOf(state.settings.systemPrompt)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("الإعدادات", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "إعدادات التحميل",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "تُطبّق قيم السياق والخيوط والـBatch عند تحميل الموديل التالي.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )

            NumberField("Context Size", contextSize) {
                contextSize = it
            }
            NumberField("CPU Threads", threads) {
                threads = it
            }
            NumberField("Batch Size", batch) {
                batch = it
            }
            NumberField("Temperature", temperature) {
                temperature = it
            }
            NumberField("Max output tokens", maxTokens) {
                maxTokens = it
            }

            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("System Prompt اختياري") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
                shape = RoundedCornerShape(18.dp),
            )

            Button(
                onClick = {
                    onSave(
                        AppSettings(
                            contextSize = contextSize.toIntOrNull() ?: 2048,
                            threads = threads.toIntOrNull() ?: 4,
                            batchSize = batch.toIntOrNull() ?: 256,
                            temperature = temperature.toFloatOrNull() ?: 0.7f,
                            maxTokens = maxTokens.toIntOrNull() ?: 512,
                            systemPrompt = systemPrompt,
                        )
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("حفظ")
            }

            Text(
                text = "الإعداد الافتراضي الموصى به الآن: " +
                    "Context 2048، Threads 4، Batch 256. " +
                    "النسخة الأولى تستخدم CPU فقط.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
    )
}
