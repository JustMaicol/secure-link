package com.senda.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.senda.core.node.MeshState
import com.senda.permissions.PermissionsRoute
import com.senda.permissions.PermissionsViewModel
import com.senda.service.MeshServiceStarter

/**
 * Raíz de la app (spec 100, sección 5.3): muestra la pantalla de permisos hasta que el usuario la pasa con
 * "Continuar" y no falta ningún requisito bloqueante; después, la navegación. Si más tarde se pierde un permiso
 * bloqueante, vuelve a la pantalla de permisos con un aviso.
 */
@Composable
fun SendaRoot(
    permissions: PermissionsViewModel,
    meshState: MeshState,
    showsRationale: (String) -> Boolean,
    modifier: Modifier = Modifier,
    internetOnline: Boolean = false,
) {
    val state by permissions.state.collectAsStateWithLifecycle()
    val passed by permissions.passed.collectAsStateWithLifecycle()
    val showNavigation = passed && state.canContinue

    // El usuario pudo cambiar permisos en los ajustes mientras la app estaba en segundo plano.
    LifecycleResumeEffect(permissions) {
        permissions.refresh(showsRationale)
        onPauseOrDispose {}
    }

    if (showNavigation) {
        val context = LocalContext.current
        LaunchedEffect(Unit) { MeshServiceStarter.ensureStarted(context) }
        SendaApp(meshState = meshState, modifier = modifier, internetOnline = internetOnline)
    } else {
        PermissionsRoute(viewModel = permissions, showsRationale = showsRationale, modifier = modifier)
    }
}
