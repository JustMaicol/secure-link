package com.securelink.core.common

// spec 100 · Identidad fija para pruebas y fakes. Exige 0..DEVICE_ID_MAX.
class FixedDeviceIdentity(override val deviceId: Long) : DeviceIdentity {
    init {
        require(deviceId in 0L..DEVICE_ID_MAX) { "deviceId fuera de 48 bits: $deviceId" }
    }
}
