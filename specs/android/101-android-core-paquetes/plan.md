# Plan · 101 android-core-paquetes

Spec: `specs/android/101-android-core-paquetes/spec.md` (manda la spec; si este plan la contradice, se corrige el plan).
Persona: **P1**. Contexto: `specs/plan-prioridades-android.md`, fases 0 y 1.
Carpeta: `android/core/src/main/kotlin/com/senda/core/protocol/` y sus pruebas. Nada fuera de ella.

## 1. Por qué el orden importa

La 101 es código puro sin fake. Otros la usan **tal cual**:

| Quién | Qué usa | Desde |
|---|---|---|
| P1 (103, 107) | todo | fase 1 |
| P2 (108, 109) | modelos y claves (`Alert`, `EmergencyType`, `Vulnerable`, `AlertKey`, `StatusCode`) | fase 0 |
| P3 (102, 104) | modelos, `PacketCodec`, `PacketFactory` y `ProtocolConfig` | inicio de la fase 1 |

Por eso va en **dos entregas**: el contrato en la fase 0 y el resto **al inicio de la fase 1**, antes que la 103.

## 2. Pasos

| Paso | Qué | Verificación (criterios de la spec) | Entrega |
|---|---|---|---|
| **C1** · contrato | Modelos (sección 2): `EmergencyType`, `StatusCode`, `Vulnerable`, `Packet`, `Alert`, `OnTheWay`, `Status`, `WirePacket`. Claves (sección 3): `PacketKey`, `AlertKey`, `OnTheWayKey`, `StatusKey`, `canonical()`, orden y `relayTagOf`. `ProtocolConfig` (sección 4). Firmas de `PacketCodec` y `PacketFactory` que devuelven `TODO()`, para que otros compilen | `KeyTest` (criterio 5) y la forma canónica de la 001 (`a1b2c3d4e5f6-0001`, `…/0c0c0c0c0c0c`, `…#2`) | **PR fase 0.2** |
| **C2** · `Hex` y `Mulberry32` | Sección 7 y 8. `seedOf` | salidas de la 001, sección 7, escritas en el código (criterio 8) | PR fase 1 (a) |
| **C3** · `PacketCodec` | `encode` y `decode` (sección 5): orden de errores, primer campo inválido por offset, bit reservado, conversión de hora | ejemplos de la 001, sección 10, en el código (criterio 1); `EncodeValidationTest`; `RoundTripTest` con 1000 paquetes de `Mulberry32(1)` (criterio 3); bit reservado (criterio 4); 10000 entradas aleatorias de `Mulberry32(42)` sin excepciones (criterio 6) | PR fase 1 (a) |
| **C4** · `PacketFactory` | Sección 6: hora redondeada, tope de 255, alerta automática, `newStatus` solo del origen, validación igual que `encode` | `FactoryTest` (criterio 7) | PR fase 1 (a) |
| **C5** · constantes | Prueba que busca el byte mágico fuera de `ProtocolConfig` | criterio 9 | PR fase 1 (a) |
| **C6** · vectores | `PacketVectorsTest` y `PrngVectorsTest` contra `specs/shared/vectors/` (los escribe P2 en la fase 0) | criterio 2 | PR aparte cuando estén los vectores |

**Orden:** C1 → C2 → C3 → C4 → C5 → (C6 cuando P2 fusione los vectores).
**PR fase 1 (a)** lleva C2-C5 juntos, y es el **primero de P1 en la fase 1**: desbloquea a P3.

## 3. Dependencias

| Necesita | De quién | Si no está |
|---|---|---|
| Módulo `:core` compilando | P1 (100-A, A1) | — (es de P1) |
| `DEVICE_ID_MAX` | P1 (100-A, A2) | — |
| Vectores `packets-*.json` y `prng.json` | P2 (fase 0) | C1-C5 no lo necesitan; C6 queda `bloqueada` y se registra |

## 4. Terminado

Pasan los criterios 1-10 (`./gradlew :core:test`), los PR están fusionados, y `memory/P1.md` marca la 101 como `terminada` con el commit `spec 101: …`.

## 5. Riesgos

| Riesgo | Mitigación |
|---|---|
| Un vector de P2 no coincide con los ejemplos de la 001 | Mandan los ejemplos de la spec (calculados y verificados). Se avisa a P2 con `[PIDE a P2]` y se corrige el vector, no el código |
| Desbordes con `Int` y `Long` en 48 bits y uint32 | `RoundTripTest` y los bordes de la sección 5 (máximos de cada campo) |
| P3 empieza la fase 1 antes de que llegue el PR (a) | P3 tiene trabajo que no lo necesita (105 puro, pruebas de contrato de la 102, rotación de la 104); P1 prioriza este PR |
