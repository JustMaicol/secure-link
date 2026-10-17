package com.securelink

import android.app.Application

// spec 100 · Crea el único AppContainer y lo expone como propiedad (no hay singletons globales).
class SecureLinkApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
