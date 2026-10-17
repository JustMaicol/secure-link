package com.senda.common

// spec 100 · Guarda el deviceId como texto. Separa PrefsDeviceIdentity de Android para probarla en la JVM.
interface IdStore {
    /** El texto guardado, o null si todavía no hay ninguno. */
    fun read(): String?

    fun write(value: String)
}
