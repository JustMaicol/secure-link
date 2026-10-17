package com.securelink.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LocalSendaColors = staticCompositionLocalOf<SendaColors> {
    error("SendaTheme no está presente: envuelve la pantalla en SendaTheme { ... }")
}

/** Acceso a los tokens del diseño: `Senda.colors.accent`. Los estilos de texto están en [SendaText]. */
object Senda {
    val colors: SendaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSendaColors.current
}

/**
 * Tema de la app, claro u oscuro. Los componentes propios leen [Senda.colors]; el esquema de Material 3 solo
 * existe para los componentes de Material que se usen sin estilo propio.
 */
@Composable
fun SendaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) SendaDarkColors else SendaLightColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.background,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceMuted,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            error = colors.red.content,
            onError = colors.background,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = Color.White,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceMuted,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            error = colors.red.content,
            onError = Color.White,
        )
    }
    val typography = Typography(
        bodyLarge = SendaText.bodyLarge,
        bodyMedium = SendaText.subtitle,
        bodySmall = SendaText.caption,
        titleLarge = SendaText.title,
        labelLarge = SendaText.button,
        labelMedium = SendaText.captionMedium,
        labelSmall = SendaText.tiny,
    )
    CompositionLocalProvider(
        LocalSendaColors provides colors,
        LocalContentColor provides colors.textPrimary,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
