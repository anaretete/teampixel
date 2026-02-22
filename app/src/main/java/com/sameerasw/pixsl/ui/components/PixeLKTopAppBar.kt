package com.sameerasw.pixsl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.sameerasw.pixsl.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.pixsl.ui.components.menus.SegmentedDropdownMenuItem
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sameerasw.pixsl.R
import com.sameerasw.pixsl.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PixeLKTopAppBar(
    title: Any,
    hasBack: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    subtitle: Any? = null,
    isSmall: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    avatarUrl: String? = null,
    isSignedIn: Boolean = false,
    isExpert: Boolean = false,
    username: String? = null,
    email: String? = null,
    onSignInClick: (() -> Unit)? = null,
    onSignOutClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    var showProfileMenu by remember { mutableStateOf(false) }

    val titleContent: @Composable () -> Unit = {
        val resolvedTitle = when (title) {
            is Int -> stringResource(id = title)
            is String -> title
            else -> ""
        }
        if (subtitle != null) {
            Column {
                Text(resolvedTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val resolvedSubtitle = when (subtitle) {
                    is Int -> stringResource(id = subtitle)
                    is String -> subtitle
                    else -> ""
                }
                Text(
                    resolvedSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Text(resolvedTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }

    val navigationIconContent: @Composable () -> Unit = {
        if (hasBack) {
            val view = LocalView.current
            IconButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onBackClick?.invoke()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }

    val actionsContent: @Composable RowScope.() -> Unit = {
        actions()

        val view = LocalView.current
        Box {
            IconButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (isSignedIn) {
                        showProfileMenu = true
                    } else {
                        onSignInClick?.invoke()
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                ),
                modifier = Modifier.size(48.dp)
            ) {
                PixeLKAvatar(
                    avatarUrl = if (isSignedIn) avatarUrl else null,
                    username = username,
                    isExpert = isExpert
                )
            }

            if (isSignedIn) {
                SegmentedDropdownMenu(
                    expanded = showProfileMenu,
                    onDismissRequest = { showProfileMenu = false }
                ) {
                    SegmentedDropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = username ?: stringResource(R.string.label_guest),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                if (email != null) {
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = { },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                    SegmentedDropdownMenuItem(
                        text = { Text(stringResource(R.string.action_sign_out)) },
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onSignOutClick?.invoke()
                            showProfileMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }
            }
        }
    }

    if (isSmall) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor),
            modifier = Modifier.padding(horizontal = 8.dp),
            title = titleContent,
            navigationIcon = navigationIconContent,
            actions = actionsContent,
            scrollBehavior = scrollBehavior
        )
    } else {
        LargeFlexibleTopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor),
            modifier = Modifier.padding(horizontal = 8.dp),
            expandedHeight = if (subtitle != null) 200.dp else 160.dp,
            collapsedHeight = 64.dp,
            title = titleContent,
            navigationIcon = navigationIconContent,
            actions = actionsContent,
            scrollBehavior = scrollBehavior
        )
    }
}
