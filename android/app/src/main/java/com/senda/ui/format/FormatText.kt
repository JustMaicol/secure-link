package com.senda.ui.format

import android.content.res.Resources
import com.senda.R
import com.senda.core.node.MeshHealth
import com.senda.core.protocol.EmergencyType
import com.senda.ui.theme.UiTone

// spec 100, sección 11 · Textos de las reglas de formato. La clasificación es pura (DistanceFormat, ElapsedFormat…);
// aquí cada etiqueta se convierte en el texto de strings.xml, con sus plurales en español.

fun Resources.distanceText(label: DistanceLabel): String = when (label) {
    DistanceLabel.Unknown -> getString(R.string.format_unknown)
    is DistanceLabel.Meters -> getString(R.string.format_distance_meters, label.meters)
    // Siempre con coma decimal, como pide la spec, sea cual sea el idioma del teléfono.
    is DistanceLabel.Kilometers ->
        getString(R.string.format_distance_kilometers, "${label.tenths / 10},${label.tenths % 10}")
    is DistanceLabel.WholeKilometers -> getString(R.string.format_distance_kilometers, label.km.toString())
}

fun Resources.elapsedText(label: ElapsedLabel): String = when (label) {
    ElapsedLabel.Now -> getString(R.string.format_elapsed_now)
    is ElapsedLabel.Seconds -> getQuantityString(R.plurals.format_elapsed_seconds, label.seconds, label.seconds)
    is ElapsedLabel.Minutes -> getQuantityString(R.plurals.format_elapsed_minutes, label.minutes, label.minutes)
    is ElapsedLabel.Hours -> getQuantityString(R.plurals.format_elapsed_hours, label.hours, label.hours)
    is ElapsedLabel.Days -> getQuantityString(R.plurals.format_elapsed_days, label.days, label.days)
}

fun Resources.peopleText(label: PeopleLabel): String = when (label) {
    PeopleLabel.One -> getQuantityString(R.plurals.format_people, 1, 1)
    is PeopleLabel.Many -> getQuantityString(R.plurals.format_people, label.count, label.count)
    is PeopleLabel.AtLeast -> getString(R.string.format_people_max, label.count)
}

/** `1 vulnerable`, `2 vulnerables` o `sin vulnerables`. */
fun Resources.vulnerableText(count: Int): String =
    if (count <= 0) getString(R.string.format_vulnerable_none) else getQuantityString(R.plurals.format_vulnerable, count, count)

/** `3 personas · 1 vulnerable`. */
fun Resources.peopleAndVulnerableText(people: Int, vulnerableCount: Int): String = getString(
    R.string.format_people_and_vulnerable,
    peopleText(classifyPeople(people)),
    vulnerableText(vulnerableCount),
)

/** Siglas del rumbo para la lista: `NE`. */
fun Resources.cardinalShortText(point: CardinalPoint): String = getString(
    when (point) {
        CardinalPoint.N -> R.string.cardinal_short_n
        CardinalPoint.NE -> R.string.cardinal_short_ne
        CardinalPoint.E -> R.string.cardinal_short_e
        CardinalPoint.SE -> R.string.cardinal_short_se
        CardinalPoint.S -> R.string.cardinal_short_s
        CardinalPoint.SO -> R.string.cardinal_short_so
        CardinalPoint.O -> R.string.cardinal_short_o
        CardinalPoint.NO -> R.string.cardinal_short_no
    },
)

/** Nombre completo del rumbo para el detalle: `Noreste`. */
fun Resources.cardinalNameText(point: CardinalPoint): String = getString(
    when (point) {
        CardinalPoint.N -> R.string.cardinal_name_n
        CardinalPoint.NE -> R.string.cardinal_name_ne
        CardinalPoint.E -> R.string.cardinal_name_e
        CardinalPoint.SE -> R.string.cardinal_name_se
        CardinalPoint.S -> R.string.cardinal_name_s
        CardinalPoint.SO -> R.string.cardinal_name_so
        CardinalPoint.O -> R.string.cardinal_name_o
        CardinalPoint.NO -> R.string.cardinal_name_no
    },
)

/** `Nivel naranja`. El tipo de emergencia nunca se comunica solo por color. */
fun Resources.emergencyLevelText(type: EmergencyType): String = getString(
    when (type) {
        EmergencyType.FOOD_WATER -> R.string.emergency_level_yellow
        EmergencyType.MEDICAL -> R.string.emergency_level_orange
        EmergencyType.TRAPPED_OR_SEVERE -> R.string.emergency_level_red
    },
)

/** `Naranja`: la opción del selector del formulario. */
fun Resources.emergencyOptionText(type: EmergencyType): String = getString(
    when (type) {
        EmergencyType.FOOD_WATER -> R.string.emergency_option_yellow
        EmergencyType.MEDICAL -> R.string.emergency_option_orange
        EmergencyType.TRAPPED_OR_SEVERE -> R.string.emergency_option_red
    },
)

/** Textos y tono de la franja de conectividad, ya resueltos. */
data class ConnectivityText(val mesh: String, val meshTone: UiTone, val internet: String)

/**
 * Franja de conectividad a partir del estado de la malla (107) y de si hay internet (106).
 * Figma solo dibuja `Malla activa · Sin internet`; los demás estados de la malla no están en el diseño.
 */
fun Resources.connectivityText(health: MeshHealth, internetOnline: Boolean): ConnectivityText {
    val (mesh, tone) = when (health) {
        MeshHealth.RUNNING -> R.string.connectivity_mesh_running to UiTone.SUCCESS
        MeshHealth.STARTING -> R.string.connectivity_mesh_starting to UiTone.INFO
        MeshHealth.STOPPED -> R.string.connectivity_mesh_stopped to UiTone.WARNING
        MeshHealth.BLUETOOTH_OFF -> R.string.connectivity_mesh_bluetooth_off to UiTone.WARNING
        MeshHealth.NO_PERMISSION -> R.string.connectivity_mesh_no_permission to UiTone.ERROR
        MeshHealth.ADVERTISE_UNSUPPORTED -> R.string.connectivity_mesh_advertise_unsupported to UiTone.ERROR
        MeshHealth.ADVERTISE_FAILED -> R.string.connectivity_mesh_advertise_failed to UiTone.ERROR
        MeshHealth.SCAN_FAILED -> R.string.connectivity_mesh_scan_failed to UiTone.ERROR
    }
    val internet = if (internetOnline) R.string.connectivity_internet_on else R.string.connectivity_internet_off
    return ConnectivityText(getString(mesh), tone, getString(internet))
}
