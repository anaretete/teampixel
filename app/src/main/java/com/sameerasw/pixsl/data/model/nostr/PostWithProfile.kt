package com.sameerasw.pixsl.data.model.nostr

import com.sameerasw.pixsl.data.model.Profile

data class PostWithProfile(
    val event: NostrEvent,
    val profile: Profile? // Null if the user profile hasn't been fetched from Supabase yet, or if it's an external Nostr user
)
