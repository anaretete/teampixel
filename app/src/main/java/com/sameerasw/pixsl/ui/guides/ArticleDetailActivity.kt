package com.sameerasw.pixsl.ui.guides

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.pixsl.data.model.Guide
import com.sameerasw.pixsl.ui.theme.PixeLKTheme
import com.sameerasw.pixsl.viewmodel.MainViewModel

class ArticleDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Placeholder: In a real app, we'd pass the guide ID and fetch it
        // For now, we'll recreate the guide from intent extras or just use a placeholder
        val title = intent.getStringExtra("title") ?: "Article"
        val content = intent.getStringExtra("content") ?: "Content not found"
        val date = intent.getStringExtra("date") ?: ""
        val readTime = intent.getStringExtra("readTime") ?: ""

        val guide = Guide(
            id = "placeholder",
            title = title,
            description = "",
            imageUrl = "",
            content = content,
            date = date,
            readTime = readTime
        )

        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val pitchBlackTheme by mainViewModel.pitchBlackTheme.collectAsState()

            PixeLKTheme(pitchBlackTheme = pitchBlackTheme) {
                ArticleDetailScreen(
                    guide = guide,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
