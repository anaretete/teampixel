package com.sameerasw.pixsl.ui.community

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.pixsl.ui.theme.PixeLKTheme
import com.sameerasw.pixsl.viewmodel.MainViewModel

class PostDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val postId = intent.getStringExtra("postId") ?: run {
            finish()
            return
        }

        setContent {
            PixeLKTheme {
                val mainViewModel: MainViewModel = viewModel()
                val authState by mainViewModel.authState.collectAsState()
                
                PostDetailScreen(
                    postId = postId,
                    currentUserId = (authState as? com.sameerasw.pixsl.data.model.AuthState.SignedIn)?.profile?.id,
                    currentNostrPubKey = (authState as? com.sameerasw.pixsl.data.model.AuthState.SignedIn)?.profile?.nostrPubKey,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
