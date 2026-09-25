package com.ahmed9461.nawa.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ahmed9461.nawa.data.ChatMessage
import com.ahmed9461.nawa.data.MessageRole

@Composable
internal fun NawaMark(size: Int = 38) {
    Surface(
        modifier = Modifier.size(size.dp),
        shape = RoundedCornerShape((size * 0.28f).dp),
        color = MaterialTheme.colorScheme.primary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "N",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
internal fun ErrorDialog(error: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حدث خطأ") },
        text = { Text(error) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("حسنًا")
            }
        },
    )
}

@Composable
internal fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 20.dp,
            ),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.88f else 0.96f),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DirectionalMessage(
                    text = message.content.ifEmpty {
                        if (message.isStreaming) "…" else ""
                    }
                )

                if (message.error != null) {
                    HorizontalDivider()
                    Text(
                        text = message.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectionalMessage(text: String) {
    val direction = if (looksRtl(text)) LayoutDirection.Rtl else LayoutDirection.Ltr
    val fence = 96.toChar().toString().repeat(3)
    val parts = text.split(fence)

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            parts.forEachIndexed { index, part ->
                if (index % 2 == 0) {
                    if (part.isNotEmpty()) {
                        SelectionContainer {
                            Text(
                                text = part,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                } else {
                    CodeBlock(part)
                }
            }
        }
    }
}

@Composable
private fun CodeBlock(raw: String) {
    val context = LocalContext.current
    val lines = raw.trim('\n').lines()
    val language = lines.firstOrNull()?.takeIf {
        it.matches(Regex("[A-Za-z0-9_+.#-]{1,20}"))
    }
    val code = if (language != null) {
        lines.drop(1).joinToString("\n")
    } else {
        lines.joinToString("\n")
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 4.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = language?.uppercase() ?: "CODE",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = { copyText(context, code) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ")
                    }
                }

                HorizontalDivider()

                SelectionContainer {
                    Text(
                        text = code,
                        modifier = Modifier.padding(12.dp),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

internal fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 ->
        "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))

    bytes >= 1024L * 1024 ->
        "%.1f MB".format(bytes / (1024.0 * 1024))

    else ->
        "%.0f KB".format(bytes / 1024.0)
}

private fun looksRtl(text: String): Boolean {
    text.forEach { ch ->
        if (ch.code in 0x0600..0x08FF || ch.code in 0xFB50..0xFEFF) {
            return true
        }
        if (ch.isLetter() && ch.code < 128) {
            return false
        }
    }
    return true
}

private fun copyText(context: Context, text: String) {
    val clipboard =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Nawa", text))
}
