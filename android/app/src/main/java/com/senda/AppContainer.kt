package com.senda

import android.content.Context
import com.senda.common.PrefsDeviceIdentity
import com.senda.common.SharedPreferencesIdStore
import com.senda.common.SystemClock
import com.senda.core.common.Clock
import com.senda.core.common.DeviceIdentity
import com.senda.core.node.AutoAlertController
import com.senda.core.node.FakeAutoAlertController
import com.senda.core.node.FakeMeshControl
import com.senda.core.node.FakeMeshStatus
import com.senda.core.node.MeshControl
import com.senda.core.node.MeshStatus
import com.senda.permissions.AndroidPermissionChecker
import com.senda.permissions.AndroidPlatformState
import com.senda.permissions.PermissionChecker
import com.senda.permissions.PermissionsModel
import com.senda.permissions.PlatformState
import com.senda.permissions.SharedPreferencesPermissionFlowStore
import com.senda.permissions.SharedPreferencesPermissionHistory
import java.security.SecureRandom

// spec 100 · Cableado manual, sin singletons ni inyección por reflexión (regla 10).
// Las specs de capa 3 agregan aquí sus dependencias a medida que se fusionan sus contratos (A3.3).
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    val clock: Clock = SystemClock()

    val deviceIdentity: DeviceIdentity =
        PrefsDeviceIdentity(SharedPreferencesIdStore(appContext), SecureRandom()::nextLong)

    // Permisos reales (A3'): lo que lee el verificador y lo que recuerda la pantalla de permisos.
    val platformState: PlatformState = AndroidPlatformState(appContext)
    val permissionChecker: PermissionChecker = AndroidPermissionChecker(platformState)
    val permissionsModel: PermissionsModel = PermissionsModel(
        checker = permissionChecker,
        platform = platformState,
        history = SharedPreferencesPermissionHistory(appContext),
        flow = SharedPreferencesPermissionFlowStore(appContext),
    )

    // provisional: 107 (S4 los cambia por las implementaciones reales del nodo y del servicio)
    val meshStatus: MeshStatus = FakeMeshStatus()
    val meshControl: MeshControl = FakeMeshControl()
    val autoAlertController: AutoAlertController = FakeAutoAlertController()
}
