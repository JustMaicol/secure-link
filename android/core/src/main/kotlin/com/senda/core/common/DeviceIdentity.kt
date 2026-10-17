package com.senda.core.common

// spec 100 · Mayor deviceId posible: 48 bits (spec 001).
const val DEVICE_ID_MAX: Long = 0xFFFF_FFFF_FFFFL

// spec 100
interface DeviceIdentity {
    /** deviceId de 48 bits (spec 001), estable mientras no se reinstale la app. */
    val deviceId: Long
}
