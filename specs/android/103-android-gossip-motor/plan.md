# Plan · 103 android-gossip-motor

Spec: `specs/android/103-android-gossip-motor/spec.md` (manda la spec; si este plan la contradice, se corrige el plan).
Persona: **P1**. Contexto: `specs/plan-prioridades-android.md`, fase 1.
Carpeta: `android/core/src/main/kotlin/com/securelink/core/gossip/` y sus pruebas (`ScenarioRunner` vive en las pruebas).

## 1. Lugar en el plan

- Empieza **después** del PR fase 1 (a) de la 101 (codec, fábricas y `Mulberry32`).
- Nadie más la usa hasta la 107 (fase 2, también de P1): no bloquea a P2 ni a P3.
- Es el corazón de la tesis: debe quedar **completa** en la fase 1, con los escenarios de referencia pasando.

## 2. Pasos

| Paso | Qué | Verificación (criterios de la spec) |
|---|---|---|
| **G1** · estado y `originate` | `GossipEngine` con `seen`, `pending`, `windows` y `blocked` (sección 3). `originate` según la sección 4.1: duplicado, alta en `seen` y `StartAnnounce` con `originAnnounceWindowMs`; no consume `rng` | `EngineRulesTest`: originar dos veces da `DUPLICATE` |
| **G2** · `onReceived` | Los 6 pasos de la sección 4.2, en orden: `BLOCKED` (sin marcar como visto), `DUPLICATE` **con su `relayTag`**, `Store` + `RECEIVED`, `NO_FORWARD_TTL`, `NO_FORWARD_EXPIRED`, y retardo con `rng.next() mod (forwardDelayMaxMs + 1)` | una prueba por paso; criterio 3 (bloqueado y después recibido por otro relay); criterio 4 (`initiallySeen` da `DUPLICATE`) |
| **G3** · tiempo | `tick` (primero cierra ventanas, luego inicia los reenvíos vencidos en orden `(dueMs, key)`), `nextWakeupMs`, `active`, `setBlockedRelayTags`. `tick` tardío empieza en `now` | criterio 6 |
| **G4** · `ScenarioRunner` | El algoritmo **exacto** de la 001, sección 6.7: cola `(t, kind, node, key)`, despertares con clave vacía, entregas a cada vecino en el mismo `t` y la traza de eventos | `ReferenceScenariosTest`: `line-a-b-c` y `triangle-c-blocks-a` de la 001, sección 10, con retardos de 1220 ms y 1570 ms (criterio 1) |
| **G5** · propiedades | `InvariantsTest`: 200 topologías con `Mulberry32(7)`. `OnTheWayTest`. Prueba de determinismo: misma semilla y entradas = mismos efectos. Prueba que busca `System.` y `Random(` en `gossip/` | criterios 5, 7 y 8 |
| **G6** · vectores | `GossipVectorsTest` contra `specs/shared/vectors/gossip/*.json` (los escribe P2) | criterio 2 |

**Orden:** G1 → G2 → G3 → G4 → G5 → (G6 cuando estén los vectores). Un PR después de G4 (con los escenarios de referencia pasando) y otro con G5 y G6.

## 3. Referencia para verificar G4

Un script de Node (fuera del repositorio) ya reprodujo las dos tablas de la 001, sección 10, con el algoritmo de la sección 6.7. Si el `ScenarioRunner` no da **exactamente** esas secuencias, el error está en la implementación, no en la spec. Puntos a revisar primero:
- Que los anuncios (`kind 0`) se procesen antes que las entregas (`kind 1`) del mismo `t`.
- Que la clave vacía de los despertares vaya antes que cualquier clave.
- Que la semilla uint32 del JSON se pase a `Int` con `toLong().toInt()`.

## 4. Dependencias

| Necesita | De quién | Si no está |
|---|---|---|
| `WirePacket`, claves, `ProtocolConfig`, `Mulberry32`, `relayTagOf`, `PacketCodec` | P1 (101) | empezar G1 con los modelos de C1; G4 necesita el codec (C3) |
| Vectores `gossip/*.json` | P2 | G1-G5 no los necesitan; G6 queda `bloqueada` y se registra |

## 5. Terminado

Pasan los criterios 1-9 (`./gradlew :core:test`), los PR están fusionados, y `memory/P1.md` marca la 103 como `terminada` con el commit `spec 103: …`.

## 6. Riesgos

| Riesgo | Mitigación |
|---|---|
| La secuencia difiere del simulador (300) en el futuro | El algoritmo está en la 001 (contrato compartido); cualquier cambio se hace allí primero |
| `InvariantsTest` falla por la limitación de "primera copia por camino largo" | La invariante solo exige entrega total con `TTL_MAX ≥ teléfonos − 1` (spec, sección 6). No relajarla más |
| `seen` crece sin límite | Limitación aceptada para el MVP (spec) |
