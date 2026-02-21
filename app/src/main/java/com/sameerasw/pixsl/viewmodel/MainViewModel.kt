package com.sameerasw.pixsl.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sameerasw.pixsl.data.model.AuthState
import com.sameerasw.pixsl.data.model.Profile
import com.sameerasw.pixsl.data.supabase
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import io.github.jan.supabase.auth.status.SessionStatus

class MainViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentSession()
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
                                val avatarUrl = user.userMetadata?.get("avatar_url")?.toString()?.trim('"')
                                _authState.value = AuthState.SignedIn(
                                    profile = profile ?: Profile(id = user.id),
                                    avatarUrl = avatarUrl
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

                    if (profile?.nostrPubKey == null) {
                        // New user: generate Nostr identity (32-byte private key)
                        val privKey = ByteArray(32)
                        java.security.SecureRandom().nextBytes(privKey)
                        val privKeyHex = privKey.joinToString("") { "%02x".format(it) }

                        // Generate x-only public key for Nostr (Schnorr)
                        val secp256k1 = fr.acinq.secp256k1.Secp256k1.get()
                        val compressedPubKey = secp256k1.pubKeyCompress(secp256k1.pubkeyCreate(privKey))
                        // Nostr strictly uses the 32-byte x-coordinate (bytes 1 through 32 of compressed key)
                        val pubKeyHex = compressedPubKey.copyOfRange(1, 33).joinToString("") { "%02x".format(it) }

                        val prefs = context.getSharedPreferences("pixsl_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putString("nostr_private_key", privKeyHex).apply()

                        val upsertProfile = Profile(
                            id = user.id,
                            username = user.userMetadata?.get("full_name")?.toString()?.trim('"'),
                            nostrPubKey = pubKeyHex
                        )

                        supabase.from("profiles").upsert(upsertProfile)
                        profile = upsertProfile
                    }

                    _authState.value = AuthState.SignedIn(
                        profile = profile,
                        avatarUrl = avatarUrl
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
