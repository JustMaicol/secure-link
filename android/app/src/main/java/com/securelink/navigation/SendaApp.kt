package com.securelink.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.securelink.R
import com.securelink.core.node.MeshState
import com.securelink.features.nearby.NearbyScreen
import com.securelink.features.sendalert.SendAlertScreen
import com.securelink.features.status.StatusScreen
import com.securelink.ui.components.BottomNavItem
import com.securelink.ui.components.SendaBottomBar
import com.securelink.ui.components.SendaIcons
import com.securelink.ui.format.connectivityText
import com.securelink.ui.theme.Senda

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

    Column(modifier = modifier.fillMaxSize().background(Senda.colors.background)) {
        NavHost(
            navController = navController,
            startDestination = Routes.SEND_ALERT,
            modifier = Modifier.weight(1f),
        ) {
            composable(Routes.SEND_ALERT) { SendAlertScreen(connectivity) }
            composable(Routes.NEARBY) { NearbyScreen(connectivity) }
            composable(Routes.STATUS) { StatusScreen(connectivity) }
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
}
