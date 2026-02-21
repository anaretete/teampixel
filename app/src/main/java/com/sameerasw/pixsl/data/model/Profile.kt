package com.sameerasw.pixsl.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val username: String? = null,
    @SerialName("nostr_pubkey")
    val nostrPubKey: String? = null,
    @SerialName("is_expert")
    val isExpert: Boolean = false
)
