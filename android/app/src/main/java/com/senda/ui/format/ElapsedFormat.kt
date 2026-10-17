package com.senda.ui.format

// spec 100, sección 11 · Tiempo transcurrido. < 1 s: "ahora"; < 1 min: segundos; < 1 h: minutos; < 24 h: horas;
// si no, días. Los segundos salen del diseño (Figma: "hace 20 s", "hace 12 s").
sealed interface ElapsedLabel {
    /** Menos de un segundo (también si el reloj del otro teléfono va adelantado: tiempo negativo). */
    data object Now : ElapsedLabel

    data class Seconds(val seconds: Int) : ElapsedLabel

    data class Minutes(val minutes: Int) : ElapsedLabel

    data class Hours(val hours: Int) : ElapsedLabel

    data class Days(val days: Int) : ElapsedLabel
}

private const val SECOND_MS = 1_000L
private const val MINUTE_MS = 60 * SECOND_MS
private const val HOUR_MS = 60 * MINUTE_MS
private const val DAY_MS = 24 * HOUR_MS

/** Cada unidad se trunca hacia abajo: 119 min son `hace 1 h`, no `hace 2 h`. */
fun classifyElapsed(elapsedMs: Long): ElapsedLabel = when {
    elapsedMs < SECOND_MS -> ElapsedLabel.Now
    elapsedMs < MINUTE_MS -> ElapsedLabel.Seconds((elapsedMs / SECOND_MS).toInt())
    elapsedMs < HOUR_MS -> ElapsedLabel.Minutes((elapsedMs / MINUTE_MS).toInt())
    elapsedMs < DAY_MS -> ElapsedLabel.Hours((elapsedMs / HOUR_MS).toInt())
    else -> ElapsedLabel.Days((elapsedMs / DAY_MS).toInt())
}
