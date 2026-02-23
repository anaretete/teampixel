package com.sameerasw.pixsl.ui.components

import DeviceImageMapper
import DeviceInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.pixsl.R
import com.sameerasw.pixsl.domain.PixeLKTabs
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PixeLKFloatingToolbar(
    modifier: Modifier = Modifier,
    currentPage: Int,
    tabs: List<PixeLKTabs>,
    onTabSelected: (Int) -> Unit,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    badges: Map<PixeLKTabs, Boolean> = emptyMap(),
    // Profile related
    isSignedIn: Boolean = false,
    avatarUrl: String? = null,
    isExpert: Boolean = false,
    username: String? = null,
    deviceInfo: DeviceInfo? = null,
    onProfileClick: () -> Unit = {}
) {
    val expanded = true
    var bumpingTab by remember { mutableIntStateOf(-1) }
    var bumpKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(bumpKey) {
        if (bumpingTab >= 0) {
            delay(200)
            bumpingTab = -1
        }
    }

    val toolbarScale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "toolbar_scale"
    )

    HorizontalFloatingToolbar(
        modifier = modifier
            .graphicsLayer {
                scaleX = toolbarScale
                scaleY = toolbarScale
            },
        expanded = expanded,
        scrollBehavior = scrollBehavior,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
            toolbarContentColor = MaterialTheme.colorScheme.onSurface,
            toolbarContainerColor = MaterialTheme.colorScheme.primary,
        ),
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = currentPage == index
                    val isBumping = bumpingTab == index
                    val isHomeTab = tab == PixeLKTabs.HOME

                    val itemScale by animateFloatAsState(
                        targetValue = if (isBumping) 1.2f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessHigh
                        ),
                        label = "item_scale_$index"
                    )

                    ToolbarItem(
                        iconRes = tab.icon,
                        label = stringResource(id = tab.title),
                        isSelected = isSelected,
                        hasBadge = badges[tab] == true,
                        isHomeTab = isHomeTab,
                        deviceInfo = deviceInfo,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                bumpingTab = index
                                bumpKey++
                                onTabSelected(index)
                            }
                    )
                }

                // Profile Item
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    PixeLKAvatar(
                        avatarUrl = avatarUrl,
                        username = username,
                        isExpert = isExpert,
                        size = 42.dp
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PixeLKActionToolbar(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onShareClick: (() -> Unit)? = null,
    scrollBehavior: FloatingToolbarScrollBehavior? = null
) {
    val expanded = true
    val view = androidx.compose.ui.platform.LocalView.current

    HorizontalFloatingToolbar(
        modifier = modifier,
        expanded = expanded,
        scrollBehavior = scrollBehavior,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
            toolbarContentColor = MaterialTheme.colorScheme.onSurface,
            toolbarContainerColor = MaterialTheme.colorScheme.primary,
        ),
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Back Button
                ToolbarActionButton(
                    icon = R.drawable.rounded_arrow_back_24,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = {
                        com.sameerasw.pixsl.utils.HapticUtil.performUIHaptic(view)
                        onBackClick()
                    }
                )


                // Share Button
                if (onShareClick != null) {
                    ToolbarActionButton(
                        icon = R.drawable.rounded_share_24,
                        contentDescription = stringResource(R.string.action_share),
                        onClick = {
                            com.sameerasw.pixsl.utils.HapticUtil.performUIHaptic(view)
                            onShareClick()
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun ToolbarActionButton(
    icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ToolbarItem(
// ... (rest of the file)
    @androidx.annotation.DrawableRes iconRes: Int,
    label: String,
    isSelected: Boolean,
    hasBadge: Boolean,
    isHomeTab: Boolean = false,
    deviceInfo: DeviceInfo? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.background
                else Color.Transparent,
                CircleShape
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box {
            if (isHomeTab && deviceInfo != null) {
                Icon(
                    painter = painterResource(
                        id = DeviceImageMapper.getDeviceDrawable(
                            deviceInfo.model
                        )
                    ),
                    contentDescription = label,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            // Expand the thin strokes slightly for icon visibility
                            scaleX = 1.1f
                            scaleY = 1.1f
                        }
                        .drawWithCache {
                            onDrawWithContent {
                                // Draw the icon slightly shifted in all directions to simulate a stroke
                                val strokeOffset = 0.5.dp.toPx()
                                drawContent()

                                translate(left = strokeOffset, top = 0f) {
                                    this@onDrawWithContent.drawContent()
                                }
                                translate(left = -strokeOffset, top = 0f) {
                                    this@onDrawWithContent.drawContent()
                                }
                                translate(left = 0f, top = strokeOffset) {
                                    this@onDrawWithContent.drawContent()
                                }
                                translate(left = 0f, top = -strokeOffset) {
                                    this@onDrawWithContent.drawContent()
                                }
                            }
                        }
                )
            } else {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.background,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (hasBadge) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                ) {
                    drawCircle(color = Color.Red)
                }
            }
        }

        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Row {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }
    }
}
