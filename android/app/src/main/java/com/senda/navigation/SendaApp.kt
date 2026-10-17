package com.senda.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.senda.R
import com.senda.core.node.MeshState
import com.senda.features.nearby.NearbyScreen
import com.senda.features.sendalert.AUTO_COUNTDOWN_MS
import com.senda.features.sendalert.AutoAlertCountdown
import com.senda.features.sendalert.AutoAlertCountdownUiState
import com.senda.features.sendalert.SendAlertScreen
import com.senda.features.status.StatusScreen
import com.senda.ui.components.BottomNavItem
import com.senda.ui.components.SendaBottomBar
import com.senda.ui.components.SendaIcons
import com.senda.ui.format.connectivityText
import com.senda.ui.theme.Senda
import kotlinx.coroutines.delay

// spec 100, sección 6 · Rutas de las tres pestañas.
object Routes {
    const val SEND_ALERT = "sendalert"
    const val NEARBY = "nearby"
    const val STATUS = "status"
}

private class Tab(val route: String, val label: Int, val icon: Int)

private val TABS = listOf(
    Tab(Routes.SEND_ALERT, R.string.nav_sendalert, SendaIcons.RadioTower),
    Tab(Routes.NEARBY, R.string.nav_nearby, SendaIcons.Radar),
    Tab(Routes.STATUS, R.string.nav_status, SendaIcons.Activity),
)

/**
 * Raíz de la app tras los permisos: las tres pestañas y la barra inferior. Recibe el estado de la malla (107)
 * ya leído, y si hay internet (106, todavía sin contrato: por ahora siempre "sin internet").
 */
@Composable
fun SendaApp(
    meshState: MeshState,
    modifier: Modifier = Modifier,
    internetOnline: Boolean = false,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.SEND_ALERT
    val selectedIndex = TABS.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val connectivity = LocalResources.current.connectivityText(meshState.health, internetOnline)

    // provisional: la cuenta regresiva real la lleva la 107 (AutoAlertController); mientras no exista, "Simular movimiento
    // brusco" del Estado lanza una de prueba aquí, sobre cualquier pestaña, que solo se cierra (no envía nada).
    var simulatedShake by remember { mutableStateOf(false) }
    var remainingMs by remember { mutableLongStateOf(AUTO_COUNTDOWN_MS) }
    LaunchedEffect(simulatedShake) {
        if (simulatedShake) {
            remainingMs = AUTO_COUNTDOWN_MS
            while (remainingMs > 0) {
                delay(COUNTDOWN_TICK_MS)
                remainingMs -= COUNTDOWN_TICK_MS
            }
            simulatedShake = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().background(Senda.colors.background)) {
        NavHost(
            navController = navController,
            startDestination = Routes.SEND_ALERT,
            modifier = Modifier.weight(1f),
        ) {
            composable(Routes.SEND_ALERT) { SendAlertScreen(connectivity) }
            composable(Routes.NEARBY) { NearbyScreen(connectivity) }
            composable(Routes.STATUS) { StatusScreen(connectivity, onSimulateShake = { simulatedShake = true }) }
        }
        SendaBottomBar(
            items = TABS.map { BottomNavItem(stringResource(it.label), it.icon) },
            selectedIndex = selectedIndex,
            onSelect = { index ->
                navController.navigate(TABS[index].route) {
                    // Una sola copia de cada pestaña, con su estado guardado al cambiar de una a otra.
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }
    if (simulatedShake) {
        AutoAlertCountdown(AutoAlertCountdownUiState(remainingMs = remainingMs), onCancel = { simulatedShake = false })
    }
    }
}

private const val COUNTDOWN_TICK_MS = 100L
