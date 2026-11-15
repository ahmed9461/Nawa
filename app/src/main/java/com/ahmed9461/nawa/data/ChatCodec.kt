package com.ahmed9461.nawa.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Versioned, bounded backup format. Legacy v0 array files remain readable. */
object ChatCodec {
    const val MAX_BYTES = 32 * 1024 * 1024
    fun encode(threads: List<ChatThread>): ByteArray {
        val items = JSONArray()
        threads.forEach { thread ->
            val messages = JSONArray()
            thread.messages.forEach { m ->
                messages.put(JSONObject().put("id", m.id).put("role", m.role.name)
                    .put("content", m.content).put("isStreaming", m.isStreaming)
                    .put("error", m.error ?: "").put("modelName", m.modelName ?: "")
                    .put("notice", m.notice ?: ""))
            }
            items.put(JSONObject().put("id", thread.id).put("title", thread.title)
                .put("modelName", thread.modelName ?: "").put("updatedAt", thread.updatedAt)
                .put("messages", messages))
        }
        return JSONObject().put("format", "nawa-chats").put("version", 1)
            .put("threads", items).toString().toByteArray(Charsets.UTF_8).also {
                require(it.size <= MAX_BYTES) { "سجل المحادثات كبير جدًا. صدّر نسخة ثم احذف بعض المحادثات القديمة." }
            }
    }

    fun decode(bytes: ByteArray): List<ChatThread> {
        require(bytes.size <= MAX_BYTES) { "ملف النسخة الاحتياطية يتجاوز 32 ميجابايت." }
        val text = bytes.toString(Charsets.UTF_8).trim()
        require(text.isNotEmpty()) { "ملف المحادثات فارغ أو تالف." }
        val items = if (text.startsWith("[")) JSONArray(text) else {
            val root = JSONObject(text)
            require(root.optString("format") == "nawa-chats" && root.getInt("version") == 1) {
                "صيغة النسخة الاحتياطية غير مدعومة. لم تتغير محادثاتك."
            }
            root.getJSONArray("threads")
        }
        require(items.length() <= 10000) { "عدد المحادثات أكبر من الحد المدعوم." }
        val ids = HashSet<String>()
        var messageCount = 0
        return List(items.length()) { i ->
            val item = items.getJSONObject(i)
            val id = item.getString("id")
            require(id.isNotBlank() && ids.add(id)) { "معرّفات محادثات مكررة أو غير صالحة." }
            val array = item.getJSONArray("messages")
            messageCount += array.length()
            require(messageCount <= 100000) { "عدد الرسائل أكبر من الحد المدعوم." }
            val messageIds = HashSet<String>()
            val messages = List(array.length()) { j ->
                val m = array.getJSONObject(j)
                val messageId = m.getString("id")
                require(messageId.isNotBlank() && messageIds.add(messageId)) { "معرّف رسالة غير صالح." }
                val interrupted = m.optBoolean("isStreaming", false)
                ChatMessage(messageId, MessageRole.valueOf(m.getString("role")), m.getString("content"),
                    error = m.optString("error").ifBlank { null },
                    modelName = m.optString("modelName").ifBlank { null },
                    notice = if (interrupted) "توقف هذا الرد عند إغلاق التطبيق؛ يمكنك إعادة توليده."
                        else m.optString("notice").ifBlank { null })
            }
            ChatThread(id, item.optString("title", "محادثة جديدة"),
                item.optString("modelName").ifBlank { null }, messages,
                item.optLong("updatedAt", 0L))
        }.sortedByDescending { it.updatedAt }
    }

    /** Preserve local edits; skip identical imports, retain conflicting copies under fresh ids. */
    fun merge(existing: List<ChatThread>, incoming: List<ChatThread>): List<ChatThread> {
        val result = existing.toMutableList()
        val byId = existing.associateBy { it.id }
        incoming.forEach { thread ->
            val current = byId[thread.id]
            when {
                current == null -> result.add(thread)
                current == thread -> Unit
                else -> result.add(thread.copy(id = UUID.randomUUID().toString(), title = "${thread.title} (مستعادة)"))
            }
        }
        return result.sortedByDescending { it.updatedAt }
    }
}
