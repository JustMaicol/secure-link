package com.securelink.common

import com.securelink.core.common.DEVICE_ID_MAX
import com.securelink.core.common.DeviceIdentity

// spec 100 · deviceId estable de 48 bits. La primera vez lo genera con [generate] (inyectado, regla 8)
// y lo guarda como 12 hexadecimales en minúscula; después solo lo lee.
// En producción: PrefsDeviceIdentity(SharedPreferencesIdStore(context), SecureRandom()::nextLong).
class PrefsDeviceIdentity(
    private val store: IdStore,
    private val generate: () -> Long,
) : DeviceIdentity {

    override val deviceId: Long by lazy { load() }

    private fun load(): Long {
        val stored = store.read()
        if (stored != null) {
            // Un valor corrupto no se reemplaza: cambiaría la identidad sin avisar (regla 16).
            check(HEX_12.matches(stored)) { "deviceId guardado inválido: \"$stored\"" }
            return stored.toLong(16)
        }
        val id = generate() and DEVICE_ID_MAX
        store.write(id.toString(16).padStart(HEX_DIGITS, '0'))
        return id
    }

    private companion object {
        const val HEX_DIGITS = 12
        val HEX_12 = Regex("[0-9a-f]{12}")
    }
}
