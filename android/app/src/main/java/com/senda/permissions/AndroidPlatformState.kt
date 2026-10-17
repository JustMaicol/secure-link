package com.senda.permissions

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.PowerManager

// spec 100, sección 5.2 · Lectura directa del sistema. Sin lógica: solo traduce cada pregunta a su API.
class AndroidPlatformState(context: Context) : PlatformState {
    private val appContext: Context = context.applicationContext

    override fun isGranted(permission: String): Boolean =
        appContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    override fun bluetoothEnabled(): Boolean =
        appContext.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true

    override fun locationEnabled(): Boolean =
        appContext.getSystemService(LocationManager::class.java)?.isLocationEnabled == true

    override fun ignoringBatteryOptimizations(): Boolean =
        appContext.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(appContext.packageName) == true
}
