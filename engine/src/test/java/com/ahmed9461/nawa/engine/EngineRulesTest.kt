package com.ahmed9461.nawa.engine

import org.junit.Assert.*
import org.junit.Test

class EngineRulesTest {
    @Test fun configIsSafeEvenForNegativeOrNonFiniteInputs() {
        val config = EngineConfig(-1, 99, 900, Float.NaN).normalized()
        assertEquals(512, config.contextSize)
        assertEquals(8, config.threads)
        assertEquals(512, config.batchSize)
        assertEquals(0.7f, config.temperature, 0f)
        assertEquals(0.7f, EngineConfig(temperature = Float.POSITIVE_INFINITY).normalized().temperature, 0f)
    }
    @Test fun emojiAndArabicSurviveEveryPossibleChunkBoundary() {
        val text = "Hello مرحبًا 😀🧠 \u0000 end"
        val bytes = text.toByteArray(Charsets.UTF_8)
        for (split in 0..bytes.size) {
            val decoder = Utf8StreamDecoder()
            val actual = decoder.append(bytes.copyOfRange(0, split)) +
                decoder.append(bytes.copyOfRange(split, bytes.size)) + decoder.finish()
            assertEquals("split=$split", text, actual)
        }
    }
    @Test fun oneByteAtATimeAndInvalidSequencesDoNotCrash() {
        val text = "العربية 😀"
        val decoder = Utf8StreamDecoder()
        val actual = buildString {
            text.toByteArray().forEach { append(decoder.append(byteArrayOf(it))) }
            append(decoder.finish())
        }
        assertEquals(text, actual)
        val invalid = Utf8StreamDecoder()
        assertTrue((invalid.append(byteArrayOf(0xF0.toByte())) + invalid.finish()).contains('\uFFFD'))
    }
}
