# Tasks · 104 android-transporte-ble

Spec: `104-android-transporte-ble/spec.md` · Plan: `104-android-transporte-ble/plan.md` (si algo choca, manda la spec).
Persona: **P3**. Carpetas: `core/**/mesh/`, `app/**/mesh/ble/` y `app/src/debug/**/mesh/`.

Al terminar cada task: commit `spec 104: <qué>` (salvo T0, que no se fusiona) y nota en `memory/P3.md`.

## T0 · Prototipo de 1 hora (fase 0.1, **primero, no espera a nadie**)

Proyecto Android aparte, **fuera del repositorio** (o en una rama que no se fusiona). Se instala el APK en 2 Samsung, sin ADB.

- [ ] **T0.1** Proyecto mínimo, `minSdk 34`, con una pantalla: botones "Anunciar" y "Escanear", y una lista de recibidos (hex, RSSI y hora) con contadores.
- [ ] **T0.2** Pedir en tiempo de ejecución `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT` y `ACCESS_FINE_LOCATION`.
- [ ] **T0.3** Diez payloads escritos a mano: el `ALERT` de la 001, sección 10 (`a711a1b2c3d4e5f600012a03ec084e40d5e3aa6068f09fc008e5f6`), cambiando a mano el contador (bytes 8-9: `0001` … `000a`).
- [ ] **T0.4** Anunciante: `startAdvertisingSet` con `setLegacyMode(true)`, `setConnectable(false)`, `setScannable(false)`, `INTERVAL_MIN` y `TX_POWER_MEDIUM`; `AdvertiseData` solo con `addManufacturerData(0xFFFF, payload)`; rotación cada 500 ms con `setAdvertisingData`. Mostrar el `status` de cada callback.
- [ ] **T0.5** Escáner: `ScanFilter.setManufacturerData(0xFFFF, [0xA7, 0x10], [0xFF, 0xF0])`, `SCAN_MODE_LOW_LATENCY` y `CALLBACK_TYPE_ALL_MATCHES`; mostrar `getManufacturerSpecificData(0xFFFF)` en hex.
- [ ] **T0.6** Pruebas (001 y 104, sección 7):
  - [ ] **P1:** el anuncio de 27 bytes inicia **sin** `ADVERTISE_FAILED_DATA_TOO_LARGE`.
  - [ ] **P2:** receptor con la **pantalla apagada** (al encenderla, la lista muestra lo recibido): los bytes coinciden exactamente con el hex enviado.
  - [ ] **P3:** a 10 m, de los 10 paquetes rotados llegan al menos 9 en menos de 10 s.
  - [ ] **P4:** un anuncio con byte mágico `0xA8` desde el otro teléfono no aparece en la lista.
- [ ] **T0.7** Registrar en `memory/P3.md` como `[HALLAZGO]`: modelos, Android, One UI y resultado de P1-P4.
- [ ] **T0.8** **Decisión:**
  - Todo pasa → seguir con BLE (`[DECISIÓN]` en `memory/P3.md`).
  - Falla P1 → `[DECISIÓN]` **inmediata** a todo el equipo: hay que achicar la 001. Se detiene la malla hasta decidir.
  - Fallan P2 o P3 → `[DECISIÓN]`: la 104 se reescribe para Nearby (sección 8) antes de la fase 2.

## T1 · Contrato (fase 0.2, después del esqueleto A1)

- [ ] **T1.1** En `core/.../mesh/`: `MeshTransport` (con `state`, `received`, `start`, `stop`, `setAnnouncements` y `setScanMode`), `ScanPowerMode`, `ReceivedPacket`, `TransportState`, `AdvertisingState` y `ScanningState`.
- [ ] **T1.2** `FakeMeshTransport`: guarda `lastAnnouncements` y el último `ScanPowerMode`; `emit(packet)` y `setState(state)`.
- [ ] **T1.3** `BleConfig` con las constantes de la sección 6 (incluidos `SCAN_MODE_ACTIVE`, `SCAN_MODE_IDLE` y `SCAN_MODE_CHANGE_MIN_MS`).
- [ ] **T1.4** PR "contrato 104" y aviso en `memory/P3.md` (P1 lo usa en la 107).
  *Depende de:* A1 (100-A) y C1 (101).

## T2 · Rotación (fase 1, JVM)

- [ ] **T2.1** `AnnouncementRotation(config)` con `update(wires)` (tope de `MAX_ACTIVE_ANNOUNCEMENTS`, conserva el orden) y `current(nowMs)` (turnos de `ROTATION_SLOT_MS`).
- [ ] **T2.2** Pruebas con tiempos fijos (criterio 2): con 10 paquetes, cada uno sale una vez cada 5 s; respeta el orden y el tope; un `update` a mitad de ronda no repite ni salta paquetes que siguen en la lista; lista vacía → `null`.

## T3 · `BleController` (fase 1, JVM con puertos falsos)

- [ ] **T3.1** Puertos `AdvertiserPort` y `ScannerPort`, con sus fakes de prueba.
- [ ] **T3.2** Estado: `start`/`stop`, `permissionsOk` (desde `PermissionChecker`), `bluetoothOn`, `advertising` y `scanning`.
- [ ] **T3.3** Errores y reintentos de anuncio y escaneo según las tablas de las secciones 3 y 4 (`ADVERTISE_RETRY_*`, `SCAN_RETRY_*`).
- [ ] **T3.4** Bluetooth apagado → libera; encendido → reanuda solo si `running`. `SecurityException` → `permissionsOk = false`, registrado.
- [ ] **T3.5** Recepción: `PacketCodec.decode` (101). Error → `decodeErrors++`. Mismos bytes dentro de `INBOUND_DEDUP_WINDOW_MS` → descartado.
- [ ] **T3.6** `setScanMode`: reinicia el escaneo con el modo pedido, nunca dos veces en menos de `SCAN_MODE_CHANGE_MIN_MS` (un cambio anticipado se aplica al cumplirse el plazo).
- [ ] **T3.7** Pruebas: criterios 3, 4, 5 y 9.
  *Depende de:* T1. T3.5 necesita el codec real (PR fase 1 a de P1): dejarlo para el final de T3.

## T4 · Constantes

- [ ] **T4.1** Prueba: las constantes de la radio solo aparecen en `BleConfig` (criterio 8).

## T5 · Radio real (fase 2)

- [ ] **T5.1** `AdvertiserPort` real: un `AdvertisingSet` legacy no conectable; `setAdvertisingData` en cada turno; se detiene sin paquetes.
- [ ] **T5.2** `ScannerPort` real: filtro y ajustes de la sección 4; lectura con `getManufacturerSpecificData(0xFFFF)`.
- [ ] **T5.3** `BleMeshTransport` que une `BleController`, los puertos, `AnnouncementRotation` y un receptor de `BluetoothAdapter.ACTION_STATE_CHANGED`.

## T6 · Pantalla de depuración de la malla (fase 2)

- [ ] **T6.1** `Activity` en `app/src/debug/**/mesh/` (no entra en producción): anunciar N paquetes de prueba (con `PacketFactory`) y ver recibidos, hex, RSSI y contadores (recibidos, repetidos descartados, errores de decodificación).

## T7 · Prueba en teléfonos (fase 2)

- [ ] **T7.1** Dos Samsung con la app completa y la pantalla apagada: un paquete anunciado por uno aparece en `received` del otro (criterio 7). Registrar en `memory/P3.md`.

## Cierre

- [ ] Criterios 1-9.
- [ ] `memory/P3.md`: 104 `terminada`.
