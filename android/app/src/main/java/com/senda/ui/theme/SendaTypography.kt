package com.senda.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.senda.R

// Tipografía del diseño: Roboto (variable) en cuatro pesos. Se incluye en la app porque Samsung usa su propia
// fuente del sistema. Licencia: third_party/roboto/OFL.txt.
private fun roboto(weight: FontWeight) = Font(
    resId = R.font.roboto_vf,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight), FontVariation.width(100f)),
)

val SendaFont = FontFamily(
    roboto(FontWeight.Normal),
    roboto(FontWeight.Medium),
    roboto(FontWeight.SemiBold),
    roboto(FontWeight.Bold),
)

private fun style(size: Int, weight: FontWeight, lineHeight: Float? = null) = TextStyle(
    fontFamily = SendaFont,
    fontSize = size.sp,
    fontWeight = weight,
    // Figma usa `leading-[normal]` (la del tipo de letra) o un múltiplo del tamaño.
    lineHeight = if (lineHeight == null) TextUnit.Unspecified else (size * lineHeight).sp,
    letterSpacing = 0.sp,
)

/** Estilos de texto del diseño, nombrados por su función; los que no tienen función propia, por tamaño y peso. */
object SendaText {
    val title = style(29, FontWeight.Bold, 1.15f)
    val headline = style(25, FontWeight.Bold, 1.2f)
    val countdown = style(48, FontWeight.SemiBold)
    val figureLarge = style(34, FontWeight.Bold)
    val figure = style(25, FontWeight.Bold)
    val counter = style(24, FontWeight.Bold)
    val distance = style(20, FontWeight.Bold)
    val cardTitle = style(16, FontWeight.Bold, 1.2f)
    val button = style(16, FontWeight.SemiBold)
    val sectionTitle = style(15, FontWeight.Bold)
    val bodyLarge = style(15, FontWeight.Normal, 1.4f)
    val subtitle = style(14, FontWeight.Normal, 1.4f)
    val label = style(14, FontWeight.Bold, 1.3f)
    val labelSemiBold = style(14, FontWeight.SemiBold)
    val labelMedium = style(14, FontWeight.Medium, 1.3f)
    val body13 = style(13, FontWeight.Normal)
    val body13Medium = style(13, FontWeight.Medium)
    val body13SemiBold = style(13, FontWeight.SemiBold)
    val body13Bold = style(13, FontWeight.Bold)
    val caption = style(12, FontWeight.Normal, 1.4f)
    val captionMedium = style(12, FontWeight.Medium)
    val captionSemiBold = style(12, FontWeight.SemiBold)
    val captionBold = style(12, FontWeight.Bold)
    val small = style(11, FontWeight.Normal, 1.4f)
    val smallBold = style(11, FontWeight.Bold)
    val tiny = style(10, FontWeight.Normal)
    val tinyBold = style(10, FontWeight.Bold)
}
