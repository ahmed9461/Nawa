package com.ahmed9461.nawa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahmed9461.nawa.NawaUiState
import com.ahmed9461.nawa.data.ChatThread

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: NawaUiState,
    onNewChat: () -> Unit,
    onOpenChat: (String) -> Unit,
    onModels: () -> Unit,
    onSettings: () -> Unit,
    onDelete: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }

    val filtered = state.threads.filter { thread ->
        query.isBlank() ||
            thread.title.contains(query, ignoreCase = true) ||
            thread.messages.any { message ->
                message.content.contains(query, ignoreCase = true)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NawaMark()
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Nawa", fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = state.loadedModel?.name ?: "لا يوجد موديل محمّل",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onModels) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "الموديلات")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewChat) {
                Icon(Icons.Default.Add, contentDescription = "محادثة جديدة")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("بحث في المحادثات") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
            )

            if (filtered.isEmpty()) {
                EmptyHome(
                    hasModel = state.loadedModel != null,
                    onNewChat = onNewChat,
                    onModels = onModels,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = 10.dp,
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.id }) { thread ->
                        ChatTile(
                            thread = thread,
                            onOpen = onOpenChat,
                            onDelete = onDelete,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHome(
    hasModel: Boolean,
    onNewChat: () -> Unit,
    onModels: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp),
        ) {
            NawaMark(88)
            Text(
                text = "Nawa جاهز",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                text = if (hasModel) {
                    "ابدأ محادثة محلية؛ كل شيء يبقى على جهازك."
                } else {
                    "أضف ملف GGUF أولًا ثم حمّله لبدء الدردشة المحلية."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            androidx.compose.material3.Button(
                onClick = if (hasModel) onNewChat else onModels,
                modifier = Modifier.padding(top = 18.dp),
            ) {
                Text(if (hasModel) "ابدأ محادثة" else "اختيار موديل")
            }
        }
    }
}

@Composable
private fun ChatTile(
    thread: ChatThread,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Card(onClick = { onOpen(thread.id) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                NawaMark(42)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = thread.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = thread.messages.lastOrNull()
                        ?.content
                        ?.replace("\n", " ")
                        ?.takeIf { it.isNotBlank() }
                        ?: (thread.modelName ?: "بدون موديل"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            IconButton(onClick = { onDelete(thread.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "حذف")
            }
        }
    }
}
