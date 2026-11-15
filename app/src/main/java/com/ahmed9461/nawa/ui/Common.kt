package com.ahmed9461.nawa.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ahmed9461.nawa.data.ChatMessage
import com.ahmed9461.nawa.data.MessageRole

@Composable
internal fun NawaMark(size: Int = 38) {
    Surface(Modifier.size(size.dp), shape = RoundedCornerShape((size * .28f).dp), color = MaterialTheme.colorScheme.primary) {
        Box(contentAlignment = Alignment.Center) { Text("N", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge) }
    }
}
@Composable
internal fun ErrorDialog(error: String, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("تعذر إكمال العملية") },
        text = { SelectionContainer { Text(error) } }, confirmButton = { TextButton(onClick = onDismiss) { Text("حسنًا") } })
}
@Composable
internal fun MessageBubble(message: ChatMessage) {
    val user = message.role == MessageRole.USER
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Surface(modifier = Modifier.fillMaxWidth(if (user) .88f else .97f), shape = RoundedCornerShape(20.dp),
            color = if (user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!user) message.modelName?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                DirectionalMessage(message.content.ifEmpty { if (message.isStreaming) "جاري التفكير…" else "" })
                if (message.isStreaming) LinearProgressIndicator(Modifier.width(54.dp))
                message.error?.let { HorizontalDivider(); Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                message.notice?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (message.content.isNotBlank() && !message.isStreaming) TextButton(onClick = { copyText(context, message.content) }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("نسخ") }
            }
        }
    }
}
@Composable
private fun DirectionalMessage(text: String) {
    val rtl = firstDirection(text) == LayoutDirection.Rtl
    val parts = text.split("```")
    CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            parts.forEachIndexed { index, part ->
                if (index % 2 == 0) {
                    if (part.isNotEmpty()) SelectionContainer {
                        Text(part, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content))
                    }
                } else CodeBlock(part)
            }
        }
    }
}
@Composable
private fun CodeBlock(raw: String) {
    val context = LocalContext.current
    // Only the opening fence's same-line suffix is a language; never drop the first actual code line.
    val header = raw.substringBefore('\n').trim()
    val hasHeader = raw.contains('\n') && header.matches(Regex("[A-Za-z0-9_+.#-]{1,20}"))
    val language = if (hasHeader) header else "CODE"
    val code = when {
        hasHeader -> raw.substringAfter('\n').trimEnd('\n')
        raw.startsWith('\n') -> raw.removePrefix("\n").trimEnd('\n')
        else -> raw.trimEnd('\n')
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(language, style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = { copyText(context, code) }) { Text("نسخ الكود") }
                }
                HorizontalDivider()
                SelectionContainer {
                    Text(code, Modifier.horizontalScroll(rememberScrollState()).padding(12.dp),
                        fontFamily = FontFamily.Monospace, softWrap = false,
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr))
                }
            }
        }
    }
}
internal fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024L * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.0f KB".format(bytes / 1024.0)
}
private fun firstDirection(text: String): LayoutDirection {
    text.forEach { ch -> when (Character.getDirectionality(ch)) {
        Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return LayoutDirection.Ltr
        Character.DIRECTIONALITY_RIGHT_TO_LEFT, Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> return LayoutDirection.Rtl
    } }
    return LayoutDirection.Rtl
}
private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Nawa", text))
}
