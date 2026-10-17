# 001 protocolo y paquetes

Capa: 0 · Redacta: P1 · Revisan: P2, P3
Depende de: 000
Entrega: formato binario de los paquetes, reglas de reenvío, generador con semilla, configuración del protocolo y formato de los vectores de prueba (`specs/shared/vectors/`)
Consume: ninguna

## Objetivo

Fijar byte a byte los paquetes que viajan por la malla y las reglas con que cada teléfono los reenvía, de modo que Android (101, 103, 104) y el simulador (300) produzcan exactamente el mismo resultado con los mismos vectores (`CONSTITUTION.md`, regla 15).

## Alcance

- Envoltura BLE del anuncio y filtro de escaneo.
- Paquetes `ALERT`, `ON_THE_WAY` y `STATUS` (este último reservado; opcional según la spec 000).
- Identificadores, claves de deduplicación y su forma canónica en texto (la usan la spec 002 y el panel).
- Reglas de codificación, decodificación y errores.
- Reglas de reenvío: TTL, retardo con semilla, ventana de re-anuncio, caducidad y filtro de depuración.
- Generador pseudoaleatorio `mulberry32`.
- Formato y conjunto mínimo de vectores de prueba.

## Fuera de alcance

- Rotación de la cola de anuncios, ciclo de trabajo y API de Android (spec 104).
- Almacenamiento (102) y subida a Supabase (106, 002).
- Firmas, cifrado y autenticación del origen (trabajo futuro, spec 000).
- Pérdida de paquetes, movilidad y RSSI en los vectores: los vectores modelan enlaces ideales (ver sección 6.6).

## 1. Envoltura BLE

| Elemento | Valor |
|---|---|
| Tipo de anuncio | Legacy, **no conectable** (sin AD de flags) |
| Estructura AD | Una sola: Manufacturer Specific Data (`0xFF`) |
| Company ID | `0xFFFF` (reservado por el Bluetooth SIG para pruebas), little-endian en el aire como exige BLE |
| Payload útil | Máximo **27 bytes** (31 − 1 longitud − 1 tipo AD − 2 company ID) |
| Filtro de escaneo | Manufacturer data `0xFFFF`, datos `[0xA7, 0x10]`, máscara `[0xFF, 0xF0]` (byte mágico y versión 1) |

Todo lo que sigue describe el **payload** (lo que va después del company ID). Los campos multibyte del payload son **big-endian**.

## 2. Cabecera común (2 bytes)

| Offset | Tamaño | Campo | Valor |
|---|---|---|---|
| 0 | 1 | `magic` | `0xA7` fijo |
| 1 | 1 | `versionAndType` | bits 7-4: versión (`1`); bits 3-0: tipo de paquete |

Tipos de paquete:

| Código | Nombre en código | Nombre en la UI | Tamaño |
|---|---|---|---|
| `0x1` | `ALERT` | Alerta | 27 bytes |
| `0x2` | `ON_THE_WAY` | Voy en camino | 23 bytes |
| `0x3` | `STATUS` | Cancelada / A salvo (opcional) | 18 bytes |

Los códigos `0x0` y `0x4`-`0xF` quedan reservados.

## 3. Paquetes

### 3.1 `ALERT` (27 bytes)

| Offset | Tamaño | Campo | Tipo | Rango válido |
|---|---|---|---|---|
| 0 | 2 | cabecera | | `A7 11` |
| 2 | 6 | `deviceId` | uint48 | cualquiera |
| 8 | 2 | `counter` | uint16 | 1-65535 |
| 10 | 1 | `info` | bits (ver abajo) | tipo de emergencia 0-2 |
| 11 | 1 | `people` | uint8 | 1-255 (255 = "255 o más") |
| 12 | 4 | `latE7` | int32 complemento a dos | −900000000 a 900000000 |
| 16 | 4 | `lonE7` | int32 complemento a dos | −1800000000 a 1800000000 |
| 20 | 4 | `createdAtS` | uint32, segundos epoch UTC | cualquiera |
| 24 | 1 | `ttl` | uint8 | 1-`TTL_MAX` |
| 25 | 2 | `relayTag` | uint16 | cualquiera |

Byte `info` (bit 0 = el menos significativo):

| Bits | Campo | Valores |
|---|---|---|
| 0-1 | `emergencyType` | `0` comida o agua (amarillo) · `1` asistencia médica (naranja) · `2` atrapado o grave (rojo) · `3` inválido |
| 2 | `automatic` | `1` si la generó el movimiento brusco |
| 3 | `children` | niños |
| 4 | `elderly` | adultos mayores |
| 5 | `injured` | personas heridas |
| 6 | `reducedMobility` | movilidad reducida |
| 7 | reservado | se codifica `0`; al decodificar se ignora |

Una alerta automática (movimiento brusco) lleva `emergencyType = 2`, `automatic = 1`, `people = 1` y sin vulnerables (spec 000).

### 3.2 `ON_THE_WAY` (23 bytes)

| Offset | Tamaño | Campo | Tipo | Rango válido |
|---|---|---|---|---|
| 0 | 2 | cabecera | | `A7 12` |
| 2 | 6 | `alertDeviceId` | uint48 | cualquiera |
| 8 | 2 | `alertCounter` | uint16 | 1-65535 |
| 10 | 6 | `volunteerDeviceId` | uint48 | cualquiera |
| 16 | 4 | `createdAtS` | uint32 | cualquiera |
| 20 | 1 | `ttl` | uint8 | 1-`TTL_MAX` |
| 21 | 2 | `relayTag` | uint16 | cualquiera |

### 3.3 `STATUS` (18 bytes, reservado y opcional)

| Offset | Tamaño | Campo | Tipo | Rango válido |
|---|---|---|---|---|
| 0 | 2 | cabecera | | `A7 13` |
| 2 | 6 | `alertDeviceId` | uint48 | cualquiera |
| 8 | 2 | `alertCounter` | uint16 | 1-65535 |
| 10 | 1 | `status` | uint8 | `1` cancelada · `2` a salvo |
| 11 | 4 | `createdAtS` | uint32 | cualquiera |
| 15 | 1 | `ttl` | uint8 | 1-`TTL_MAX` |
| 16 | 2 | `relayTag` | uint16 | cualquiera |

Solo lo emite el teléfono que creó la alerta. Los receptores no pueden verificarlo (limitación declarada, sin firmas en el MVP).

## 4. Identificadores y deduplicación

- `deviceId`: 48 bits aleatorios generados una vez al instalar con una función inyectable (regla 8) y guardados. Si se reinstala la app, se genera uno nuevo.
- `counter`: empieza en 1 y sube en 1 por cada `ALERT` que crea el teléfono. Se guarda. Si llegara a 65535, crear otra alerta devuelve un error explícito.
- `relayTag`: los 16 bits bajos del `deviceId` de quien emite físicamente el anuncio. El origen pone el suyo y cada relay lo reemplaza por el propio.

**Campos mutables:** `ttl` y `relayTag`. Cambian en cada salto y **nunca** forman parte de la clave de deduplicación.

| Paquete | Clave de deduplicación (campos inmutables) | Forma canónica en texto |
|---|---|---|
| `ALERT` | `(deviceId, counter)` | `a1b2c3d4e5f6-0001` |
| `ON_THE_WAY` | `(alertDeviceId, alertCounter, volunteerDeviceId)` | `a1b2c3d4e5f6-0001/0c0c0c0c0c0c` |
| `STATUS` | `(alertDeviceId, alertCounter, status)` | `a1b2c3d4e5f6-0001#2` |

Forma canónica: `deviceId` en 12 dígitos hexadecimales en minúscula, `-`, `counter` en 4 dígitos hexadecimales en minúscula. Ese texto es el `alertId` que usan la base de datos (002) y el panel. Las claves se ordenan comparando su forma canónica como texto (regla 9).

Si llega con otro TTL o desde otro relay un paquete cuya clave ya se vio, se descarta: no se actualiza nada.

El contador de voluntarios de una alerta es el número de claves `ON_THE_WAY` distintas con ese `alertId`.

## 5. Codificación y decodificación

Las funciones son puras (regla 5):

- `encode(packet) -> ByteArray`: escribe los campos reservados en `0`. Rechaza con error explícito un paquete con campos fuera de rango; nunca lo trunca.
- `decode(bytes) -> Result<Packet, DecodeError>`: valida en este orden y devuelve el primer error:

| Orden | Error | Condición |
|---|---|---|
| 1 | `NOT_OUR_PACKET` | longitud < 2 o `magic != 0xA7` |
| 2 | `UNSUPPORTED_VERSION` | versión distinta de `1` |
| 3 | `UNKNOWN_TYPE` | tipo de paquete reservado |
| 4 | `INVALID_LENGTH` | longitud distinta de la del tipo |
| 5 | `INVALID_FIELD` | cualquier campo fuera de su rango (sección 3); se indica el nombre del campo |

- Conversión de hora: `createdAtS = floor(epochMs / 1000)` al codificar y `epochMs = createdAtS * 1000` al decodificar.
- Los paquetes no llevan texto libre (regla 24).

## 6. Reglas de reenvío (gossip)

Las aplica el motor puro (103) y el simulador (300). El reloj (`Clock`) y el generador se inyectan.

### 6.1 Origen
1. Al crear un paquete propio (`ALERT`, `ON_THE_WAY` o `STATUS`), el teléfono lo guarda primero (regla 21) y marca su clave como vista.
2. Lo anuncia **sin retardo**, con `ttl = TTL_MAX` y su propio `relayTag`, durante `ORIGIN_ANNOUNCE_WINDOW_MS`.
3. El origen no consume valores del generador.

### 6.2 Recepción
Al recibir un paquete decodificado sin error, en este orden:
1. Si su `relayTag` está en la lista del filtro de depuración → se descarta (`BLOCKED`). Este filtro es una herramienta de demo rotulada como simulación (spec 000).
2. Si su clave ya se vio → se descarta (`DUPLICATE`).
3. Se guarda y se marca la clave como vista (`RECEIVED`).
4. Si `ttl <= 1` → no se reenvía (`NO_FORWARD_TTL`).
5. Si `now - createdAtS * 1000 > MAX_AGE_MS` → no se reenvía (`NO_FORWARD_EXPIRED`). Se guarda igual y se sube igual. Una hora futura (reloj desfasado) no se rechaza.
6. Si no, se toma `delay = next() mod (FORWARD_DELAY_MAX_MS + 1)` del generador del teléfono y se programa un anuncio en `now + delay`, con `ttl - 1` y el `relayTag` propio, durante `ANNOUNCE_WINDOW_MS`.

Cada clave se reenvía **una sola vez** por teléfono.

### 6.3 Re-anuncio
Durante su ventana, el paquete permanece en la cola de anuncios y se re-emite. Cómo rota la cola es asunto de la spec 104. Al cerrarse la ventana sale de la cola, aunque sigue guardado.

### 6.4 Generador por teléfono
Cada teléfono tiene **un** generador `mulberry32`. En la app, su semilla son los 32 bits bajos del `deviceId`; en los vectores, la semilla se da explícitamente. Se consume un valor por cada paquete que llega al paso 6, en el orden en que se procesan.

### 6.5 Orden de procesamiento
Los eventos del mismo instante se procesan en este orden estable (regla 9):
1. Por tiempo `t`.
2. Los anuncios antes que las recepciones.
3. Por el identificador del teléfono (texto).
4. Por la clave canónica del paquete.

### 6.6 Modelo de enlace de los vectores
En los vectores, cuando un teléfono empieza un anuncio en `t`, cada vecino recibe el paquete en ese mismo `t`, una vez y sin pérdida. Este modelo ideal fija la lógica. La pérdida, la repetición dentro de la ventana y la movilidad las agregan el transporte real (104) y el simulador (300), fuera de los vectores.

### 6.7 Algoritmo del ejecutor de escenarios
Android (103) y el simulador (300) lo implementan igual, para producir la misma secuencia `expected`. Cada teléfono tiene un motor con las operaciones `originate`, `onReceived`, `tick` y `nextWakeupMs` (detalle en la spec 103).

1. Cola de entradas `(t, kind, node, key)`. `kind 0` es una acción del teléfono (originar o despertar); `kind 1` es una entrega.
2. Cada `events[i]` con `originate` entra como `kind 0` con la clave de su paquete. Un despertar entra como `kind 0` con clave vacía `""`.
3. Se saca siempre la entrada menor por `(t, kind, node, key)`: el tiempo como número y los textos por comparación de texto. La clave vacía va antes que cualquier otra.
4. Según la entrada:
   - `originate`: se llama a `originate(wire, nowMs + t)`.
   - despertar: se llama a `tick(nowMs + t)`.
   - entrega: se llama a `onReceived(wire, nowMs + t)`.
5. Por cada `StartAnnounce` devuelto, en orden, se agrega una entrega `kind 1` en el mismo `t` a cada vecino del teléfono (sin pérdida, sección 6.6).
6. Después de cada entrada, si `nextWakeupMs()` del teléfono no es nulo y no hay ya un despertar en la cola para ese teléfono en ese tiempo, se agrega uno.
7. La secuencia `expected` son los eventos devueltos (`ANNOUNCE`, `RECEIVED`, `DUPLICATE`, `BLOCKED`, `NO_FORWARD_TTL`, `NO_FORWARD_EXPIRED`), en el orden en que se producen, con su `t` y su teléfono. El escenario termina cuando la cola queda vacía.

Campos de cada evento esperado: `t`, `event`, `node` y `key` siempre; `ttl` solo en `ANNOUNCE` y `RECEIVED`; `relayTag` (entero) solo en `BLOCKED`.

## 7. Generador `mulberry32`

Estado: un entero de 32 bits `a`, inicializado con la semilla. Cada `next()` devuelve un uint32:

```
a = (a + 0x6D2B79F5) mod 2^32
t = imul32(a XOR (a >>> 15), a OR 1)
t = (t + imul32(t XOR (t >>> 7), t OR 61)) XOR t
return (t XOR (t >>> 14)) como uint32
```

`imul32` es la multiplicación de 32 bits con desbordamiento: `Math.imul` en JS, `Int * Int` en Kotlin. `>>>` es el desplazamiento sin signo: `>>>` en JS, `ushr` en Kotlin. En Kotlin el resultado se lleva a uint32 con `toLong() and 0xFFFFFFFFL` antes del `mod`.

Salidas de referencia (las cinco primeras):

| Semilla | Salidas |
|---|---|
| `1` | 2693262067, 11749833, 2265367787, 4213581821, 4159151403 |
| `42` | 2581720956, 1925393290, 3661312704, 2876485805, 750819978 |
| `185273099` (`0x0B0B0B0B`) | 2943612290, 4173463580, 1481614005, 1144772106, 3950213351 |
| `202116108` (`0x0C0C0C0C`) | 1474678540, 413007807, 3323536170, 359873878, 2421111283 |

Prohibido usar `Math.random()`, `Random()` sin semilla o el generador de la plataforma en el protocolo (regla 7).

## 8. Configuración del protocolo

Una sola configuración documentada (regla 17), con los mismos nombres en Kotlin y en JS:

| Constante | Valor |
|---|---|
| `COMPANY_ID` | `0xFFFF` |
| `MAGIC` | `0xA7` |
| `PROTOCOL_VERSION` | `1` |
| `MAX_PAYLOAD_BYTES` | `27` |
| `TTL_MAX` | `8` |
| `FORWARD_DELAY_MAX_MS` | `2000` |
| `ANNOUNCE_WINDOW_MS` | `60000` |
| `ORIGIN_ANNOUNCE_WINDOW_MS` | `60000` |
| `MAX_AGE_MS` | `86400000` (24 h) |

Cambiar un valor cambia esta spec y sus vectores.

## 9. Vectores de prueba

Viven en `specs/shared/vectors/`. Android y el simulador deben pasar todos (`ARCHITECTURE.md`, sección 3). Los números van como enteros JSON (48 bits caben en un número de JS). Los bytes van como texto hexadecimal en minúscula, sin separadores.

| Archivo | Contenido | Mínimo |
|---|---|---|
| `prng.json` | `[{ "seed": 1, "outputs": [..5..] }]` | las 4 semillas de la sección 7 |
| `packets-valid.json` | `[{ "name", "packet": {campos}, "hex", "key" }]`: `encode(packet) == hex`, `decode(hex) == packet` y la clave canónica | los 3 de la sección 10 más: alerta automática, todos los vulnerables, `people = 255`, latitud y longitud en los extremos, `ttl = 1` |
| `packets-invalid.json` | `[{ "name", "hex", "error", "field?" }]` | cada error de la sección 5; `emergencyType = 3`; `people = 0`; `counter = 0`; `ttl = 0` y `ttl = 9`; latitud y longitud fuera de rango; bit reservado en `1` (es **válido** y se ignora) |
| `gossip/*.json` | escenario (abajo) | los 2 de la sección 10 más: `ttl` agotado, alerta caducada, dos orígenes al mismo tiempo (desempate) |

Escenario de reenvío:

```json
{
  "name": "line-a-b-c",
  "config": {},
  "nodes": [{ "id": "A", "deviceId": 177789161760246, "seed": 3285509622 }],
  "links": [["A", "B"], ["B", "C"]],
  "blockedRelayTags": { "C": [58870] },
  "nowMs": 1760600000000,
  "events": [{ "t": 0, "node": "A", "originate": { "hex": "..." } }],
  "expected": [{ "t": 0, "event": "ANNOUNCE", "node": "A", "key": "...", "ttl": 8 }]
}
```

- `t` es en milisegundos desde el inicio del escenario. El reloj del teléfono vale `nowMs + t`.
- `config` sobrescribe constantes de la sección 8 solo para ese escenario.
- `expected` es la secuencia completa y ordenada (sección 6.5) de eventos `ANNOUNCE`, `RECEIVED`, `DUPLICATE`, `BLOCKED`, `NO_FORWARD_TTL` y `NO_FORWARD_EXPIRED`.

## 10. Ejemplos de referencia

Teléfonos (semilla = 32 bits bajos del `deviceId`, sección 6.4):

| Teléfono | `deviceId` | `relayTag` | Semilla |
|---|---|---|---|
| A | `a1b2c3d4e5f6` (177789161760246) | `e5f6` (58870) | `0xC3D4E5F6` (3285509622) |
| B | `0b0b0b0b0b0b` | `0b0b` | `0x0B0B0B0B` (185273099) |
| C | `0c0c0c0c0c0c` | `0c0c` | `0x0C0C0C0C` (202116108) |

**`ALERT`** de A: contador 1, atrapado o grave, con niños y heridos, 3 personas, −33.5 / −70.65, `createdAtS = 1760600000`, `ttl = 8`:
```
a711 a1b2c3d4e5f6 0001 2a 03 ec084e40 d5e3aa60 68f09fc0 08 e5f6
```
Clave: `a1b2c3d4e5f6-0001`.

**`ON_THE_WAY`** de C hacia esa alerta, `createdAtS = 1760600120`, `ttl = 8`:
```
a712 a1b2c3d4e5f6 0001 0c0c0c0c0c0c 68f0a038 08 0c0c
```
Clave: `a1b2c3d4e5f6-0001/0c0c0c0c0c0c`.

**`STATUS`** de A, a salvo, `createdAtS = 1760600600`, `ttl = 8`:
```
a713 a1b2c3d4e5f6 0001 02 68f0a218 08 e5f6
```
Clave: `a1b2c3d4e5f6-0001#2`.

(Los espacios solo facilitan la lectura; en los vectores el hex va sin separadores.)

**Escenario `line-a-b-c`** (enlaces A-B y B-C; A origina el `ALERT` en `t = 0`):

| t (ms) | Evento | Teléfono | ttl |
|---|---|---|---|
| 0 | `ANNOUNCE` | A | 8 |
| 0 | `RECEIVED` | B | 8 |
| 1220 | `ANNOUNCE` | B | 7 |
| 1220 | `DUPLICATE` | A | |
| 1220 | `RECEIVED` | C | 7 |
| 2790 | `ANNOUNCE` | C | 6 |
| 2790 | `DUPLICATE` | B | |

Retardo de B: `2943612290 mod 2001 = 1220`. Retardo de C: `1474678540 mod 2001 = 1570`.

**Escenario `triangle-c-blocks-a`** (enlaces A-B, B-C y A-C; C bloquea el relayTag `e5f6`). Reproduce la demo de la spec 000 en una sala:

| t (ms) | Evento | Teléfono | ttl |
|---|---|---|---|
| 0 | `ANNOUNCE` | A | 8 |
| 0 | `RECEIVED` | B | 8 |
| 0 | `BLOCKED` | C | |
| 1220 | `ANNOUNCE` | B | 7 |
| 1220 | `DUPLICATE` | A | |
| 1220 | `RECEIVED` | C | 7 |
| 2790 | `ANNOUNCE` | C | 6 |
| 2790 | `DUPLICATE` | A | |
| 2790 | `DUPLICATE` | B | |

## Criterios de aceptación

1. Los tres paquetes de la sección 10 se codifican exactamente al hex indicado y se decodifican de vuelta a los mismos campos.
2. Ningún paquete supera `MAX_PAYLOAD_BYTES`. `ALERT` mide 27 bytes, `ON_THE_WAY` 23 y `STATUS` 18.
3. Cada entrada de `packets-invalid.json` produce el error y el campo indicados.
4. Cambiar `ttl` o `relayTag` no cambia la clave de deduplicación. Cambiar cualquier campo inmutable sí la cambia.
5. `mulberry32` reproduce las salidas de la sección 7 en Kotlin y en JS.
6. Los escenarios `gossip/*.json` producen exactamente la secuencia `expected`, tanto en el motor de Android (103) como en el simulador (300).
7. Ninguna constante del protocolo aparece fuera de la configuración de la sección 8.

## Limitaciones declaradas

- Sin firmas: cualquiera puede fabricar paquetes, incluido un `STATUS` falso. Es trabajo futuro (spec 000).
- `relayTag` usa 16 bits: dos teléfonos pueden compartirlo (1 entre 65536 por pareja). Solo afecta al filtro de depuración.
- Un `ALERT` ya usa los 27 bytes: no queda espacio para batería ni otros campos sin una versión 2 del protocolo.
- Reenviar una sola vez por clave: si la primera copia que llega vino por un camino largo (con menos TTL), la del camino corto se descarta y el paquete puede no alcanzar a teléfonos que estaban dentro de `TTL_MAX` saltos. Solo está garantizado que todos reciben si `TTL_MAX ≥ número de teléfonos − 1`. La simulación (300) lo mide.
- Con ventanas de 60 s, si nadie está al alcance del origen durante ese minuto, la alerta no sale. Mejoras posibles: re-crear la alerta o alargar `ORIGIN_ANNOUNCE_WINDOW_MS`.

## Tareas

1. Revisión de P2 y P3 (comentarios en el PR) y aprobación de los tres (regla 4).
2. Crear los vectores de la sección 9 en `specs/shared/vectors/` (ola 1, `ARCHITECTURE.md` sección 6).
3. Spec 101: codec, claves y configuración en Kotlin, probados con `packets-*.json` y `prng.json`.
4. Spec 103: motor de reenvío, probado con `gossip/*.json`.
5. Spec 104: envoltura BLE y filtro de escaneo de la sección 1.
6. Spec 300: mismos vectores en JS.
