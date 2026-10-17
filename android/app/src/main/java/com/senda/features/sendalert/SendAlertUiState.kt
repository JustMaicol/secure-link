package com.senda.features.sendalert

import com.senda.core.protocol.EmergencyType
import com.senda.core.protocol.Vulnerable

// spec 108, secciones 3.2, 4.2 y 5.2 · Estado que dibujan las pantallas de la pestaña (E1, E2 y E3). Es dato puro:
// los ViewModels (con LocationSource, MeshStatus y OwnPacketRepository) lo arman; las pantallas solo lo dibujan.

/** Estado de la ubicación del formulario. `NO_PERMISSION` no está: vuelve a la pantalla de permisos (100). */
enum class LocationStatus { SEARCHING, FIX, LAST_KNOWN, DISABLED, UNAVAILABLE }

/**
 * Ubicación que se usará. [coordinates] ya viene formateado (5 decimales con punto, `formatCoordinates`);
 * [ageMs] es el tiempo transcurrido desde la lectura; [isStale] = `ageMs > LOCATION_STALE_WARN_MS`.
 */
data class LocationUi(
    val status: LocationStatus,
    val coordinates: String? = null,
    val accuracyM: Int? = null,
    val ageMs: Long? = null,
    val isStale: Boolean = false,
) {
    /** Con `FIX` o `LAST_KNOWN` hay con qué armar el paquete. */
    val usable: Boolean get() = status == LocationStatus.FIX || status == LocationStatus.LAST_KNOWN
}

/** Por qué se explica algo bajo el botón de enviar, en lugar del texto fijo del diseño. */
enum class SendHint { RELEASED_EARLY, NEEDS_TYPE, NEEDS_LOCATION, SENDING }

const val MIN_PEOPLE = 1
const val MAX_PEOPLE = 255

data class SendAlertFormUiState(
    val selectedType: EmergencyType? = null,
    val people: Int = MIN_PEOPLE,
    val vulnerable: Vulnerable = Vulnerable(),
    val location: LocationUi = LocationUi(LocationStatus.SEARCHING),
    /** 0 a 1 mientras se mantiene presionado. */
    val holdProgress: Float = 0f,
    val sending: Boolean = false,
    val releasedEarly: Boolean = false,
) {
    val vulnerableCount: Int
        get() = listOf(vulnerable.children, vulnerable.elderly, vulnerable.injured, vulnerable.reducedMobility).count { it }

    val canSend: Boolean get() = selectedType != null && location.usable && !sending

    /** Aviso que reemplaza al texto fijo bajo el botón; nulo si no hay nada que explicar. */
    val hint: SendHint?
        get() = when {
            sending -> SendHint.SENDING
            releasedEarly -> SendHint.RELEASED_EARLY
            selectedType == null -> SendHint.NEEDS_TYPE
            !location.usable -> SendHint.NEEDS_LOCATION
            else -> null
        }
}

/**
 * "Mi alerta". Las horas llegan ya formateadas (`formatClock` con la zona del teléfono); la edad de la ubicación y de la
 * retransmisión llegan en milisegundos y la pantalla las escribe con las reglas de formato (100, sección 11).
 */
data class MyAlertUiState(
    val type: EmergencyType,
    val people: Int,
    val vulnerableCount: Int,
    val createdAtText: String,
    /** Referencia corta de la alerta, derivada de su clave (campos de la 001, nunca texto libre). */
    val reference: String,
    val savedAtText: String,
    /** Teléfonos distintos que la retransmitieron (`relayTag` distintos). */
    val echoCount: Int,
    val lastEchoAgeMs: Long?,
    /** Hora a la que este u otro teléfono la subió; nulo si todavía no. */
    val uploadedAtText: String?,
    val volunteers: Int,
    val coordinates: String,
    val accuracyM: Int,
    val locationAgeMs: Long,
)

/** Cuenta regresiva de la alerta automática (E3), mientras cuenta. */
data class AutoAlertCountdownUiState(
    val remainingMs: Long,
    val totalMs: Long = AUTO_COUNTDOWN_MS,
) {
    /** Fracción que queda, de 1 a 0, para el anillo. */
    val progress: Float get() = if (totalMs <= 0) 0f else (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)
}
