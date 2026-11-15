package com.ahmed9461.nawa.engine

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction

/** Native pieces may end in the middle of an Arabic character or emoji. */
internal class Utf8StreamDecoder {
    private val decoder = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)
    private var pending = byteArrayOf()

    fun append(bytes: ByteArray, end: Boolean = false): String {
        val input = ByteBuffer.wrap(pending + bytes)
        val output = CharBuffer.allocate(input.remaining() * 2 + 4)
        decoder.decode(input, output, end).throwIfError()
        pending = ByteArray(input.remaining()).also { input.get(it) }
        if (end) decoder.flush(output).throwIfError()
        output.flip()
        return output.toString()
    }
    fun finish(): String = append(byteArrayOf(), end = true)
    private fun java.nio.charset.CoderResult.throwIfError() { if (isError) throwException() }
}
