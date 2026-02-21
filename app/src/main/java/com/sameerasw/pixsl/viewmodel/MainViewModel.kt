package com.sameerasw.pixsl.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sameerasw.pixsl.data.model.AuthState
import com.sameerasw.pixsl.data.model.Profile
import com.sameerasw.pixsl.data.supabase
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.NativeSignInAction
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentSession()
    }

    private fun checkCurrentSession() {
        viewModelScope.launch {
            try {
                val user = supabase.auth.currentUserOrNull()
                if (user != null) {
                    val profile = fetchProfile(user.id)
                    val avatarUrl = user.userMetadata?.get("avatar_url")?.toString()
                        ?.trim('"')
                    _authState.value = AuthState.SignedIn(
                        profile = profile ?: Profile(id = user.id),
                        avatarUrl = avatarUrl
                    )
                } else {
                    _authState.value = AuthState.SignedOut
                }
            } catch (e: Exception) {
                _authState.value = AuthState.SignedOut
            }
        }
    }

    suspend fun signIn(action: NativeSignInAction) {
        action.startFlow()
    }

    fun onSignInResult(result: NativeSignInResult, context: Context) {
        if (result is NativeSignInResult.Success) {
            viewModelScope.launch {
                try {
                    val user = supabase.auth.currentUserOrNull() ?: return@launch
                    val avatarUrl = user.userMetadata?.get("avatar_url")?.toString()?.trim('"')

                    var profile = fetchProfile(user.id)

                    if (profile?.nostrPubKey == null) {
                        // New user: generate Nostr identity
                        val secKey = app.cash.nostrino.crypto.SecKeyGenerator().generate()

                        // Save private key locally using SharedPreferences
                        val prefs = context.getSharedPreferences("pixsl_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putString("nostr_private_key", secKey.hex).apply()

                        val upsertProfile = Profile(
                            id = user.id,
                            username = user.userMetadata?.get("full_name")?.toString()?.trim('"'),
                            nostrPubKey = secKey.pubKey.hex
                        )

                        supabase.from("profiles").upsert(upsertProfile)
                        profile = upsertProfile
                    }

                    _authState.value = AuthState.SignedIn(
                        profile = profile,
                        avatarUrl = avatarUrl
                    )
                } catch (e: Exception) {
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
