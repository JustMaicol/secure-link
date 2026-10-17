package com.senda.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.senda.core.protocol.EmergencyType

// Colores del diseño (Figma SENDA APP, archivo uOFoD8Z44RTcdUX8KTEtEc). Figma no define variables: estos valores
// salen de los marcos P1, E1 y C1, en claro y oscuro. El tema oscuro es monocromo salvo los tres niveles de
// emergencia, que conservan su matiz. El tipo de emergencia nunca se comunica solo por color: siempre lleva texto.

/** Los tres colores de un nivel de emergencia: texto e ícono, fondo de la insignia y borde al estar seleccionado. */
@Immutable
class EmergencyPalette(val content: Color, val container: Color, val selectedBorder: Color)

@Immutable
class SendaColors(
    val isDark: Boolean,
    val background: Color,
    /** Tarjetas, barra inferior y diálogos. */
    val surface: Color,
    /** Franja de conectividad, botones pequeños y bloques secundarios. */
    val surfaceMuted: Color,
    /** Bloques de énfasis suave (introducción, voluntarios en camino). */
    val surfaceTint: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentStrong: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    /** Botón principal: fondo, texto y borde. */
    val actionContainer: Color,
    val onActionContainer: Color,
    val actionBorder: Color,
    /** Botón de cancelar la alerta automática (E3). */
    val warningAction: Color,
    val onWarningAction: Color,
    val warningActionBorder: Color,
    /** Velo detrás de la cuenta regresiva (E3). */
    val scrim: Color,
    val yellow: EmergencyPalette,
    val orange: EmergencyPalette,
    val red: EmergencyPalette,
) {
    /** Paleta del nivel de un paquete: comida o agua = amarillo, asistencia médica = naranja, atrapado o grave = rojo. */
    fun palette(type: EmergencyType): EmergencyPalette = when (type) {
        EmergencyType.FOOD_WATER -> yellow
        EmergencyType.MEDICAL -> orange
        EmergencyType.TRAPPED_OR_SEVERE -> red
    }

    /** Color del texto o el ícono de un mensaje según su tono (spec 100, sección 9). */
    fun tone(tone: UiTone): Color = when (tone) {
        UiTone.INFO -> accent
        UiTone.SUCCESS -> success
        UiTone.WARNING, UiTone.SIMULATION -> warning
        UiTone.ERROR -> red.content
    }
}

val SendaLightColors = SendaColors(
    isDark = false,
    background = Color(0xFFF8FBFF),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF3F8FA),
    surfaceTint = Color(0xFFEAF4F7),
    border = Color(0xFFD7E4EA),
    textPrimary = Color(0xFF243B4A),
    textSecondary = Color(0xFF526E78),
    accent = Color(0xFF08798A),
    accentStrong = Color(0xFF1B5F6A),
    success = Color(0xFF237447),
    successContainer = Color(0xFFEAF5ED),
    warning = Color(0xFFA35A00),
    warningContainer = Color(0xFFFFF4E5),
    actionContainer = Color(0xFF243B4A),
    onActionContainer = Color(0xFFFFFFFF),
    actionBorder = Color(0xFF243B4A),
    warningAction = Color(0xFFA35A00),
    onWarningAction = Color(0xFFFFFFFF),
    warningActionBorder = Color(0xFFA35A00),
    scrim = Color(0x99243B4A), // rgba(36, 59, 74, 0,6)
    yellow = EmergencyPalette(Color(0xFF866900), Color(0xFFFFF7D6), Color(0xFF866900)),
    orange = EmergencyPalette(Color(0xFFAA510E), Color(0xFFFFF0E1), Color(0xFFAA510E)),
    red = EmergencyPalette(Color(0xFFB83837), Color(0xFFFCEAEA), Color(0xFFB83837)),
)

// Visto en Figma (marcos P1, C1 y E1 oscuros). PROVISIONAL, a confirmar al construir esas pantallas con sus marcos
// oscuros: successContainer, warningAction y su texto y borde, y scrim (E2, E3 y S1 todavía no se leyeron).
val SendaDarkColors = SendaColors(
    isDark = true,
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceMuted = Color(0xFF0A0A0A),
    surfaceTint = Color(0xFF0A0A0A),
    border = Color(0xFF2C2C2C),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFFB5B5B5),
    accent = Color(0xFFE5E5E5),
    accentStrong = Color(0xFFE5E5E5),
    success = Color(0xFFE5E5E5),
    successContainer = Color(0xFF0A0A0A),
    warning = Color(0xFFB5B5B5),
    warningContainer = Color(0xFF101010),
    actionContainer = Color(0xFF171717),
    onActionContainer = Color(0xFFF5F5F5),
    actionBorder = Color(0xFF454545),
    warningAction = Color(0xFF171717),
    onWarningAction = Color(0xFFF5F5F5),
    warningActionBorder = Color(0xFF454545),
    scrim = Color(0x99000000),
    yellow = EmergencyPalette(Color(0xFFF4D56A), Color(0xFF352F18), Color(0xFFF4D56A)),
    orange = EmergencyPalette(Color(0xFFFFB36B), Color(0xFF3C291C), Color(0xFFFFB36B)),
    red = EmergencyPalette(Color(0xFFFF9290), Color(0xFF3B2429), Color(0xFFFF9290)),
)
