package com.sameerasw.pixsl.data.model.nostr

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

@Serializable
data class NostrEvent(
    val id: String,
    val pubkey: String,
    val created_at: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
    val sig: String
) {
    fun getTag(key: String): String? = tags.firstOrNull { it.getOrNull(0) == key }?.getOrNull(1)

    fun getMediaUrl(): String? {
        if (kind == 1063 || kind == 94) return getTag("url")
        // Fallback to regex for common Kind 1 media links
        return content.split(Regex("\\s+")).find {
            it.startsWith("http") && (it.endsWith(".jpg") || it.endsWith(".png") || it.endsWith(".webp") || it.endsWith(
                ".jpeg"
            ))
        }
    }
}

@Serializable
data class NostrFilter(
    val ids: List<String>? = null,
    val authors: List<String>? = null,
    val kinds: List<Int>? = null,
    val since: Long? = null,
    val until: Long? = null,
    val limit: Int? = null,
    // Using a Map for `#e`, `#p`, `#t` tag filters
    val tags: Map<String, List<String>>? = null
)

// A sealed class and custom serializer to handle the array-based ["EVENT", {}, ...] structure Nostr uses
@Serializable(with = ClientMessageSerializer::class)
sealed class NostrClientMessage {
    data class EventMessage(val event: NostrEvent) : NostrClientMessage()
    data class ReqMessage(val subscriptionId: String, val filters: List<NostrFilter>) :
        NostrClientMessage()

    data class CloseMessage(val subscriptionId: String) : NostrClientMessage()
}

object ClientMessageSerializer : KSerializer<NostrClientMessage> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("NostrClientMessage")

    override fun serialize(encoder: Encoder, value: NostrClientMessage) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw IllegalStateException("This class can only be serialized by Json")
        when (value) {
            is NostrClientMessage.EventMessage -> {
                val array = buildList {
                    add(JsonPrimitive("EVENT"))
                    add(jsonEncoder.json.encodeToJsonElement(NostrEvent.serializer(), value.event))
                }
                jsonEncoder.encodeJsonElement(JsonArray(array))
            }

            is NostrClientMessage.ReqMessage -> {
                val array = buildList {
                    add(JsonPrimitive("REQ"))
                    add(JsonPrimitive(value.subscriptionId))
                    // Serialize filters, extracting tag map into top-level # keys
                    value.filters.forEach { filter ->
                        val filterJson = jsonEncoder.json.encodeToJsonElement(
                            NostrFilter.serializer(),
                            filter
                        ).jsonObject.toMutableMap()
                        // Convert "tags": {"t": ["PixeLK"]} to "#t": ["PixeLK"]
                        val tagsNode = filterJson.remove("tags")?.jsonObject
                        tagsNode?.forEach { (key, array) ->
                            filterJson["#$key"] = array
                        }
                        add(JsonObject(filterJson))
                    }
                }
                jsonEncoder.encodeJsonElement(JsonArray(array))
            }

            is NostrClientMessage.CloseMessage -> {
                val array = buildList {
                    add(JsonPrimitive("CLOSE"))
                    add(JsonPrimitive(value.subscriptionId))
                }
                jsonEncoder.encodeJsonElement(JsonArray(array))
            }
        }
    }

    override fun deserialize(decoder: Decoder): NostrClientMessage {
        throw UnsupportedOperationException("Client messages do not need to be deserialized locally")
    }
}

@Serializable(with = ServerMessageSerializer::class)
sealed class NostrServerMessage {
    data class EventMessage(val subscriptionId: String, val event: NostrEvent) :
        NostrServerMessage()

    data class EoseMessage(val subscriptionId: String) : NostrServerMessage()
    data class NoticeMessage(val message: String) : NostrServerMessage()
    data class OkMessage(val eventId: String, val accepted: Boolean, val message: String) :
        NostrServerMessage()
}

object ServerMessageSerializer : KSerializer<NostrServerMessage> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("NostrServerMessage")

    override fun serialize(encoder: Encoder, value: NostrServerMessage) {
        throw UnsupportedOperationException("Server messages do not need to be serialized locally")
    }

    override fun deserialize(decoder: Decoder): NostrServerMessage {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw IllegalStateException("This class can only be deserialized by Json")
        val array = jsonDecoder.decodeJsonElement().jsonArray
        return when (val type = array[0].toString().trim('"')) {
            "EVENT" -> {
                val subId = array[1].toString().trim('"')
                val event =
                    jsonDecoder.json.decodeFromJsonElement(NostrEvent.serializer(), array[2])
                NostrServerMessage.EventMessage(subId, event)
            }

            "EOSE" -> {
                val subId = array[1].toString().trim('"')
                NostrServerMessage.EoseMessage(subId)
            }

            "NOTICE" -> {
                val msg = array[1].toString().trim('"')
                NostrServerMessage.NoticeMessage(msg)
            }

            "OK" -> {
                val eventId = array[1].toString().trim('"')
                val accepted = array[2].toString().toBoolean()
                val msg = array.getOrNull(3)?.toString()?.trim('"') ?: ""
                NostrServerMessage.OkMessage(eventId, accepted, msg)
            }

            else -> throw IllegalArgumentException("Unknown message type: $type")
        }
    }
}
