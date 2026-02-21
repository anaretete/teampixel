package com.sameerasw.pixsl.ui.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.pixsl.ui.components.cards.PostCard
import com.sameerasw.pixsl.ui.components.sheets.ZapAmountSheet
import com.sameerasw.pixsl.ui.components.sheets.PostQuestionSheet
import com.sameerasw.pixsl.data.model.nostr.PostWithProfile
import com.sameerasw.pixsl.ui.components.containers.RoundedCardContainer
import com.sameerasw.pixsl.viewmodel.CommunityViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CommunityScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    currentNostrPubKey: String? = null,
    onPostClick: (String) -> Unit = {},
    viewModel: CommunityViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val zapTally by viewModel.zapTally.collectAsState()
    var selectedZapPostId by remember { mutableStateOf<Pair<String, String>?>(null) } // postId, authorPubKey
    var showPostSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showPostSheet = true },
                modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding() + 48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Post to Community")
            }
        }
    ) { localPadding ->
        if (posts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(localPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.LoadingIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Waiting for messages from Nostr relays...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding() + 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 88.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    RoundedCardContainer {
                        posts.forEach { post ->
                            PostCard(
                                post = post,
                                currentNostrPubKey = currentNostrPubKey,
                                zapAmount = zapTally[post.event.id] ?: 0L,
                                onLikeClick = { viewModel.likePost(post.event.id, post.event.pubkey) },
                                onRepostClick = { viewModel.repostPost(post.event.id, post.event.pubkey) },
                                onZapClick = { selectedZapPostId = post.event.id to post.event.pubkey },
                                onReplyClick = { onPostClick(post.event.id) },
                                onPostClick = { onPostClick(post.event.id) }
                            )
                        }
                    }
                }
            }
        }

        if (showPostSheet) {
            val isUploading by viewModel.isUploading.collectAsState()
            val uploadedImageUrl by viewModel.uploadedImageUrl.collectAsState()
            
            PostQuestionSheet(
                isUploading = isUploading,
                uploadedImageUrl = uploadedImageUrl,
                onDismiss = { 
                    viewModel.clearUploadedMedia()
                    showPostSheet = false 
                },
                onMediaPick = { uri -> viewModel.uploadMedia(uri) },
                onClearMedia = { viewModel.clearUploadedMedia() },
                onPost = { content ->
                    viewModel.sendPost(content)
                    viewModel.clearUploadedMedia()
                    showPostSheet = false
                }
            )
        }
    }

    if (selectedZapPostId != null) {
        ZapAmountSheet(
            onDismiss = { selectedZapPostId = null },
            onZap = { amount ->
                viewModel.zapPost(selectedZapPostId!!.first, selectedZapPostId!!.second, amount)
                selectedZapPostId = null
            }
        )
    }
}
