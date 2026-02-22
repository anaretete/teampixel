package com.sameerasw.pixsl.ui.guides

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.pixsl.R
import com.sameerasw.pixsl.data.model.Guide
import com.sameerasw.pixsl.ui.components.containers.RoundedCardContainer
import com.sameerasw.pixsl.ui.components.guides.GuideCard
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun GuidesScreen(
    onGuideClick: (Guide) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val guides = remember {
        listOf(
            Guide(
                id = "1",
                title = "Master Your Pixel Camera",
                description = "Learn how to use Night Sight, Magic Eraser, and Pro Controls to take stunning photos.",
                imageUrl = "",
                content = "Content for Master Your Pixel Camera...",
                date = "Oct 24, 2023",
                readTime = "5 min read"
            ),
            Guide(
                id = "2",
                title = "Battery Saving Tips",
                description = "Extend your Pixel's battery life with these simple settings and habits.",
                imageUrl = "",
                content = "Content for Battery Saving Tips...",
                date = "Oct 22, 2023",
                readTime = "3 min read"
            ),
            Guide(
                id = "3",
                title = "Android 15 Features",
                description = "Discover the best new features in the latest Android update for your Pixel.",
                imageUrl = "",
                content = "Content for Android 15 Features...",
                date = "Oct 20, 2023",
                readTime = "7 min read"
            ),
            Guide(
                id = "4",
                title = "Pixel Fold Multitasking",
                description = "Get the most out of your foldable screen with these multitasking shortcuts.",
                imageUrl = "",
                content = "Content for Pixel Fold Multitasking...",
                date = "Oct 18, 2023",
                readTime = "4 min read"
            ),
            Guide(
                id = "5",
                title = "Pixel Buds Pro Tips",
                description = "Maximize your audio experience with Spatial Audio and Multipoint connection.",
                imageUrl = "",
                content = "Content for Pixel Buds Pro Tips...",
                date = "Oct 15, 2023",
                readTime = "3 min read"
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(
                top = 0.dp,
                bottom = contentPadding.calculateBottomPadding() + 88.dp,
                start = 16.dp,
                end = 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
//        Text(
//            text = stringResource(R.string.label_guides),
//            style = MaterialTheme.typography.headlineSmall,
//            fontWeight = FontWeight.Bold,
//            color = MaterialTheme.colorScheme.onSurface,
//            modifier = Modifier.padding(vertical = 8.dp)
//        )

        RoundedCardContainer {
            guides.forEach { guide ->
                GuideCard(
                    guide = guide,
                    onClick = { onGuideClick(guide) }
                )
            }
        }
    }
}
