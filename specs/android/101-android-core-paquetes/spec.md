# 101 android-core-paquetes

Capa: 2 · Persona: P1
Depende de: 001, 100 (solo el módulo `:core`)
Entrega: modelos de paquete, claves de deduplicación, `PacketCodec`, `PacketFactory`, `Mulberry32`, `ProtocolConfig` y `Hex`, en `:core`, paquete `com.senda.core.protocol`
Consume: ninguna interfaz

## Objetivo

Implementar en Kotlin puro el formato de la spec 001 (modelos, codificación, decodificación, claves, generador y configuración) para que el motor de reenvío (103), el transporte BLE (104), el almacenamiento (102) y las pantallas (108, 109) trabajen con paquetes ya validados.

## Alcance

- Modelos inmutables de `ALERT`, `ON_THE_WAY` y `STATUS`, separados del sobre que cambia en cada salto.
- Claves de deduplicación y su forma canónica en texto.
- `encode` y `decode`, con errores explícitos.
- Fábricas puras que crean paquetes nuevos.
- `Mulberry32` y `ProtocolConfig`.
- Pruebas con los vectores `packets-valid.json`, `packets-invalid.json` y `prng.json`.

## Fuera de alcance

- Reglas de reenvío y el estado de "ya visto" (103). La 101 da la clave; quién la guarda lo decide la 103 (en memoria) y la 102 (en la base).
- Envoltura BLE y company ID en el aire (104). La 101 trabaja solo con el payload.
- Contador persistente de alertas (102) y `deviceId` (100).
- Los vectores `gossip/*.json` (103).

## 1. Sin fake

La 101 son funciones puras sin efectos: la implementación real ya es determinista y sirve directamente en las pruebas de las demás specs. Por eso no entrega fake (excepción a `ARCHITECTURE.md`, regla de independencia 5, justificada por la regla 5 de la constitución).

## 2. Modelos

```kotlin
// spec 101
enum class EmergencyType(val code: Int) { FOOD_WATER(0), MEDICAL(1), TRAPPED_OR_SEVERE(2) }

enum class StatusCode(val code: Int) { CANCELLED(1), SAFE(2) }

data class Vulnerable(
    val children: Boolean = false,
    val elderly: Boolean = false,
    val injured: Boolean = false,
    val reducedMobility: Boolean = false,
)

/** Contenido inmutable: lo que crea el origen y nunca cambia en la malla. */
sealed interface Packet {
    val key: PacketKey
    val createdAtMs: Long   // epoch ms UTC, múltiplo de 1000 (spec 001)
}

data class Alert(
    val deviceId: Long, val counter: Int,
    val emergencyType: EmergencyType, val automatic: Boolean, val vulnerable: Vulnerable,
    val people: Int, val latE7: Int, val lonE7: Int,
    override val createdAtMs: Long,
) : Packet { override val key: AlertKey get() = AlertKey(deviceId, counter) }

data class OnTheWay(
    val alert: AlertKey, val volunteerDeviceId: Long,
    override val createdAtMs: Long,
) : Packet { override val key get() = OnTheWayKey(alert, volunteerDeviceId) }

data class Status(
    val alert: AlertKey, val status: StatusCode,
    override val createdAtMs: Long,
) : Packet { override val key get() = StatusKey(alert, status) }

/** Sobre: lo que cambia en cada salto. Nunca entra en la clave. */
data class WirePacket(val packet: Packet, val ttl: Int, val relayTag: Int)
```

- Los constructores no validan. Validan `encode`, `decode` y las fábricas, que devuelven errores en lugar de lanzar excepciones.
- `deviceId` y `volunteerDeviceId` son `Long` con 48 bits útiles (`0..DEVICE_ID_MAX`, de la 100).

## 3. Claves

```kotlin
// spec 101
sealed interface PacketKey : Comparable<PacketKey> {
    fun canonical(): String
    override fun compareTo(other: PacketKey) = canonical().compareTo(other.canonical())
}
data class AlertKey(val deviceId: Long, val counter: Int) : PacketKey      // "a1b2c3d4e5f6-0001"
data class OnTheWayKey(val alert: AlertKey, val volunteerDeviceId: Long) : PacketKey  // "a1b2c3d4e5f6-0001/0c0c0c0c0c0c"
data class StatusKey(val alert: AlertKey, val status: StatusCode) : PacketKey        // "a1b2c3d4e5f6-0001#2"

fun relayTagOf(deviceId: Long): Int = (deviceId and 0xFFFF).toInt()
```

Forma canónica exacta de la 001, sección 4: hexadecimal en minúscula, con ceros a la izquierda. El orden de las claves es el orden de su texto canónico (regla 9).

## 4. `ProtocolConfig`

```kotlin
// spec 101
data class ProtocolConfig(
    val companyId: Int = 0xFFFF,
    val magic: Int = 0xA7,
    val protocolVersion: Int = 1,
    val maxPayloadBytes: Int = 27,
    val ttlMax: Int = 8,
    val forwardDelayMaxMs: Long = 2_000,
    val announceWindowMs: Long = 60_000,
    val originAnnounceWindowMs: Long = 60_000,
    val maxAgeMs: Long = 86_400_000,
) { companion object { val DEFAULT = ProtocolConfig() } }
```

Valores de la 001, sección 8. Es el único lugar del código Android donde aparecen (regla 17). Se pasa como parámetro, no como global, para que los escenarios de la 103 puedan sobrescribirlo.

## 5. `PacketCodec`

```kotlin
// spec 101
object PacketCodec {
    fun encode(wire: WirePacket, config: ProtocolConfig = ProtocolConfig.DEFAULT): EncodeResult
    fun decode(payload: ByteArray, config: ProtocolConfig = ProtocolConfig.DEFAULT): DecodeResult
}

sealed interface EncodeResult {
    class Ok(val payload: ByteArray) : EncodeResult
    data class Err(val error: FieldError) : EncodeResult
}

sealed interface DecodeResult {
    data class Ok(val wire: WirePacket) : DecodeResult
    data class Err(val error: DecodeError) : DecodeResult
}

sealed interface DecodeError {
    data object NotOurPacket : DecodeError
    data class UnsupportedVersion(val version: Int) : DecodeError
    data class UnknownType(val code: Int) : DecodeError
    data class InvalidLength(val expected: Int, val actual: Int) : DecodeError
    data class InvalidField(val field: String) : DecodeError
}

data class FieldError(val field: String)
```

Reglas:
- `decode` sigue el orden de errores de la 001, sección 5. Si varios campos están fuera de rango, informa el **primero por offset**.
- `encode` valida los campos en orden de offset y devuelve el primero inválido. Además de los rangos de la 001, rechaza:
  - `deviceId` o `volunteerDeviceId` fuera de `0..DEVICE_ID_MAX`
  - `createdAtMs` negativo, no múltiplo de 1000 o mayor que `4294967295000` (uint32 en segundos)
  - `relayTag` fuera de `0..65535`
- Los nombres de campo del error son los de la 001: `deviceId`, `counter`, `emergencyType`, `people`, `latE7`, `lonE7`, `createdAtS`, `ttl`, `relayTag`, `alertDeviceId`, `alertCounter`, `volunteerDeviceId` y `status`.
- El bit reservado de `info` se escribe en `0` y se ignora al leer.
- `encode(decode(x)) == x` para todo `x` válido sin el bit reservado activo, y `decode(encode(w)) == w` para todo `w` válido.
- `EncodeResult.Ok` no es `data class` porque `ByteArray` no compara por contenido. Las pruebas comparan con `contentEquals`.

## 6. `PacketFactory`

Crea paquetes nuevos en el origen. Todas devuelven `CreateResult.Ok(wire)` o `CreateResult.Err(FieldError)`, con `ttl = config.ttlMax` y `relayTag = relayTagOf(deviceId)`.

```kotlin
// spec 101
object PacketFactory {
    fun newAlert(deviceId: Long, counter: Int, type: EmergencyType, vulnerable: Vulnerable,
                 people: Int, latE7: Int, lonE7: Int, nowMs: Long,
                 config: ProtocolConfig = ProtocolConfig.DEFAULT): CreateResult
    fun newAutomaticAlert(deviceId: Long, counter: Int, latE7: Int, lonE7: Int, nowMs: Long,
                          config: ProtocolConfig = ProtocolConfig.DEFAULT): CreateResult
    fun newOnTheWay(volunteerDeviceId: Long, alert: AlertKey, nowMs: Long,
                    config: ProtocolConfig = ProtocolConfig.DEFAULT): CreateResult
    fun newStatus(deviceId: Long, alert: AlertKey, status: StatusCode, nowMs: Long,
                  config: ProtocolConfig = ProtocolConfig.DEFAULT): CreateResult
}
```

| Regla | De dónde sale |
|---|---|
| `createdAtMs = floorDiv(nowMs, 1000) * 1000` | 001, sección 5 |
| `people > 255` se guarda como `255`; `people < 1` es error | 001 (255 = "255 o más") |
| `newAutomaticAlert` = `TRAPPED_OR_SEVERE`, `automatic = true`, 1 persona, sin vulnerables | 000 y 001, sección 3.1 |
| `newAlert` siempre lleva `automatic = false` | 001 |
| `newStatus` da error `FieldError("alertDeviceId")` si `alert.deviceId != deviceId`: solo el origen cancela o marca a salvo | 001, sección 3.3 |
| El resultado pasa por las mismas validaciones que `encode` | 001 |

El contador lo asigna la 102. Si llega fuera de `1..65535`, el error es `FieldError("counter")`.

## 7. `Mulberry32`

```kotlin
// spec 101
class Mulberry32(seed: Int) {
    /** Siguiente valor uint32 en un Long (0..4294967295). */
    fun next(): Long
}

fun seedOf(deviceId: Long): Int = deviceId.toInt()   // 32 bits bajos (001, sección 6.4)
```

- Implementación exacta de la 001, sección 7. La multiplicación es `Int * Int` (desborda igual que `Math.imul`) y el desplazamiento sin signo es `ushr`. El resultado se convierte con `toLong() and 0xFFFFFFFFL`.
- Las semillas de los vectores van como uint32 en JSON: se convierten con `json.toLong().toInt()`.
- Es una instancia con estado explícito, no un global (regla 10). La 101 no consume valores: solo los da.

## 8. `Hex`

```kotlin
// spec 101
object Hex {
    fun encode(bytes: ByteArray): String           // minúsculas, sin separadores
    fun decode(text: String): ByteArray            // lanza IllegalArgumentException si no es hex par
}
```

La usan las pruebas de vectores y la pantalla de estado (109) para depurar. La excepción de `decode` está declarada (regla 16): un texto mal formado es un error de programación, no un dato de la malla.

## 9. Pruebas

En `core/src/test/kotlin/com/senda/core/protocol/`. Leen los vectores desde la propiedad `vectorsDir` (100).

| Prueba | Qué verifica |
|---|---|
| `PacketVectorsTest` | cada entrada de `packets-valid.json` (hex, campos y clave canónica) y de `packets-invalid.json` (error y campo) |
| `PrngVectorsTest` | `prng.json` |
| `RoundTripTest` | 1000 paquetes válidos generados con `Mulberry32(1)`: `decode(encode(w)) == w` |
| `KeyTest` | cambiar `ttl` o `relayTag` no cambia la clave; cambiar cualquier campo inmutable sí; formato y orden canónicos |
| `FactoryTest` | cada regla de la tabla de la sección 6 |
| `EncodeValidationTest` | cada rechazo de la sección 5, con el primer campo inválido por offset |

Los tres ejemplos de la 001, sección 10, se prueban también escritos directamente en el código, para que la 101 avance aunque falten los archivos de vectores.

## Criterios de aceptación

1. Los tres paquetes de la 001, sección 10, se codifican exactamente al hex indicado y se decodifican a los mismos campos y la misma clave canónica.
2. Pasan todas las entradas de `packets-valid.json`, `packets-invalid.json` y `prng.json`.
3. `decode(encode(w)) == w` para los 1000 paquetes del `RoundTripTest`.
4. El bit reservado en `1` se decodifica sin error y se codifica de vuelta como `0`.
5. Cambiar `ttl` o `relayTag` no cambia la clave; cambiar un campo inmutable sí.
6. `encode` y `decode` nunca lanzan excepciones con entradas de cualquier tamaño, incluidos 0 bytes y 255 bytes aleatorios. Lo comprueba una prueba con 10000 entradas generadas con `Mulberry32(42)`.
7. Las fábricas cumplen cada regla de la sección 6.
8. `Mulberry32` reproduce las salidas de la 001, sección 7.
9. Las constantes del protocolo solo aparecen en `ProtocolConfig`. Una prueba busca el byte mágico (`0xA7`, `0xa7` o `167`) en `core/src/main` fuera de ese archivo. Las demás constantes se revisan en el PR.
10. `./gradlew :core:test` pasa sin dispositivo.

## Tareas

1. Modelos y claves (sección 2-3), con `KeyTest`.
2. `ProtocolConfig` y `Hex`.
3. `Mulberry32`, con `PrngVectorsTest` y las salidas de la 001 escritas en el código.
4. `PacketCodec.encode` y `decode`, con los ejemplos de la 001, `EncodeValidationTest`, `RoundTripTest` y la prueba de entradas aleatorias.
5. `PacketFactory`, con `FactoryTest`.
6. `PacketVectorsTest` contra los archivos de `specs/shared/vectors/`. Si todavía no existen (ola 1), la tarea queda `bloqueada` y se registra en `memory/P1.md`.
