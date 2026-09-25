package com.ahmed9461.nawa.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahmed9461.nawa.NawaUiState
import com.ahmed9461.nawa.data.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModelsScreen(
    state: NawaUiState,
    onBack: () -> Unit,
    onImport: (Uri) -> Unit,
    onLoad: (LocalModel) -> Unit,
    onUnload: () -> Unit,
    onDelete: (LocalModel) -> Unit,
) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                onImport(uri)
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("الموديلات", fontWeight = FontWeight.Bold)
                        Text(
                            text = state.engineLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Button(
                    onClick = { launcher.launch(arrayOf("*/*")) },
                    enabled = !state.importBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.importBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                    }

                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.importBusy) {
                            "جاري نسخ الموديل…"
                        } else {
                            "إضافة ملف GGUF"
                        }
                    )
                }

                Text(
                    text = "يُنسخ الملف مرة واحدة إلى مساحة Nawa الخاصة. " +
                        "الدردشة نفسها لا تحتاج إلى الإنترنت.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (state.models.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "لا توجد موديلات مضافة بعد.",
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                }
            }

            items(state.models, key = { it.path }) { model ->
                ModelCard(
                    model = model,
                    loaded = state.loadedModel?.path == model.path,
                    loading = state.loadingModelPath == model.path,
                    onLoad = { onLoad(model) },
                    onUnload = onUnload,
                    onDelete = { onDelete(model) },
                )
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: LocalModel,
    loaded: Boolean,
    loading: Boolean,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDelete: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = model.name,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = formatBytes(model.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (loaded) {
                    OutlinedButton(onClick = onUnload) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Unload")
                    }
                } else {
                    Button(
                        onClick = onLoad,
                        enabled = !loading,
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        }

                        Spacer(Modifier.width(6.dp))
                        Text(if (loading) "Loading…" else "Load")
                    }
                }

                OutlinedButton(
                    onClick = onDelete,
                    enabled = !loaded && !loading,
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("حذف")
                }
            }
        }
    }
}
