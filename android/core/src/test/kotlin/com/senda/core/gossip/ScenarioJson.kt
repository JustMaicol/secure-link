package com.senda.core.gossip

import com.senda.core.protocol.ProtocolConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

// spec 103, sección 5 · Lee un `gossip/*.json` (formato de la 001, sección 9) a un Scenario y a su traza esperada.
object ScenarioJson {

    class Parsed(val scenario: Scenario, val expected: List<ScenarioEvent>)

    fun parse(text: String): Parsed {
        val root = Json.parseToJsonElement(text).jsonObject

        val scenario = Scenario(
            name = root.getValue("name").jsonPrimitive.content,
            config = configOf(root.getValue("config").jsonObject),
            nodes = root.getValue("nodes").jsonArray.map {
                val node = it.jsonObject
                ScenarioNode(
                    id = node.getValue("id").jsonPrimitive.content,
                    deviceId = node.getValue("deviceId").jsonPrimitive.long,
                    // La semilla va como uint32 en el JSON: toLong().toInt() recupera los 32 bits.
                    seed = node.getValue("seed").jsonPrimitive.long.toInt(),
                )
            },
            links = root.getValue("links").jsonArray.map {
                val pair = it.jsonArray
                pair[0].jsonPrimitive.content to pair[1].jsonPrimitive.content
            },
            blockedRelayTags = root.getValue("blockedRelayTags").jsonObject.mapValues { (_, tags) ->
                tags.jsonArray.map { it.jsonPrimitive.int }.toSet()
            },
            nowMs = root.getValue("nowMs").jsonPrimitive.long,
            originations = root.getValue("events").jsonArray.map {
                val event = it.jsonObject
                ScenarioOrigination(
                    t = event.getValue("t").jsonPrimitive.long,
                    node = event.getValue("node").jsonPrimitive.content,
                    hex = event.getValue("originate").jsonObject.getValue("hex").jsonPrimitive.content,
                )
            },
        )

        val expected = root.getValue("expected").jsonArray.map {
            val event = it.jsonObject
            ScenarioEvent(
                t = event.getValue("t").jsonPrimitive.long,
                type = GossipEventType.valueOf(event.getValue("event").jsonPrimitive.content),
                node = event.getValue("node").jsonPrimitive.content,
                key = event.getValue("key").jsonPrimitive.content,
                ttl = event["ttl"]?.jsonPrimitive?.int,
                relayTag = event["relayTag"]?.jsonPrimitive?.int,
            )
        }
        return Parsed(scenario, expected)
    }

    /** Las claves de `config` usan los nombres de la 001, sección 8, y se pasan a los campos de ProtocolConfig. */
    private fun configOf(json: JsonObject): ProtocolConfig {
        var config = ProtocolConfig.DEFAULT
        for ((name, value) in json) {
            val number = value.jsonPrimitive.long
            config = when (name) {
                "TTL_MAX" -> config.copy(ttlMax = number.toInt())
                "FORWARD_DELAY_MAX_MS" -> config.copy(forwardDelayMaxMs = number)
                "ANNOUNCE_WINDOW_MS" -> config.copy(announceWindowMs = number)
                "ORIGIN_ANNOUNCE_WINDOW_MS" -> config.copy(originAnnounceWindowMs = number)
                "MAX_AGE_MS" -> config.copy(maxAgeMs = number)
                else -> throw AssertionError("Constante de config desconocida en el escenario: $name")
            }
        }
        return config
    }
}
