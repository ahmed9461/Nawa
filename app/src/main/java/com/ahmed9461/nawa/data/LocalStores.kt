package com.ahmed9461.nawa.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ModelStore(private val context: Context) {
    private val root = File(context.filesDir, "models")
    private val mutex = Mutex()

    suspend fun list(): List<LocalModel> = withContext(Dispatchers.IO) {
        root.mkdirs()
        root.listFiles()?.filter { it.isFile && it.extension.equals("gguf", true) }
            ?.sortedBy { it.name.lowercase() }
            ?.map { LocalModel(it.name, it.absolutePath, it.length()) }.orEmpty()
    }

    suspend fun cleanInterruptedImports() = withContext(Dispatchers.IO) {
        mutex.withLock {
            root.mkdirs()
            root.listFiles()?.filter { it.isFile && it.name.endsWith(".part") }?.forEach { it.delete() }
        }
    }

    suspend fun import(uri: Uri, progress: (ImportProgress) -> Unit): LocalModel = withContext(Dispatchers.IO) {
        mutex.withLock {
            root.mkdirs()
            val resolver = context.contentResolver
            var name: String? = null
            var size: Long? = null
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) name = cursor.getString(nameIndex)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex).takeIf { it >= 0 }
                }
            }
            val safeName = safeModelName(name ?: "model.gguf")
            require(!Regex("-\\d{5}-of-\\d{5}\\.gguf$", RegexOption.IGNORE_CASE).containsMatchIn(safeName)) {
                "هذا موديل مقسم. استخدم ملف GGUF واحدًا في هذه النسخة."
            }
            val reserve = 64L * 1024 * 1024
            size?.let { require(it <= root.usableSpace - reserve) { "المساحة لا تكفي لنسخ الموديل. حرّر مساحة ثم أعد المحاولة." } }
            var target = File(root, safeName)
            if (target.exists()) target = File(root, "${target.nameWithoutExtension}-${System.currentTimeMillis()}.gguf")
            val temp = File(root, ".${target.name}.part")
            try {
                val coroutine = currentCoroutineContext()
                resolver.openInputStream(uri).use { raw ->
                    val input = requireNotNull(raw) { "تعذر فتح الموديل." }.buffered(256 * 1024)
                    val header = ByteArray(24)
                    var read = 0
                    while (read < header.size) {
                        val count = input.read(header, read, header.size - read)
                        require(count > 0) { "ملف GGUF ناقص أو تالف." }
                        read += count
                    }
                    validateGgufHeader(header)
                    FileOutputStream(temp).use { output ->
                        output.write(header)
                        val buffer = ByteArray(256 * 1024)
                        var copied = header.size.toLong()
                        var lastUpdate = 0L
                        while (true) {
                            coroutine.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(root.usableSpace > reserve) { "نفدت المساحة الآمنة أثناء النسخ. لم يُعتمد الملف الجزئي." }
                            output.write(buffer, 0, count)
                            copied += count
                            val now = System.nanoTime()
                            if (now - lastUpdate >= 100_000_000L) {
                                progress(ImportProgress(copied, size))
                                lastUpdate = now
                            }
                        }
                        size?.let { require(copied == it) { "لم يصل الملف كاملًا. أعد اختيار الموديل." } }
                        output.fd.sync()
                        progress(ImportProgress(copied, size))
                    }
                }
                currentCoroutineContext().ensureActive()
                check(temp.renameTo(target)) { "تعذر اعتماد ملف الموديل." }
                LocalModel(target.name, target.absolutePath, target.length())
            } catch (t: Throwable) {
                temp.delete()
                throw t
            }
        }
    }

    suspend fun delete(model: LocalModel): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = File(model.path)
            require(file.canonicalFile.parentFile == root.canonicalFile) { "مسار موديل غير صالح." }
            file.delete()
        }
    }

    companion object {
        fun safeModelName(name: String): String {
            require(name.endsWith(".gguf", true)) { "اختر ملفًا بصيغة GGUF." }
            val cleaned = name.replace(Regex("[\\p{Cntrl}/\\\\:*?\"<>|]"), "_").trim(' ', '.')
            require(cleaned.length in 6..180) { "اسم ملف الموديل طويل جدًا أو غير صالح." }
            return cleaned
        }
        fun validateGgufHeader(header: ByteArray) {
            require(header.size >= 24 && header.copyOfRange(0, 4).contentEquals(byteArrayOf(71, 71, 85, 70))) {
                "الملف ليس GGUF صالحًا؛ تغيير الامتداد وحده لا يكفي."
            }
            val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val version = buffer.getInt(4)
            require(version == 2 || version == 3) { "إصدار GGUF غير مدعوم: $version" }
            require(buffer.getLong(8) > 0 && buffer.getLong(16) >= 0) { "رأس ملف GGUF غير صالح." }
        }
    }
}

/** All AtomicFile reads and writes are guarded by the same lock. Corruption is never an empty history. */
class ChatStore(context: Context) {
    private val file = File(context.filesDir, "chats.json")
    private val atomic = AtomicFile(file)
    private val mutex = Mutex()
    private var writable = false

    suspend fun load(): List<ChatThread> = withContext(Dispatchers.IO) {
        mutex.withLock {
            writable = false
            val legacyTemp = File(file.parentFile, "chats.json.tmp")
            if (!file.exists() && !File(file.path + ".bak").exists() && legacyTemp.exists()) {
                val recovered = FileInputStream(legacyTemp).use { ChatCodec.decode(readBounded(it)) }
                writeLocked(ChatCodec.encode(recovered))
                legacyTemp.delete()
            }
            val threads = if (file.exists() || File(file.path + ".bak").exists()) {
                atomic.openRead().use { ChatCodec.decode(readBounded(it)) }
            } else emptyList()
            writable = true
            threads
        }
    }

    suspend fun save(threads: List<ChatThread>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(writable) { "تعذر قراءة السجل الأصلي. أعد المحاولة أو استعد نسخة احتياطية؛ لن نكتب فوقه." }
            writeLocked(ChatCodec.encode(threads))
        }
    }

    suspend fun restoreAfterReadFailure(threads: List<ChatThread>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val encoded = ChatCodec.encode(threads)
            if (!writable && file.exists()) {
                file.copyTo(File(file.parentFile, "chats-unreadable-${System.currentTimeMillis()}.json"), overwrite = false)
            }
            writeLocked(encoded)
            writable = true
        }
    }

    private fun writeLocked(bytes: ByteArray) {
        val stream = atomic.startWrite()
        try {
            stream.write(bytes)
            atomic.finishWrite(stream)
        } catch (t: Throwable) {
            atomic.failWrite(stream)
            throw t
        }
    }

    companion object {
        fun readBounded(input: InputStream): ByteArray {
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var count: Int
            while (input.read(buffer).also { count = it } != -1) {
                require(output.size() + count <= ChatCodec.MAX_BYTES) { "النسخة الاحتياطية تتجاوز الحجم المدعوم (32 ميجابايت)." }
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        }
    }
}
