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
import kotlinx.coroutines.launch

class CommunityViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = CommunityRepository(application)

    private val _posts = MutableStateFlow<List<PostWithProfile>>(emptyList())
    val posts: StateFlow<List<PostWithProfile>> = _posts.asStateFlow()

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

        val newPost = PostWithProfile(event, profile)
        
        // 3. Add to list and sort by created_at descending (newest first)
        val updatedList = (_posts.value + newPost)
            .distinctBy { it.event.id }
            .sortedByDescending { it.event.created_at }

        _posts.value = updatedList
    }

    fun sendPost(content: String) {
        viewModelScope.launch {
            repository.publishPost(content)
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopListening()
    }
}
