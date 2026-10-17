# 105 android-sensores

Capa: 2 · Persona: P3 (reparto "prioridad Android"; antes P2)
Depende de: 100
Entrega:
- En `:core`: `LocationSource`, `Compass`, `MapsLauncher` y `MotionSource` (interfaces + fakes); funciones puras de distancia, rumbo y estado de ubicación; `HeadingSmoother`; `ShakeDetector`; `SensorsConfig`. Paquetes `com.securelink.core.geo` y `com.securelink.core.motion`.
- En `:app`: implementaciones con FusedLocationProvider, SensorManager e intents. Paquetes `location` y `motion`.

Consume: `Clock` y `PermissionChecker` (100)

## Objetivo

Dar a las pantallas y al servicio la ubicación, la orientación, la distancia y el rumbo **sin internet**, abrir Google Maps cuando se pueda, y detectar una caída o un derrumbe para la alerta automática. Todo con el menor gasto de batería posible.

## Alcance

- Ubicación de alta precisión **solo mientras alguien la necesita**; el resto del tiempo, pasiva.
- Clasificación del estado de la ubicación (`FIX`, `LAST_KNOWN`, `SEARCHING`, `UNAVAILABLE`, `DISABLED` o `NO_PERMISSION`), pura y probada.
- Brújula con norte verdadero, precisión y suavizado.
- Distancia y rumbo con haversine, puros.
- Detector de caída libre seguida de impacto, puro, y su adaptador del acelerómetro.
- Botón de Maps con navegación a pie, con respaldo a cualquier app de mapas.
- Orientación de la app fijada en vertical (pedido a la 100).

## Fuera de alcance

- La cuenta regresiva y la alerta automática (107, 108). La 105 solo emite el evento de caída.
- Ruteo sin internet y mapas dentro de la app (000).
- Decidir cuándo escuchar el movimiento: lo hace la 107 según el ajuste de la 102.

## 1. Configuración

`SensorsConfig` en `:core` (regla 17):

| Constante | Valor | Para |
|---|---|---|
| `HIGH_ACCURACY_INTERVAL_MS` | 5000 | intervalo de GPS de alta precisión |
| `HIGH_ACCURACY_MIN_INTERVAL_MS` | 2000 | intervalo mínimo aceptado |
| `FRESH_FIX_MS` | 30000 | hasta aquí una ubicación es `FIX`; después, `LAST_KNOWN` |
| `SEARCH_TIMEOUT_MS` | 60000 | sin ninguna ubicación tras este tiempo de búsqueda: `UNAVAILABLE` |
| `EARTH_RADIUS_M` | 6371008.8 | radio medio de la Tierra para haversine |
| `HEADING_SMOOTHING_ALPHA` | 0.2 | suavizado de la brújula |
| `HEADING_MIN_EMIT_MS` | 100 | como máximo 10 actualizaciones de la brújula por segundo |
| `HEADING_MIN_CHANGE_DEG` | 1 | no se emite si el cambio es menor |
| `FREEFALL_MAX_G` | 0.4 | por debajo de esto se considera caída libre |
| `FREEFALL_MIN_MS` | 300 | duración mínima de la caída libre |
| `IMPACT_MIN_G` | 2.5 | impacto que confirma la caída |
| `IMPACT_WINDOW_MS` | 1000 | plazo para el impacto tras la caída libre |
| `SAMPLE_GAP_RESET_MS` | 100 | un hueco mayor entre muestras reinicia el detector |
| `SHAKE_COOLDOWN_MS` | 60000 | tras un evento, se ignoran los siguientes durante este tiempo |
| `STANDARD_GRAVITY` | 9.80665 | m/s² por g |
| `METER_PEAK_WINDOW_MS` | 2000 | ventana del pico del medidor |
| `METER_EMIT_MS` | 100 | frecuencia máxima del medidor en pantalla |

Los umbrales del movimiento son un punto de partida: se ajustan con pruebas en los Samsung (tarea 8) y el cambio se registra en la memoria.

## 2. Ubicación

### 2.1 Interfaz (`:core/geo`)

```kotlin
// spec 105
data class Fix(val latE7: Int, val lonE7: Int, val accuracyM: Int, val fixTimeMs: Long)

sealed interface LocationState {
    data class Fresh(val fix: Fix) : LocationState        // FIX en la 108
    data class LastKnown(val fix: Fix) : LocationState    // LAST_KNOWN
    data object Searching : LocationState                 // SEARCHING
    data object Unavailable : LocationState               // UNAVAILABLE: buscando sin éxito
    data object Disabled : LocationState                  // ubicación del sistema apagada
    data object NoPermission : LocationState
}

interface LocationSource {
    /** Mientras se recolecta este Flow, el GPS va en alta precisión. */
    fun highAccuracy(): Flow<LocationState>
    /** No enciende el GPS: solo ubicaciones que otras apps o el sistema ya obtuvieron. */
    fun passive(): Flow<LocationState>
}

class FakeLocationSource : LocationSource { fun emit(state: LocationState) /* ... */ }
```

### 2.2 Clasificación (pura)

```kotlin
// spec 105
fun classifyLocation(
    permission: Boolean, enabled: Boolean, lastFix: Fix?,
    nowMs: Long, searchingSinceMs: Long?, config: SensorsConfig,
): LocationState
```

En este orden:
1. Sin permiso → `NoPermission`.
2. Ubicación del sistema apagada → `Disabled`.
3. Hay `lastFix` y `nowMs − fixTimeMs ≤ FRESH_FIX_MS` → `Fresh`.
4. Hay `lastFix` (más antigua) → `LastKnown`.
5. Sin `lastFix`, buscando desde hace menos de `SEARCH_TIMEOUT_MS` → `Searching`.
6. Sin `lastFix` → `Unavailable` (se sigue buscando si hay alguien recolectando `highAccuracy`).

Los dos `Flow` reevalúan la clasificación con cada ubicación nueva, con cada cambio de permiso o de la ubicación del sistema, y una vez por segundo con el `Clock` (100), para que `Fresh` pase a `LastKnown` aunque no lleguen ubicaciones nuevas.

### 2.3 Implementación (`:app/location`)

- `FusedLocationProviderClient` con un **conteo de recolectores**:
  - Si al menos uno recolecta `highAccuracy()`: `Priority.PRIORITY_HIGH_ACCURACY`, `HIGH_ACCURACY_INTERVAL_MS` y `setMinUpdateIntervalMillis(HIGH_ACCURACY_MIN_INTERVAL_MS)`.
  - Si no, y alguien recolecta `passive()`: `Priority.PRIORITY_PASSIVE`.
  - Si no recolecta nadie: se eliminan las actualizaciones.
- Al empezar, se pide `getLastLocation()` para tener una `LastKnown` inmediata si existe.
- Conversión a `Fix`:
  - `latE7 = Math.round(latitude × 10^7).toInt()` (igual para la longitud, regla 11)
  - `accuracyM = ceil(accuracy)`
  - `fixTimeMs = location.time`
- Ubicación del sistema: `LocationManager.isLocationEnabled()` más el receptor de `PROVIDERS_CHANGED_ACTION`.
- Permiso: `PermissionChecker` (100). Un `SecurityException` se captura y da `NoPermission` (regla 16).
- **Quién recolecta `highAccuracy`:** las pantallas de la 108 (formulario) y la 109 (lista y detalle), solo mientras están visibles; y la 107 durante una cuenta regresiva. El servicio recolecta `passive` el resto del tiempo.

## 3. Distancia y rumbo (`:core/geo`, puras)

```kotlin
// spec 105
data class GeoPoint(val latE7: Int, val lonE7: Int)
fun distanceM(from: GeoPoint, to: GeoPoint): Int                 // haversine, redondeado al metro
fun bearingDeg(from: GeoPoint, to: GeoPoint): Int?               // rumbo inicial 0-359; null si son el mismo punto
fun relativeBearingDeg(bearingDeg: Int, headingDeg: Int): Int    // ((bearing − heading) mod 360 + 360) mod 360
fun e7ToDecimal(e7: Int): String                                 // "-33.5000000", sin pasar por flotantes
```

- Haversine con `EARTH_RADIUS_M`. Los ángulos se calculan en `Double` y el resultado se redondea a entero. Nunca se comparan flotantes con `==` (regla 11).
- Rumbo: `atan2(sin Δλ · cos φ2, cos φ1 · sin φ2 − sin φ1 · cos φ2 · cos Δλ)`, pasado a grados en `[0, 360)` y redondeado; 360 se lleva a 0.
- `e7ToDecimal` arma el texto con aritmética entera (signo, parte entera y 7 decimales con ceros a la izquierda). Lo usa el intent de Maps.

Valores de referencia desde `A = (−335000000, −706500000)`:

| Destino (latE7, lonE7) | `distanceM` | `bearingDeg` |
|---|---|---|
| −334990000, −706500000 (0,001° al norte) | 111 | 0 |
| −335000000, −706490000 (0,001° al este) | 93 | 90 |
| −335010000, −706500000 (0,001° al sur) | 111 | 180 |
| −335000000, −706510000 (0,001° al oeste) | 93 | 270 |
| −334990000, −706490000 (noreste) | 145 | 40 |
| −330472000, −716127000 (Valparaíso) | 102688 | 299 |
| el mismo punto A | 0 | `null` |

Las pruebas admiten ±1 en la distancia y en el rumbo, por diferencias de redondeo entre plataformas.

## 4. Brújula

### 4.1 Interfaz (`:core/geo`)

```kotlin
// spec 105
enum class HeadingAccuracy { HIGH, MEDIUM, LOW, UNRELIABLE }

sealed interface Heading {
    data class Available(val degrees: Int, val accuracy: HeadingAccuracy, val trueNorth: Boolean) : Heading
    data object Unavailable : Heading                      // el teléfono no tiene los sensores
}

interface Compass { fun observe(): Flow<Heading> }        // registra los sensores solo mientras se recolecta
class FakeCompass : Compass { fun emit(heading: Heading) }

/** Puro: suavizado circular (por seno y coseno), con límite de frecuencia y de cambio mínimo. */
class HeadingSmoother(config: SensorsConfig) { fun update(rawDeg: Double, nowMs: Long): Int? }
```

### 4.2 Implementación (`:app/location`)

- Sensor: `TYPE_ROTATION_VECTOR` → `getRotationMatrixFromVector` → `getOrientation` → azimut. Si el teléfono no tiene ese sensor: acelerómetro + magnetómetro con `getRotationMatrix`. Sin magnetómetro: `Unavailable`.
- **Norte verdadero:** se suma la declinación de `GeomagneticField(lat, lon, 0, fixTimeMs)` con la última ubicación conocida. Funciona sin internet (el modelo viene en Android). Sin ninguna ubicación: rumbo magnético con `trueNorth = false`. El rumbo de la sección 3 es respecto al norte verdadero, así que la flecha es correcta solo con `trueNorth = true`.
- Precisión: el valor de `onAccuracyChanged` del sensor (`SENSOR_STATUS_ACCURACY_*`) se traduce a `HeadingAccuracy`. Con dos sensores, vale el peor de los dos.
- Pantalla fija en vertical (sección 7): no hace falta reajustar el sistema de coordenadas según la rotación de la pantalla.

## 5. Detección de caída

### 5.1 Detector (`:core/motion`, puro)

```kotlin
// spec 105
data class ShakeEvent(val atMs: Long)

class ShakeDetector(private val config: SensorsConfig) {
    /** Muestra del acelerómetro (m/s², incluida la gravedad). Devuelve un evento si se confirma una caída. */
    fun onSample(tMs: Long, x: Double, y: Double, z: Double): ShakeEvent?
}
```

Máquina de estados, con `g = |a| / STANDARD_GRAVITY`:

| Estado | Pasa a | Cuando |
|---|---|---|
| `IDLE` | `FREEFALL` | `g < FREEFALL_MAX_G` |
| `FREEFALL` | `IDLE` | `g ≥ FREEFALL_MAX_G` antes de cumplir `FREEFALL_MIN_MS` |
| `FREEFALL` | `ARMED` | lleva `FREEFALL_MIN_MS` en caída libre |
| `ARMED` | **evento** e `IDLE` | `g > IMPACT_MIN_G` antes de `IMPACT_WINDOW_MS` desde el fin de la caída libre |
| `ARMED` | `IDLE` | pasa `IMPACT_WINDOW_MS` sin impacto |
| cualquiera | `IDLE` | hueco entre muestras mayor que `SAMPLE_GAP_RESET_MS` |

Tras un evento, se ignoran los siguientes durante `SHAKE_COOLDOWN_MS`.

### 5.2 Fuente (`:app/motion`)

```kotlin
// spec 105 (interfaz en :core/motion)
interface MotionSource { fun observeShakes(): Flow<ShakeEvent> }   // registra el acelerómetro solo mientras se recolecta
class FakeMotionSource : MotionSource { fun trigger(atMs: Long) }

/** Solo en depuración: emite un ShakeEvent por la misma vía que el sensor (botón "Simular movimiento brusco", 109). */
interface ShakeTrigger { fun trigger(atMs: Long) }
```

- `TYPE_ACCELEROMETER` con `SENSOR_DELAY_GAME` (unos 50 Hz) y `maxReportLatencyUs = 1 s` (lo pide la 107 para ahorrar batería): el hardware agrupa las lecturas y despierta la CPU una vez por segundo. Cada muestra conserva su marca de tiempo y va al `ShakeDetector`, así que la detección es la misma; solo se retrasa como mucho 1 s.
- El tiempo de la muestra se convierte de `event.timestamp` (ns de reloj de arranque) a una base relativa. El detector solo compara diferencias, y el `atMs` del evento sale del `Clock` (100).
- La 107 recolecta este `Flow` solo si la detección de movimiento está activada (102).
- **Medidor para ajustar los umbrales sin ADB** (solo en versiones de depuración): `MotionSource.observeReadings(): Flow<MotionReading>`, que la pantalla de Estado (109) muestra en vivo.

```kotlin
// spec 105 (:core/motion)
data class MotionReading(
    val currentG: Double,            // |a| / STANDARD_GRAVITY de la última muestra
    val peakG: Double,               // máximo de los últimos METER_PEAK_WINDOW_MS
    val lastFreefallMs: Long?,       // duración de la última caída libre detectada, aunque no haya terminado en evento
    val detectorState: String,       // IDLE, FREEFALL, ARMED o COOLDOWN
    val events: Int,                 // eventos de caída desde que se abrió el medidor
)
/** Puro: agrega las muestras para el medidor. Comparte el ShakeDetector, así muestra exactamente lo que este decide. */
class MotionMeter(config: SensorsConfig) { fun onSample(tMs: Long, x: Double, y: Double, z: Double): MotionReading }
```

  Se emite como máximo cada `METER_EMIT_MS`. Solo mide mientras la pantalla lo recolecta.
- **Disparo de prueba para la demo:** en las versiones de depuración, la pantalla de estado (109) ofrece "Simular movimiento brusco" (rotulado como simulación). Llama a `ShakeTrigger.trigger(atMs)`, que la `MotionSource` real implementa solo en depuración y que emite un `ShakeEvent` por la misma vía que el sensor. Así la demo no exige dejar caer un teléfono.

## 6. Maps

```kotlin
// spec 105 (interfaz en :core/geo)
enum class MapsResult { OK, NO_APP }
interface MapsLauncher { fun open(latE7: Int, lonE7: Int): MapsResult }
class FakeMapsLauncher(var result: MapsResult) : MapsLauncher
```

Implementación (`:app/location`), en orden:
1. Navegación a pie con Google Maps: `google.navigation:q=<lat>,<lon>&mode=w`, con `setPackage("com.google.android.apps.maps")`.
2. Si no está Google Maps (`ActivityNotFoundException`), cualquier app de mapas: `geo:<lat>,<lon>?q=<lat>,<lon>`.
3. Si tampoco hay ninguna: `NO_APP`.

Las coordenadas salen de `e7ToDecimal`. La excepción se captura y se traduce, nunca se traga (regla 16). No hace falta declarar `<queries>`, porque no se consulta qué apps hay instaladas.

## 7. Pedido a la 100

`MainActivity` con `android:screenOrientation="portrait"`: la app es solo vertical. Esto simplifica la brújula y deja un solo diseño en Figma.

## Criterios de aceptación

1. `classifyLocation` cumple el orden de la sección 2.2, incluidos los bordes: `FRESH_FIX_MS` exacto es `Fresh`, y 1 ms más es `LastKnown`.
2. Con un `FakeClock`, una ubicación `Fresh` pasa a `LastKnown` sin ubicaciones nuevas cuando se cumple `FRESH_FIX_MS`.
3. La implementación pide alta precisión solo mientras hay recolectores de `highAccuracy`, y vuelve a pasiva al dejar de haberlos (prueba en dispositivo o con un puerto falso del cliente de ubicación).
4. `distanceM` y `bearingDeg` dan la tabla de la sección 3 (±1). El mismo punto da 0 y `null`.
5. `relativeBearingDeg` siempre devuelve un valor en `[0, 360)`, también con valores negativos y mayores que 360.
6. `e7ToDecimal(−335000000) = "-33.5000000"`, `e7ToDecimal(5) = "0.0000005"` y `e7ToDecimal(−5) = "-0.0000005"`.
7. `HeadingSmoother`: pasar de 359° a 1° da un rumbo cercano a 0°, nunca cercano a 180°. No emite más de una vez cada `HEADING_MIN_EMIT_MS` ni con cambios menores a `HEADING_MIN_CHANGE_DEG`.
8. `ShakeDetector` con series de muestras sintéticas:
   - caída libre de 300 ms + impacto de 3 g → un evento;
   - caída libre de 200 ms + impacto → nada;
   - caminar y correr (picos de 1,5-2 g sin caída libre) → nada;
   - caída libre + impacto 1,2 s después → nada;
   - dos caídas seguidas en menos de `SHAKE_COOLDOWN_MS` → un solo evento;
   - un hueco de 150 ms entre muestras reinicia el detector.
9. `MapsLauncher` devuelve `OK` con Google Maps, `OK` con otra app de mapas y `NO_APP` sin ninguna (prueba en dispositivo o con un puerto falso).
10. Con los permisos o la ubicación del sistema retirados en caliente, la ubicación pasa a `NoPermission` o `Disabled` sin cerrar la app.
11. Prueba en un Samsung real: la flecha apunta a una alerta de prueba con un error menor a 20° tras calibrar (manual, registrado en `memory/P2.md`).
12. `MotionMeter` con las series del criterio 8: `peakG` es el máximo de los últimos 2 s, `lastFreefallMs` mide la caída libre y `detectorState` y `events` coinciden con el `ShakeDetector`.

## Limitaciones declaradas

- Una caída de unos 45 cm (300 ms de caída libre), por ejemplo un teléfono que se cae de la mano al suelo, dispara la cuenta regresiva. Por eso existen los 15 s para cancelar.
- Un derrumbe donde el teléfono no cae libremente (queda en un bolsillo, aplastado) puede no detectarse.
- La brújula es poco fiable cerca de metales y estructuras, justo lo que hay entre escombros. Por eso la 109 muestra siempre el rumbo cardinal como respaldo.
- La precisión del GPS bajo techo o entre edificios puede ser de decenas de metros.

## Tareas

1. `SensorsConfig`, interfaces y fakes en `:core`.
2. `distanceM`, `bearingDeg`, `relativeBearingDeg` y `e7ToDecimal`, con sus pruebas (criterios 4-6).
3. `classifyLocation`, con sus pruebas (criterios 1 y 2).
4. `LocationSource` con FusedLocationProvider y conteo de recolectores (criterios 3 y 10).
5. `HeadingSmoother` y `Compass` con declinación (criterio 7).
6. `ShakeDetector`, con sus pruebas (criterio 8), y `MotionSource`.
7. `MapsLauncher` (criterio 9).
8. `MotionMeter` y `observeReadings` (criterio 12).
9. Pruebas en un Samsung: brújula (criterio 11) y ajuste de los umbrales de caída con el medidor, registrados en la memoria.
