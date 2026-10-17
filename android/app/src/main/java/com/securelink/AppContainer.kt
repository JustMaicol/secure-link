package com.securelink

import android.content.Context
import com.securelink.common.PrefsDeviceIdentity
import com.securelink.common.SharedPreferencesIdStore
import com.securelink.common.SystemClock
import com.securelink.core.common.Clock
import com.securelink.core.common.DeviceIdentity
import com.securelink.core.node.AutoAlertController
import com.securelink.core.node.FakeAutoAlertController
import com.securelink.core.node.FakeMeshControl
import com.securelink.core.node.FakeMeshStatus
import com.securelink.core.node.MeshControl
import com.securelink.core.node.MeshStatus
import com.securelink.permissions.FakePermissionChecker
import com.securelink.permissions.PermissionChecker
import java.security.SecureRandom

// spec 100 · Cableado manual, sin singletons ni inyección por reflexión (regla 10).
// Las specs de capa 3 agregan aquí sus dependencias a medida que se fusionan sus contratos (A3.3).
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    val clock: Clock = SystemClock()

    val deviceIdentity: DeviceIdentity =
        PrefsDeviceIdentity(SharedPreferencesIdStore(appContext), SecureRandom()::nextLong)

    // provisional: A3' (cambiar por AndroidPermissionChecker cuando P2 termine la B5)
    val permissionChecker: PermissionChecker = FakePermissionChecker.allGranted()

    // provisional: 107 (S4 los cambia por las implementaciones reales del nodo y del servicio)
    val meshStatus: MeshStatus = FakeMeshStatus()
    val meshControl: MeshControl = FakeMeshControl()
    val autoAlertController: AutoAlertController = FakeAutoAlertController()
}
