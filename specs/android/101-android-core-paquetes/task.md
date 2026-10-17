# Tasks · 101 android-core-paquetes

Spec: `101-android-core-paquetes/spec.md` · Plan: `101-android-core-paquetes/plan.md` (si algo choca, manda la spec).
Persona: **P1**. Carpeta: `android/core/src/{main,test}/kotlin/com/securelink/core/protocol/`.
Referencia de bytes: 001, secciones 2-5 y 10.

Al terminar cada task: commit `spec 101: <qué>` y nota en `memory/P1.md`.

## C1 · Contrato (fase 0.2)

- [x] **C1.1** `EmergencyType(code)` (0, 1, 2) y `StatusCode(code)` (1, 2).
- [x] **C1.2** `Vulnerable` con 4 booleanos (`false` por defecto).
- [x] **C1.3** `sealed interface Packet` con `key` y `createdAtMs`; `Alert`, `OnTheWay`, `Status` y `WirePacket(packet, ttl, relayTag)`. Los constructores **no validan**.
- [x] **C1.4** Claves: `PacketKey : Comparable` (compara por `canonical()`), `AlertKey`, `OnTheWayKey`, `StatusKey` y `relayTagOf(deviceId)`.
- [x] **C1.5** `ProtocolConfig` con los valores de la 001, sección 8, y `DEFAULT`.
- [x] **C1.6** Firmas de `PacketCodec` (`encode`, `decode`), `EncodeResult`, `DecodeResult`, `DecodeError`, `FieldError`, `PacketFactory` (4 funciones) y `CreateResult`. Los cuerpos con `TODO("spec 101: C3")` o `TODO("spec 101: C4")`, para que otros compilen.
- [x] **C1.7** `KeyTest`:
  - formas canónicas exactas: `a1b2c3d4e5f6-0001`, `a1b2c3d4e5f6-0001/0c0c0c0c0c0c`, `a1b2c3d4e5f6-0001#2`;
  - ceros a la izquierda (`deviceId = 1`, `counter = 1`);
  - cambiar `ttl` o `relayTag` no cambia la clave, y cambiar un campo inmutable sí (criterio 5);
  - orden por texto.
- [ ] **C1.8** PR "contrato 101" hacia `main`.
  *Depende de:* A1 y A2 (100-A).

## PR fase 1 (a) · lo que necesita P3 (C2-C5 juntos, **primer PR de P1 en la fase 1**)

### C2 · `Hex` y `Mulberry32`
- [x] **C2.1** `Hex.encode` (minúsculas, sin separadores) y `Hex.decode` (lanza `IllegalArgumentException` si no es hex par). Pruebas.
- [x] **C2.2** `Mulberry32(seed: Int).next(): Long` según la 001, sección 7 (`Int * Int`, `ushr`, `toLong() and 0xFFFFFFFFL`), y `seedOf(deviceId)`.
- [x] **C2.3** Prueba con las 4 semillas y las 5 salidas de la 001, sección 7, escritas en el código (criterio 8). Semillas uint32 → `toLong().toInt()`.

### C3 · `PacketCodec`
- [x] **C3.1** `encode`: cabecera (`0xA7`, versión y tipo), campos big-endian según la sección 3 de la 001, bit reservado en 0, `createdAtS = createdAtMs / 1000`.
- [x] **C3.2** Validación de `encode` **en orden de offset**, que devuelve el primer `FieldError`: rangos de la 001, `deviceId` en 48 bits, `createdAtMs` múltiplo de 1000 y ≤ `4294967295000`, y `relayTag` en `0..65535`.
- [x] **C3.3** `decode` en el orden de la 001, sección 5: `NotOurPacket` → `UnsupportedVersion` → `UnknownType` → `InvalidLength` → `InvalidField` (primero por offset). Ignora el bit reservado.
- [x] **C3.4** Pruebas con los 3 hex de la 001, sección 10, escritos en el código: `encode` da el hex exacto y `decode` da los campos y la clave (criterio 1).
- [x] **C3.5** `EncodeValidationTest`: un caso por cada rechazo.
- [x] **C3.6** `RoundTripTest`: 1000 paquetes válidos generados con `Mulberry32(1)`; `decode(encode(w)) == w` (criterio 3).
- [x] **C3.7** Bit reservado en 1: se decodifica sin error y vuelve a codificarse en 0 (criterio 4).
- [x] **C3.8** 10000 entradas aleatorias de 0 a 255 bytes con `Mulberry32(42)`: `encode` y `decode` nunca lanzan (criterio 6).

### C4 · `PacketFactory`
- [x] **C4.1** `newAlert`, `newAutomaticAlert`, `newOnTheWay` y `newStatus` según la sección 6: hora redondeada, `people > 255` → 255, `people < 1` → error, automática = roja con 1 persona y sin vulnerables, `newStatus` solo del origen (`alertDeviceId`), `ttl = ttlMax` y `relayTag` propio; validan igual que `encode`.
- [x] **C4.2** `FactoryTest`: una prueba por regla (criterio 7).

### C5 · Constantes
- [x] **C5.1** Prueba que busca el byte mágico (`0xA7`, `0xa7` o `167`) en `core/src/main` fuera de `ProtocolConfig` (criterio 9).

- [ ] **PR fase 1 (a)** hacia `main`, y aviso en `memory/P1.md`: "codec y fábricas listos" (P3 lo espera).
  *Depende de:* C1.

## C6 · Vectores (cuando P2 fusione `specs/shared/vectors/`)

- [x] **C6.1** `PacketVectorsTest` con `packets-valid.json` (hex, campos y clave) y `packets-invalid.json` (error y campo), leídos desde `vectorsDir`.
- [x] **C6.2** `PrngVectorsTest` con `prng.json` (criterio 2).
- [ ] Si los vectores no existen todavía: marcar C6 `bloqueada` en `memory/P1.md`.
  *Si un vector contradice la 001:* manda la spec. Avisar a P2 con `[PIDE a P2]`.

## Cierre

- [x] `./gradlew :core:test` pasa los criterios 1-10.
- [ ] `memory/P1.md`: 101 `terminada`.
