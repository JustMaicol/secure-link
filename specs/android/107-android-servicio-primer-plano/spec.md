# 107 android-servicio-primer-plano

Capa: 3 · Persona: P1
Depende de: 100, 101, 102, 103, 104, 105, 106 (solo interfaces)
Entrega:
- En `:core`, paquete `com.securelink.core.node`: `MeshNode` (coordinador, con la lógica sin Android), `MeshStatus`, `MeshControl` y `AutoAlertController` (interfaces + fakes), `PowerPolicy` (pura), `NodeConfig`, y los puertos `NotificationPort`, `WakeLockPort` y `AppVisibilityPort`.
- En `:app`, paquete `service`: `MeshService` (Foreground Service), notificaciones, wake lock, visibilidad de la app y el cableado en `AppContainer`.

Consume: `GossipEngine` (103); `MeshTransport` (104); `PacketStore`, `OwnPacketRepository`, `SettingsRepository` y `UploadQueue` (102); `LocationSource`, `MotionSource` y geo (105); `SyncEngine`, `SyncScheduler` y `ConnectivityPort` (106); `Clock`, `DeviceIdentity` y `PermissionChecker` (100); `PacketKey`, `Mulberry32`, `seedOf`, `relayTagOf` y `ProtocolConfig` (101)

## Objetivo

Mantener la malla funcionando **siempre, con la app cerrada y con bajo consumo**: recibir, guardar, retransmitir, anunciar lo propio, subir cuando haya internet, avisar de alertas cercanas y lanzar la alerta automática. Es el único lugar donde se juntan el motor, la radio, la base, los sensores y la subida.

## Alcance

- Foreground Service, siempre activo desde que se abre la app hasta que el usuario apaga la malla.
- Bucle del nodo: aplicar los efectos del motor (103) sobre el transporte (104) y la base (102).
- Bandeja de salida: entregar al motor los paquetes propios.
- Ecos de paquetes propios y conteo de teléfonos cercanos.
- Política de energía (ACTIVO / REPOSO) y wake lock solo con anuncios activos.
- Alerta automática: cuenta regresiva, ubicación y guardado.
- Notificaciones: servicio, alertas cercanas, ayuda en camino y cuenta regresiva.
- Disparo de la subida (106).
- `MeshStatus`, `MeshControl` y `AutoAlertController` para las pantallas (108, 109).

## Fuera de alcance

- Reglas de reenvío (103), radio (104), base (102), sensores (105) y protocolo de subida (106).
- Arrancar solo al encender el teléfono: tras un reinicio, la malla vuelve al abrir la app. La subida la cubre WorkManager (106).
- Las pantallas (108, 109). Los textos de las notificaciones vienen de sus catálogos.

## 1. Configuración

`NodeConfig` en `:core/node` (regla 17):

| Constante | Valor | Para |
|---|---|---|
| `ACTIVE_HOLD_MS` | 120000 | tras recibir un paquete, el nodo se queda en modo ACTIVO este tiempo |
| `MODE_MIN_DWELL_MS` | 30000 | tiempo mínimo entre cambios de modo (Android limita los reinicios de escaneo) |
| `NEIGHBOR_WINDOW_MS` | 60000 | "teléfonos cerca": `relayTag` distintos oídos en este tiempo |
| `AUTO_COUNTDOWN_MS` | 15000 | cuenta regresiva de la alerta automática (la 108 la muestra) |
| `AUTO_SAVE_RETRY_MAX` | 3 | reintentos al guardar la alerta automática |
| `WAKELOCK_TIMEOUT_MS` | 70000 | el wake lock se pide siempre con tiempo límite y se renueva mientras haya anuncios |
| `SERVICE_NOTIF_MIN_UPDATE_MS` | 30000 | la notificación del servicio no se actualiza más seguido |
| `ALERT_SOUND_MIN_GAP_MS` | 60000 | solo suena la primera alerta cercana de cada minuto |

## 2. Interfaces para las pantallas (`:core/node`)

```kotlin
// spec 107
enum class MeshHealth { STARTING, RUNNING, STOPPED, BLUETOOTH_OFF, NO_PERMISSION, ADVERTISE_UNSUPPORTED, ADVERTISE_FAILED, SCAN_FAILED }
enum class PowerMode { ACTIVE, IDLE }

data class MeshState(
    val health: MeshHealth,
    val scanning: Boolean, val advertising: Boolean,
    val activeAnnouncements: Int,
    val neighbors: Int,
    val lastReceivedAtMs: Long?,
    val powerMode: PowerMode,
)
interface MeshStatus { val state: StateFlow<MeshState> }
interface MeshControl { val enabled: StateFlow<Boolean>; fun setEnabled(enabled: Boolean) }

sealed interface CountdownState {
    data object Idle : CountdownState
    data class Counting(val remainingMs: Long) : CountdownState
    data object WaitingLocation : CountdownState
    data object Sent : CountdownState
    data object Cancelled : CountdownState
    data object Failed : CountdownState                    // no se pudo guardar tras AUTO_SAVE_RETRY_MAX
}
interface AutoAlertController { val state: StateFlow<CountdownState>; fun cancel() }

// Fakes: FakeMeshStatus, FakeMeshControl, FakeAutoAlertController
```

**`TransportState` (104) → `MeshHealth`**, con la primera condición que se cumpla:

| Orden | Condición | `MeshHealth` |
|---|---|---|
| 1 | malla apagada por el usuario | `STOPPED` |
| 2 | `permissionsOk = false` | `NO_PERMISSION` (la app vuelve a la pantalla de permisos, 100) |
| 3 | `bluetoothOn = false` | `BLUETOOTH_OFF` |
| 4 | `scanning = FAILED` | `SCAN_FAILED` |
| 5 | `advertising = UNSUPPORTED` | `ADVERTISE_UNSUPPORTED` |
| 6 | `advertising = FAILED` | `ADVERTISE_FAILED` |
| 7 | `running` y `scanning = ACTIVE` | `RUNNING` |
| 8 | lo demás | `STARTING` |

## 3. El nodo (`MeshNode`, `:core/node`)

Todo el estado del nodo se toca desde **una sola corrutina** (un despachador con `limitedParallelism(1)`): no hay carreras ni candados (regla 10). El reloj se inyecta (regla 6).

### 3.1 Arranque
1. `seen = packetStore.allKeys()`.
2. `engine = GossipEngine(deviceId, Mulberry32(seedOf(deviceId)), ProtocolConfig.DEFAULT, seen, settings.blockedRelayTags.value)`.
3. `transport.start()` en modo REPOSO, salvo que la política diga ACTIVO.
4. Empiezan las entradas de la sección 3.2.

### 3.2 Entradas y qué hace con cada una

| Entrada | Acción |
|---|---|
| `transport.received` | anota `relayTag` y hora (teléfonos cerca, `lastReceivedAtMs`); `engine.onReceived(wire, now)`; aplica los efectos |
| `packetStore.observePendingOrigination` | por cada paquete propio pendiente: `engine.originate(wire, now)`, aplica los efectos y `markOriginated(key, now, untilMs)` con el `untilMs` de su `StartAnnounce` |
| temporizador en `engine.nextWakeupMs()` | `engine.tick(now)`; aplica los efectos |
| `settings.blockedRelayTags` | `engine.setBlockedRelayTags(...)` |
| `settings.motionDetectionEnabled` | empieza o deja de recolectar `MotionSource.observeShakes()` |
| `MotionSource.observeShakes` | inicia la cuenta regresiva (sección 5) |
| `ConnectivityPort.online`, `UploadQueue.observePendingCount` | dispara la subida (sección 7) |
| visibilidad de la app y anuncios activos | reevalúa el modo de energía (sección 4) |

### 3.3 Aplicar los efectos (en el orden en que los devuelve el motor)

| Efecto | Acción |
|---|---|
| `Store(wire)` | `packetStore.saveReceived(wire, now)`. Si falla, la clave entra en `unsaved` y **no se anuncia** (regla 21); se registra el error. Si se guardó (`INSERTED`), se evalúan las notificaciones (sección 6) |
| `StartAnnounce(wire, until)` | si la clave no está en `unsaved`: se agrega a los anuncios activos y se actualiza `transport.setAnnouncements(...)` |
| `StopAnnounce(key)` | se quita de los anuncios activos y se actualiza el transporte |
| `Event(DUPLICATE, key, relayTag)` | si el paquete es propio y `relayTag != relayTagOf(deviceId)`: `packetStore.recordEcho(key, relayTag)` |
| otros `Event` | solo se registran (depuración) |

**Orden de la lista que recibe el transporte** (104 la corta en `MAX_ACTIVE_ANNOUNCEMENTS`):
1. Paquetes propios, del más reciente al más antiguo.
2. Paquetes ajenos, del anuncio más reciente al más antiguo.
3. Desempate por clave canónica (regla 9).

### 3.4 Teléfonos cerca
`neighbors` = `relayTag` distintos de todos los paquetes recibidos (incluidos duplicados y bloqueados) en los últimos `NEIGHBOR_WINDOW_MS`, sin contar el propio.

## 4. Energía

### 4.1 Política (`PowerPolicy`, pura)

```kotlin
// spec 107
fun desiredMode(appVisible: Boolean, activeAnnouncements: Int, countdownRunning: Boolean,
                lastReceivedAtMs: Long?, nowMs: Long, config: NodeConfig): PowerMode
```

Es `ACTIVE` si se cumple cualquiera de estas condiciones:
- la app está visible;
- hay anuncios activos;
- corre una cuenta regresiva;
- llegó un paquete hace menos de `ACTIVE_HOLD_MS`.

Si no, es `IDLE`. Un cambio de modo se aplica solo si pasaron `MODE_MIN_DWELL_MS` desde el anterior. La única excepción es pasar a `ACTIVE` por una cuenta regresiva o un anuncio propio, que es inmediato.

### 4.2 Qué cambia con cada modo

| Recurso | ACTIVO | REPOSO |
|---|---|---|
| Escaneo BLE (104) | `setScanMode(LOW_LATENCY)` | `setScanMode(BALANCED)`. Cada paquete se re-anuncia durante 60 s (001), así que igual se capta |
| Anuncio BLE (104) | rota los activos | sin anuncios no hay nada que rotar: el anunciante se detiene (104) |
| Wake lock parcial | **solo mientras haya anuncios activos**, con `WAKELOCK_TIMEOUT_MS` y renovado | ninguno: el chip Bluetooth despierta la CPU al recibir |
| GPS (105) | `passive()`; `highAccuracy()` solo durante una cuenta regresiva | `passive()` |
| Acelerómetro (105) | con lecturas agrupadas en lotes de 1 s, si la detección está activada | igual |
| Subida (106) | solo cuando hay internet; sin sondeo | igual |
| Notificación del servicio | se actualiza como mucho cada `SERVICE_NOTIF_MIN_UPDATE_MS` | igual |

### 4.3 Pedidos de energía a otras specs

| Spec | Pedido |
|---|---|
| 104 | `MeshTransport.setScanMode(mode: ScanPowerMode)` con `LOW_LATENCY` y `BALANCED`. Reinicia el escaneo con el modo nuevo, respetando el límite de Android (unos 5 inicios cada 30 s) |
| 105 | Registrar el acelerómetro con `maxReportLatencyUs = 1 s` (lecturas agrupadas por el hardware). El detector recibe las muestras con su marca de tiempo original, así que la detección no cambia; solo se retrasa como mucho 1 s |

## 5. Alerta automática (`AutoAlertController`)

1. Si llega un `ShakeEvent` con la detección activada y el estado en `Idle`, `Cancelled`, `Sent` o `Failed`: el estado pasa a `Counting(AUTO_COUNTDOWN_MS)` y se empieza a recolectar `LocationSource.highAccuracy()`. Los eventos que llegan durante una cuenta en curso se ignoran.
2. Cada segundo: `remainingMs` baja y el teléfono vibra. Si la app no está visible, la notificación de la cuenta regresiva (108, `sendalert_notif_countdown`) se actualiza; su acción "Cancelar" llama a `cancel()`.
3. `cancel()` en `Counting` o `WaitingLocation` → `Cancelled`, sin guardar nada.
4. Al llegar a 0:
   - Con ubicación `Fresh` o `LastKnown`: `ownRepository.saveNewAutomaticAlert(...)` (102) → `Sent`. La alerta entra en la bandeja de salida y el nodo la anuncia (sección 3.2).
   - Sin ubicación: `WaitingLocation`. Se guarda en cuanto llegue una, sin tiempo límite: una persona atrapada puede tardar en tener GPS.
5. Si guardar falla, se reintenta hasta `AUTO_SAVE_RETRY_MAX` veces y luego queda en `Failed` (mensaje `sendalert_auto_failed`, 108).
6. Al enviarse: notificación `sendalert_notif_sent` si la app no está visible. Se deja de recolectar la alta precisión.

## 6. Notificaciones

Canales: **Servicio** (importancia baja) y **Alertas** (importancia alta), con los textos de la 108 y la 109.

| Notificación | Cuándo | Reglas |
|---|---|---|
| Servicio (`status_notif_service`, `status_notif_service_bt_off`) | siempre que el servicio está activo | fija; muestra `neighbors`; versión de pausa con `BLUETOOTH_OFF` |
| Alerta cercana nueva (`nearby_notif_new_alert`) | `Store` con resultado `INSERTED` de un `ALERT` ajeno, no resuelto y con menos de `maxAgeMs` | **una por alerta**, agrupadas (grupo `nearby_alerts` con resumen); la distancia sale de la última ubicación pasiva (105) o dice "distancia desconocida"; solo suena la primera de cada `ALERT_SOUND_MIN_GAP_MS`; no se muestra si la app está visible |
| Ayuda en camino (`sendalert_notif_volunteer`) | `INSERTED` de un `ON_THE_WAY` para una alerta propia | una por alerta propia, que se actualiza con el número de voluntarios |
| Cuenta regresiva y enviada (`sendalert_notif_countdown`, `sendalert_notif_sent`) | sección 5 | solo si la app no está visible |

- Al tocar una notificación se abre `MainActivity` con un destino: el detalle de una alerta (C2, por su clave), "Mi alerta" (E2) o la cuenta regresiva (E3). La 107 publica los extras en `NotificationRoutes` (`app/service`): `route` = `nearby_detail`, `my_alert` o `countdown`, y `alert_key` con la clave canónica. La navegación (100-B, P2) los lee y abre la pantalla. La 107 no edita `MainActivity` ni `navigation/`.
- Sin el permiso `POST_NOTIFICATIONS`, las notificaciones se omiten y se registra el problema (regla 16). La notificación del servicio es obligatoria, y la pantalla de permisos (100) ya la exige.

## 7. Subida (106)

- `SyncEngine` y `SyncScheduler` se crean en `AppContainer`. `ensureScheduled()` se llama en `SecureLinkApp.onCreate`.
- El nodo llama a `SyncRunner.runOnce()` (interfaz de la 106; `SyncEngine` la implementa):
  - cuando `online` pasa a `true`;
  - cuando aumenta `pendingCount` estando `online`;
  - cuando vence `nextRetryAtMs`.
- Las pasadas corren fuera de la corrutina del nodo, para no bloquear la malla. El `Mutex` de la 106 evita que se solapen.

## 8. El servicio Android (`:app/service`)

| Aspecto | Decisión |
|---|---|
| Tipo | `foregroundServiceType="connectedDevice\|location"`. `connectedDevice` por el BLE; `location` por la ubicación de la cuenta regresiva con la app cerrada. **No** se usa `dataSync`, que en Android 15 tiene un límite de horas al día |
| Inicio | `MeshServiceStarter.ensureStarted(context)` (`app/service`), que `MainActivity` llama al pasar la pantalla de permisos: si `meshEnabled` (102) es verdadero, `startForegroundService`, y `startForeground` de inmediato con la notificación del servicio |
| Reinicio | `START_STICKY`: si Android mata el proceso, intenta reiniciarlo solo |
| Detención | `MeshControl.setEnabled(false)` guarda `meshEnabled = false`, detiene el transporte y llama a `stopSelf()`. La subida por WorkManager sigue (106) |
| Manifiesto | la 107 declara `MeshService` en el `AndroidManifest.xml` (es spec de composición) |
| Visibilidad de la app | `ProcessLifecycleOwner` → `AppVisibilityPort` |
| Wake lock | `PowerManager.PARTIAL_WAKE_LOCK`, etiqueta `securelink:announce`, siempre con `acquire(WAKELOCK_TIMEOUT_MS)` |

**Pendiente heredado de `AGENTS.md`:** antes de implementar, revisar la documentación oficial de Android 15 y 16 (y 17 si ya salió) sobre servicios en primer plano, tipos, límites de arranque desde segundo plano y reinicio con `START_STICKY`, y sobre BLE en segundo plano. Lo que cambie se registra como `[HALLAZGO]` en `memory/P1.md` y, si hace falta, se revisa esta spec.

## 9. Pedidos a otras specs

| Spec | Pedido |
|---|---|
| 102 | `SettingsRepository.meshEnabled: StateFlow<Boolean>` (por defecto `true`) y `setMeshEnabled` |
| 104 | `setScanMode` (sección 4.3) |
| 105 | Acelerómetro con lecturas agrupadas (sección 4.3) |
| 108 | `AUTO_COUNTDOWN_MS` pasa a `NodeConfig` de la 107: la 108 solo muestra `remainingMs`. Nuevo mensaje `sendalert_auto_failed` para el estado `Failed` |
| 109 | `NEIGHBOR_WINDOW_MS` pasa a `NodeConfig` de la 107: la 109 solo muestra `neighbors` |

## Criterios de aceptación

Con `MeshNode` y fakes de todas las interfaces (JVM, con tiempo virtual):

1. Un paquete recibido nuevo se guarda **antes** de anunciarse. Si guardar falla, no se anuncia.
2. Un paquete propio de la bandeja se entrega al motor, se anuncia, y `markOriginated` recibe el `untilMs` de su ventana.
3. Un `DUPLICATE` de un paquete propio con un `relayTag` ajeno llama a `recordEcho`. Con el `relayTag` propio, no.
4. `neighbors` cuenta los `relayTag` distintos de los últimos 60 s, sin el propio.
5. Los anuncios llegan al transporte con los propios primero, y `StopAnnounce` los quita.
6. Al arrancar con claves guardadas, esas claves dan `DUPLICATE` y no se reenvían.
7. Cambiar `blockedRelayTags` en los ajustes llega al motor sin reiniciar el nodo.
8. `PowerPolicy` y el nodo:
   - con la app oculta, sin anuncios, sin cuenta regresiva y sin recepciones en 2 min → `IDLE` y `BALANCED`;
   - un anuncio propio → `ACTIVE` inmediato y wake lock tomado;
   - sin anuncios → wake lock liberado;
   - nunca dos cambios de modo en menos de `MODE_MIN_DWELL_MS`, salvo pasar a `ACTIVE` por algo propio.
9. Cuenta regresiva:
   - un `ShakeEvent` da `Counting(15000)` y luego baja de a segundos;
   - `cancel()` da `Cancelled` y no guarda nada;
   - al llegar a 0 con ubicación, guarda una alerta automática y da `Sent`;
   - sin ubicación da `WaitingLocation`, y guarda en cuanto llega una;
   - un fallo de guardado repetido da `Failed`.
10. Notificaciones: una por alerta ajena nueva; ninguna por las propias, las resueltas, las viejas o con la app visible; solo suena una por minuto; "Ayuda en camino" se actualiza con el número de voluntarios.
11. `TransportState` se traduce a `MeshHealth` según la tabla de la sección 2.
12. La subida se dispara al pasar a con internet y al aumentar la cola estando con internet.

En dispositivo (manual, registrado en `memory/P1.md`):

13. Con 3 Samsung y el filtro de depuración (000, criterio 1), una alerta de A llega a C con **las tres apps cerradas y las pantallas apagadas**.
14. Con la app cerrada, una alerta cercana nueva produce su notificación, y al tocarla se abre su detalle.
15. **Consumo:** se mide el porcentaje de batería gastado en 1 hora en REPOSO con la pantalla apagada, y en 1 hora con anuncios activos. Los resultados y el modelo se registran. El objetivo de consumo se acuerda con el equipo a partir de esta primera medición.
16. El servicio sigue activo tras quitar la app de recientes. Tras "Forzar detención", no (limitación de Android, registrada).

## Limitaciones declaradas

- En REPOSO, el escaneo equilibrado puede tardar algunos segundos más en captar un paquete. La ventana de 60 s lo compensa.
- Tras un reinicio del teléfono, la malla no vuelve hasta que se abre la app. La subida sí (WorkManager, 106).
- Los anuncios abiertos se pierden si el proceso muere (103).
- Samsung puede restringir la app si el usuario no sigue los pasos de la pantalla de permisos (100).

## Tareas

1. Revisar la documentación oficial de Android 15, 16 y 17 sobre servicios en primer plano y BLE en segundo plano, y registrar los hallazgos.
2. Interfaces, fakes, `NodeConfig` y `PowerPolicy`, con sus pruebas (criterio 8, parte pura).
3. `MeshNode`: arranque, entradas, efectos, ecos, vecinos y orden de anuncios (criterios 1-7 y 11).
4. `AutoAlertController` (criterio 9).
5. Notificaciones y destinos de navegación (criterios 10 y 14).
6. Disparo de la subida (criterio 12).
7. `MeshService`, manifiesto, wake lock, visibilidad y `AppContainer`.
8. Pruebas en dispositivo: demo con apps cerradas, notificaciones, consumo y recientes (criterios 13-16).
