package com.ahmed9461.nawa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
internal fun ChatScreen(
    thread: ChatThread?,
    state: NawaUiState,
    onBack: () -> Unit,
    onModels: () -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
) {
    if (thread == null) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    val listState = rememberLazyListState()

    LaunchedEffect(thread.messages.size) {
        if (thread.messages.isNotEmpty()) {
            listState.animateScrollToItem(thread.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = thread.title,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            text = state.loadedModel?.name
                                ?: "لا يوجد موديل محمّل",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onModels) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = "الموديلات",
                        )
                    }
                },
            )
        },
        bottomBar = {
            ChatComposer(
                generating = state.generatingThreadId == thread.id,
                enabled = state.loadedModel != null,
                onSend = onSend,
                onStop = onStop,
            )
        },
    ) { padding ->
        if (thread.messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(28.dp),
                ) {
                    NawaMark(78)
                    Text(
                        text = "كيف أساعدك؟",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                    Text(
                        text = if (state.loadedModel != null) {
                            "المحادثة تعمل محليًا على جهازك."
                        } else {
                            "حمّل موديل من زر المجلد بالأعلى."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(thread.messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
            }
        }
    }
}

@Composable
private fun ChatComposer(
    generating: Boolean,
    enabled: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
) {
    var text by remember { mutableStateOf("") }

    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(10.dp),
        shape = RoundedCornerShape(25.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 10.dp,
                    end = 6.dp,
                    top = 5.dp,
                    bottom = 5.dp,
                ),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                enabled = enabled && !generating,
                placeholder = {
                    Text(
                        if (enabled) {
                            "اكتب رسالة إلى Nawa…"
                        } else {
                            "حمّل موديل أولًا"
                        }
                    )
                },
                minLines = 1,
                maxLines = 6,
                shape = RoundedCornerShape(20.dp),
            )

            Spacer(Modifier.width(6.dp))

            FilledIconButton(
                onClick = {
                    if (generating) {
                        onStop()
                    } else if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                    }
                },
                enabled = generating || (enabled && text.isNotBlank()),
            ) {
                Icon(
                    imageVector = if (generating) {
                        Icons.Default.Stop
                    } else {
                        Icons.Default.Send
                    },
                    contentDescription = if (generating) {
                        "إيقاف"
                    } else {
                        "إرسال"
                    },
                )
            }
        }
    }
}
