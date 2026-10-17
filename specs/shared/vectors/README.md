# Vectores de prueba (`specs/shared/vectors/`)

Contratos compartidos entre Android (Kotlin) y el simulador (JavaScript). Los dos
implementan el **mismo** protocolo (spec 001) y las **mismas** reglas de prioridad y
zonas (spec 003); estos archivos son la única fuente de verdad que ambos deben pasar
(`ARCHITECTURE.md`, sección 3). Si un vector cambia, cambia su spec.

Dueño: **P2** (fase prioridad Android, `specs/plan-prioridades-android.md`, paso 0.1).

## Convenciones

- Los números van como enteros JSON (48 bits caben en un número de JavaScript).
- Los bytes van como texto hexadecimal **en minúscula y sin separadores**.
- Todo tiempo es UTC.
- El hex de un paquete es **solo el payload** (lo que va después del company ID); la
  envoltura BLE de la spec 001, sección 1, no entra.

### `createdAtS` contra `createdAtMs`

- En los **paquetes** (spec 001) el campo es `createdAtS`: segundos epoch, tal como
  viaja en el aire (4 bytes, uint32). `decode` lo convierte a `createdAtMs = createdAtS * 1000`.
- En **prioridad y zonas** (spec 003) el campo es `createdAtMs`: epoch en milisegundos,
  como en `alert_overview` (spec 002).

## Spec 001 · protocolo y paquetes

### `prng.json`

Lista de `{ "seed", "outputs" }` con los primeros 5 valores de `mulberry32` (spec 001,
sección 7). Semillas: `1`, `42`, `185273099` (`0x0B0B0B0B`) y `202116108` (`0x0C0C0C0C`).

### `packets-valid.json`

Lista de `{ "name", "packet", "hex", "key" }`:

- `encode(packet) == hex` y `decode(hex) == packet`.
- `key` es la forma canónica de la clave de deduplicación (spec 001, sección 4).
- `packet` usa los nombres de campo de la spec 001, sección 3. Para `ALERT`, el byte
  `info` se despliega en sus componentes (`emergencyType`, `automatic`, `children`,
  `elderly`, `injured`, `reducedMobility`). Las tres formas son:

```json
{ "type": "ALERT", "deviceId", "counter", "emergencyType", "automatic",
  "children", "elderly", "injured", "reducedMobility", "people",
  "latE7", "lonE7", "createdAtS", "ttl", "relayTag" }

{ "type": "ON_THE_WAY", "alertDeviceId", "alertCounter", "volunteerDeviceId",
  "createdAtS", "ttl", "relayTag" }

{ "type": "STATUS", "alertDeviceId", "alertCounter", "status",
  "createdAtS", "ttl", "relayTag" }
```

Cubre los 3 ejemplos de la spec 001, sección 10, más: alerta automática, todos los
vulnerables, `people = 255`, latitud y longitud en los extremos y `ttl = 1`.

### `packets-invalid.json`

Lista de `{ "name", "hex", "error", "field?", "reencodedHex?", "note?" }`:

- `error` es el nombre del error de la spec 001, sección 5: `NOT_OUR_PACKET`,
  `UNSUPPORTED_VERSION`, `UNKNOWN_TYPE`, `INVALID_LENGTH` o `INVALID_FIELD`.
- `field` solo aparece con `INVALID_FIELD`.
- Entrada especial: el **bit reservado en 1** de `info` es **válido y se ignora**
  (criterio 4 de la 101). Se marca con `"error": "NONE"` y trae `reencodedHex`, el hex
  que resulta de volver a codificar (`encode(decode(hex))`), con el bit otra vez en 0.

### `gossip/*.json`

Escenario de reenvío (spec 001, sección 9 y 6.7; spec 103, sección 5):

- `nodes`: `{ "id", "deviceId", "seed" }` (`seed` es uint32).
- `links`: pares de ids sin orden.
- `blockedRelayTags`: `{ "<id de nodo>": [relayTag...] }`.
- `config`: sobrescribe constantes de la spec 001, sección 8, solo para el escenario.
- `nowMs` y `events`: `{ "t", "node", "originate": { "hex" } }`.
- `expected`: secuencia completa y ordenada. Campos: `t`, `event`, `node` y `key`
  siempre; `ttl` solo en `ANNOUNCE` y `RECEIVED`; `relayTag` (entero) solo en `BLOCKED`.

Archivos: `line-a-b-c` y `triangle-c-blocks-a` (los de la spec 001, sección 10),
`ttl-exhausted`, `expired-alert` y `two-origins-same-time` (desempate).

## Spec 003 · prioridad y zonas

### `priority.json`

`{ "nowMs", "alerts", "expected": { "priorities", "order" } }`.

`alerts[i]` usa la forma de entrada de la spec 003, sección 1: `alertId`,
`emergencyType`, `people`, `children`, `elderly`, `injured`, `reducedMobility`,
`latE7`, `lonE7`, `createdAtMs`, `volunteers`, `latestStatus`.

`expected.priorities` es un objeto `alertId -> { tier, points, resolved }` y
`expected.order` la lista de `alertId` en orden. Cubre el ejemplo de la sección 8, más
topes de personas, vulnerables, espera y voluntarios, hora futura y `alertId` repetido.

### `cells.json`

Lista de `{ "latE7", "lonE7", "cellId" }` con los bordes de la spec 003, sección 8
(incluidos los negativos).

### `zones.json`

`{ "nowMs", "alerts", "expected" }`. Cada zona esperada trae `cellId`, `cellY`,
`cellX`, `alertCount`, `peopleSum`, `volunteersSum`, `maxTier`, `topAlertId` y el estilo
resuelto (`color`, `opacityPercent`). Cubre el ejemplo de la sección 8, un caso de cada
tramo de opacidad y un `alertId` repetido en una zona.
