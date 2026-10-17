package com.securelink.common

import android.content.Context

// spec 100 · IdStore sobre SharedPreferences: archivo "identity", clave "device_id".
class SharedPreferencesIdStore(context: Context) : IdStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun read(): String? = prefs.getString(KEY_DEVICE_ID, null)

    override fun write(value: String) {
        // commit() y no apply(): la identidad tiene que quedar en disco antes de usarla (regla 16).
        check(prefs.edit().putString(KEY_DEVICE_ID, value).commit()) { "No se pudo guardar el deviceId" }
    }

    companion object {
        const val PREFS_NAME = "identity"
        const val KEY_DEVICE_ID = "device_id"
    }
}
