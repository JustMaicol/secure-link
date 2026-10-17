# Plan · 104 android-transporte-ble

Spec: `specs/android/104-android-transporte-ble/spec.md` (manda la spec; si este plan la contradice, se corrige el plan).
Persona: **P3**. Contexto: `specs/plan-prioridades-android.md`, fases 0, 1 y 2.
Carpetas: `core/**/mesh/`, `app/**/mesh/ble/` y `app/src/debug/**/mesh/` (pantalla de depuración de la malla).

## 1. Lugar en el plan

| Entrega | Fase | Para qué |
|---|---|---|
| **Prototipo de 1 hora** | 0.1 | decidir si se sigue con BLE o se pasa a Nearby **antes** de construir la malla |
| **Contrato** | 0.2 | P1 programa la 107 contra `FakeMeshTransport` |
| **Lógica sin radio** | 1 | rotación y `BleController` probados en la JVM |
| **Radio real** | 2 | hito **H2**: alerta A → B → C |

## 2. Pasos

### Fase 0.1 · prototipo de 1 hora (proyecto aparte, no se fusiona)

| Paso | Qué | Verificación |
|---|---|---|
| **T0** · prototipo | Proyecto Android mínimo **fuera del repositorio** (o en una rama que no se fusiona), con dos botones (anunciar y escanear) y una lista de recibidos. Los payloads son los **hex de la 001, sección 10**, escritos a mano: no espera a la 101. `AdvertisingSet` legacy, no conectable y no escaneable, con `addManufacturerData(0xFFFF, payload)` y rotación cada 500 ms entre 10 payloads. Escaneo con el filtro de la sección 4 y `SCAN_MODE_LOW_LATENCY` | P1-P4 de la spec, sección 7, con 2 Samsung (sin ADB, con el APK instalado) |

- **Resultado:** se registra como `[HALLAZGO]` en `memory/P3.md`, con los modelos, Android, One UI y el resultado de P1-P4.
- **Si falla P1** (`DATA_TOO_LARGE`): se avisa con `[DECISIÓN]` a todo el equipo **ese mismo momento**. Hay que achicar la 001 (por ejemplo, quitar el `relayTag`) y eso cambia la 101, la 103, la 109 y los vectores.
- **Si fallan P2 o P3:** se avisa con `[DECISIÓN]` y se reescribe la 104 para Nearby (sección 8) antes de la fase 2. El contrato no cambia.

### Fase 0.2 · contrato

| Paso | Qué | Verificación |
|---|---|---|
| **T1** · contrato | `MeshTransport` (con `setScanMode`), `ScanPowerMode`, `ReceivedPacket`, `TransportState`, `AdvertisingState`, `ScanningState`, `FakeMeshTransport` y `BleConfig` (secciones 1 y 6) | compila; P1 puede inyectar recepciones y cambiar el estado en pruebas |

### Fase 1 · lógica sin radio (JVM)

| Paso | Qué | Verificación (criterios de la spec) |
|---|---|---|
| **T2** · rotación | `AnnouncementRotation` (sección 2): turnos de 500 ms, orden, tope de 32 y `update` a mitad de ronda | criterio 2 |
| **T3** · controlador | `BleController` con `AdvertiserPort` y `ScannerPort` (sección 5): estados, reintentos por código de error, Bluetooth apagado y encendido, `SecurityException`, repetidos con `INBOUND_DEDUP_WINDOW_MS`, errores de decodificación (usa `PacketCodec` de la 101) y `setScanMode` con `SCAN_MODE_CHANGE_MIN_MS` | criterios 3, 4, 5 y 9 con puertos falsos |
| **T4** · constantes | Prueba de que las constantes solo están en `BleConfig` | criterio 8 |

### Fase 2 · radio real

| Paso | Qué | Verificación (criterios de la spec) |
|---|---|---|
| **T5** · puertos reales | `AdvertiserPort` con `startAdvertisingSet` y `setAdvertisingData`; `ScannerPort` con `ScanFilter` y `ScanSettings` (secciones 3 y 4); `BleMeshTransport`; receptor de `ACTION_STATE_CHANGED` | la app anuncia y recibe |
| **T6** · pantalla de depuración | Llevar el prototipo T0 a `app/src/debug/**/mesh/`, ahora con `BleController` y los puertos reales (sección 7): anunciar N paquetes de prueba (con `PacketFactory`), y ver recibidos, hex, RSSI y contadores | se instala con el APK de depuración, sin ADB |
| **T7** · prueba en teléfonos | Dos Samsung con la app completa y la pantalla apagada: un paquete de uno aparece en `received` del otro | criterio 7, registrado en `memory/P3.md` |

**Orden:** T0 → T1 (fase 0) → T2 → T3 → T4 (fase 1) → T5 → T6 → T7 (fase 2).

## 3. Dependencias

| Necesita | De quién | Cuándo | Si no está |
|---|---|---|---|
| Nada | — | T0 | es independiente a propósito |
| Esqueleto que compila | P1 (100-A, A1) | T1 | T1 espera a A1 |
| `WirePacket`, `ProtocolConfig` | P1 (101, C1) | T1 | pedir con `[PIDE a P1]` |
| `PacketCodec`, `PacketFactory` reales | P1 (101, PR fase 1 a) | T3 (decodificar) y T6 | T2 avanza sin ellos; T3 deja para el final la parte de decodificación |
| `Clock`, `PermissionChecker` + fakes | P1 (100-A), P2 (100-B, B1) | T3 | pedir con `[PIDE a P1]` o `[PIDE a P2]` |
| Que la 107 llame a `setScanMode` y a `setAnnouncements` | P1 (107) | fase 2-3 | no bloquea a la 104 |

## 4. Terminado

Criterios 1-9, PR fusionados, y `memory/P3.md` marca la 104 como `terminada` con el commit `spec 104: …`.

## 5. Riesgos

| Riesgo | Mitigación |
|---|---|
| Android agrega el AD de flags y el payload de 27 bytes no cabe | Es justo lo que mide P1 del prototipo, en la primera hora |
| El filtro con máscara no funciona con la pantalla apagada en Samsung | P2 del prototipo; alternativa: filtrar solo por company ID y descartar el resto con `decode` (más CPU) |
| Un modelo de Samsung no soporta anunciar | `ADVERTISE_UNSUPPORTED`: ese teléfono solo recibe (estado válido). Registrar el modelo |
| Android limita los reinicios de escaneo | `setScanMode` respeta `SCAN_MODE_CHANGE_MIN_MS`; el escaneo no se reinicia para rotar |
