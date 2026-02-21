package com.sameerasw.pixsl.ui.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.pixsl.ui.components.cards.PostCard
import com.sameerasw.pixsl.ui.components.containers.RoundedCardContainer
import com.sameerasw.pixsl.viewmodel.CommunityViewModel

import com.sameerasw.pixsl.ui.components.sheets.ZapAmountSheet

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onNavigateBack: () -> Unit,
    currentNostrPubKey: String? = null,
    viewModel: CommunityViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val repliesMap by viewModel.replies.collectAsState()
    val zapTally by viewModel.zapTally.collectAsState()
    var selectedZapPostId by remember { mutableStateOf<Pair<String, String>?>(null) } // postId, authorPubKey
    var showReplySheet by remember { mutableStateOf(false) }
    
    val parentPost = posts.find { it.event.id == postId }
    
    // Auto-fetch replies when screen opens
    LaunchedEffect(postId) {
        viewModel.fetchReplies(postId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thread") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (parentPost != null && currentNostrPubKey == parentPost.event.pubkey) {
                        IconButton(onClick = {
                            viewModel.deletePost(postId)
                            onNavigateBack()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Post",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showReplySheet = true }) {
                Icon(Icons.Default.AddComment, contentDescription = "Reply")
            }
        }
    ) { padding ->
        if (parentPost == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                androidx.compose.material3.LoadingIndicator()
            }
            return@Scaffold
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    RoundedCardContainer {
                        PostCard(
                            post = parentPost,
                            currentNostrPubKey = currentNostrPubKey,
                            zapAmount = zapTally[parentPost.event.id] ?: 0L,
                            onLikeClick = { viewModel.likePost(parentPost.event.id, parentPost.event.pubkey) },
                            onRepostClick = { viewModel.repostPost(parentPost.event.id, parentPost.event.pubkey) },
                            onZapClick = { selectedZapPostId = parentPost.event.id to parentPost.event.pubkey },
                            onReplyClick = { showReplySheet = true },
                            onPostClick = null 
                        )

                        val postReplies = repliesMap[postId] ?: emptyList()
                        postReplies.forEach { reply ->
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 24.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.width(24.dp))
                                PostCard(
                                    post = reply,
                                    currentNostrPubKey = currentNostrPubKey,
                                    zapAmount = zapTally[reply.event.id] ?: 0L,
                                    onLikeClick = { viewModel.likePost(reply.event.id, reply.event.pubkey) },
                                    onRepostClick = { viewModel.repostPost(reply.event.id, reply.event.pubkey) },
                                    onZapClick = { selectedZapPostId = reply.event.id to reply.event.pubkey },
                                    onReplyClick = { showReplySheet = true },
                                    onPostClick = null
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showReplySheet) {
            val isUploading by viewModel.isUploading.collectAsState()
            val uploadedImageUrl by viewModel.uploadedImageUrl.collectAsState()

            com.sameerasw.pixsl.ui.components.sheets.PostQuestionSheet(
                sheetTitle = "Reply to Thread",
                actionLabel = "Reply",
                isUploading = isUploading,
                uploadedImageUrl = uploadedImageUrl,
                onDismiss = { 
                    viewModel.clearUploadedMedia()
                    showReplySheet = false 
                },
                onMediaPick = { uri -> viewModel.uploadMedia(uri) },
                onClearMedia = { viewModel.clearUploadedMedia() },
                onPost = { content ->
                    viewModel.sendPost(content, replyToId = postId)
                    viewModel.clearUploadedMedia()
                    showReplySheet = false
                }
            )
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
}
