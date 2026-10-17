package com.securelink.core.protocol

// spec 101, sección 8 · Hexadecimal para pruebas de vectores y para depurar (pantalla Estado, 109).
object Hex {
    private const val DIGITS = "0123456789abcdef"

    /** Minúsculas, sin separadores. */
    fun encode(bytes: ByteArray): String {
        val out = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            out.append(DIGITS[v ushr 4]).append(DIGITS[v and 0x0F])
        }
        return out.toString()
    }

    /**
     * Acepta mayúsculas y minúsculas.
     * @throws IllegalArgumentException si el texto no es hexadecimal de largo par. Un texto mal formado es
     * un error de programación, no un dato de la malla (regla 16).
     */
    fun decode(text: String): ByteArray {
        require(text.length % 2 == 0) { "Hex de largo impar: ${text.length}" }
        return ByteArray(text.length / 2) { i ->
            val high = digit(text[2 * i], 2 * i)
            val low = digit(text[2 * i + 1], 2 * i + 1)
            ((high shl 4) or low).toByte()
        }
    }

    private fun digit(c: Char, index: Int): Int {
        val value = Character.digit(c, 16)
        require(value >= 0) { "Carácter no hexadecimal '$c' en la posición $index" }
        return value
    }
}
