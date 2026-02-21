package com.sameerasw.pixsl.utils

import com.sameerasw.pixsl.data.model.nostr.NostrEvent
import fr.acinq.secp256k1.Secp256k1
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer
import java.security.MessageDigest

object NostrCrypto {
    
    // We need an exact JSON encoding without unexpected spacing/formatting for the SHA-256 hash
    private val compactJson = Json {
        encodeDefaults = true
        explicitNulls = false
    }

    /**
     * Nostr (NIP-01) requires serializing the event attributes into a strict JSON Array:
     * [0, "pubkey", created_at, kind, tags, "content"]
     * Then generating a SHA-256 hash of that array.
     */
    fun calculateEventId(
        pubkey: String,
        createdAt: Long,
        kind: Int,
        tags: List<List<String>>,
        content: String
    ): ByteArray {
        val jsonArrayElements = buildList {
            add(JsonPrimitive(0))
            add(JsonPrimitive(pubkey))
            add(JsonPrimitive(createdAt))
            add(JsonPrimitive(kind))
            
            // Serialize tags list
            val tagsElement = compactJson.encodeToJsonElement(
                kotlinx.serialization.builtins.ListSerializer(
                    kotlinx.serialization.builtins.ListSerializer(String.serializer())
                ), tags
            )
            add(tagsElement)
            add(JsonPrimitive(content))
        }

        val serializedEvent = compactJson.encodeToString(JsonArray(jsonArrayElements))
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(serializedEvent.toByteArray(Charsets.UTF_8))
    }

    /**
     * Signs the 32-byte event ID hash using Schnorr signature via the native secp256k1 library.
     * @param eventIdHash The SHA-256 hash of the serialized event (32 bytes)
     * @param privKeyHex The 64-character hex string of the private key
     */
    fun signEventId(eventIdHash: ByteArray, privKeyHex: String): String {
        require(privKeyHex.length == 64) { "Private key hex must be 64 characters (32 bytes)" }
        val privKey = privKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        
        // Ensure standard native binding loader
        val secp = Secp256k1.get()
        
        // Generate Schnorr signature (64 bytes)
        val signatureBytes = secp.signSchnorr(eventIdHash, privKey, null)
        
        // Convert to hex
        return signatureBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Helper to convert ByteArray to Hex string
     */
    fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    /**
     * Complete helper to generate an unsigned event, hash it, sign it, and return the immutable final Event.
     */
    fun createSignedEvent(
        content: String,
        privKeyHex: String,
        pubKeyHex: String,
        tags: List<List<String>> = emptyList()
    ): NostrEvent {
        val createdAt = System.currentTimeMillis() / 1000L
        val kind = 1 // Short Text Note

        // 1. Serialize and SHA-256 Hash
        val eventIdHash = calculateEventId(pubKeyHex, createdAt, kind, tags, content)
        val eventIdHex = eventIdHash.toHex()

        // 2. Schnorr Sign over the hash
        val signatureHex = signEventId(eventIdHash, privKeyHex)

        // 3. Assemble
        return NostrEvent(
            id = eventIdHex,
            pubkey = pubKeyHex,
            created_at = createdAt,
            kind = kind,
            tags = tags,
            content = content,
            sig = signatureHex
        )
    }
}
