package com.sameerasw.pixsl.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sameerasw.pixsl.R
import com.sameerasw.pixsl.data.model.nostr.PostWithProfile
import com.sameerasw.pixsl.utils.HapticUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PostCard(
    post: PostWithProfile,
    currentNostrPubKey: String? = null,
    zapAmount: Long = 0,
    likeCount: Int = 0,
    repostCount: Int = 0,
    isLiked: Boolean = false,
    isReposted: Boolean = false,
    onLikeClick: (() -> Unit)? = null,
    onRepostClick: (() -> Unit)? = null,
    onZapClick: (() -> Unit)? = null,
    onReplyClick: (() -> Unit)? = null,
    onPostClick: (() -> Unit)? = null
) {
    val view = LocalView.current
    Card(
        onClick = { onPostClick?.invoke() },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val username = post.profile?.username ?: stringResource(R.string.label_anonymous)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = username,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (post.profile?.isExpert == true) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_check_circle_24),
                            contentDescription = stringResource(R.string.label_expert),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val timeString = sdf.format(Date(post.event.created_at * 1000))
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = post.event.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Media support: use the model helper
            val imageUrl = post.event.getMediaUrl()
            if (imageUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = imageUrl,
                    contentDescription = stringResource(R.string.content_desc_post_media),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interaction Row (Right Aligned)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reply
                IconButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onReplyClick?.invoke()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_chat_bubble_24),
                        contentDescription = stringResource(R.string.label_reply),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Repost
                IconButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onRepostClick?.invoke()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(
                                id = if (isReposted) R.drawable.rounded_repeat_on_24 else R.drawable.rounded_repeat_24
                            ),
                            contentDescription = stringResource(R.string.label_repost),
                            modifier = Modifier.size(20.dp),
                            tint = if (isReposted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (repostCount > 0) {
                            Text(
                                text = repostCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isReposted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Zap
                IconButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onZapClick?.invoke()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(
                                id = if (zapAmount > 0) R.drawable.round_bolt_24 else R.drawable.rounded_bolt_24
                            ),
                            contentDescription = stringResource(R.string.label_zap),
                            modifier = Modifier.size(20.dp),
                            tint = if (zapAmount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (zapAmount > 0) {
                            Text(
                                text = zapAmount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Like
                IconButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onLikeClick?.invoke()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(
                                id = if (isLiked) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24
                            ),
                            contentDescription = stringResource(R.string.label_like),
                            modifier = Modifier.size(20.dp),
                            tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (likeCount > 0) {
                            Text(
                                text = likeCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


