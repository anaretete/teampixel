package com.sameerasw.pixsl.domain

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.ui.graphics.vector.ImageVector
import com.sameerasw.pixsl.R

enum class PixeLKTabs(@StringRes val title: Int, val icon: ImageVector) {
    HOME(R.string.tab_pixel, Icons.Default.Home),
    COMMUNITY(R.string.tab_community, Icons.Rounded.ChatBubble)
}
