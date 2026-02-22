package com.sameerasw.pixsl.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sameerasw.pixsl.data.model.Profile
import com.sameerasw.pixsl.data.model.nostr.NostrEvent
import com.sameerasw.pixsl.data.model.nostr.PostWithProfile
import com.sameerasw.pixsl.data.repository.CommunityRepository
import com.sameerasw.pixsl.data.repository.MediaRepository
import com.sameerasw.pixsl.data.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class CommunityViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = CommunityRepository(application)
    private val mediaRepository = MediaRepository(application)

    private val _posts = MutableStateFlow<List<PostWithProfile>>(emptyList())
    val posts: StateFlow<List<PostWithProfile>> = _posts.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadedImageUrl = MutableStateFlow<String?>(null)
    val uploadedImageUrl: StateFlow<String?> = _uploadedImageUrl.asStateFlow()

    private val _replies = MutableStateFlow<Map<String, List<PostWithProfile>>>(emptyMap())
    val replies: StateFlow<Map<String, List<PostWithProfile>>> = _replies.asStateFlow()

    private val _zapTally = MutableStateFlow<Map<String, Long>>(emptyMap())
    val zapTally: StateFlow<Map<String, Long>> = _zapTally.asStateFlow()

    private val _likeTally = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val likeTally: StateFlow<Map<String, Set<String>>> = _likeTally.asStateFlow()

    private val _repostTally = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val repostTally: StateFlow<Map<String, Set<String>>> = _repostTally.asStateFlow()

    private val _currentNostrPubKey = MutableStateFlow<String?>(null)
    val currentNostrPubKey: StateFlow<String?> = _currentNostrPubKey.asStateFlow()

    private var currentUserId: String? = null
    private val profileCache = mutableMapOf<String, Profile>()
    private val seenEventIds = ConcurrentHashMap.newKeySet<String>()

    init {
        repository.startListeningToCommunity("PixeLK")

        viewModelScope.launch {
            repository.eventsFlow.collect { event ->
                handleNewEvent(event)
            }
        }

        // Sync reactions for loaded posts
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            _posts.debounce(1000).collect { currentPosts ->
                val ids = currentPosts.map { it.event.id }
                if (ids.isNotEmpty()) {
                    repository.startListeningToReactions(ids)
                }
            }
        }
    }

    fun setCurrentUser(userId: String?, pubKey: String?) {
        currentUserId = userId
        _currentNostrPubKey.value = pubKey
    }

    private suspend fun handleNewEvent(event: NostrEvent) {
        // 1. Deduplicate by event ID
        if (!seenEventIds.add(event.id)) return

        // 2. Handle Reactions FIRST (No profile needed for counts)
        when (event.kind) {
            7 -> { // Like
                val targetedEventId = event.getTag("e")
                if (targetedEventId != null) {
                    val currentTally = _likeTally.value.toMutableMap()
                    val currentLikes =
                        currentTally[targetedEventId]?.toMutableSet() ?: mutableSetOf()
                    currentLikes.add(event.pubkey)
                    currentTally[targetedEventId] = currentLikes
                    _likeTally.value = currentTally
                }
                return // Don't add to posts/replies
            }

            6 -> { // Repost
                val targetedEventId = event.getTag("e")
                if (targetedEventId != null) {
                    val currentTally = _repostTally.value.toMutableMap()
                    val currentReposts =
                        currentTally[targetedEventId]?.toMutableSet() ?: mutableSetOf()
                    currentReposts.add(event.pubkey)
                    currentTally[targetedEventId] = currentReposts
                    _repostTally.value = currentTally
                }
                return // Don't add to posts/replies
            }

            9735 -> { // Zap Receipt
                val targetedEventId = event.getTag("e")
                val amountTag = event.getTag("amount")?.toLongOrNull() ?: 0L
                if (targetedEventId != null) {
                    val currentTally = _zapTally.value.toMutableMap()
                    currentTally[targetedEventId] = (currentTally[targetedEventId]
                        ?: 0L) + (amountTag / 1000) // amount is millisats
                    _zapTally.value = currentTally
                }
                return // Don't add to posts/replies
            }
        }

        val pubkey = event.pubkey

        // 2. Fetch profile for main content (Kind 1)
        var profile = profileCache[pubkey]
        if (profile == null) {
            try {
                // Query the profiles table where nostr_pubkey matches
                val fetchedProfiles = supabase.from("profiles")
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("nostr_pubkey", pubkey)
                        }
                    }.decodeList<Profile>()

                profile = fetchedProfiles.firstOrNull()
                if (profile != null) {
                    profileCache[pubkey] = profile
                }
            } catch (e: Exception) {
                Log.e("CommunityViewModel", "Failed to fetch profile for $pubkey", e)
            }
        }

        // Only Kind 1 (Note) should reach here for posts/replies
        if (event.kind != 1) return

        val newPost = PostWithProfile(event, profile)

        // 3. Check if it's a reply
        val eTag = event.tags.firstOrNull { it.isNotEmpty() && it[0] == "e" }
        val parentId = eTag?.getOrNull(1)

        if (parentId != null) {
            // Add to replies map
            val currentMap = _replies.value.toMutableMap()
            val currentReplies = currentMap[parentId]?.toMutableList() ?: mutableListOf()

            // Avoid duplicates
            if (currentReplies.none { it.event.id == event.id }) {
                currentReplies.add(newPost)
                currentReplies.sortBy { it.event.created_at } // oldest first for replies usually
                currentMap[parentId] = currentReplies
                _replies.value = currentMap
            }
        } else {
            // Add to main list and sort by created_at descending (newest first)
            val updatedList = (_posts.value + newPost)
                .distinctBy { it.event.id }
                .sortedByDescending { it.event.created_at }
            _posts.value = updatedList
        }
    }

    fun fetchReplies(postId: String) {
        repository.startListeningToReplies(postId)
    }

    fun sendPost(content: String, replyToId: String? = null) {
        viewModelScope.launch {
            repository.publishPost(content, replyToId, currentUserId)
        }
    }

    fun likePost(eventId: String, authorPubKey: String) {
        viewModelScope.launch {
            repository.likePost(eventId, authorPubKey, currentUserId)
        }
    }

    fun repostPost(eventId: String, authorPubKey: String) {
        viewModelScope.launch {
            repository.repostPost(eventId, authorPubKey, "", currentUserId)
        }
    }

    fun zapPost(eventId: String, authorPubKey: String, amount: Long, comment: String = "") {
        viewModelScope.launch {
            repository.zapPost(eventId, authorPubKey, amount, comment, currentUserId)
        }
    }

    fun deletePost(eventId: String) {
        viewModelScope.launch {
            if (repository.deletePost(eventId, currentUserId)) {
                _posts.value = _posts.value.filterNot { it.event.id == eventId }
            }
        }
    }

    fun uploadMedia(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            val url = mediaRepository.uploadImage(uri)
            _uploadedImageUrl.value = url
            _isUploading.value = false
        }
    }

    fun clearUploadedMedia() {
        _uploadedImageUrl.value = null
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}
