# 104 android-transporte-ble

Capa: 2 · Persona: P3 (reparto "prioridad Android"; antes P1)
Depende de: 001, 100, 101
Entrega: `MeshTransport` + `FakeMeshTransport` y `AnnouncementRotation` (puro), en `:core`, paquete `com.senda.core.mesh`; `BleMeshTransport` en `:app`, paquete `mesh/ble`; prototipo de 1 hora en `src/debug`
Consume: `WirePacket`, `PacketCodec` y `ProtocolConfig` (101); `Clock` (100); `PermissionChecker` (100)

## Objetivo

Mover los paquetes de la 001 por el aire con anuncios BLE y escaneo, sin conexiones, detrás de una interfaz que la 107 usa sin saber si debajo hay BLE o Nearby Connections. Y comprobar en la **primera hora** que esto funciona en los Samsung de la demo.

## Alcance

- Prototipo de 1 hora con criterio de continuar o cambiar de transporte.
- Interfaz `MeshTransport` y su fake.
- Rotación de los paquetes activos en un único anuncio.
- Escaneo continuo con filtro por fabricante y decodificación con la 101.
- Estado del transporte (Bluetooth, permisos, anuncio y escaneo) con errores explícitos y reintentos acotados.
- Filtrado de reportes idénticos repetidos.

## Fuera de alcance

- Qué paquetes anunciar y cuándo: lo decide el motor (103) y lo aplica la 107. La 104 solo emite la lista que recibe.
- Deduplicación por clave (103) y servicio en primer plano (107).
- Pedir permisos (100).
- Nearby Connections: solo se describe como respaldo (sección 8). Si el prototipo falla, se escribe en una revisión de esta spec.

## 1. Interfaz (`:core`, paquete `mesh`)

```kotlin
// spec 104
interface MeshTransport {
    val state: StateFlow<TransportState>
    /** Paquetes recibidos y decodificados sin error (101). */
    val received: Flow<ReceivedPacket>
    fun start()
    fun stop()
    /** Reemplaza la lista de paquetes a anunciar, en orden de preferencia. Lista vacía = no anunciar. */
    fun setAnnouncements(wires: List<WirePacket>)
    /** Cambia el modo de escaneo (política de energía de la 107). Reinicia el escaneo respetando el límite de Android. */
    fun setScanMode(mode: ScanPowerMode)
}

enum class ScanPowerMode { LOW_LATENCY, BALANCED }

data class ReceivedPacket(val wire: WirePacket, val rssi: Int, val receivedAtMs: Long)

data class TransportState(
    val running: Boolean,
    val bluetoothOn: Boolean,
    val permissionsOk: Boolean,
    val advertising: AdvertisingState,   // IDLE, ACTIVE, UNSUPPORTED, FAILED(code)
    val scanning: ScanningState,         // OFF, ACTIVE, FAILED(code)
    val decodeErrors: Long,              // anuncios con 0xFFFF que la 101 rechazó
)

class FakeMeshTransport : MeshTransport {
    // Guarda lo último que recibió setAnnouncements, permite inyectar recepciones y cambiar el estado.
    fun emit(packet: ReceivedPacket)
    fun setState(state: TransportState)
    val lastAnnouncements: List<WirePacket>
}
```

`ADVERTISE_UNSUPPORTED` con `scanning = ACTIVE` es un estado válido: el teléfono solo recibe. La 107 traduce `TransportState` al `MeshState` de la 108 y la 109.

## 2. Rotación (`:core`, pura)

```kotlin
// spec 104
class AnnouncementRotation(private val config: BleConfig) {
    fun update(wires: List<WirePacket>)        // corta a MAX_ACTIVE_ANNOUNCEMENTS, conserva el orden
    /** Paquete que corresponde emitir en nowMs, o null si no hay ninguno. */
    fun current(nowMs: Long): WirePacket?
}
```

- Ronda por turnos (round-robin): cada paquete ocupa un turno de `ROTATION_SLOT_MS` en el orden recibido, y al terminar la lista vuelve a empezar.
- Si `update` llega a mitad de una ronda, la siguiente ronda empieza en el turno siguiente con la lista nueva. Un paquete que sigue en la lista no pierde su lugar relativo.
- Con N paquetes activos, cada uno vuelve cada `N × ROTATION_SLOT_MS` (10 paquetes = 5 s).
- Si llegan más de `MAX_ACTIVE_ANNOUNCEMENTS`, se quedan los primeros. La 107 ordena la lista: primero los propios, después los más recientes.

## 3. Anuncio BLE (`:app`, `mesh/ble`)

| Parámetro | Valor |
|---|---|
| API | `BluetoothLeAdvertiser.startAdvertisingSet` con **un** `AdvertisingSet` |
| `AdvertisingSetParameters` | `setLegacyMode(true)`, `setConnectable(false)`, `setScannable(false)`, intervalo `ADVERTISE_INTERVAL`, potencia `TX_POWER` |
| `AdvertiseData` | solo `addManufacturerData(0xFFFF, payload)`; sin nombre del dispositivo ni potencia TX |
| Cambio de paquete | `AdvertisingSet.setAdvertisingData(...)` en cada turno, sin detener el anuncio |
| Sin paquetes | se detiene el `AdvertisingSet` y se vuelve a iniciar cuando haya alguno |

`payload` es el resultado de `PacketCodec.encode` (101): hasta 27 bytes. Los 31 bytes del anuncio legacy son 27 de payload + 2 del company ID + 1 de tipo + 1 de longitud: **no queda espacio para el AD de flags**. El prototipo verifica que Android no lo agregue en un anuncio no conectable (sección 7, criterio P1).

Errores de `onAdvertisingSetStarted` y `onAdvertisingDataSet`:

| Código de Android | Estado | Reacción |
|---|---|---|
| `ADVERTISE_FAILED_FEATURE_UNSUPPORTED` | `UNSUPPORTED` | no se reintenta; el teléfono solo recibe |
| `ADVERTISE_FAILED_DATA_TOO_LARGE` | `FAILED(code)` | no se reintenta. Es un `[HALLAZGO]` que bloquea: hay que cambiar la 001 |
| `ADVERTISE_FAILED_TOO_MANY_ADVERTISERS`, `ADVERTISE_FAILED_INTERNAL_ERROR` | `FAILED(code)` | hasta `ADVERTISE_RETRY_MAX` reintentos, con `ADVERTISE_RETRY_DELAY_MS` entre ellos |
| `ADVERTISE_FAILED_ALREADY_STARTED` | se ignora | — |

## 4. Escaneo BLE (`:app`, `mesh/ble`)

| Parámetro | Valor |
|---|---|
| `ScanFilter` | `setManufacturerData(0xFFFF, [0xA7, 0x10], [0xFF, 0xF0])` (001, sección 1). Obligatorio: sin filtro, Android no escanea con la pantalla apagada |
| `ScanSettings` | modo según `setScanMode` (por defecto `SCAN_MODE_ACTIVE`), `CALLBACK_TYPE_ALL_MATCHES`, `reportDelay = 0`, `setLegacy(true)` |
| Lectura | `scanRecord.getManufacturerSpecificData(0xFFFF)` → `PacketCodec.decode` |
| Escaneo | continuo desde `start()` hasta `stop()`. No se detiene ni se reinicia para rotar: Android limita a unos 5 inicios cada 30 s |

- Un `decode` con error suma a `decodeErrors` y se descarta. No es un fallo del transporte.
- **Reportes repetidos:** el mismo payload (mismos bytes) recibido de nuevo dentro de `INBOUND_DEDUP_WINDOW_MS` se descarta. Si cambian los bytes (por ejemplo, otro `relayTag` o TTL), se emite. Así llegan a la 107 las retransmisiones de otros teléfonos, que necesita para detectar ecos y contar vecinos.
- `receivedAtMs` sale del `Clock` (100).

Errores de `onScanFailed`:

| Código | Reacción |
|---|---|
| `SCAN_FAILED_SCANNING_TOO_FREQUENTLY` | `FAILED(code)`; se reintenta tras `SCAN_RETRY_DELAY_MS` |
| `SCAN_FAILED_FEATURE_UNSUPPORTED` | `FAILED(code)`; no se reintenta |
| otros (`INTERNAL_ERROR`, `APPLICATION_REGISTRATION_FAILED`, `OUT_OF_HARDWARE_RESOURCES`) | `FAILED(code)`; hasta `SCAN_RETRY_MAX` reintentos |
| `SCAN_FAILED_ALREADY_STARTED` | se ignora |

## 5. Ciclo de vida y estado

- `start()`: comprueba los permisos (`PermissionChecker`, 100) y el adaptador. Si falta algo, el estado lo refleja (`permissionsOk = false` o `bluetoothOn = false`) y no se lanza ninguna excepción.
- Un `BroadcastReceiver` de `BluetoothAdapter.ACTION_STATE_CHANGED`: al apagarse el Bluetooth, `bluetoothOn = false` y se liberan el anuncio y el escaneo; al encenderse, se reanudan solos si `running`.
- Un `SecurityException` de Android (permiso revocado en caliente) se captura, pone `permissionsOk = false` y se registra. Nunca se traga en silencio (regla 16).
- `stop()` libera el anuncio, el escaneo y el receptor.
- La lógica de estado, reintentos y repetidos va en una clase sin Android (`BleController`) que habla con dos puertos: `AdvertiserPort` y `ScannerPort`. `BleMeshTransport` los implementa con la API real. Así, casi todo se prueba en la JVM con puertos falsos.

## 6. Configuración

`BleConfig` en `:core`, paquete `mesh` (regla 17):

| Constante | Valor |
|---|---|
| `ROTATION_SLOT_MS` | 500 |
| `ADVERTISE_INTERVAL` | `AdvertisingSetParameters.INTERVAL_MIN` (160 = 100 ms; la `:app` lo traduce) |
| `TX_POWER` | `TX_POWER_MEDIUM` |
| `SCAN_MODE_ACTIVE`, `SCAN_MODE_IDLE` | `SCAN_MODE_LOW_LATENCY`, `SCAN_MODE_BALANCED` (los elige la 107 con `setScanMode`) |
| `SCAN_MODE_CHANGE_MIN_MS` | 6000 (no más de 5 reinicios de escaneo cada 30 s) |
| `MAX_ACTIVE_ANNOUNCEMENTS` | 32 |
| `INBOUND_DEDUP_WINDOW_MS` | 1000 |
| `ADVERTISE_RETRY_MAX`, `ADVERTISE_RETRY_DELAY_MS` | 3, 2000 |
| `SCAN_RETRY_MAX`, `SCAN_RETRY_DELAY_MS` | 3, 30000 |

## 7. Prototipo de 1 hora (se hace primero)

Una `Activity` de depuración en `app/src/debug/` (no entra en la versión de producción) que usa `BleController` con los puertos reales:
- Botón para anunciar una lista de N paquetes de prueba (hechos con `PacketFactory`, 101).
- Lista de lo recibido: hex, RSSI y hora.
- Contadores: recibidos, descartados por repetidos y errores de decodificación.

Criterios para **seguir con BLE**:

| # | Prueba | Se cumple si |
|---|---|---|
| P1 | Anunciar un `ALERT` de 27 bytes | inicia sin `DATA_TOO_LARGE` |
| P2 | Receptor con la **pantalla apagada** y el filtro de la sección 4 | recibe; los bytes son idénticos al hex enviado |
| P3 | 2 Samsung a 10 m; el emisor rota 10 paquetes a 500 ms | el receptor recibe al menos 9 de los 10 en menos de 10 s |
| P4 | Un anuncio ajeno con `0xFFFF` y otro byte mágico | no llega como paquete (filtro o `decodeErrors`) |

- Resultado, modelos, versión de Android y de One UI se registran en `memory/P1.md` como `[HALLAZGO]`.
- **Si falla P1:** se detiene el trabajo y se avisa con `[DECISIÓN]`: hay que achicar el paquete en la 001 (por ejemplo, quitar el `relayTag` y resolver el filtro de demo de otra forma).
- **Si fallan P2 o P3:** se pasa a Nearby Connections (sección 8), detrás de la misma interfaz.

## 8. Respaldo: Nearby Connections (solo si el prototipo falla)

Esquema para la revisión de la spec, sin detalle todavía:
- Estrategia `P2P_CLUSTER`: cada teléfono anuncia y descubre a la vez, y acepta conexiones automáticamente.
- `setAnnouncements` envía los mismos bytes de la 001 como `Payload.fromBytes` a cada endpoint conectado, con la misma rotación.
- Lo recibido pasa por `PacketCodec.decode`, igual que con BLE.
- La 103 y la 107 no cambian. Requiere agregar la dependencia `play-services-nearby` (pedido a la 100) y revisar los permisos.

## Criterios de aceptación

1. Pasan P1-P4 del prototipo, o queda registrada la decisión de cambiar a Nearby (manual, en `memory/P1.md`).
2. `AnnouncementRotation`: con 10 paquetes, cada uno sale una vez cada 5 s; respeta el orden y el tope; un `update` a mitad de ronda no repite ni salta paquetes que siguen en la lista (JVM, con tiempos fijos).
3. `BleController` con puertos falsos:
   - cada código de error produce su estado y su política de reintento;
   - el Bluetooth apagado y luego encendido reanuda solo;
   - un `SecurityException` pone `permissionsOk = false`.
4. Repetidos: los mismos bytes dentro de `INBOUND_DEDUP_WINDOW_MS` se emiten una sola vez; bytes distintos, o los mismos pasado ese tiempo, se emiten.
5. Un payload que la 101 rechaza suma a `decodeErrors` y no se emite.
6. `FakeMeshTransport` permite a la 107 probar todo sin radio.
7. En dos Samsung reales con la app completa y la pantalla apagada, un paquete anunciado por uno aparece en `received` del otro (manual, registrado en `memory/P1.md`).
8. Las constantes solo están en `BleConfig`.
9. `setScanMode` reinicia el escaneo con el modo pedido y nunca reinicia dos veces en menos de `SCAN_MODE_CHANGE_MIN_MS`: un cambio pedido antes de tiempo se aplica al cumplirse el plazo (prueba con puertos falsos).

## Limitaciones declaradas

- Anunciar no confirma la recepción: la fiabilidad depende de repetir (ventanas de la 001 y rotación).
- Con muchos paquetes activos, cada uno vuelve con menos frecuencia: con 32, cada 16 s.
- El escaneo en baja latencia gasta batería. Por eso la 107 lo usa solo en modo ACTIVO y pasa a `BALANCED` en reposo.
- El alcance real (de 10 a 50 m) depende del lugar y de los obstáculos; no se puede garantizar.

## Tareas

1. **Prototipo** (sección 7): `BleController` mínimo, puertos reales y la `Activity` de depuración. Pruebas P1-P4 y registro del resultado. Decidir si se sigue con BLE o se pasa a Nearby.
2. `MeshTransport`, `TransportState`, `FakeMeshTransport` y `BleConfig` en `:core` (criterios 6 y 8).
3. `AnnouncementRotation`, con sus pruebas (criterio 2).
4. `BleController` completo con puertos falsos: estados, reintentos y repetidos (criterios 3-5).
5. `BleMeshTransport` con la API real y el receptor del estado del Bluetooth.
6. Prueba en dos teléfonos con la app (criterio 7).
