# 109 android-ui-alertas-cercanas-y-estado

Capa: 3 · Persona: P2
Depende de: 100, 101, 102, 103, 105, 106, 107 (solo interfaces)
Entrega: pestañas **Cercanas** (lista y detalle con brújula y "Voy en camino") y **Estado** (malla, servidor, ajustes y filtro de depuración), con sus `UiState`, ViewModels y textos; texto de las notificaciones de alertas cercanas y del servicio
Consume: `AlertKey`, `relayTagOf` y `ProtocolConfig` (101); `NearbyRepository`, `OwnPacketRepository`, `SettingsRepository` y `StorageStats` (102); `LocationSource`, `Compass`, distancia y rumbo, y `MapsLauncher` (105); `SyncStatus` (106); `MeshStatus` y `MeshControl` (107); componentes, formato y convenciones (100)

## Objetivo

Que un voluntario vea qué alertas hay cerca, sepa hacia dónde y a qué distancia están sin internet, y avise que va en camino. Y que cualquiera vea si la malla y la subida funcionan, y pueda preparar la demo.

## Alcance

- Lista de alertas cercanas y su detalle con la flecha de rumbo.
- "Voy en camino" y el botón "Abrir en Google Maps".
- Pantalla de estado: malla, servidor, identidad del teléfono, ajustes y filtro de depuración.
- Catálogo completo de variables visibles y mensajes (100, sección 9).
- Texto de las notificaciones de alerta cercana y del servicio en primer plano (las publica la 107).

## Fuera de alcance

- Calcular la distancia y el rumbo (105), recibir paquetes (104, 107) y subirlos (106).
- Colores, tipografía y espaciado finales (Figma).
- Estados `CANCELADA / A_SALVO`: **opcionales** (000). Se describen rotulados como opcionales.

## 1. Configuración

`features/nearby/NearbyConfig.kt` y `features/status/StatusConfig.kt` (regla 17):

| Constante | Valor | Para |
|---|---|---|
| `UI_REFRESH_MS` | 1000 | recálculo de los tiempos transcurridos mientras la pantalla está visible |

Las alertas con más de `maxAgeMs` (001) no se muestran.

## 2. Cercanas: lista

### 2.1 Estructura temporal
1. `ScreenScaffold` con el título y un `StatusChip` de la malla.
2. Banners (solo los que apliquen), arriba: malla, ubicación y brújula.
3. Línea de resumen: número de alertas.
4. Lista de tarjetas, una por alerta. Cada tarjeta tiene tres zonas: izquierda (tipo y personas), centro (vulnerables, tiempo y voluntarios) y derecha (flecha y distancia).
5. Sin alertas: `EmptyState`.

### 2.2 Orden
1. Distancia conocida, de menor a mayor.
2. Distancia desconocida, después.
3. Desempate: la más reciente primero y luego `alertKey` (regla 9).

No se muestran las alertas propias (están en la pestaña Enviar alerta). Las resueltas, si STATUS está implementado, van al final en tono apagado.

### 2.3 Variables visibles (por tarjeta)

| Variable | Origen | Formato | Ejemplo | Si falta |
|---|---|---|---|---|
| `type` | 102 | texto del tipo + color temporal (100) | `Atrapado o grave` | — |
| `people` | 102 | regla de personas (100) | `3 personas` | — |
| `vulnerable` | 102 | lista corta | `Niños · Heridos` | se oculta |
| `automatic` | 102 | etiqueta | `Automática` | se oculta |
| `createdAtMs` | 102 | tiempo transcurrido (100) | `hace 5 min` | — |
| `distanceM` | 105, desde mi ubicación | distancia (100) | `120 m` | `—` |
| `relativeBearingDeg` | rumbo hacia la alerta (105) − orientación (`Compass`) | ángulo de la flecha | `45°` | flecha oculta |
| `cardinal` | rumbo absoluto (105) | rumbo cardinal (100) | `NE` | `—` |
| `volunteers` | 102 | entero | `2 en camino` | `Nadie en camino` |
| `iAmGoing` | 102 | etiqueta | `Vas en camino` | se oculta |
| `latestStatus` | 102 (opcional) | etiqueta | `A salvo` | se oculta |

### 2.4 Mensajes

| id | Cuándo | Dónde | Tono | Texto provisional | Acción |
|---|---|---|---|---|---|
| `nearby_title` | siempre | barra superior | — | Alertas cercanas | — |
| `nearby_count` | hay alertas | resumen | — | 5 alertas cerca | — |
| `nearby_empty_title` | lista vacía | estado vacío | — | No hay alertas cercanas | — |
| `nearby_empty_body` | lista vacía y `meshState = RUNNING` | estado vacío | INFO | La app sigue escuchando. Te avisaremos cuando llegue una. | — |
| `nearby_empty_mesh_off` | lista vacía y malla sin funcionar | estado vacío | WARNING | No estás recibiendo alertas: revisa el estado de la malla. | Ir a Estado |
| `nearby_banner_bt_off` | `meshState = BLUETOOTH_OFF` | banner | ERROR | El Bluetooth está apagado: no llegarán alertas nuevas. | Encender |
| `nearby_banner_scan_failed` | `meshState = SCAN_FAILED` | banner | ERROR | Este teléfono no puede escuchar alertas ahora. Reinicia la app. | — |
| `nearby_banner_mesh_stopped` | `meshState = STOPPED` | banner | WARNING | La malla está detenida. | Activar |
| `nearby_banner_no_location` | ubicación sin `FIX` ni `LAST_KNOWN` | banner | WARNING | Sin tu ubicación no podemos calcular distancias. | — |
| `nearby_banner_location_stale` | ubicación antigua (108, `LOCATION_STALE_WARN_MS`) | banner | INFO | Las distancias usan tu ubicación de hace 12 min. | — |
| `nearby_banner_compass_calibrate` | `Compass` con precisión `UNRELIABLE` o `LOW` | banner | WARNING | La brújula necesita calibrarse: mueve el teléfono dibujando un 8. | — |
| `nearby_banner_compass_missing` | el teléfono no tiene brújula | banner | INFO | Este teléfono no tiene brújula: guíate por el rumbo (N, NE, E…). | — |
| `nearby_item_volunteers_none` | `volunteers = 0` | tarjeta | INFO | Nadie en camino | — |
| `nearby_item_volunteers` | `volunteers > 0` | tarjeta | SUCCESS | 2 en camino | — |
| `nearby_item_going` | `iAmGoing` | tarjeta | SUCCESS | Vas en camino | — |
| `nearby_item_automatic` | `automatic` | tarjeta | INFO | Automática | — |
| `nearby_item_resolved_safe` / `_cancelled` | opcional | tarjeta | INFO | A salvo · Cancelada | — |

## 3. Cercanas: detalle

### 3.1 Estructura temporal
1. Encabezado: tipo (texto y color), personas y etiquetas.
2. Bloque de navegación, el más grande: flecha de rumbo, distancia en grande y rumbo cardinal con grados.
3. `SectionCard` "Ayuda": voluntarios en camino y, si aplica, "Vas en camino".
4. `SectionCard` "Detalles": hora, vulnerables, coordenadas, saltos y clave.
5. Acciones, fijas abajo: "Voy en camino" (principal) y "Abrir en Google Maps" (secundaria).

### 3.2 Variables visibles

Además de las de la tarjeta (sección 2.3):

| Variable | Origen | Formato | Ejemplo | Si falta |
|---|---|---|---|---|
| `bearingDeg` | 105 | grados enteros + cardinal | `45° NE` | `—` |
| `myAccuracyM` | 105 | precisión (100) | `Tu ubicación: ±15 m` | `—` |
| `createdAtClock` | 102 | hora (100) | `07:33` | — |
| `coordinates` | `latE7`, `lonE7` | 5 decimales | `-33.50100, -70.65100` | — |
| `hops` | `ttlMax - ttl + 1` con el TTL guardado (101, 102) | entero | `Llegó por 2 saltos` | — |
| `alertKey` | 101 | clave canónica | `a1b2c3d4e5f6-0001` | — |
| `onTheWayError` | resultado de guardar | ver mensajes | `null` | `null` |

### 3.3 Mensajes

| id | Cuándo | Dónde | Tono | Texto provisional | Acción |
|---|---|---|---|---|---|
| `nearby_detail_title` | siempre | barra superior | — | Detalle de la alerta | Volver |
| `nearby_detail_distance` | distancia conocida | navegación | — | 120 m | — |
| `nearby_detail_direction` | rumbo conocido | navegación | — | Hacia el NE (45°) | — |
| `nearby_detail_no_compass_hint` | sin brújula usable | navegación | INFO | Gira hasta mirar al NE. | — |
| `nearby_detail_location_note` | siempre | navegación | INFO | La ubicación de la alerta es la que envió su teléfono al pedir ayuda. | — |
| `nearby_detail_hops` | siempre | detalles | INFO | Llegó por 2 saltos | — |
| `nearby_detail_going_button` | no `iAmGoing` | botón principal | — | Voy en camino | `ConfirmDialog` |
| `nearby_detail_going_confirm` | al tocar "Voy en camino" | diálogo | WARNING | ¿Confirmas que vas en camino? Los demás voluntarios y la persona que pidió ayuda lo verán. No se puede deshacer. | Confirmar · Volver |
| `nearby_detail_going_done` | `iAmGoing` | botón (deshabilitado) | SUCCESS | Vas en camino | — |
| `nearby_detail_going_error` | la 102 falló al guardar | banner | ERROR | No se pudo guardar tu aviso. Inténtalo de nuevo. | Reintentar |
| `nearby_detail_maps_button` | siempre | botón secundario | — | Abrir en Google Maps | abre Maps |
| `nearby_detail_maps_hint` | sin internet (`SyncStatus.online = false`) | bajo el botón | INFO | Google Maps necesita datos para calcular la ruta. Sin internet, guíate con la flecha. | — |
| `nearby_detail_maps_missing` | `MapsLauncher` devuelve `NO_APP` | aviso breve | ERROR | No hay una app de mapas instalada. | — |
| `nearby_detail_resolved` | opcional, alerta resuelta | banner | INFO | Esta persona marcó que está a salvo. | — |

## 4. Estado

### 4.1 Estructura temporal
`ScreenScaffold` con secciones `SectionCard` en este orden:
1. **Malla:** chip de estado, interruptor de la malla y filas `InfoRow`.
2. **Servidor:** chip de internet y filas.
3. **Este teléfono:** `deviceId` y `relayTag`.
4. **Ajustes:** detección de movimiento y "Revisar permisos".
5. **Filtro de depuración**, con `SimulationBadge`: lista de etiquetas bloqueadas, campo para agregar y explicación.

### 4.2 Variables visibles

| Variable | Origen | Formato | Ejemplo | Si falta |
|---|---|---|---|---|
| `meshState` | `MeshStatus` (107) | chip | `Activa` | `Iniciando` |
| `meshEnabled` | `MeshControl` (107) | interruptor | encendido | — |
| `scanning`, `advertising` | 107 | Sí / No | `Sí` | `—` |
| `activeAnnouncements` | 107 (de `GossipEngine.active()`, 103) | entero | `3 anuncios activos` | `0` |
| `neighbors` | 107: `relayTag` distintos en 60 s (`NEIGHBOR_WINDOW_MS` de la 107) | entero | `Teléfonos cerca: 4` | `0` |
| `lastReceivedAtMs` | 107 | tiempo transcurrido | `Último paquete: hace 8 s` | `Ninguno todavía` |
| `storedAlerts`, `storedOnTheWay` | `StorageStats` (102) | enteros | `12 alertas · 5 avisos` | `0` |
| `online` | `SyncStatus` (106) | chip | `Con internet` | `Sin internet` |
| `pendingUploads` | 106 | entero | `4 pendientes de subir` | `0` |
| `lastUploadAtMs` | 106 | hora | `Última subida: 07:36` | `Nunca` |
| `syncError` | 106 | ver mensajes | `NETWORK` | `null` |
| `nextRetryAtMs` | 106 | tiempo restante | `Reintento en 20 s` | se oculta |
| `deviceId`, `relayTag` | 100, 101 | formato de la 100 | `a1b2 c3d4 e5f6` · `e5f6` | — |
| `motionDetectionEnabled` | `SettingsRepository` (102) | interruptor | encendido | encendido |
| `blockedRelayTags` | `SettingsRepository` (102) | lista de etiquetas de 4 hex | `e5f6`, `0b0b` | lista vacía |
| `blockInput`, `blockInputError` | usuario | texto y error | `e5f` → error | — |
| `motionReading` | `MotionSource.observeReadings` (105), solo en depuración | g con 2 decimales y coma, ms enteros | `1,02 g` · `320 ms` | `—` |

### 4.3 Mensajes

| id | Cuándo | Dónde | Tono | Texto provisional | Acción |
|---|---|---|---|---|---|
| `status_title` | siempre | barra superior | — | Estado | — |
| `status_mesh_header` | siempre | malla | — | Malla Bluetooth | — |
| `status_mesh_running` | `RUNNING` | chip | SUCCESS | Activa | — |
| `status_mesh_starting` | `STARTING` | chip | INFO | Iniciando… | — |
| `status_mesh_stopped` | `STOPPED` | chip | WARNING | Detenida | — |
| `status_mesh_bt_off` | `BLUETOOTH_OFF` | chip y banner | ERROR | Bluetooth apagado | Encender |
| `status_mesh_adv_unsupported` | `ADVERTISE_UNSUPPORTED` | banner | ERROR | Este teléfono no puede anunciar por Bluetooth: solo recibirá alertas. | — |
| `status_mesh_adv_failed` | `ADVERTISE_FAILED` | banner | ERROR | No se pudo anunciar por Bluetooth. Reinicia la app. | — |
| `status_mesh_scan_failed` | `SCAN_FAILED` | banner | ERROR | No se pudo escuchar por Bluetooth. Reinicia la app. | — |
| `status_mesh_toggle` | siempre | interruptor | — | Malla activa | encender / apagar |
| `status_mesh_off_confirm` | al apagar | diálogo | WARNING | Si detienes la malla, no recibirás ni retransmitirás alertas. | Detener · Volver |
| `status_server_header` | siempre | servidor | — | Servidor | — |
| `status_online` / `status_offline` | `online` | chip | SUCCESS / WARNING | Con internet · Sin internet | — |
| `status_sync_pending` | `pendingUploads > 0` | fila | INFO | 4 pendientes de subir | — |
| `status_sync_error_network` | `syncError = NETWORK` o `TIMEOUT` | banner | WARNING | No se pudo conectar con el servidor. Reintento en 20 s. | — |
| `status_sync_error_config` | `syncError = CONFIG_MISSING` | banner | ERROR | Falta configurar el servidor en esta compilación. Las alertas se guardan, pero no se suben. | — |
| `status_sync_error_auth` | `syncError = AUTH` (401, 403) | banner | ERROR | El servidor rechazó la clave de la app. Las alertas se guardan, pero no se suben. | — |
| `status_sync_error_rejected` | `syncError = REJECTED` (400, 409) | banner | ERROR | El servidor rechazó un paquete inválido. Se descartó y se sigue con los demás. | — |
| `status_device_header` | siempre | este teléfono | — | Este teléfono | — |
| `status_device_id` | siempre | fila | — | ID: a1b2 c3d4 e5f6 | — |
| `status_relay_tag` | siempre | fila | — | Etiqueta de relay: e5f6 | — |
| `status_settings_header` | siempre | ajustes | — | Ajustes | — |
| `status_motion_toggle` | siempre | interruptor | — | Alerta por movimiento brusco | encender / apagar |
| `status_motion_hint` | siempre | bajo el interruptor | INFO | Si se detecta un golpe o una caída fuerte, la app enviará una alerta tras 15 s si no la cancelas. | — |
| `status_permissions_button` | siempre | botón | — | Revisar permisos | → pantalla de permisos (100) |
| `status_debug_header` | siempre | filtro | SIMULATION | Filtro de depuración · SIMULACIÓN | — |
| `status_debug_explain` | siempre | filtro | SIMULATION | Ignora los anuncios emitidos por estos teléfonos. Sirve para simular distancia en la demo. | — |
| `status_debug_empty` | lista vacía | filtro | — | No hay teléfonos bloqueados. | — |
| `status_debug_add` | siempre | campo + botón | — | Etiqueta (4 caracteres) · Bloquear | agrega |
| `status_debug_remove` | por etiqueta | chip | — | e5f6 ✕ | quita |
| `status_debug_error_format` | el texto no son 4 hexadecimales | bajo el campo | ERROR | Escribe 4 caracteres entre 0-9 y a-f. | — |
| `status_debug_error_duplicate` | la etiqueta ya está | bajo el campo | ERROR | Esa etiqueta ya está bloqueada. | — |
| `status_debug_error_self` | es mi propio `relayTag` | bajo el campo | ERROR | Esa es la etiqueta de este teléfono. | — |
| `status_debug_simulate_shake` | solo en versiones de depuración | filtro | SIMULATION | Simular movimiento brusco | emite un evento de caída por `MotionSource` (105) |
| `status_debug_meter_title` | solo en depuración | filtro | SIMULATION | Medidor de movimiento | — |
| `status_debug_meter_current` | solo en depuración | medidor | — | Ahora: 1,02 g | — |
| `status_debug_meter_peak` | solo en depuración | medidor | — | Pico (2 s): 2,87 g | — |
| `status_debug_meter_freefall` | solo en depuración; hubo caída libre | medidor | — | Última caída libre: 320 ms | — |
| `status_debug_meter_state` | solo en depuración | medidor | INFO | Detector: en reposo · en caída · esperando impacto · en pausa | — |
| `status_debug_meter_events` | solo en depuración | medidor | — | Caídas detectadas: 1 | — |
| `status_debug_meter_hint` | solo en depuración | medidor | INFO | Umbrales: caída libre < 0,4 g durante 300 ms e impacto > 2,5 g antes de 1 s. | — |
| `status_debug_active_banner` | lista no vacía | banner en Cercanas y Estado | SIMULATION | Filtro de depuración activo: se ignoran 2 teléfonos. | Ir a Estado |

## 5. Notificaciones (las publica la 107; el texto se fija aquí)

| id | Cuándo | Título | Texto | Acciones |
|---|---|---|---|---|
| `nearby_notif_new_alert` | `RECEIVED` de un `ALERT` ajeno y no resuelto | Nueva alerta: Atrapado o grave | 3 personas · a 120 m · hace 1 min (sin ubicación: "distancia desconocida") | Abrir (→ detalle) |
| `status_notif_service` | el servicio en primer plano funciona | Senda activo | Escuchando alertas cercanas · 4 teléfonos cerca | Abrir |
| `status_notif_service_bt_off` | servicio con `BLUETOOTH_OFF` | Senda en pausa | Bluetooth apagado: no se reciben alertas. | Abrir |

Canales: "Alertas cercanas" (importancia alta) y "Servicio" (importancia baja).

## 6. Comportamiento

1. "Voy en camino": tras confirmar, el ViewModel llama a `OwnPacketRepository.saveOnTheWay(alertKey, nowMs)` (102), que crea el paquete con `PacketFactory.newOnTheWay` (101) y lo guarda. Es idempotente: un segundo toque no crea otro. La 107 lo anuncia, igual que una alerta propia.
2. La flecha gira `relativeBearingDeg`. Se actualiza con `Compass` y la ubicación, sin leer el reloj en la UI salvo con el `Clock` (100) cada `UI_REFRESH_MS`.
3. Cambiar el filtro de depuración o la detección de movimiento solo escribe en `SettingsRepository` (102). La 107 lo observa y lo aplica (`GossipEngine.setBlockedRelayTags`, 103).
4. Una etiqueta del filtro se normaliza a minúsculas antes de validarla y guardarla.

## 7. Requisitos para otras specs

La 109 se implementa con los fakes de estas interfaces. Cada dueño las incluye en su spec (se registran como `[PIDE]` en la memoria).

| Spec | Interfaz | Debe ofrecer |
|---|---|---|
| 102 | `NearbyRepository` | **resuelto en la 102, secciones 1.2 y 1.7:** `observeNearby()` y `observe(key)` con `NearbyAlertView` |
| 102 | `OwnPacketRepository` | **resuelto en la 102, sección 1.1:** `saveOnTheWay(alertKey, nowMs)` |
| 102 | `SettingsRepository` | **resuelto en la 102, sección 1.6** |
| 102 | `StorageStats` | **resuelto en la 102, sección 1.5** |
| 105 | `LocationSource` | **resuelto en la 105, sección 2:** `highAccuracy()` mientras la lista o el detalle están visibles |
| 105 | `Compass` | **resuelto en la 105, sección 4.** La flecha solo se muestra con `trueNorth = true`; si no, se usa el rumbo cardinal |
| 105 | geo (puro, `:core`) | **resuelto en la 105, sección 3:** `distanceM`, `bearingDeg` (nulo si es el mismo punto) y `relativeBearingDeg` |
| 105 | `MapsLauncher` | **resuelto en la 105, sección 6** |
| 106 | `SyncStatus` | **resuelto en la 106, sección 5:** `state` (`SyncState`) |
| 107 | `MeshStatus` | **resuelto en la 107, sección 2:** `MeshState` |
| 107 | `MeshControl` | **resuelto en la 107, secciones 2 y 8** |
| 107 | notificaciones | **resuelto en la 107, sección 6** |

## Criterios de aceptación

1. La lista sigue el orden de la sección 2.2, sin alertas propias ni alertas con más de `maxAgeMs` (prueba con fakes y `FakeClock`).
2. Cada banner de las secciones 2.4 y 3.3 aparece solo en su condición (pruebas de Compose con fakes de 105, 106 y 107).
3. La flecha usa `rumbo − orientación` y se oculta con `UNAVAILABLE`. Sin brújula usable, se muestra el rumbo cardinal.
4. "Voy en camino" pide confirmación, guarda exactamente un `ON_THE_WAY` con la clave de la alerta y deja el botón en "Vas en camino". Un segundo toque no crea otro.
5. `hops` vale `ttlMax - ttl + 1` (ejemplo: `ttl = 7` da 2 saltos).
6. El filtro rechaza un formato inválido, un repetido y la etiqueta propia con su mensaje, y acepta `E5F6` como `e5f6`.
7. Con el filtro no vacío, el banner `SIMULATION` aparece en Cercanas y en Estado (regla 30).
8. Cada estado de malla y de sincronización de la sección 4 muestra su chip o banner.
9. Cada `id` de las secciones 2.4, 3.3, 4.3 y 5 existe en `strings.xml` (prueba con una lista).
10. Ninguna pantalla de la 109 lee el reloj del sistema.

## Tareas

1. Configuración y textos de `strings.xml`.
2. `UiState`, ViewModel y pantalla de la lista (criterios 1-3).
3. Detalle, "Voy en camino" y Maps (criterios 4 y 5).
4. Pantalla de estado y filtro de depuración (criterios 6-8).
5. Prueba del catálogo de textos (criterio 9).
6. Registrar en `memory/P2.md` los pedidos de la sección 7.
