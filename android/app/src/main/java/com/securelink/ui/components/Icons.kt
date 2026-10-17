package com.securelink.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.securelink.R
import com.securelink.ui.format.CardinalPoint

/**
 * Íconos del diseño: Lucide convertidos con `tools/lucide_to_vector.py` (ISC, `third_party/lucide`). Los cuatro
 * sectores diagonales y los extremos que Figma no dibuja (N, S, SE, NO) son de la misma familia.
 */
object SendaIcons {
    val Activity = R.drawable.ic_activity
    val ArrowDown = R.drawable.ic_arrow_down
    val ArrowDownLeft = R.drawable.ic_arrow_down_left
    val ArrowDownRight = R.drawable.ic_arrow_down_right
    val ArrowLeft = R.drawable.ic_arrow_left
    val ArrowRight = R.drawable.ic_arrow_right
    val ArrowUp = R.drawable.ic_arrow_up
    val ArrowUpLeft = R.drawable.ic_arrow_up_left
    val ArrowUpRight = R.drawable.ic_arrow_up_right
    val Bell = R.drawable.ic_bell
    val Bluetooth = R.drawable.ic_bluetooth
    val Check = R.drawable.ic_check
    val ChevronRight = R.drawable.ic_chevron_right
    val CloudOff = R.drawable.ic_cloud_off
    val Copy = R.drawable.ic_copy
    val Fingerprint = R.drawable.ic_fingerprint_pattern
    val ListFilter = R.drawable.ic_list_filter
    val Map = R.drawable.ic_map
    val MapPin = R.drawable.ic_map_pin
    val Minus = R.drawable.ic_minus
    val Navigation = R.drawable.ic_navigation
    val Network = R.drawable.ic_network
    val Plus = R.drawable.ic_plus
    val Radar = R.drawable.ic_radar
    val Radio = R.drawable.ic_radio
    val RadioTower = R.drawable.ic_radio_tower
    val ShieldCheck = R.drawable.ic_shield_check
    val Smartphone = R.drawable.ic_smartphone
    val TriangleAlert = R.drawable.ic_triangle_alert
    val Users = R.drawable.ic_users
    val X = R.drawable.ic_x

    /** Todos, en orden alfabético, para la galería de depuración. */
    val all: List<Pair<String, Int>>
        get() = listOf(
            "activity" to Activity, "arrow-down" to ArrowDown, "arrow-down-left" to ArrowDownLeft,
            "arrow-down-right" to ArrowDownRight, "arrow-left" to ArrowLeft, "arrow-right" to ArrowRight,
            "arrow-up" to ArrowUp, "arrow-up-left" to ArrowUpLeft, "arrow-up-right" to ArrowUpRight,
            "bell" to Bell, "bluetooth" to Bluetooth, "check" to Check, "chevron-right" to ChevronRight,
            "cloud-off" to CloudOff, "copy" to Copy, "fingerprint" to Fingerprint, "list-filter" to ListFilter,
            "map" to Map, "map-pin" to MapPin, "minus" to Minus, "navigation" to Navigation,
            "network" to Network, "plus" to Plus, "radar" to Radar, "radio" to Radio,
            "radio-tower" to RadioTower, "shield-check" to ShieldCheck, "smartphone" to Smartphone,
            "triangle-alert" to TriangleAlert, "users" to Users, "x" to X,
        )
}

/** Flecha de la dirección de un sector cardinal (lista de cercanas y detalle). */
@DrawableRes
fun CardinalPoint.arrowIcon(): Int = when (this) {
    CardinalPoint.N -> SendaIcons.ArrowUp
    CardinalPoint.NE -> SendaIcons.ArrowUpRight
    CardinalPoint.E -> SendaIcons.ArrowRight
    CardinalPoint.SE -> SendaIcons.ArrowDownRight
    CardinalPoint.S -> SendaIcons.ArrowDown
    CardinalPoint.SO -> SendaIcons.ArrowDownLeft
    CardinalPoint.O -> SendaIcons.ArrowLeft
    CardinalPoint.NO -> SendaIcons.ArrowUpLeft
}

/** Ícono de trazo con el color y el tamaño que pide quien lo usa. [contentDescription] null = decorativo. */
@Composable
fun SendaIcon(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}
