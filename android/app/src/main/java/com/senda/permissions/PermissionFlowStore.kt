package com.senda.permissions

import android.content.Context

/**
 * Dos datos que la pantalla de permisos guarda entre sesiones (spec 100, sección 5.3):
 * - [samsungAcknowledged]: el usuario tocó "Ya lo hice" en el paso de Samsung. La app no puede comprobarlo.
 * - [onboardingDone]: el usuario ya pasó la pantalla alguna vez con "Continuar". Después, la pantalla solo vuelve
 *   si falta un requisito bloqueante (y entonces avisa de que se perdió un permiso).
 */
interface PermissionFlowStore {
    var samsungAcknowledged: Boolean
    var onboardingDone: Boolean
}

class FakePermissionFlowStore(
    override var samsungAcknowledged: Boolean = false,
    override var onboardingDone: Boolean = false,
) : PermissionFlowStore

class SharedPreferencesPermissionFlowStore(context: Context) : PermissionFlowStore {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override var samsungAcknowledged: Boolean
        get() = prefs.getBoolean(KEY_SAMSUNG, false)
        set(value) = save(KEY_SAMSUNG, value)

    override var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) = save(KEY_ONBOARDING, value)

    private fun save(key: String, value: Boolean) {
        check(prefs.edit().putBoolean(key, value).commit()) { "No se pudo guardar $key" }
    }

    companion object {
        const val PREFS_NAME = "permission_flow"
        private const val KEY_SAMSUNG = "samsung_acknowledged"
        private const val KEY_ONBOARDING = "onboarding_done"
    }
}
