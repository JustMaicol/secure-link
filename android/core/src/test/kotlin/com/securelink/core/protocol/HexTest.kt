package com.securelink.core.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

// spec 101 · C2.1
class HexTest {

    @Test
    fun `encode uses lowercase without separators`() {
        assertEquals("a711ff00", Hex.encode(byteArrayOf(0xA7.toByte(), 0x11, 0xFF.toByte(), 0x00)))
    }

    @Test
    fun `encode of an empty array is empty`() {
        assertEquals("", Hex.encode(ByteArray(0)))
    }

    @Test
    fun `decode reads lowercase and uppercase`() {
        val expected = byteArrayOf(0xA7.toByte(), 0x11, 0xFF.toByte(), 0x00)

        assertArrayEquals(expected, Hex.decode("a711ff00"))
        assertArrayEquals(expected, Hex.decode("A711FF00"))
    }

    @Test
    fun `decode of an empty text is an empty array`() {
        assertEquals(0, Hex.decode("").size)
    }

    @Test
    fun `decode throws on odd length`() {
        assertThrows(IllegalArgumentException::class.java) { Hex.decode("abc") }
    }

    @Test
    fun `decode throws on non hex characters`() {
        assertThrows(IllegalArgumentException::class.java) { Hex.decode("zz") }
        assertThrows(IllegalArgumentException::class.java) { Hex.decode("a7 1") }
        assertThrows(IllegalArgumentException::class.java) { Hex.decode("a7-1") }
    }

    @Test
    fun `round trip over every byte value`() {
        val all = ByteArray(256) { it.toByte() }

        assertArrayEquals(all, Hex.decode(Hex.encode(all)))
    }
}
