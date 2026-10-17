# Plan · 105 android-sensores

Spec: `specs/android/105-android-sensores/spec.md` (manda la spec; si este plan la contradice, se corrige el plan).
Persona: **P3**. Contexto: `specs/plan-prioridades-android.md`, fases 0 a 3.
Carpetas: `core/**/geo/`, `core/**/motion/`, `app/**/location/` y `app/**/motion/`.

## 1. Lugar en el plan

| Entrega | Fase | Para qué |
|---|---|---|
| **Contrato** | 0.2 | P2 construye 108 y 109 con `FakeLocationSource`, `FakeCompass`, `FakeMapsLauncher` y `FakeMotionSource` |
| **Cálculo puro** | 1 | distancia, rumbo y estado de ubicación, probados (P2 puede usar las funciones reales en lugar de las inyectadas) |
| **Sensores reales** | 2 | hito **H2**: distancia y rumbo reales hacia la alerta de A |
| **Movimiento** | 3 | hito **H3**: alerta automática, medidor y ajuste de umbrales |

## 2. Pasos

### Fase 0.2 · contrato

| Paso | Qué | Verificación |
|---|---|---|
| **E0** · contrato | `SensorsConfig`. En `geo`: `Fix`, `LocationState`, `LocationSource`, `GeoPoint`, `HeadingAccuracy`, `Heading`, `Compass`, `MapsResult`, `MapsLauncher`, las firmas de `distanceM`, `bearingDeg`, `relativeBearingDeg` y `e7ToDecimal`, y `HeadingSmoother`. En `motion`: `ShakeEvent`, `MotionReading`, `MotionSource` (con `observeShakes` y `observeReadings`) y `ShakeTrigger`. Todos los fakes | compila; los fakes permiten emitir estados |

### Fase 1 · cálculo puro (JVM)

| Paso | Qué | Verificación (criterios de la spec) |
|---|---|---|
| **E1** · geo (**primero**) | `distanceM` (haversine con `EARTH_RADIUS_M`), `bearingDeg` (nulo si es el mismo punto), `relativeBearingDeg` y `e7ToDecimal` con aritmética entera | tabla de la sección 3, con ±1 (criterio 4); criterios 5 y 6 |
| **E2** · estado de ubicación | `classifyLocation` en el orden de la sección 2.2, con los bordes de `FRESH_FIX_MS` | criterios 1 y 2 con `FakeClock` |
| **E3** · suavizado | `HeadingSmoother` circular, con límite de frecuencia y de cambio | criterio 7 (de 359° a 1° da cerca de 0°) |
| **E4** · detector y medidor | `ShakeDetector` (máquina de estados de la sección 5.1) y `MotionMeter`, con series sintéticas: caída de 300 ms + 3 g, caída de 200 ms, caminar y correr, impacto tardío, enfriamiento y hueco de 150 ms | criterios 8 y 12. Es puro: se adelanta a la fase 1 si hay tiempo; si no, va en la fase 3 |

### Fase 2 · sensores reales

| Paso | Qué | Verificación (criterios de la spec) |
|---|---|---|
| **E5** · ubicación | `LocationSource` con FusedLocationProvider y **conteo de recolectores**: alta precisión mientras alguien recolecta `highAccuracy()`, pasiva si no, y nada si no hay nadie. `getLastLocation` al empezar. Ubicación del sistema con `PROVIDERS_CHANGED_ACTION`. `SecurityException` → `NoPermission`. Reevaluación cada segundo con el `Clock` | criterios 3 y 10 |
| **E6** · brújula | `Compass` con `TYPE_ROTATION_VECTOR` (o acelerómetro + magnetómetro), declinación de `GeomagneticField` con la última ubicación, precisión desde `onAccuracyChanged` y `HeadingSmoother` | la flecha de la 109 gira en el teléfono |
| **E7** · Maps | `MapsLauncher`: `google.navigation:…&mode=w` con el paquete de Google Maps → `geo:` → `NO_APP`, capturando `ActivityNotFoundException` | criterio 9 |

### Fase 3 · movimiento y pruebas en dispositivo

| Paso | Qué | Verificación (criterios de la spec) |
|---|---|---|
| **E8** · acelerómetro | `MotionSource` real: `TYPE_ACCELEROMETER` con `SENSOR_DELAY_GAME` y `maxReportLatencyUs = 1 s`; `observeShakes` y `observeReadings` (con `MotionMeter`); `ShakeTrigger` solo en depuración | el medidor de la 109 se mueve en el teléfono |
| **E9** · pruebas reales | Brújula con un error menor a 20° tras calibrar (criterio 11). **Ajuste de umbrales** con el medidor: caídas reales, caminar, correr, dejar el teléfono en una mesa. Si se cambian valores de `SensorsConfig`, se registran con el motivo | registrado en `memory/P3.md` |

**Orden:** E0 (fase 0) → E1 → E2 → E3 → (E4) (fase 1) → E5 → E6 → E7 (fase 2) → (E4 si faltó) → E8 → E9 (fase 3).

## 3. Dependencias

| Necesita | De quién | Cuándo | Si no está |
|---|---|---|---|
| Esqueleto que compila | P1 (100-A, A1) | E0 | E0 espera a A1 |
| `Clock` + `FakeClock` | P1 (100-A, A2) | E2, E5 | pedir con `[PIDE a P1]` |
| `PermissionChecker` + fake | P2 (100-B, B1) | E5 | pedir con `[PIDE a P2]` |
| `play-services-location` en el catálogo y la app solo en vertical | P1 (100-A, A1) | E5, E6 | pedir con `[PIDE a P1]` |
| Que la 107 recolecte `highAccuracy()` durante la cuenta regresiva y `observeShakes()` según el ajuste | P1 (107) | fase 3 | no bloquea a la 105 |

## 4. Terminado

Criterios 1-12, PR fusionados, umbrales revisados en un Samsung, y `memory/P3.md` marca la 105 como `terminada` con el commit `spec 105: …`.

## 5. Riesgos

| Riesgo | Mitigación |
|---|---|
| Diferencias de redondeo con la tabla de referencia | La spec admite ±1; los casos elegidos no están cerca de x,5 |
| La brújula es mala cerca de metales | Limitación declarada; la 109 siempre muestra el rumbo cardinal |
| Falsas alarmas de caída | Ajuste con el medidor (E9) y los 15 s para cancelar |
| `maxReportLatencyUs` no soportado en algún modelo | Android lo ignora y entrega las muestras al momento: la detección sigue igual, solo gasta algo más. Registrar el modelo |
