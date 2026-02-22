package com.sameerasw.pixsl.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.sameerasw.pixsl.data.model.nostr.NostrEvent
import com.sameerasw.pixsl.utils.NostrCrypto
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.security.MessageDigest

@Serializable
data class Nip96Response(
    val status: String,
    val message: String? = null,
    val nip94: NostrEvent? = null,
    val url: String? = null
)

class MediaRepository(private val context: Context) {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val uploadServer =
        "https://void.cat/api/v1/nip96" // Standard NIP-96 endpoint for void.cat

    suspend fun uploadImage(uri: Uri): String? {
        val keys = getEventKeys() ?: return null
        val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bytes = inputStream.readBytes()
        inputStream.close()

        val fileName = "upload_${System.currentTimeMillis()}.jpg"

        try {
            // 1. Generate NIP-98 Authorization Header (Kind 27235)
            val authHeader = generateNip98Header(
                url = uploadServer,
                method = "POST",
                payloadHash = sha256(bytes),
                keys = keys
            )

            // 2. Perform Multipart Upload
            val response: HttpResponse = client.submitFormWithBinaryData(
                url = uploadServer,
                formData = formData {
                    append("file", bytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                    append("caption", "Uploaded via PixeLK")
                }
            ) {
                header("Authorization", "Nostr $authHeader")
            }

            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                val body = response.bodyAsText()
                val nip96Response = json.decodeFromString<Nip96Response>(body)
                return nip96Response.url ?: nip96Response.nip94?.getTag("url")
            } else {
                Log.e(
                    "MediaUpload",
                    "Error response: ${response.status} - ${response.bodyAsText()}"
                )
            }
        } catch (e: Exception) {
            Log.e("MediaUpload", "Upload failed", e)
        }
        return null
    }

    private fun generateNip98Header(
        url: String,
        method: String,
        payloadHash: String,
        keys: Pair<String, String>
    ): String {
        val tags = listOf(
            listOf("u", url),
            listOf("method", method),
            listOf("payload", payloadHash)
        )

        val event = NostrCrypto.createSignedEvent(
            content = "",
            privKeyHex = keys.first,
            pubKeyHex = keys.second,
            tags = tags,
            kind = 27235
        )

        val eventJson = Json.encodeToString(NostrEvent.serializer(), event)
        return Base64.encodeToString(eventJson.toByteArray(), Base64.NO_WRAP)
    }

    private fun sha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun getEventKeys(): Pair<String, String>? {
        val prefs = context.getSharedPreferences("pixsl_prefs", Context.MODE_PRIVATE)
        val privKeyHex = prefs.getString("nostr_private_key", null) ?: return null

        val privKeyBytes = privKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val secp = fr.acinq.secp256k1.Secp256k1.get()
        val pubkeyCompressed = secp.pubKeyCompress(secp.pubkeyCreate(privKeyBytes))
        val pubKeyHex = pubkeyCompressed.copyOfRange(1, 33).joinToString("") { "%02x".format(it) }

        return Pair(privKeyHex, pubKeyHex)
    }
}
