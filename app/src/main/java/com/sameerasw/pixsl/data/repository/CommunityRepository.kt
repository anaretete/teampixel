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
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

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
    private val activeSessions = CopyOnWriteArrayList<WebSocketSession>()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + Job())

    // A shared flow where incoming Nostr events from the relays will be pushed
    private val _eventsFlow = MutableSharedFlow<NostrEvent>(replay = 50)
    val eventsFlow: SharedFlow<NostrEvent> = _eventsFlow.asSharedFlow()

    private val activeSubIds = ConcurrentHashMap.newKeySet<String>()
    private var currentReactionsSubId: String? = null

    /**
     * Connects to all predefined relays and subscribes to the matching hashtag/kind
     */
    fun startListeningToCommunity(hashtag: String = "PixeLK") {
        val subId = "community-${UUID.randomUUID().toString().take(8)}"
        activeSubIds.add(subId)

        // Build our subscription filter: Kind 1 (Short Text Note) targeting the hashtag
        val filter = NostrFilter(
            kinds = listOf(
                1,
                6,
                7,
                9735
            ), // Kind 1 (Note), 6 (Repost), 7 (Like), 9735 (Zap Receipt)
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
                                val serverMessage =
                                    json.decodeFromString(NostrServerMessage.serializer(), text)
                                if (serverMessage is NostrServerMessage.EventMessage) {
                                    // Push valid events to the UI if they belong to our active subscriptions
                                    if (activeSubIds.contains(serverMessage.subscriptionId)) {
                                        _eventsFlow.emit(serverMessage.event)
                                    }
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
     * Subscribes to replies for a specific post.
     */
    fun startListeningToReplies(postId: String) {
        val subId = "replies-$postId-${UUID.randomUUID().toString().take(8)}"
        activeSubIds.add(subId)
        val filter = NostrFilter(
            kinds = listOf(1, 6, 7, 9735),
            tags = mapOf("e" to listOf(postId)),
            limit = 100
        )
        sendSubscription(subId, listOf(filter))
    }

    /**
     * Subscribes to reactions for a list of event IDs.
     */
    fun startListeningToReactions(eventIds: List<String>) {
        if (eventIds.isEmpty()) return

        // Close previous reaction subscription if it exists
        currentReactionsSubId?.let { oldSubId ->
            closeSubscription(oldSubId)
        }

        val subId = "reactions-${UUID.randomUUID().toString().take(8)}"
        currentReactionsSubId = subId
        activeSubIds.add(subId)
        val filter = NostrFilter(
            kinds = listOf(6, 7, 9735),
            tags = mapOf("e" to eventIds),
            limit = 500
        )
        sendSubscription(subId, listOf(filter))
    }

    private fun closeSubscription(subId: String) {
        activeSubIds.remove(subId)
        val closeMessage = NostrClientMessage.CloseMessage(subId)
        val closeJson = json.encodeToString(NostrClientMessage.serializer(), closeMessage)
        repositoryScope.launch {
            activeSessions.forEach { session ->
                if (session.isActive) {
                    try {
                        session.send(Frame.Text(closeJson))
                    } catch (e: Exception) {
                        Log.e("NostrRelay", "Error closing subscription $subId", e)
                    }
                }
            }
        }
    }

    private fun sendSubscription(subId: String, filters: List<NostrFilter>) {
        val reqMessage = NostrClientMessage.ReqMessage(subId, filters)
        val reqJson = json.encodeToString(NostrClientMessage.serializer(), reqMessage)

        repositoryScope.launch {
            activeSessions.forEach { session ->
                if (session.isActive) {
                    try {
                        session.send(Frame.Text(reqJson))
                    } catch (e: Exception) {
                        Log.e("NostrRelay", "Error sending subscription $subId", e)
                    }
                }
            }
        }
    }

    fun stopAllSubscriptions() {
        activeSubIds.forEach { subId ->
            val closeMessage = NostrClientMessage.CloseMessage(subId)
            val closeJson = json.encodeToString(NostrClientMessage.serializer(), closeMessage)
            repositoryScope.launch {
                activeSessions.forEach { session ->
                    if (session.isActive) {
                        try {
                            session.send(Frame.Text(closeJson))
                        } catch (e: Exception) {
                            Log.e("NostrRelay", "Error closing subscription $subId", e)
                        }
                    }
                }
            }
        }
        activeSubIds.clear()
    }

    fun close() {
        stopAllSubscriptions()
        repositoryScope.cancel()
        client.close()
    }

    /**
     * Publishes a new note. If replyToId is provided, it's a thread reply.
     */
    suspend fun publishPost(
        content: String,
        replyToId: String? = null,
        userId: String? = null
    ): Boolean {
        val keys = getEventKeys(userId) ?: return false

        val postTags = mutableListOf(listOf("t", "PixeLK"))
        if (replyToId != null) {
            postTags.add(listOf("e", replyToId, "", "reply"))
        }

        val event = NostrCrypto.createSignedEvent(
            content = content,
            privKeyHex = keys.first,
            pubKeyHex = keys.second,
            tags = postTags
        )

        return broadcastEvent(event)
    }

    /**
     * Publishes a Kind 5 deletion event to remove a given post by its event ID.
     */
    suspend fun deletePost(eventId: String, userId: String? = null): Boolean {
        val keys = getEventKeys(userId) ?: return false

        val event = NostrCrypto.createSignedEvent(
            content = "Deleted by user",
            privKeyHex = keys.first,
            pubKeyHex = keys.second,
            tags = listOf(listOf("e", eventId)),
            kind = 5
        )

        return broadcastEvent(event)
    }

    /**
     * Publishes a Kind 7 reaction (Like).
     */
    suspend fun likePost(eventId: String, authorPubKey: String, userId: String? = null): Boolean {
        val keys = getEventKeys(userId) ?: return false
        val event = NostrCrypto.createSignedEvent(
            content = "+",
            privKeyHex = keys.first,
            pubKeyHex = keys.second,
            tags = listOf(
                listOf("e", eventId),
                listOf("p", authorPubKey),
                listOf("t", "PixeLK")
            ),
            kind = 7
        )
        return broadcastEvent(event)
    }

    /**
     * Publishes a Kind 6 repost.
     */
    suspend fun repostPost(
        eventId: String,
        authorPubKey: String,
        content: String = "",
        userId: String? = null
    ): Boolean {
        val keys = getEventKeys(userId) ?: return false
        val event = NostrCrypto.createSignedEvent(
            content = content,
            privKeyHex = keys.first,
            pubKeyHex = keys.second,
            tags = listOf(
                listOf("e", eventId, "", "mention"),
                listOf("p", authorPubKey),
                listOf("t", "PixeLK")
            ),
            kind = 6
        )
        return broadcastEvent(event)
    }

    /**
     * Publishes a Kind 9734 Zap Request.
     * Note: This is a simplified version that creates the Zap Request event.
     * In a real app, this would be followed by a payment via LNURL.
     */
    suspend fun zapPost(
        eventId: String,
        authorPubKey: String,
        amount: Long,
        comment: String = "",
        userId: String? = null
    ): Boolean {
        val keys = getEventKeys(userId) ?: return false
        val tags = mutableListOf(
            listOf("e", eventId),
            listOf("p", authorPubKey),
            listOf("relays", relays.first()),
            listOf("amount", amount.toString())
        )

        val event = NostrCrypto.createSignedEvent(
            content = comment,
            privKeyHex = keys.first,
            pubKeyHex = keys.second,
            tags = tags,
            kind = 9734
        )
        return broadcastEvent(event)
    }

    fun getEventKeys(userId: String? = null): Pair<String, String>? {
        val prefs = context.getSharedPreferences("pixsl_prefs", Context.MODE_PRIVATE)
        val scopedKey = if (userId != null) "nostr_private_key_$userId" else "nostr_private_key"
        var privKeyHex = prefs.getString(scopedKey, null)

        // Fallback for cases where migration hasn't happened yet but we have a user context
        if (privKeyHex == null && userId != null) {
            privKeyHex = prefs.getString("nostr_private_key", null)
        }

        if (privKeyHex == null) return null

        return try {
            val privKeyBytes = privKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val secp = fr.acinq.secp256k1.Secp256k1.get()
            val pubkeyCompressed = secp.pubKeyCompress(secp.pubkeyCreate(privKeyBytes))
            val pubKeyHex =
                pubkeyCompressed.copyOfRange(1, 33).joinToString("") { "%02x".format(it) }
            Pair(privKeyHex, pubKeyHex)
        } catch (e: Exception) {
            Log.e("CommunityRepository", "Failed to derive keys", e)
            null
        }
    }

    private suspend fun broadcastEvent(event: NostrEvent): Boolean {
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

}
