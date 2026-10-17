# 106 android-sync-supabase

Capa: 3 · Persona: P2
Depende de: 002, 100, 101, 102
Entrega:
- En `:core`, paquete `com.securelink.core.sync`: `SyncEngine`, `SyncStatus` + `FakeSyncStatus`, `RetryPolicy`, el mapeo de paquete a fila, y los puertos `HttpPort` y `ConnectivityPort` con sus fakes.
- En `:app`, paquete `sync`: `OkHttpPort`, `AndroidConnectivityPort`, `UploadWorker` y `SyncScheduler`.

Consume: `UploadQueue` (102); `WirePacket` y `PacketKey` (101); `Clock`, `DeviceIdentity` y la configuración de Supabase (100); contrato de subida (002, sección 6)

## Objetivo

Que **cualquier teléfono que tenga internet suba lo que guardó, sin que nadie abra la app**: sus alertas y las que recibió de otros. Lo hace sin duplicar nada en el servidor (regla 13) y sin gastar batería cuando no hay red.

## Alcance

- Mapeo de los paquetes a las filas de la 002.
- Subida por lotes con `INSERT … ON CONFLICT DO NOTHING` (002, sección 6).
- Detección de internet validado.
- Reintentos con espera exponencial y errores explícitos.
- Aislamiento de una fila rechazada sin bloquear las demás.
- Dos caminos con el mismo motor: el servicio en primer plano (al instante) y WorkManager (respaldo tras reinicios o si el proceso muere).
- `SyncStatus` para la pantalla de estado (109).

## Fuera de alcance

- Ejecutar y mantener vivo el servicio (107). La 106 entrega el motor; la 107 lo arranca.
- Leer del servidor: el teléfono solo sube.
- Iniciar la app sola al encender el teléfono (fuera de alcance en la 100). Tras un reinicio, la subida la hace WorkManager; la malla vuelve cuando se abre la app.

## 1. Configuración

`SyncConfig` en `:core` (regla 17):

| Constante | Valor | Para |
|---|---|---|
| `BATCH_SIZE` | 100 | filas por petición |
| `CONNECT_TIMEOUT_MS`, `READ_TIMEOUT_MS`, `CALL_TIMEOUT_MS` | 10000, 10000, 15000 | tiempos límite de OkHttp (regla 23) |
| `RETRY_BASE_MS` | 5000 | primera espera tras un error de red |
| `RETRY_MAX_MS` | 300000 (5 min) | espera máxima |
| `AUTH_RETRY_MS` | 900000 (15 min) | espera tras un `401` o `403` |
| `WORKER_PERIOD_MIN` | 15 | periodo de WorkManager (el mínimo que permite Android) |

## 2. Del paquete a la fila (puro, `:core/sync`)

```kotlin
// spec 106
enum class Table(val path: String, val onConflict: String) {
    ALERTS("alerts", "device_id,counter"),
    ON_THE_WAY("on_the_way", "alert_device_id,alert_counter,volunteer_device_id"),
    ALERT_STATUS("alert_status", "alert_device_id,alert_counter,status"),
}
data class OutRow(val table: Table, val key: PacketKey, val fields: Map<String, Any>)   // números y booleanos
fun toRow(pending: PendingUpload, uploaderDeviceId: Long): OutRow
```

- Las columnas y los nombres son exactamente los de la 002, sección 3.1: `createdAtMs` va como `created_at_ms`, el TTL guardado como `ttl_at_upload`, y el `deviceId` propio como `uploaded_by_device_id`.
- **Nunca** se envían `received_at_ms` ni `alert_id`: los pone la base, y `anon` no tiene permiso sobre ellos (002).
- `relayTag` no se sube (002).

Ejemplo: el `ALERT` de la 001, sección 10, guardado en el teléfono B (`0b0b0b0b0b0b`) con `ttl = 8`, da esta fila en `alerts`:

```json
{"device_id":177789161760246,"counter":1,"emergency_type":2,"automatic":false,
 "children":true,"elderly":false,"injured":true,"reduced_mobility":false,"people":3,
 "lat_e7":-335000000,"lon_e7":-706500000,"created_at_ms":1760600000000,
 "uploaded_by_device_id":12142057818891,"ttl_at_upload":8}
```

## 3. Puertos

```kotlin
// spec 106 (:core/sync)
sealed interface HttpOutcome {
    data object Created : HttpOutcome                    // 201 (nueva o repetida)
    data class Rejected(val code: Int) : HttpOutcome     // 400, 409
    data class Auth(val code: Int) : HttpOutcome         // 401, 403
    data class ServerError(val code: Int) : HttpOutcome  // 5xx o 429
    data object Timeout : HttpOutcome
    data object NetworkError : HttpOutcome
}
interface HttpPort { suspend fun insertIgnoreDuplicates(table: Table, rows: List<OutRow>): HttpOutcome }
interface ConnectivityPort { val online: StateFlow<Boolean> }   // internet validado
```

`OkHttpPort` (`:app/sync`):
- `POST {SUPABASE_URL}/rest/v1/{table.path}?on_conflict={table.onConflict}` con un arreglo JSON.
- Cabeceras: `apikey: {anon}`, `Authorization: Bearer {anon}`, `Content-Type: application/json`, `Prefer: resolution=ignore-duplicates,return=minimal`.
- Los JSON se construyen con kotlinx-serialization (`JsonObject`), sin texto armado a mano.
- Cualquier excepción de OkHttp se traduce a `Timeout` o `NetworkError`. Nunca se traga (regla 16).

`AndroidConnectivityPort`: `registerDefaultNetworkCallback`. `online` es `true` solo con `NET_CAPABILITY_INTERNET` **y** `NET_CAPABILITY_VALIDATED`; un Wi-Fi sin salida a internet no cuenta.

## 4. Motor

```kotlin
// spec 106 (:core/sync)
/** Lo que la 107 necesita para disparar la subida, sin depender de SyncEngine. */
interface SyncRunner { suspend fun runOnce(): RunResult }
class FakeSyncRunner : SyncRunner { var calls: Int; var result: RunResult }

class SyncEngine(
    queue: UploadQueue, http: HttpPort, identity: DeviceIdentity, clock: Clock,
    configured: Boolean,                 // false si faltan SUPABASE_URL o SUPABASE_ANON_KEY (100)
    config: SyncConfig = SyncConfig(),
) {
    /** Sube hasta vaciar la cola o hasta el primer error. Un único run a la vez (Mutex). */
    suspend fun runOnce(): RunResult
    val status: StateFlow<SyncState>
}
```

### 4.1 Una pasada (`runOnce`)
1. Si `configured = false`: no hace ninguna llamada, y el error es `CONFIG_MISSING`.
2. Pide `queue.pending(BATCH_SIZE)`. Si está vacía, termina con éxito.
3. Agrupa por tabla y sube en este orden: `alerts`, `on_the_way`, `alert_status`. Así el panel suele recibir la alerta antes que sus voluntarios, aunque no es obligatorio (002, sin claves foráneas).
4. Según la respuesta de cada lote:

| Resultado | Acción |
|---|---|
| `Created` | `markUploaded(claves, now)` y sigue |
| `Rejected` con más de una fila | divide el lote en dos mitades y sube cada una, para aislar la fila mala |
| `Rejected` con una sola fila | `markRejected(clave, now)` (102), error `REJECTED`, y sigue con las demás |
| `Auth` | se detiene; error `AUTH`; próximo intento en `AUTH_RETRY_MS` |
| `ServerError`, `Timeout` o `NetworkError` | se detiene; error `NETWORK` o `TIMEOUT`; próximo intento según `RetryPolicy` |

5. Vuelve al paso 2 hasta vaciar la cola.

Es seguro repetir cualquier lote: el servidor ignora los duplicados (002) y `markUploaded` es idempotente (102). Si la app muere entre el `201` y `markUploaded`, el lote se vuelve a subir sin efecto.

### 4.2 Reintentos (`RetryPolicy`, pura)
- Espera exponencial: `min(RETRY_BASE_MS × 2^(n−1), RETRY_MAX_MS)` tras el error número n: 5 s, 10 s, 20 s… hasta 5 min.
- Una pasada con éxito pone n en 0.
- Pasar de sin internet a con internet también pone n en 0 y dispara una pasada de inmediato.

### 4.3 Cuándo corre

| Camino | Cuándo llama a `runOnce` |
|---|---|
| **Servicio en primer plano** (107) | al pasar `online` a `true`; cuando aumenta la cola estando `online`; y cuando vence el reintento. Sin internet, no hace nada: no hay sondeo |
| **WorkManager** (`UploadWorker`) | tarea periódica única (`enqueueUniquePeriodicWork`, `KEEP`) cada `WORKER_PERIOD_MIN` minutos con la condición `NetworkType.CONNECTED`. Sigue programada después de un reinicio del teléfono |

- Los dos usan **la misma instancia** del motor (desde `AppContainer`), protegida por un `Mutex`: si coinciden, la segunda llamada espera a la primera y encuentra la cola ya vacía.
- `SyncScheduler.ensureScheduled()` programa el worker. Se llama al iniciar la app (lo cablea la 107).
- El worker devuelve `Result.success()` aunque haya error de red, porque el periodo ya hace de reintento, y no compite con la espera del servicio.

## 5. `SyncStatus` (109)

```kotlin
// spec 106
enum class SyncError { NETWORK, TIMEOUT, CONFIG_MISSING, AUTH, REJECTED }
data class SyncState(
    val online: Boolean,
    val uploading: Boolean,
    val pendingUploads: Int,            // de UploadQueue.observePendingCount (102)
    val lastUploadAtMs: Long?,          // se guarda en SharedPreferences "sync" para mostrarlo tras reiniciar
    val syncError: SyncError?,          // null tras una pasada con éxito
    val nextRetryAtMs: Long?,
)
interface SyncStatus { val state: StateFlow<SyncState> }
class FakeSyncStatus(initial: SyncState) : SyncStatus { fun set(state: SyncState) }
```

`REJECTED` se muestra hasta la siguiente pasada sin rechazos. No detiene la subida de las demás filas.

## 6. Pedidos a otras specs

| Spec | Pedido |
|---|---|
| 102 | En `UploadQueue`: `markRejected(key, atMs)` y una columna `upload_rejected_at_ms`. Una fila rechazada sale de `pending` y no se reintenta, pero se conserva en el teléfono |
| 100 | Dependencias en `:app`: `okhttp`, `work-runtime-ktx` y el runtime y el plugin de `kotlinx-serialization`, con versión exacta |
| 107 | Crear `SyncEngine` en `AppContainer`, llamar a `ensureScheduled()` al iniciar y ejecutar el motor desde el servicio según la sección 4.3 |

## Criterios de aceptación

1. `toRow` del ejemplo de la sección 2 produce exactamente ese JSON, sin `received_at_ms`, `alert_id` ni `relayTag`.
2. Con fakes de `UploadQueue` y `HttpPort`, 250 paquetes pendientes se suben en lotes de como máximo 100, en orden de tabla, y todos quedan marcados como subidos.
3. Subir dos veces el mismo lote (el servidor responde `201` las dos) deja todo marcado una sola vez y sin errores.
4. Un lote de 8 filas con una fila mala termina con 7 filas subidas, 1 rechazada (`markRejected`) y el error `REJECTED`, con a lo sumo 3 niveles de división (8 → 4 → 2 → 1).
5. `Auth` detiene la pasada y programa el próximo intento en `AUTH_RETRY_MS`. `NetworkError` sigue la secuencia 5 s, 10 s, 20 s… hasta 5 min (prueba con tiempo virtual de `kotlinx-coroutines-test`).
6. Con `configured = false` no hay ninguna llamada HTTP, y el error es `CONFIG_MISSING`.
7. Pasar de sin internet a con internet dispara una pasada inmediata y reinicia la espera.
8. Dos `runOnce` simultáneos no suben el mismo lote dos veces.
9. `SyncState` refleja `online`, `pendingUploads`, `lastUploadAtMs`, `syncError` y `nextRetryAtMs` en cada caso de los criterios anteriores.
10. Prueba real contra el proyecto de la 200 (manual, registrada en `memory/P2.md`):
    - un teléfono sin internet guarda una alerta;
    - se le activa el internet **con la app cerrada** (servicio vivo);
    - la alerta aparece en el panel una sola vez;
    - repetir con otro teléfono que tiene la misma alerta no la duplica.
11. Tras reiniciar un teléfono con paquetes pendientes y **sin abrir la app**, el worker los sube en menos de `WORKER_PERIOD_MIN` + 5 minutos con internet (manual, registrado).

## Limitaciones declaradas

- Tras "Forzar detención" en los ajustes de Android, ni el servicio ni WorkManager funcionan hasta que se abra la app. Android lo impone.
- En modo de ultra ahorro de batería, o con la app restringida por Samsung, WorkManager puede retrasarse mucho (000).
- `uploaded_by_device_id` y `ttl_at_upload` quedan con los valores de la primera subida que llegó al servidor (002), no necesariamente la de este teléfono.

## Tareas

1. `SyncConfig`, `Table`, `toRow`, con sus pruebas (criterio 1).
2. `RetryPolicy`, con sus pruebas (criterio 5, parte pura).
3. `SyncEngine` con puertos falsos (criterios 2-9).
4. `OkHttpPort` y `AndroidConnectivityPort`.
5. `UploadWorker` y `SyncScheduler`.
6. Pruebas reales (criterios 10 y 11), registradas en la memoria.
