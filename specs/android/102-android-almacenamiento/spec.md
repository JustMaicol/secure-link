# 102 android-almacenamiento

Capa: 2 · Persona: P3 (reparto "prioridad Android"; antes P2)
Depende de: 001, 100, 101
Entrega: interfaces `OwnPacketRepository`, `NearbyRepository`, `PacketStore`, `UploadQueue`, `StorageStats` y `SettingsRepository`, con sus fakes, en `:core`, paquete `com.senda.core.storage`; implementación con Room y `SharedPreferences` en `:app`, paquete `storage`
Consume: `Alert`, `OnTheWay`, `Status`, `WirePacket`, `PacketKey`, `PacketFactory` y `ProtocolConfig` (101); `Clock` y `DeviceIdentity` (100)

## Objetivo

Guardar en el teléfono, antes que cualquier otra cosa (regla 21), todo paquete propio o recibido. Y dar a cada spec exactamente la vista que necesita: las pantallas (108, 109), el servicio (107), la subida (106) y el motor al reiniciar (103).

## Alcance

- Base Room con una tabla por tipo de paquete y una de ecos.
- Asignación del contador de alertas propias en la misma transacción que la guarda.
- Guardado idempotente de lo recibido.
- "Bandeja de salida" de paquetes propios que la 107 debe anunciar.
- Cola de subida para la 106.
- Vistas derivadas: voluntarios, "voy en camino" propio y último estado.
- Ajustes en `SharedPreferences`.
- Esquema exportado y migraciones explícitas desde la versión 1.

## Fuera de alcance

- Decidir qué se reenvía (103), anunciar (104, 107) o subir (106).
- Borrar datos viejos: en el MVP no se borra nada.
- Validar el texto del filtro de depuración: lo hace la 109. La 102 guarda la lista ya validada.

## 1. Interfaces (`:core`, paquete `storage`)

Todas las funciones que escriben son `suspend` y devuelven un resultado explícito (regla 16). Las lecturas continuas son `Flow`.

```kotlin
// spec 102
sealed interface StorageError {
    data object StorageFailed : StorageError                 // error de SQLite u otro de disco
    data object CounterExhausted : StorageError              // contador > 65535 (101)
    data class InvalidField(val field: String) : StorageError // la 101 rechazó un campo
}
sealed interface StorageResult<out T> {
    data class Ok<T>(val value: T) : StorageResult<T>
    data class Err(val error: StorageError) : StorageResult<Nothing>
}
```

### 1.1 `OwnPacketRepository` (108, 109)

```kotlin
data class AlertDraft(
    val type: EmergencyType, val vulnerable: Vulnerable, val people: Int,
    val latE7: Int, val lonE7: Int, val accuracyM: Int, val fixTimeMs: Long,
)

interface OwnPacketRepository {
    /** Asigna el contador, crea el paquete con PacketFactory.newAlert y lo guarda, todo en una transacción. */
    suspend fun saveNewAlert(draft: AlertDraft, nowMs: Long): StorageResult<WirePacket>
    /** Igual, con PacketFactory.newAutomaticAlert (lo usa la 107 al terminar la cuenta regresiva). */
    suspend fun saveNewAutomaticAlert(latE7: Int, lonE7: Int, accuracyM: Int, fixTimeMs: Long, nowMs: Long): StorageResult<WirePacket>
    /** Idempotente: si ya existe mi aviso para esa alerta, devuelve el existente. */
    suspend fun saveOnTheWay(alert: AlertKey, nowMs: Long): StorageResult<WirePacket>
    /** Opcional (STATUS). Solo para alertas propias. */
    suspend fun saveStatus(alert: AlertKey, status: StatusCode, nowMs: Long): StorageResult<WirePacket>
    fun observeOwnAlerts(): Flow<List<OwnAlertView>>      // más reciente primero
}
```

### 1.2 `NearbyRepository` (109)

```kotlin
interface NearbyRepository {
    fun observeNearby(): Flow<List<NearbyAlertView>>      // alertas ajenas, sin orden: ordena la 109
    fun observe(key: AlertKey): Flow<NearbyAlertView?>
}
```

### 1.3 `PacketStore` (107, 103)

```kotlin
enum class ReceiveOutcome { INSERTED, ALREADY_PRESENT }

interface PacketStore {
    /** Guarda un paquete recibido. Idempotente por clave: la primera copia gana. */
    suspend fun saveReceived(wire: WirePacket, receivedAtMs: Long): StorageResult<ReceiveOutcome>
    /** Todas las claves guardadas: initiallySeen del motor al arrancar (103, sección 4.5). */
    suspend fun allKeys(): StorageResult<List<PacketKey>>
    /** Paquetes propios todavía no entregados al motor, en orden de creación. */
    fun observePendingOrigination(): Flow<List<WirePacket>>
    suspend fun markOriginated(key: PacketKey, originatedAtMs: Long, announceUntilMs: Long): StorageResult<Unit>
    /** Otro teléfono retransmitió un paquete propio (DUPLICATE con relayTag ajeno, 103). Idempotente por (clave, relayTag). */
    suspend fun recordEcho(key: PacketKey, relayTag: Int): StorageResult<Unit>
}
```

### 1.4 `UploadQueue` (106)

```kotlin
data class PendingUpload(val wire: WirePacket, val ttlStored: Int)

interface UploadQueue {
    /** Paquetes (propios y recibidos) que este teléfono aún no subió, por receivedAtMs y clave. */
    suspend fun pending(limit: Int): StorageResult<List<PendingUpload>>
    suspend fun markUploaded(keys: List<PacketKey>, uploadedAtMs: Long): StorageResult<Unit>
    /** El servidor rechazó la fila (400/409, 106). Sale de pending y no se reintenta; se conserva en el teléfono. */
    suspend fun markRejected(key: PacketKey, rejectedAtMs: Long): StorageResult<Unit>
    fun observePendingCount(): Flow<Int>
}
```

### 1.5 `StorageStats` (109)

```kotlin
data class Stats(val alerts: Int, val onTheWay: Int, val statuses: Int)
interface StorageStats { fun observe(): Flow<Stats> }
```

### 1.6 `SettingsRepository` (107, 109)

```kotlin
interface SettingsRepository {
    val motionDetectionEnabled: StateFlow<Boolean>        // por defecto: true
    fun setMotionDetectionEnabled(enabled: Boolean)
    val blockedRelayTags: StateFlow<Set<Int>>             // por defecto: vacío
    fun setBlockedRelayTags(tags: Set<Int>)
    val meshEnabled: StateFlow<Boolean>                   // por defecto: true (107)
    fun setMeshEnabled(enabled: Boolean)
}
```

### 1.7 Vistas

```kotlin
data class OwnAlertView(
    val alert: Alert,
    val accuracyM: Int, val fixTimeMs: Long,              // ubicación usada al enviar (108)
    val originatedAtMs: Long?, val announceUntilMs: Long?,
    val echoCount: Int,                                   // relayTag distintos que la retransmitieron
    val uploadedAtMs: Long?,
    val volunteers: Int,
    val latestStatus: StatusCode?,
)

data class NearbyAlertView(
    val alert: Alert,
    val ttl: Int,                                         // TTL de la primera copia recibida (saltos en 109)
    val receivedAtMs: Long,
    val volunteers: Int,
    val iAmGoing: Boolean,
    val latestStatus: StatusCode?,
)
```

- `volunteers`: número de filas de `on_the_way` de esa alerta. La clave primaria ya impide repetidos (regla 22).
- `iAmGoing`: existe un `on_the_way` de esa alerta con `volunteerDeviceId = deviceId` propio.
- `latestStatus`: el estado con mayor `created_at_ms`, desempatando por `status` (igual que la 002).
- "Propio": un `ALERT` cuyo `deviceId` es el mío; un `ON_THE_WAY` cuyo voluntario soy yo; un `STATUS` de una alerta mía.

### 1.8 Fakes

`FakeOwnPacketRepository`, `FakeNearbyRepository`, `FakePacketStore`, `FakeUploadQueue`, `FakeStorageStats` y `FakeSettingsRepository`, en memoria y deterministas, en `:core`. Un único `InMemoryStorage` puede implementar todas las interfaces a la vez, para que las pruebas de la 107 vean un estado coherente. **No usa `PacketFactory`:** arma el `WirePacket` directamente con las mismas reglas (hora redondeada a segundos, tope de 255 personas, `ttl = ttlMax`, `relayTag` propio), así funciona antes de que exista la fábrica real de la 101. Las mismas pruebas de contrato (sección 6) corren contra el fake y contra Room y garantizan que se comportan igual.

## 2. Esquema Room (`:app`, `storage`)

Base `secure_link.db`, versión 1. Nombres en inglés y `snake_case` (regla 34). Tiempos en epoch ms y coordenadas en E7 (regla 11).

| Tabla | Clave primaria | Columnas |
|---|---|---|
| `alerts` | `(device_id, counter)` | `emergency_type`, `automatic`, `children`, `elderly`, `injured`, `reduced_mobility`, `people`, `lat_e7`, `lon_e7`, `created_at_ms`, `ttl`, `first_relay_tag`, `is_own`, `received_at_ms`, `own_accuracy_m` (nulo si es ajena), `own_fix_time_ms` (nulo si es ajena), `originated_at_ms`, `announce_until_ms`, `uploaded_at_ms`, `upload_rejected_at_ms` |
| `on_the_way` | `(alert_device_id, alert_counter, volunteer_device_id)` | `created_at_ms`, `ttl`, `first_relay_tag`, `is_own`, `received_at_ms`, `originated_at_ms`, `announce_until_ms`, `uploaded_at_ms`, `upload_rejected_at_ms` |
| `alert_status` | `(alert_device_id, alert_counter, status)` | las mismas que `on_the_way` |
| `echoes` | `(packet_key, relay_tag)` | `first_heard_at_ms` |

- Índices: `on_the_way (alert_device_id, alert_counter)` y `alert_status (alert_device_id, alert_counter)`. Además, un índice en `uploaded_at_ms` de cada tabla de paquetes, para la cola de subida.
- `ttl` y `first_relay_tag` son los de la **primera** copia: una copia posterior no los cambia (`INSERT OR IGNORE`).
- Un paquete propio se guarda con `ttl = ttlMax`, `received_at_ms = created_at_ms` y `originated_at_ms = null`.
- `packet_key` en `echoes` es la clave canónica de la 101.
- Sin claves foráneas entre tablas: un `ON_THE_WAY` puede llegar antes que su `ALERT` (igual que en la 002).

## 3. Reglas

1. **Contador:** dentro de `saveNewAlert` y `saveNewAutomaticAlert`, en una transacción: `counter = max(counter de alerts propias) + 1`, o `1` si no hay ninguna. Si pasa de 65535, el resultado es `CounterExhausted` y no se guarda nada.
2. **Crear y guardar es atómico:** si `PacketFactory` (101) devuelve error, el resultado es `InvalidField(campo)` y no se guarda nada.
3. **Recepción idempotente:** `saveReceived` usa `INSERT OR IGNORE`. Recibir la misma clave dos veces devuelve `ALREADY_PRESENT` y no cambia nada.
4. **Un paquete propio que vuelve por la malla** (mi alerta retransmitida por otro): `saveReceived` devuelve `ALREADY_PRESENT`. El eco lo registra la 107 con `recordEcho`.
5. **Bandeja de salida:** todo paquete propio nace con `originated_at_ms = null` y aparece en `observePendingOrigination` hasta que la 107 llama a `markOriginated`.
6. **Subida:** `pending` devuelve los paquetes con `uploaded_at_ms = null` y `upload_rejected_at_ms = null`, primero los más antiguos (`received_at_ms`, luego la clave canónica, regla 9). `markUploaded` es idempotente.
7. **Errores de SQLite:** se capturan y se devuelven como `StorageFailed`, con registro del detalle. Nunca se tragan (regla 16).
8. La 102 nunca lee el reloj: `nowMs` y los tiempos llegan como parámetro (regla 6).

## 4. Ajustes

- `SharedPreferences` con el nombre `settings`: `motion_detection` (booleano, por defecto `true`), `mesh_enabled` (booleano, por defecto `true`) y `blocked_relay_tags` (conjunto de textos hexadecimales de 4 caracteres en minúscula).
- Los `StateFlow` se actualizan con un `OnSharedPreferenceChangeListener`.
- Un valor guardado inválido (texto que no es hexadecimal) se ignora al leer y se registra.

## 5. Migraciones

- `exportSchema = true`, con los esquemas en `android/app/schemas/`, versionados en git (la 100 configura el argumento de KSP `room.schemaLocation`).
- **Prohibido** `fallbackToDestructiveMigration`: una actualización el día del rally no puede borrar alertas.
- Cada cambio de esquema sube la versión, agrega una `Migration` y una prueba con `MigrationTestHelper` (`room-testing`) que migra una base con datos y verifica que no se pierde nada.

## 6. Pruebas

| Prueba | Dónde | Qué verifica |
|---|---|---|
| Contrato de repositorios | `:core` (fake) y `androidTest` (Room en memoria) | las reglas de la sección 3, con las mismas pruebas para ambas implementaciones |
| Contador | ambas | 1, 2, 3…; `CounterExhausted` en 65536 sin guardar nada |
| Concurrencia del contador | `androidTest` | 20 `saveNewAlert` en paralelo dan contadores 1-20 sin repetir |
| Vistas | ambas | `volunteers`, `iAmGoing`, `latestStatus` y `echoCount`, incluido un `ON_THE_WAY` guardado antes que su alerta |
| Esquema | `androidTest` | el esquema v1 exportado existe y coincide con las entidades |
| Ajustes | `androidTest` | valores por defecto, persistencia y emisión del `StateFlow` |

## Criterios de aceptación

1. `saveNewAlert` asigna contadores consecutivos desde 1, guarda el paquete que devolvería `PacketFactory.newAlert` (101) y lo deja pendiente en `observePendingOrigination`.
2. Al pasar de 65535 devuelve `CounterExhausted`; ante un campo inválido devuelve `InvalidField`, y en ambos casos no guarda nada.
3. `saveReceived` dos veces con la misma clave y distinto TTL devuelve `INSERTED` y luego `ALREADY_PRESENT`, y conserva el TTL de la primera copia.
4. `markOriginated` saca el paquete de la bandeja y llena `originatedAtMs` y `announceUntilMs` en `OwnAlertView`.
5. `recordEcho` con dos `relayTag` distintos da `echoCount = 2`; repetir uno no suma.
6. `NearbyAlertView` calcula `volunteers`, `iAmGoing` y `latestStatus` como en la sección 1.7, aunque el `ON_THE_WAY` haya llegado antes que la alerta.
7. `saveOnTheWay` dos veces para la misma alerta guarda un solo aviso.
8. `pending` y `markUploaded` cumplen la regla 6 de la sección 3, y `observePendingCount` refleja los cambios.
9. Las pruebas de contrato pasan igual con el fake (JVM) y con Room (dispositivo).
10. No hay `fallbackToDestructiveMigration`, y el esquema v1 está exportado y versionado.
11. Los ajustes vuelven con sus valores por defecto en una instalación nueva y persisten al reiniciar la app.

## Tareas

1. Interfaces, vistas, resultados y fakes en `:core` (criterio 9, parte JVM).
2. Pruebas de contrato reutilizables para fake y Room.
3. Entidades, DAO y base Room v1 con el esquema exportado (criterio 10).
4. Repositorios Room: transacción del contador, guardado de recibidos, bandeja de salida, ecos y cola de subida (criterios 1-8).
5. `SettingsRepository` con `SharedPreferences` (criterio 11).
6. Pruebas en dispositivo, incluida la concurrencia del contador.
