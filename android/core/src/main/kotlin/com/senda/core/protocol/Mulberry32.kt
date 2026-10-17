package com.senda.core.protocol

// spec 101, sección 7 · Generador de la spec 001, sección 7. Instancia con estado explícito, no un global (regla 10).
// Es el único azar permitido en el protocolo (regla 7): nada de Random() ni Math.random().
class Mulberry32(seed: Int) {
    private var a: Int = seed

    /** Siguiente valor uint32 en un Long (0..4294967295). */
    fun next(): Long {
        a += 0x6D2B79F5 // desborda igual que mod 2^32
        var t = (a xor (a ushr 15)) * (a or 1) // Int * Int = imul32
        t = (t + (t xor (t ushr 7)) * (t or 61)) xor t
        return ((t xor (t ushr 14)).toLong()) and 0xFFFFFFFFL
    }
}

/** Semilla de un teléfono: los 32 bits bajos del deviceId (spec 001, sección 6.4). */
fun seedOf(deviceId: Long): Int = deviceId.toInt()
