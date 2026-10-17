# Plan · 109 android-ui-alertas-cercanas-y-estado

Spec: `specs/android/109-android-ui-alertas-cercanas-y-estado/spec.md` (manda la spec; si este plan la contradice, se corrige el plan).
Persona: **P2**. Contexto: `specs/plan-prioridades-android.md`, fase 1 (prototipo visual).
Carpetas: `app/**/features/nearby/`, `app/**/features/status/`, `app/**/prototype/` (compartida con la 108) y los textos `nearby_*` y `status_*` de `strings.xml`. Además, la lectura de `NotificationRoutes` en `MainActivity`/`navigation/` (dueño P2).
Diseño: `specs/design/android-design-brief.md`, pantallas C1, C2, S1 y notificaciones N/01, N/05 y N/06.

## 1. Lugar en el plan

- Se construye **entera sobre fakes** en la fase 1, en paralelo con la 108.
- Hito **H1**: todos los frames C1, C2 y S1 del brief en el APK.
- Hito **H2**: con las implementaciones reales, la lista de C muestra la alerta de A con distancia y rumbo, y "Voy en camino" funciona entre teléfonos.

## 2. Pasos

| Paso | Qué | Verificación (criterios de la spec) |
|---|---|---|
| **N1** · textos y configuración | `NearbyConfig` y `StatusConfig` (`UI_REFRESH_MS`). **Todos** los `id` de las secciones 2.4, 3.3, 4.3 y 5 en `strings.xml`, incluidos las notificaciones (`nearby_notif_new_alert`, `status_notif_service*`, que usa P1) y los de depuración (medidor y "Simular movimiento brusco") | criterio 9 |
| **N2** · lista: estado | `NearbyUiState` desde `NearbyRepository.observeNearby()`, `LocationSource.highAccuracy()` (solo visible) y `Compass`. Calcula distancia, rumbo y `relativeBearingDeg`. **Las funciones de distancia y rumbo se inyectan en el ViewModel** (con las de la 105 por defecto), para probar sin depender del código de P3. Orden de la sección 2.2, sin propias ni alertas de más de `maxAgeMs`. Banners según malla, ubicación, brújula y filtro | criterios 1 y 3 con fakes y `FakeClock` |
| **N3** · lista: pantalla | Tarjeta de tres zonas (sección 2.1), flecha que gira, banners y los dos estados vacíos | criterio 2 (cada banner solo en su condición) |
| **N4** · detalle | `NearbyDetailUiState` con `observe(key)`. Bloque de navegación, saltos (`ttlMax − ttl + 1`), coordenadas y clave. "Voy en camino" con `ConfirmDialog` → `OwnPacketRepository.saveOnTheWay(alertKey, now)` (idempotente). "Abrir en Google Maps" con `MapsLauncher`; aviso sin internet desde `SyncStatus.online` | criterios 4 y 5 |
| **N5** · Estado | Secciones Malla, Servidor, Este teléfono, Ajustes y Filtro de depuración (sección 4.1). `MeshControl` con confirmación al apagar. **Validación pura del filtro** (`parseRelayTag(texto, propio, existentes)`: formato, repetido, propio; normaliza a minúsculas) | criterios 6-8 |
| **N6** · depuración | Solo en depuración: botón "Simular movimiento brusco" y **medidor de movimiento** (`MotionSource.observeReadings()`, 105) con g actual, pico, última caída libre, estado y eventos | se ven en la galería y en el APK de depuración |
| **N7** · destinos desde notificaciones | En `MainActivity`/`navigation/`: leer `route` y `alert_key` de `NotificationRoutes` (107) y abrir C2, E2 o E3 | prueba de navegación con un `Intent` de cada tipo |
| **N8** · reglas | Prueba que busca `System.currentTimeMillis` en `features/nearby/` y `features/status/` | criterio 10 |
| **N9** · frames del prototipo | En la galería (plan de la 108, sección 4): `C1/01`-`C1/08`, `C2/01`-`C2/09` y `S1/01`-`S1/09` | todos visibles en la galería |

**Orden:** N1 → N2 → N3 → N4 → N5 → N6 → N9 → N7 → N8. N1 va en el primer PR: P1 necesita los textos de las notificaciones.

## 3. Dependencias (todas por contrato de la fase 0)

| Necesita | De quién | Si no está |
|---|---|---|
| Componentes, formato (distancia, rumbo cardinal, tiempo) y navegación | P2 (100-B) | es del mismo P2 |
| `AlertKey`, `relayTagOf`, `ProtocolConfig` | P1 (101, C1) | pedir con `[PIDE a P1]` |
| `NearbyRepository`, `OwnPacketRepository`, `SettingsRepository`, `StorageStats` + `InMemoryStorage` | P3 (102) | pedir con `[PIDE a P3]` |
| `LocationSource`, `Compass`, `MapsLauncher`, `MotionSource` (con `observeReadings`) + fakes; firmas de `distanceM`, `bearingDeg`, `relativeBearingDeg` | P3 (105) | pedir con `[PIDE a P3]`; mientras tanto, funciones inyectadas en N2 |
| **Disparador de prueba** para "Simular movimiento brusco" (por ejemplo, `ShakeTrigger.trigger(atMs)` en `core/motion`, implementado por la fuente real solo en depuración) | P3 (105) | **pedido nuevo**: `[PIDE a P3]`; mientras tanto, el botón usa `FakeMotionSource.trigger` |
| `MeshStatus`, `MeshControl` + fakes; `NotificationRoutes` | P1 (107, S0) | pedir con `[PIDE a P1]` |
| `SyncStatus` + fake | P2 (106, Y0) | es del mismo P2: va en la fase 0 |

## 4. Terminado

- **Hito H1:** N1-N6 y N9 en el APK de depuración, comparados con Figma.
- **Spec completa:** criterios 1-10 y N7, PR fusionados, y `memory/P2.md` marca la 109 como `terminada` con el commit `spec 109: …`.

## 5. Riesgos

| Riesgo | Mitigación |
|---|---|
| La flecha tiembla o salta | El suavizado es de la 105 (`HeadingSmoother`); la UI solo anima la rotación por el camino más corto (359° → 1°) |
| Recalcular cada segundo gasta batería | Solo con la pantalla visible (`collectAsStateWithLifecycle`) |
| El filtro de depuración se confunde con una función real | Rótulo `SIMULATION` en la sección y banner en Cercanas y Estado (regla 30) |
