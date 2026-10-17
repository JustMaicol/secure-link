package com.securelink.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.securelink.ui.theme.Dimens
import com.securelink.ui.theme.Senda

enum class CardStyle {
    /** Superficie con borde: alertas, grupos de información. */
    Default,

    /** Fondo suave sin contraste de borde: ubicación del formulario. */
    Muted,

    /** Fondo de énfasis suave sin borde: introducción, voluntarios en camino. */
    Tinted,
}

/** Tarjeta del diseño: esquinas de 16 dp, relleno de 16 dp y contenido en columna. */
@Composable
fun SendaCard(
    modifier: Modifier = Modifier,
    style: CardStyle = CardStyle.Default,
    padding: Dp = Dimens.cardPadding,
    verticalSpacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Senda.colors
    val container = when (style) {
        CardStyle.Default -> colors.surface
        CardStyle.Muted -> colors.surfaceMuted
        CardStyle.Tinted -> colors.surfaceTint
    }
    val border = when (style) {
        CardStyle.Default -> colors.border
        CardStyle.Muted -> colors.surfaceMuted
        CardStyle.Tinted -> null
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardRadius),
        color = container,
        contentColor = colors.textPrimary,
        border = border?.let { BorderStroke(Dimens.borderWidth, it) },
    ) {
        Column(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content,
        )
    }
}
