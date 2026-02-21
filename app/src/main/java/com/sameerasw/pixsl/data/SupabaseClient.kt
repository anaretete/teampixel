package com.sameerasw.pixsl.data

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

// TODO: Replace with your actual Supabase project credentials
val supabase = createSupabaseClient(
    supabaseUrl = "https://your-project.supabase.co",
    supabaseKey = "your-anon-key"
) {
    install(Auth)
    install(Postgrest)
    install(ComposeAuth) {
        // TODO: Replace with your Google Web Client ID from the Supabase dashboard
        googleNativeLogin(serverClientId = "YOUR_WEB_CLIENT_ID_HERE.apps.googleusercontent.com")
    }
}
