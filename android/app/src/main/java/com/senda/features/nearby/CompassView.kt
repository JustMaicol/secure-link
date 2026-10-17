package com.senda.features.nearby

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.senda.R
import com.senda.ui.format.cardinalNameText
import com.senda.ui.format.cardinalOf
import com.senda.ui.format.classifyDistance
import com.senda.ui.format.distanceText
import com.senda.ui.theme.Dimens
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText
import kotlin.math.cos
import kotlin.math.sin

/**
 * Brújula de orientación de C2 (Figma, "Brújula de orientación": tarjeta de 236 dp): círculo de 164 dp con cuatro marcas,
 * círculo punteado de referencia, la distancia grande en el centro, el rumbo en texto (`Noreste · 45°`) y una flecha
 * sobre el círculo en la dirección de la alerta. La orientación es aproximada y no es una ruta: el texto lo dice.
 *
 * Por ahora el norte queda arriba: cuando la brújula del teléfono (spec 105) entregue el rumbo del aparato, la esfera
 * girará con él. El rumbo siempre se escribe además en texto, porque la flecha sola no lo comunica.
 */
@Composable
fun CompassView(
    distanceM: Double?,
    bearingDeg: Double?,
    modifier: Modifier = Modifier,
) {
    val colors = Senda.colors
    val resources = LocalResources.current
    val distance = resources.distanceText(classifyDistance(distanceM))
    val cardinal = cardinalOf(bearingDeg)
    val bearingText = if (bearingDeg != null && cardinal != null) {
        stringResource(R.string.nearby_detail_compass_bearing, resources.cardinalNameText(cardinal), displayDegrees(bearingDeg))
    } else {
        stringResource(R.string.format_unknown)
    }
    // Una sola lectura para TalkBack; el dibujo y las letras de la esfera son decorativos.
    val spoken = "$distance, $bearingText. ${stringResource(R.string.nearby_detail_compass_hint)}"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(236.dp)
            .clip(RoundedCornerShape(Dimens.cardRadius))
            .background(colors.surfaceMuted)
            .padding(top = 8.dp, bottom = 10.dp)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(modifier = Modifier.size(width = 200.dp, height = 190.dp).clearAndSetSemantics { }) {
            Dial(bearingDeg)
            // Letras de los puntos cardinales, en las posiciones del diseño.
            Text("N", style = SendaText.smallBold, color = colors.accent, modifier = Modifier.align(Alignment.TopCenter))
            Text("O", style = SendaText.tiny, color = colors.textSecondary, modifier = Modifier.align(Alignment.CenterStart).padding(start = 3.dp))
            Text("E", style = SendaText.tiny, color = colors.textSecondary, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 3.dp))
            Text("S", style = SendaText.tiny, color = colors.textSecondary, modifier = Modifier.align(Alignment.BottomCenter))
            Column(
                modifier = Modifier.align(Alignment.Center).padding(top = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(distance, style = SendaText.figureLarge, color = colors.accent)
                Text(bearingText, style = SendaText.body13Medium, color = colors.accent)
            }
        }
        Text(
            stringResource(R.string.nearby_detail_compass_hint),
            style = SendaText.small,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clearAndSetSemantics { },
        )
    }
}

/**
 * Grados que se escriben junto al rumbo: enteros y siempre de 0 a 359 (359,6° se escribe `0°`, no `360°`).
 * El rumbo que llega ya viene normalizado de 0 a 360; esto solo evita que el redondeo escriba 360.
 */
internal fun displayDegrees(bearingDeg: Double): Int = Math.floorMod(Math.round(bearingDeg).toInt(), 360)

/** Círculo de 164 dp, su referencia punteada, las cuatro marcas y la flecha de la alerta. */
@Composable
private fun Dial(bearingDeg: Double?) {
    val colors = Senda.colors
    val ring = colors.compassRing
    val reference = colors.border
    val mark = colors.accent
    Canvas(modifier = Modifier.size(width = 200.dp, height = 190.dp)) {
        // Centro del círculo dentro de la caja de 200 × 190 (Figma: círculo de 164 dp a 18 dp del borde y 10 dp de arriba).
        val center = Offset(size.width / 2f, 10.dp.toPx() + 82.dp.toPx())
        val radius = 82.dp.toPx()
        drawCircle(ring, radius = radius - 0.5.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
        drawCircle(
            reference,
            radius = 68.dp.toPx(),
            center = center,
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx()))),
        )
        val tick = 8.dp.toPx()
        val stroke = Stroke(2.dp.toPx())
        for (angle in listOf(0.0, 90.0, 180.0, 270.0)) {
            val dx = sin(Math.toRadians(angle)).toFloat()
            val dy = -cos(Math.toRadians(angle)).toFloat()
            drawLine(
                mark,
                start = Offset(center.x + dx * radius, center.y + dy * radius),
                end = Offset(center.x + dx * (radius - tick), center.y + dy * (radius - tick)),
                strokeWidth = stroke.width,
            )
        }
        if (bearingDeg != null) {
            // La flecha del diseño apunta al noreste (45°); se gira lo que falta hasta el rumbo y se coloca sobre el círculo.
            val rad = Math.toRadians(bearingDeg)
            val arrowCenter = Offset(center.x + sin(rad).toFloat() * 69.dp.toPx(), center.y - cos(rad).toFloat() * 69.dp.toPx())
            val unit = 32.dp.toPx() / 32f
            val arrow = Path().apply {
                moveTo(2.13333f, 3.09677f)
                lineTo(32f, 0f)
                lineTo(28.8f, 28.9032f)
                lineTo(20.2667f, 20.6452f)
                lineTo(8.53333f, 32f)
                lineTo(0f, 23.7419f)
                lineTo(11.7333f, 12.3871f)
                close()
            }
            rotate(degrees = (bearingDeg - 45.0).toFloat(), pivot = arrowCenter) {
                translate(arrowCenter.x - 16.dp.toPx(), arrowCenter.y - 16.dp.toPx()) {
                    scale(unit, unit, pivot = Offset.Zero) {
                        drawPath(arrow, mark)
                    }
                }
            }
        }
    }
}
