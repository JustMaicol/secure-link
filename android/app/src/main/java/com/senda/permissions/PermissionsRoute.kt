package com.senda.permissions

import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Pantalla de permisos conectada al sistema: pide los permisos, abre los ajustes y vuelve a leer el estado al
 * regresar. La pantalla en sí ([PermissionsScreen]) no sabe nada de Android.
 */
@Composable
fun PermissionsRoute(
    viewModel: PermissionsViewModel,
    showsRationale: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refresh(showsRationale)
    }
    val enableBluetoothLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refresh(showsRationale)
    }

    fun perform(row: PermissionRowUi) {
        when (row.action) {
            PermissionAction.REQUEST -> {
                // Se marca antes de abrir el diálogo: el proceso puede morir mientras está abierto.
                viewModel.onRequestStarted(row.kind.requirement)
                permissionLauncher.launch(runtimePermissionsOf(row.kind.requirement).toTypedArray())
            }
            PermissionAction.ENABLE_BLUETOOTH -> enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            PermissionAction.OPEN_APP_SETTINGS -> context.openAppSettings()
            PermissionAction.OPEN_BLUETOOTH_SETTINGS -> context.startOrAppSettings(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            PermissionAction.OPEN_LOCATION_SETTINGS -> context.startOrAppSettings(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            PermissionAction.REQUEST_BATTERY_EXCLUSION -> context.startOrAppSettings(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")),
            )
            PermissionAction.OPEN_BATTERY_SETTINGS ->
                context.startOrAppSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    PermissionsScreen(
        state = state,
        onRowAction = ::perform,
        onSamsungOpenSettings = { context.openAppSettings() },
        onSamsungDone = { viewModel.onSamsungAcknowledged(showsRationale) },
        onContinue = viewModel::onContinue,
        modifier = modifier,
    )
}

private fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
}

/** Algunas variantes de Android no traen una de estas pantallas de ajustes: se cae a los ajustes de la app. */
private fun Context.startOrAppSettings(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        openAppSettings()
    }
}

/** La Activity que contiene este contexto; sirve para `shouldShowRequestPermissionRationale`. */
fun Context.findComponentActivity(): ComponentActivity {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    error("No hay una ComponentActivity en este contexto")
}
