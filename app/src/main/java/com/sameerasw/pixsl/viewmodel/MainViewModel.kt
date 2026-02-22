package com.sameerasw.pixsl.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import com.sameerasw.pixsl.data.model.AuthState
import com.sameerasw.pixsl.data.model.Profile
import com.sameerasw.pixsl.data.supabase
import com.sameerasw.pixsl.utils.NostrCrypto
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val settings = Settings()

    private val _pitchBlackTheme = MutableStateFlow(settings.getBoolean("pitch_black_theme", false))
    val pitchBlackTheme: StateFlow<Boolean> = _pitchBlackTheme.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    var hasRunStartupAnimation = false

    init {
        checkCurrentSession()
    }

    fun setPitchBlackTheme(enabled: Boolean) {
        settings.putBoolean("pitch_black_theme", enabled)
        _pitchBlackTheme.value = enabled
    }

    private fun checkCurrentSession() {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        try {
                            val user = supabase.auth.currentUserOrNull()
                            if (user != null) {
                                val profile = fetchProfile(user.id)
                                val avatarUrl =
                                    user.userMetadata?.get("avatar_url")?.toString()?.trim('"')
                                _authState.value = AuthState.SignedIn(
                                    profile = profile ?: Profile(id = user.id),
                                    avatarUrl = avatarUrl,
                                    email = user.email
                                )
                            } else {
                                _authState.value = AuthState.SignedOut
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainViewModel", "checkCurrentSession failed", e)
                            _authState.value = AuthState.SignedOut
                        }
                    }

                    is SessionStatus.Initializing -> {
                        _authState.value = AuthState.Loading
                    }

                    is SessionStatus.NotAuthenticated, is SessionStatus.RefreshFailure -> {
                        _authState.value = AuthState.SignedOut
                    }

                    else -> {
                        // For any other unexpected state, assume signed out for safety
                        _authState.value = AuthState.SignedOut
                    }
                }
            }
        }
    }

    fun onSignInResult(result: NativeSignInResult, context: Context) {
        if (result is NativeSignInResult.Success) {
            _authState.value = AuthState.Loading
            viewModelScope.launch {
                try {
                    // Wait for the session to become authenticated before querying the user
                    supabase.auth.sessionStatus.first { it is SessionStatus.Authenticated }

                    val user = supabase.auth.currentUserOrNull() ?: return@launch
                    val avatarUrl = user.userMetadata?.get("avatar_url")?.toString()?.trim('"')

                    var profile = fetchProfile(user.id)

                    val prefs = context.getSharedPreferences("pixsl_prefs", Context.MODE_PRIVATE)
                    val scopedKey = "nostr_private_key_${user.id}"
                    var privKeyHex = prefs.getString(scopedKey, null)

                    // Migration/Recovery: If scoped key is missing, check if we have a legacy key that matches
                    if (privKeyHex == null && profile?.nostrPubKey != null) {
                        val legacyKey = prefs.getString("nostr_private_key", null)
                        if (legacyKey != null) {
                            val derivedPub = try {
                                val evenKey = NostrCrypto.getEvenKey(legacyKey)
                                NostrCrypto.pubKeyFor(evenKey)
                            } catch (e: Exception) {
                                null
                            }

                            if (derivedPub == profile.nostrPubKey) {
                                privKeyHex = NostrCrypto.getEvenKey(legacyKey)
                                prefs.edit().putString(scopedKey, privKeyHex).apply()
                            }
                        }
                    }

                    if (privKeyHex == null) {
                        // Truly missing local key: Generate a new one to enable reactions
                        val privKey = ByteArray(32)
                        java.security.SecureRandom().nextBytes(privKey)
                        val initialHex = privKey.joinToString("") { "%02x".format(it) }

                        // Enforce BIP340 (even y)
                        privKeyHex = NostrCrypto.getEvenKey(initialHex)
                        val pubKeyHex = NostrCrypto.pubKeyFor(privKeyHex)

                        prefs.edit().putString(scopedKey, privKeyHex).apply()

                        val upsertProfile = Profile(
                            id = user.id,
                            username = user.userMetadata?.get("full_name")?.toString()?.trim('"'),
                            nostrPubKey = pubKeyHex
                        )

                        supabase.from("profiles").upsert(upsertProfile)
                        profile = upsertProfile
                        android.util.Log.i(
                            "MainViewModel",
                            "Generated new Nostr identity for user ${user.id}"
                        )
                    }

                    _authState.value = AuthState.SignedIn(
                        profile = profile ?: Profile(id = user.id),
                        avatarUrl = avatarUrl,
                        email = user.email
                    )
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "onSignInResult failed", e)
                    _authState.value = AuthState.SignedOut
                }
            }
        } else {
            _authState.value = AuthState.SignedOut
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                supabase.auth.signOut()
            } catch (e: Exception) {
                // Signed out locally regardless
            } finally {
                _authState.value = AuthState.SignedOut
            }
        }
    }

    private suspend fun fetchProfile(userId: String): Profile? {
        return try {
            supabase.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<Profile>()
        } catch (e: Exception) {
            null
        }
    }
}
