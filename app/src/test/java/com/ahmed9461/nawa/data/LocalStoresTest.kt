package com.ahmed9461.nawa.data

import android.util.AtomicFile
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalStoresTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private fun thread(id: String = "one") = ChatThread(id, "اختبار 😀", "نموذج", listOf(
        ChatMessage("u", MessageRole.USER, "مرحبًا 🧠"),
        ChatMessage("a", MessageRole.ASSISTANT, "hello", modelName = "نموذج")), 1)
    @Before fun clean() { context.filesDir.listFiles()?.forEach { it.deleteRecursively() } }

    @Test fun unicodeRoundTrip() { assertEquals(listOf(thread()), ChatCodec.decode(ChatCodec.encode(listOf(thread())))) }
    @Test fun legacyFormatStillLoads() {
        val json = org.json.JSONObject(String(ChatCodec.encode(listOf(thread())), Charsets.UTF_8))
        assertEquals(listOf(thread()), ChatCodec.decode(json.getJSONArray("threads").toString().toByteArray()))
    }
    @Test fun interruptedStreamingIsRecovered() {
        val t = thread().copy(messages = listOf(ChatMessage("a", MessageRole.ASSISTANT, "جزئي", true)))
        val message = ChatCodec.decode(ChatCodec.encode(listOf(t))).single().messages.single()
        assertFalse(message.isStreaming)
        assertNotNull(message.notice)
        assertEquals("جزئي", message.content)
    }
    @Test fun invalidRoleDoesNotSilentlyBecomeUser() {
        val bytes = String(ChatCodec.encode(listOf(thread())), Charsets.UTF_8).replace("USER", "BROKEN").toByteArray()
        assertThrows(Exception::class.java) { ChatCodec.decode(bytes) }
    }
    @Test fun mergePreservesConflictsAndSkipsIdentical() {
        assertEquals(1, ChatCodec.merge(listOf(thread()), listOf(thread())).size)
        val merged = ChatCodec.merge(listOf(thread()), listOf(thread().copy(title = "changed")))
        assertEquals(2, merged.size)
        assertEquals(2, merged.map { it.id }.toSet().size)
    }
    @Test fun corruptStoreIsNeverOverwritten() = runBlocking {
        val file = File(context.filesDir, "chats.json")
        file.writeText("invalid{")
        val store = ChatStore(context)
        assertTrue(runCatching { store.load() }.isFailure)
        assertTrue(runCatching { store.save(emptyList()) }.isFailure)
        assertEquals("invalid{", file.readText())
        store.restoreAfterReadFailure(listOf(thread()))
        assertEquals(listOf(thread()), store.load())
        assertTrue(context.filesDir.listFiles()!!.any { it.name.startsWith("chats-unreadable-") })
    }
    @Test fun concurrentWritesRemainReadable() = runBlocking {
        val store = ChatStore(context)
        store.load()
        (0..30).map { n -> async { store.save(listOf(thread(n.toString()))) } }.awaitAll()
        assertEquals(1, store.load().size)
    }
    @Test fun abortedAtomicWriteKeepsPreviousData() = runBlocking {
        val store = ChatStore(context)
        store.load()
        store.save(listOf(thread()))
        val atomic = AtomicFile(File(context.filesDir, "chats.json"))
        val stream = atomic.startWrite()
        stream.write("broken".toByteArray())
        atomic.failWrite(stream)
        assertEquals(listOf(thread()), store.load())
    }
    @Test fun legacyTemporaryFileCanBeRecovered() = runBlocking {
        File(context.filesDir, "chats.json.tmp").writeBytes(ChatCodec.encode(listOf(thread())))
        assertEquals(listOf(thread()), ChatStore(context).load())
    }
    @Test fun unicodeModelNamesAreKeptAndTraversalRemoved() {
        assertEquals("نموذج.gguf", ModelStore.safeModelName("نموذج.gguf"))
        assertFalse(ModelStore.safeModelName("../../model.gguf").contains('/'))
    }
    @Test fun headerRejectsRenamedTextAndTruncatedFiles() {
        assertThrows(IllegalArgumentException::class.java) { ModelStore.validateGgufHeader("fake".toByteArray()) }
        val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            .put(byteArrayOf(71, 71, 85, 70)).putInt(3).putLong(1).putLong(0).array()
        ModelStore.validateGgufHeader(header)
        header[4] = 9
        assertThrows(IllegalArgumentException::class.java) { ModelStore.validateGgufHeader(header) }
    }
}
