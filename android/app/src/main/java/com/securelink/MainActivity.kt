package com.securelink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securelink.navigation.SendaApp
import com.securelink.ui.theme.SendaTheme

// spec 100 · Pantalla única de la app. Los permisos (B5) se agregan antes de la navegación.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Con targetSdk 36 o más la interfaz va de borde a borde (cada pantalla respeta los márgenes del sistema).
        enableEdgeToEdge()
        val container = (application as SendaApplication).container
        setContent {
            SendaTheme {
                val meshState by container.meshStatus.state.collectAsStateWithLifecycle()
                SendaApp(meshState = meshState)
            }
        }
    }
}
