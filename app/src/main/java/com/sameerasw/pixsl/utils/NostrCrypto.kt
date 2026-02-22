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
import java.math.BigInteger

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
     * BIP340 requires the public key to have an even y-coordinate.
     * This function returns the parity-adjusted private key (negated if odd).
     */
    fun getEvenKey(privKeyHex: String): String {
        val privKey = privKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val secp = Secp256k1.get()
        val pubKey = secp.pubkeyCreate(privKey)
        
        // If the public key point is odd, we must negate the private key
        return if (secp.pubKeyCompress(pubKey)[0] == 0x03.toByte()) {
            val n = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16)
            val d = BigInteger(1, privKey)
            val evenD = n.subtract(d).toByteArray()
            // Pad to 32 bytes
            val padded = ByteArray(32)
            val start = if (evenD.size > 32) evenD.size - 32 else 0
            val len = if (evenD.size > 32) 32 else evenD.size
            System.arraycopy(evenD, start, padded, 32 - len, len)
            padded.toHex()
        } else {
            privKeyHex
        }
    }

    /**
     * Derives the x-only public key for a private key.
     */
    fun pubKeyFor(privKeyHex: String): String {
        val privKey = privKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val secp = Secp256k1.get()
        val pubKey = secp.pubkeyCreate(privKey)
        return secp.pubKeyCompress(pubKey).copyOfRange(1, 33).toHex()
    }

    /**
     * Signs the 32-byte event ID hash using Schnorr signature via the native secp256k1 library.
     * @param eventIdHash The SHA-256 hash of the serialized event (32 bytes)
     * @param privKeyHex The 64-character hex string of the private key
     */
    fun signEventId(eventIdHash: ByteArray, privKeyHex: String): String {
        require(privKeyHex.length == 64) { "Private key hex must be 64 characters (32 bytes)" }
        // Force valid BIP340 key
        val evenKeyHex = getEvenKey(privKeyHex)
        val privKey = evenKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        
        val secp = Secp256k1.get()
        val signatureBytes = secp.signSchnorr(eventIdHash, privKey, null)
        return signatureBytes.toHex()
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
        tags: List<List<String>> = emptyList(),
        kind: Int = 1
    ): NostrEvent {
        val createdAt = System.currentTimeMillis() / 1000L
        // Important: Ensure the pubkey matches our parity-adjusted signing key
        val actualPrivKey = getEvenKey(privKeyHex)
        val actualPubKey = pubKeyFor(actualPrivKey)

        // 1. Serialize and SHA-256 Hash
        val eventIdHash = calculateEventId(actualPubKey, createdAt, kind, tags, content)
        val eventIdHex = eventIdHash.toHex()

        // 2. Schnorr Sign over the hash
        val signatureHex = signEventId(eventIdHash, actualPrivKey)

        // 3. Assemble
        return NostrEvent(
            id = eventIdHex,
            pubkey = actualPubKey,
            created_at = createdAt,
            kind = kind,
            tags = tags,
            content = content,
            sig = signatureHex
        )
    }
}

