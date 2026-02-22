package com.sameerasw.pixsl.domain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sameerasw.pixsl.R

enum class PixeLKTabs(@StringRes val title: Int, @DrawableRes val icon: Int) {
    HOME(R.string.tab_pixel, R.drawable.pixel_9), // Unused, we use dynamic device icon
    GUIDES(R.string.tab_guides, R.drawable.rounded_article_24),
    COMMUNITY(R.string.tab_community, R.drawable.rounded_forum_24)
}
