package com.sameerasw.pixsl.data.model

sealed class AuthState {
    data object Loading : AuthState()
    data class SignedIn(
        val profile: Profile,
        val avatarUrl: String?,
        val email: String? = null
    ) : AuthState()

    data object SignedOut : AuthState()
}
