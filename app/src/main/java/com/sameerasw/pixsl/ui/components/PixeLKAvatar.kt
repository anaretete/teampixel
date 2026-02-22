package com.sameerasw.pixsl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sameerasw.pixsl.R

@Composable
fun PixeLKAvatar(
    avatarUrl: String?,
    username: String?,
    isExpert: Boolean,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    if (avatarUrl != null) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = stringResource(R.string.action_profile),
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .then(
                    if (isExpert) Modifier
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(2.dp)
                    else Modifier
                )
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .then(
                    if (isExpert) Modifier
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(2.dp)
                    else Modifier
                )
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            if (username?.isNotEmpty() == true) {
                Text(
                    text = username.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                androidx.compose.material3.Icon(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.rounded_person_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
        }
    }
}
