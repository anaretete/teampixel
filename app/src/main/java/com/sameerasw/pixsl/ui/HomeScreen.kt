package com.sameerasw.pixsl.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.sameerasw.pixsl.data.model.AuthState
import com.sameerasw.pixsl.ui.components.home.DeviceHeroCard
import com.sameerasw.pixsl.ui.components.home.DeviceSpecsCard
import com.sameerasw.pixsl.ui.components.home.SignInPromptCard
import com.sameerasw.pixsl.utils.DeviceInfo
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    authState: AuthState,
    deviceInfo: DeviceInfo,
    hasRunStartupAnimation: Boolean,
    onAnimationRun: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var isStartupAnimationRunning by remember { mutableStateOf(hasRunStartupAnimation) }

    LaunchedEffect(hasRunStartupAnimation) {
        if (!hasRunStartupAnimation) {
            delay(600) // Wait for splash screen exit animation to finish
            isStartupAnimationRunning = true
            onAnimationRun()
        }
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    // Calculate initial offset to center the 480dp image box
    // 64dp accounts for status bar and top padding roughly
    val initialImageOffset = (screenHeight / 2) - 240.dp - 64.dp

    val imageOffset by animateDpAsState(
        targetValue = if (isStartupAnimationRunning) 0.dp else initialImageOffset,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "imageOffset"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isStartupAnimationRunning) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 250, easing = LinearEasing),
        label = "contentAlpha"
    )

    val contentOffset by animateDpAsState(
        targetValue = if (isStartupAnimationRunning) 0.dp else 40.dp,
        animationSpec = tween(
            durationMillis = 500,
            delayMillis = 250,
            easing = FastOutSlowInEasing
        ),
        label = "contentOffset"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(
                top = 0.dp,
                bottom = contentPadding.calculateBottomPadding() + 128.dp,
                start = 16.dp,
                end = 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DeviceHeroCard(
            deviceInfo = deviceInfo,
            imageOffset = imageOffset,
            contentAlpha = contentAlpha,
            contentOffset = contentOffset
        )

        DeviceSpecsCard(
            modifier = Modifier.graphicsLayer {
                alpha = contentAlpha
                translationY = contentOffset.toPx()
            }
        )

        if (authState is AuthState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .graphicsLayer {
                        alpha = contentAlpha
                        translationY = contentOffset.toPx()
                    },
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        }

        if (authState is AuthState.SignedOut) {
            SignInPromptCard(
                onSignInClick = onSignInClick,
                modifier = Modifier.graphicsLayer {
                    alpha = contentAlpha
                    translationY = contentOffset.toPx()
                }
            )
        }
    }
}

