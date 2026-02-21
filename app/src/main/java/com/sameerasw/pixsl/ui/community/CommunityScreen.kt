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
import com.sameerasw.pixsl.data.model.nostr.PostWithProfile
import com.sameerasw.pixsl.viewmodel.CommunityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CommunityScreen(
    modifier: Modifier = Modifier,
    viewModel: CommunityViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsState()
    var showPostSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { showPostSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Post to Community")
            }
        }
    ) { padding ->
        if (posts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Waiting for messages from Nostr relays...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(posts, key = { it.event.id }) { post ->
                    PostCard(post)
                }
            }
        }

        if (showPostSheet) {
            PostQuestionSheet(
                onDismiss = { showPostSheet = false },
                onPost = { content ->
                    viewModel.sendPost(content)
                    showPostSheet = false
                }
            )
        }
    }
}

@Composable
fun PostCard(post: PostWithProfile) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val username = post.profile?.username ?: "Anonymous"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = username,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (post.profile?.isExpert == true) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "Expert",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val timeString = sdf.format(Date(post.event.created_at * 1000))
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = post.event.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "npub...${post.event.pubkey.takeLast(6)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostQuestionSheet(
    onDismiss: () -> Unit,
    onPost: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Post to #PixeLK",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Ask the community or share a tip...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
            
            Button(
                onClick = { if (text.isNotBlank()) onPost(text) },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 16.dp),
                enabled = text.isNotBlank()
            ) {
                Text("Broadcast globally")
            }
        }
    }
}
