package com.sameerasw.pixsl.data

import com.sameerasw.pixsl.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

// Supabase client uses securely loaded keys from local.properties (via BuildConfig)
val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    install(Auth)
    install(Postgrest)
    install(ComposeAuth) {
        googleNativeLogin(serverClientId = BuildConfig.GOOGLE_CLIENT_ID)
    }
}
