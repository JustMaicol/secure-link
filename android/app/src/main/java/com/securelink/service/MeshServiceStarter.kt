package com.securelink.service

import android.content.Context

// spec 107 · Contrato (S0): MainActivity lo llama al pasar la pantalla de permisos.
// Por ahora no hace nada. En S3 arranca MeshService si meshEnabled (102) es verdadero.
object MeshServiceStarter {
    @Suppress("UNUSED_PARAMETER")
    fun ensureStarted(context: Context) {
        // provisional: S3
    }
}
