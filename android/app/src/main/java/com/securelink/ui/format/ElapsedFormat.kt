package com.securelink.ui.format

// spec 100, sección 11 · Tiempo transcurrido. < 1 min: "ahora"; < 1 h: minutos; < 24 h: horas; si no, días.
sealed interface ElapsedLabel {
    /** Menos de un minuto (también si el reloj del otro teléfono va adelantado: tiempo negativo). */
    data object Now : ElapsedLabel

    data class Minutes(val minutes: Int) : ElapsedLabel

    data class Hours(val hours: Int) : ElapsedLabel

    data class Days(val days: Int) : ElapsedLabel
}

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS
private const val DAY_MS = 24 * HOUR_MS

/** Cada unidad se trunca hacia abajo: 119 min son `hace 1 h`, no `hace 2 h`. */
fun classifyElapsed(elapsedMs: Long): ElapsedLabel = when {
    elapsedMs < MINUTE_MS -> ElapsedLabel.Now
    elapsedMs < HOUR_MS -> ElapsedLabel.Minutes((elapsedMs / MINUTE_MS).toInt())
    elapsedMs < DAY_MS -> ElapsedLabel.Hours((elapsedMs / HOUR_MS).toInt())
    else -> ElapsedLabel.Days((elapsedMs / DAY_MS).toInt())
}
