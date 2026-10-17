# 103 android-gossip-motor

Capa: 2 · Persona: P1
Depende de: 001, 101
Entrega: `GossipEngine` (motor de reenvío puro) y `GossipEffect`, en `:core`, paquete `com.senda.core.gossip`; `ScenarioRunner` en las pruebas
Consume: `WirePacket`, `PacketKey`, `ProtocolConfig`, `Mulberry32`, `relayTagOf` y `PacketCodec` (101)

## Objetivo

Implementar las reglas de reenvío de la 001, sección 6, como una clase pura que recibe entradas y devuelve efectos, sin tocar BLE, la base ni el reloj. Así el mismo comportamiento se prueba con los vectores `gossip/*.json` y lo usa el servicio (107) en el teléfono.

## Alcance

- Origen, recepción, deduplicación, TTL, caducidad, retardo con semilla, ventanas de re-anuncio y filtro de depuración por `relayTag`.
- Los tres tipos de paquete con las mismas reglas: `ALERT`, `ON_THE_WAY` ("voy en camino") y `STATUS`.
- Carga inicial de claves ya vistas (reinicio de la app).
- `ScenarioRunner` de pruebas, que ejecuta varios motores conectados según la 001, sección 6.7.

## Fuera de alcance

- Rotación de la cola de anuncios y el BLE real (104).
- Guardar los paquetes (102): el motor solo emite `Store`.
- Aplicar los efectos, hilos y temporizadores reales (107).
- Contar voluntarios (109, a partir de lo guardado).

## 1. Sin fake

El motor es puro y determinista: la 107 y las pruebas usan la implementación real. No entrega fake, con la misma justificación que la 101, sección 1.

## 2. API

```kotlin
// spec 103
class GossipEngine(
    private val deviceId: Long,
    private val rng: Mulberry32,
    private val config: ProtocolConfig = ProtocolConfig.DEFAULT,
    initiallySeen: Collection<PacketKey> = emptyList(),
    blockedRelayTags: Set<Int> = emptySet(),
) {
    /** Paquete propio, ya guardado por quien lo creó (108, 109). */
    fun originate(wire: WirePacket, nowMs: Long): List<GossipEffect>
    /** Paquete decodificado sin error por la 104. */
    fun onReceived(wire: WirePacket, nowMs: Long): List<GossipEffect>
    /** Inicia los reenvíos vencidos y cierra las ventanas vencidas. */
    fun tick(nowMs: Long): List<GossipEffect>
    /** Próximo instante en que tick tiene algo que hacer, o null si nada. */
    fun nextWakeupMs(): Long?
    fun setBlockedRelayTags(tags: Set<Int>)
    /** Anuncios con ventana abierta, ordenados por clave (para 104 y la pantalla de estado). */
    fun active(): List<WirePacket>
}

sealed interface GossipEffect {
    /** Guardar antes que cualquier otra cosa (regla 21). */
    data class Store(val wire: WirePacket) : GossipEffect
    data class StartAnnounce(val wire: WirePacket, val untilMs: Long) : GossipEffect
    data class StopAnnounce(val key: PacketKey) : GossipEffect
    data class Event(val type: GossipEventType, val key: PacketKey, val ttl: Int? = null, val relayTag: Int? = null) : GossipEffect
}

enum class GossipEventType { ANNOUNCE, RECEIVED, DUPLICATE, BLOCKED, NO_FORWARD_TTL, NO_FORWARD_EXPIRED }
```

- El motor **nunca** lee el reloj: `nowMs` siempre llega como parámetro (regla 6).
- No es seguro para varios hilos. La 107 lo usa desde un único hilo o corrutina.
- `rng` es exclusivo de este motor: nadie más consume sus valores (001, sección 6.4).

## 3. Estado interno

| Estado | Contenido |
|---|---|
| `seen` | `Set<PacketKey>`, que empieza con `initiallySeen` |
| `pending` | reenvíos programados: `(dueMs, wire)` |
| `windows` | anuncios abiertos: `key -> (wire, untilMs)` |
| `blocked` | `Set<Int>` de `relayTag` |

## 4. Comportamiento

Las reglas son las de la 001, sección 6. Lo que sigue fija el orden exacto de los efectos.

### 4.1 `originate(wire, now)`
1. Si `wire.packet.key` ya está en `seen`: devuelve `[Event(DUPLICATE, key)]` y no hace nada más.
2. Agrega la clave a `seen`.
3. Devuelve `[StartAnnounce(wire, now + originAnnounceWindowMs), Event(ANNOUNCE, key, ttl)]` y abre la ventana.

`originate` no emite `Store`, porque quien crea el paquete ya lo guardó (regla 21). No consume valores de `rng`.

### 4.2 `onReceived(wire, now)`
1. Si `wire.relayTag` está en `blocked`: devuelve `[Event(BLOCKED, key, relayTag = wire.relayTag)]`. **No** marca la clave como vista: la misma alerta puede llegar después por otro relay.
2. Si la clave está en `seen`: devuelve `[Event(DUPLICATE, key, relayTag = wire.relayTag)]`. El `relayTag` sirve para que la 107 detecte que otro teléfono retransmitió un paquete propio (108); no forma parte de los vectores (001, sección 6.7).
3. Agrega la clave a `seen` y emite `Store(wire)` y `Event(RECEIVED, key, ttl)`.
4. Si `wire.ttl <= 1`: agrega `Event(NO_FORWARD_TTL, key)` y termina.
5. Si `now - packet.createdAtMs > maxAgeMs`: agrega `Event(NO_FORWARD_EXPIRED, key)` y termina.
6. Si no: `delay = rng.next() mod (forwardDelayMaxMs + 1)` y agrega a `pending` el reenvío `(now + delay, wire.copy(ttl = ttl - 1, relayTag = relayTagOf(deviceId)))`. Este paso no emite efectos.

### 4.3 `tick(now)`
1. Cierra las ventanas con `untilMs <= now`, en orden de clave, y emite `StopAnnounce(key)` por cada una.
2. Saca de `pending` los reenvíos con `dueMs <= now`, en orden `(dueMs, key)`. Para cada uno, abre la ventana `now + announceWindowMs` y emite `StartAnnounce(wire, untilMs)` y `Event(ANNOUNCE, key, ttl)`.

Si la 107 llama tarde (`now > dueMs`), el anuncio empieza en `now`: el motor no retrocede el tiempo.

### 4.4 `nextWakeupMs()`
El mínimo entre los `dueMs` de `pending` y los `untilMs` de `windows`, o `null` si ambos están vacíos.

### 4.5 Reinicio
Al arrancar, la 107 crea el motor con `initiallySeen` = todas las claves guardadas en la 102. Esos paquetes no se vuelven a anunciar: sus ventanas se perdieron con el proceso (limitación declarada).

## 5. `ScenarioRunner` (pruebas)

En `core/src/test/kotlin/com/senda/core/gossip/`. Lee un archivo `gossip/*.json`, crea un `GossipEngine` por teléfono (`Mulberry32(seed)`, `blockedRelayTags` y `config` del escenario) y los ejecuta con el algoritmo de la 001, sección 6.7. Devuelve la secuencia de `Event` con su tiempo y su teléfono, para compararla con `expected`.

- Las claves de `config` del JSON usan los nombres de la 001, sección 8 (`TTL_MAX`), y se pasan a los campos de `ProtocolConfig` (`ttlMax`).
- Los paquetes de `originate.hex` se decodifican con `PacketCodec.decode` (101). Si fallan, la prueba falla.

## 6. Pruebas

| Prueba | Qué verifica |
|---|---|
| `GossipVectorsTest` | cada `gossip/*.json` produce exactamente su `expected` |
| `EngineRulesTest` | cada paso de la sección 4 por separado, con `FakeClock` de la 100 o tiempos fijos |
| `ReferenceScenariosTest` | `line-a-b-c` y `triangle-c-blocks-a` de la 001, sección 10, escritos en el código (sin depender de los archivos) |
| `InvariantsTest` | 200 topologías aleatorias generadas con `Mulberry32(7)`; en cada una: ningún teléfono emite `RECEIVED` dos veces para la misma clave; en un grafo conexo sin bloqueos y con `TTL_MAX ≥ número de teléfonos − 1`, todos reciben (con menos TTL no está garantizado, ver limitaciones); ejecutar dos veces da la misma secuencia |
| `OnTheWayTest` | un `ON_THE_WAY` se propaga con las mismas reglas y su clave no choca con la de su `ALERT` |

## Criterios de aceptación

1. `line-a-b-c` y `triangle-c-blocks-a` (001, sección 10) dan exactamente sus tablas, incluidos los retardos de 1220 ms y 1570 ms.
2. Pasan todos los `gossip/*.json`, incluidos el `ttl` agotado, la alerta caducada y los dos orígenes al mismo tiempo.
3. Un paquete `BLOCKED` no queda como visto: si después llega por otro relay, se recibe.
4. Una clave en `initiallySeen` produce `DUPLICATE` y no se reenvía.
5. El motor solo consume valores de `rng` en el paso 6 de la sección 4.2: dos motores con la misma semilla y las mismas entradas producen los mismos efectos.
6. `tick` llamado tarde empieza el anuncio en el `now` recibido, y `nextWakeupMs` siempre devuelve el próximo evento pendiente.
7. Se cumplen las invariantes del `InvariantsTest`.
8. `GossipEngine` no referencia `System`, `Random` ni nada de Android (lo garantiza el módulo `:core` y una prueba que busca `System.` y `Random(` en `core/src/main/.../gossip`).
9. `./gradlew :core:test` pasa sin dispositivo.

## Limitaciones declaradas

- Un teléfono puede recibir primero la copia de un camino largo, con menos TTL. La copia del camino corto se descarta como duplicada, y el paquete puede no alcanzar a teléfonos que estaban dentro de `TTL_MAX` saltos. Es propio de reenviar una sola vez por clave (001). La simulación (300) debe medirlo; una mejora posible es reenviar de nuevo si llega una copia con más TTL.
- `seen` crece sin límite. Con la escala de la demo no importa; podarlo por `maxAgeMs` queda como mejora.
- Tras reiniciar la app, los anuncios abiertos se pierden y no se reanudan.
- El motor no sabe quién lo rodea: si nadie está al alcance durante la ventana, el paquete no sale de ese teléfono (001, limitaciones).

## Tareas

1. Estado y `originate`, con sus pruebas.
2. `onReceived`, con una prueba por cada paso de la sección 4.2.
3. `tick`, `nextWakeupMs` y `active`, con sus pruebas.
4. `ScenarioRunner` según la 001, sección 6.7, con `ReferenceScenariosTest`.
5. `InvariantsTest` y `OnTheWayTest`.
6. `GossipVectorsTest` contra `specs/shared/vectors/gossip/`. Si los archivos no existen todavía (ola 1), queda `bloqueada` y se registra en `memory/P1.md`.
