package com.sameerasw.pixsl.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sameerasw.pixsl.data.model.Profile
import com.sameerasw.pixsl.data.model.nostr.NostrEvent
import com.sameerasw.pixsl.data.model.nostr.PostWithProfile
import com.sameerasw.pixsl.data.repository.CommunityRepository
import com.sameerasw.pixsl.data.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.net.Uri
import com.sameerasw.pixsl.data.repository.MediaRepository
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    private val _likeTally = MutableStateFlow<Map<String, Int>>(emptyMap())
    val likeTally: StateFlow<Map<String, Int>> = _likeTally.asStateFlow()

    private val _repostTally = MutableStateFlow<Map<String, Int>>(emptyMap())
    val repostTally: StateFlow<Map<String, Int>> = _repostTally.asStateFlow()

    private val profileCache = mutableMapOf<String, Profile>()

    init {
        repository.startListeningToCommunity("PixeLK")
        viewModelScope.launch {
            repository.eventsFlow.collect { event ->
                handleNewEvent(event)
            }
        }
    }

    private suspend fun handleNewEvent(event: NostrEvent) {
        val pubkey = event.pubkey
        
        // 1. Check local cache first
        var profile = profileCache[pubkey]
        if (profile == null) {
            // 2. Fetch from Supabase
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

        // 4. Handle Reactions
        when (event.kind) {
            7 -> { // Like
                val targetedEventId = event.getTag("e")
                if (targetedEventId != null) {
                    val currentTally = _likeTally.value.toMutableMap()
                    currentTally[targetedEventId] = (currentTally[targetedEventId] ?: 0) + 1
                    _likeTally.value = currentTally
                }
                return // Don't add to posts/replies
            }
            6 -> { // Repost
                val targetedEventId = event.getTag("e")
                if (targetedEventId != null) {
                    val currentTally = _repostTally.value.toMutableMap()
                    currentTally[targetedEventId] = (currentTally[targetedEventId] ?: 0) + 1
                    _repostTally.value = currentTally
                }
                return // Don't add to posts/replies
            }
            9735 -> { // Zap Receipt
                val targetedEventId = event.getTag("e")
                val amountTag = event.getTag("amount")?.toLongOrNull() ?: 0L 
                if (targetedEventId != null) {
                    val currentTally = _zapTally.value.toMutableMap()
                    currentTally[targetedEventId] = (currentTally[targetedEventId] ?: 0L) + (amountTag / 1000) // amount is millisats
                    _zapTally.value = currentTally
                }
                return // Don't add to posts/replies
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
            repository.publishPost(content, replyToId)
        }
    }

    fun likePost(eventId: String, authorPubKey: String) {
        viewModelScope.launch {
            repository.likePost(eventId, authorPubKey)
        }
    }

    fun repostPost(eventId: String, authorPubKey: String) {
        viewModelScope.launch {
            repository.repostPost(eventId, authorPubKey)
        }
    }

    fun zapPost(eventId: String, authorPubKey: String, amount: Long, comment: String = "") {
        viewModelScope.launch {
            repository.zapPost(eventId, authorPubKey, amount, comment)
        }
    }

    fun deletePost(eventId: String) {
        viewModelScope.launch {
            if (repository.deletePost(eventId)) {
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
        repository.stopListening()
    }
}
