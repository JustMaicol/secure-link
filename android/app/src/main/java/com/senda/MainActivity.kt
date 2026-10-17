package com.senda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.senda.navigation.SendaRoot
import com.senda.permissions.PermissionsViewModel
import com.senda.ui.theme.SendaTheme

// spec 100 · Pantalla única de la app: permisos primero y, después, la navegación (SendaRoot).
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Con targetSdk 36 o más la interfaz va de borde a borde (cada pantalla respeta los márgenes del sistema).
        enableEdgeToEdge()
        val container = (application as SendaApplication).container
        setContent {
            SendaTheme {
                val permissions: PermissionsViewModel = viewModel(
                    factory = viewModelFactory { initializer { PermissionsViewModel(container.permissionsModel) } },
                )
                val meshState by container.meshStatus.state.collectAsStateWithLifecycle()
                SendaRoot(
                    permissions = permissions,
                    meshState = meshState,
                    showsRationale = ::shouldShowRequestPermissionRationale,
                )
            }
        }
    }
}
