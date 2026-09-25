package com.ahmed9461.nawa.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class ModelStore(private val context: Context) {
    private val root = File(context.filesDir, "models").apply { mkdirs() }

    fun list(): List<LocalModel> =
        root.listFiles()
            ?.filter { it.isFile && it.extension.equals("gguf", ignoreCase = true) }
            ?.sortedBy { it.name.lowercase() }
            ?.map { LocalModel(it.name, it.absolutePath, it.length()) }
            .orEmpty()

    suspend fun import(uri: Uri): LocalModel = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val displayName = queryDisplayName(resolver, uri)
            ?: throw IllegalArgumentException("تعذر قراءة اسم الملف المحدد.")
        require(displayName.endsWith(".gguf", ignoreCase = true)) {
            "اختر ملف موديل بصيغة .gguf"
        }

        val safe = displayName.replace(Regex("[^A-Za-z0-9._ -]"), "_")
        var target = File(root, safe)
        if (target.exists()) {
            val stem = target.nameWithoutExtension
            target = File(root, "${stem}-${System.currentTimeMillis()}.gguf")
        }

        val temp = File(root, ".${target.name}.part")
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Could not open selected file." }
            temp.outputStream().buffered(1024 * 1024).use { output ->
                input.copyTo(output, 1024 * 1024)
            }
        }

        check(temp.length() > 0) { "Selected model is empty." }
        check(temp.renameTo(target)) { "Could not finish model import." }
        LocalModel(target.name, target.absolutePath, target.length())
    }

    fun delete(model: LocalModel): Boolean = File(model.path).delete()

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }
}

class ChatStore(context: Context) {
    private val file = File(context.filesDir, "chats.json")

    suspend fun load(): List<ChatThread> = withContext(Dispatchers.IO) {
        if (!file.isFile) return@withContext emptyList()
        runCatching {
            val root = JSONArray(file.readText())
            buildList {
                for (i in 0 until root.length()) {
                    val item = root.getJSONObject(i)
                    val messagesJson = item.optJSONArray("messages") ?: JSONArray()
                    val messages = buildList {
                        for (j in 0 until messagesJson.length()) {
                            val message = messagesJson.getJSONObject(j)
                            add(
                                ChatMessage(
                                    id = message.optString("id", UUID.randomUUID().toString()),
                                    role = runCatching { MessageRole.valueOf(message.getString("role")) }
                                        .getOrDefault(MessageRole.USER),
                                    content = message.optString("content"),
                                    error = message.optString("error").ifBlank { null },
                                )
                            )
                        }
                    }
                    add(
                        ChatThread(
                            id = item.getString("id"),
                            title = item.optString("title", "محادثة جديدة"),
                            modelName = item.optString("modelName").ifBlank { null },
                            messages = messages,
                            updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    suspend fun save(threads: List<ChatThread>) = withContext(Dispatchers.IO) {
        val root = JSONArray()
        threads.forEach { thread ->
            val messages = JSONArray()
            thread.messages.forEach { message ->
                messages.put(
                    JSONObject()
                        .put("id", message.id)
                        .put("role", message.role.name)
                        .put("content", message.content)
                        .put("error", message.error ?: "")
                )
            }
            root.put(
                JSONObject()
                    .put("id", thread.id)
                    .put("title", thread.title)
                    .put("modelName", thread.modelName ?: "")
                    .put("updatedAt", thread.updatedAt)
                    .put("messages", messages)
            )
        }
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(root.toString())
        if (file.exists()) file.delete()
        check(temp.renameTo(file))
    }
}
