package com.sameerasw.pixsl.data.repository

import android.content.Context
import android.util.Log
import com.sameerasw.pixsl.data.model.nostr.NostrClientMessage
import com.sameerasw.pixsl.data.model.nostr.NostrEvent
import com.sameerasw.pixsl.data.model.nostr.NostrFilter
import com.sameerasw.pixsl.data.model.nostr.NostrServerMessage
import com.sameerasw.pixsl.utils.NostrCrypto
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class CommunityRepository(
    private val context: Context
) {
    // A list of reliable Nostr relays to connect to
    private val relays = listOf(
        "wss://nos.lol",
        "wss://relay.damus.io",
        "wss://relay.snort.social"
    )

    private val client = HttpClient(OkHttp) {
        install(WebSockets) {
            pingIntervalMillis = 20_000
        }
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val activeSessions = mutableListOf<WebSocketSession>()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + Job())

    // A shared flow where incoming Nostr events from the relays will be pushed
    private val _eventsFlow = MutableSharedFlow<NostrEvent>(replay = 50)
    val eventsFlow: SharedFlow<NostrEvent> = _eventsFlow.asSharedFlow()

    private var currentSubscriptionId: String? = null

    /**
     * Connects to all predefined relays and subscribes to the matching hashtag/kind
     */
    fun startListeningToCommunity(hashtag: String = "PixeLK") {
        val subId = UUID.randomUUID().toString()
        currentSubscriptionId = subId

        // Build our subscription filter: Kind 1 (Short Text Note) targeting the hashtag
        val filter = NostrFilter(
            kinds = listOf(1),
            tags = mapOf("t" to listOf(hashtag)),
            limit = 50
        )
        val reqMessage = NostrClientMessage.ReqMessage(subId, listOf(filter))
        val reqJson = json.encodeToString(NostrClientMessage.serializer(), reqMessage)

        relays.forEach { wsUrl ->
            repositoryScope.launch {
                try {
                    val session = client.webSocketSession(wsUrl)
                    activeSessions.add(session)
                    
                    // Send the REQ JSON to subscribe
                    session.send(Frame.Text(reqJson))

                    // Listen forever
                    for (frame in session.incoming) {
                        if (!isActive) break
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            try {
                                val serverMessage = json.decodeFromString(NostrServerMessage.serializer(), text)
                                if (serverMessage is NostrServerMessage.EventMessage && serverMessage.subscriptionId == subId) {
                                    // Push valid events to the UI
                                    _eventsFlow.emit(serverMessage.event)
                                } else if (serverMessage is NostrServerMessage.NoticeMessage) {
                                    Log.w("NostrRelay", "Notice securely from $wsUrl: ${serverMessage.message}")
                                }
                            } catch (e: Exception) {
                                Log.e("NostrRelay", "Failed to parse message from $wsUrl: $text", e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NostrRelay", "Connection failed to relay $wsUrl", e)
                } finally {
                    activeSessions.removeAll { !it.isActive }
                }
            }
        }
    }

    /**
     * Publishes a new note to the `#PixeLK` tag.
     */
    suspend fun publishPost(content: String): Boolean {
        // Retrieve keys from Secure Storage (SharedPreferences)
        val prefs = context.getSharedPreferences("pixsl_prefs", Context.MODE_PRIVATE)
        val privKeyHex = prefs.getString("nostr_private_key", null) ?: return false
        
        // Derive full byte-array private key to pass to the Native C engine for getting X-only pubkey
        val privKeyBytes = privKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val secp = fr.acinq.secp256k1.Secp256k1.get()
        val pubkeyCompressed = secp.pubKeyCompress(secp.pubkeyCreate(privKeyBytes))
        val pubKeyHex = pubkeyCompressed.copyOfRange(1, 33).joinToString("") { "%02x".format(it) }

        // Create and sign the exact NIP-01 Event natively
        val event = NostrCrypto.createSignedEvent(
            content = content,
            privKeyHex = privKeyHex,
            pubKeyHex = pubKeyHex,
            tags = listOf(listOf("t", "PixeLK"))
        )

        // Broadcast to all currently active relay connections
        val eventMessage = NostrClientMessage.EventMessage(event)
        val eventJson = json.encodeToString(NostrClientMessage.serializer(), eventMessage)

        var broadcastSuccess = false
        activeSessions.forEach { session ->
            if (session.isActive) {
                try {
                    session.send(Frame.Text(eventJson))
                    broadcastSuccess = true
                    Log.d("NostrRelay", "Pushed event to a relay")
                } catch (e: Exception) {
                    Log.e("NostrRelay", "Failed to push to relay", e)
                }
            }
        }
        return broadcastSuccess
    }

    /**
     * Closes the open WebSockets connections and destroys scope on logout/exit
     */
    fun stopListening() {
        val subId = currentSubscriptionId ?: return
        val closeMessage = NostrClientMessage.CloseMessage(subId)
        val closeJson = json.encodeToString(NostrClientMessage.serializer(), closeMessage)
        
        repositoryScope.launch {
            activeSessions.forEach { session ->
                if (session.isActive) {
                    try {
                        session.send(Frame.Text(closeJson))
                        session.close()
                    } catch (e: Exception) {
                        Log.e("NostrRelay", "Error closing session", e)
                    }
                }
            }
            activeSessions.clear()
        }
    }
}
