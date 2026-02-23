package com.sameerasw.pixsl

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sameerasw.pixsl.data.supabase
import com.sameerasw.pixsl.domain.PixeLKTabs
import com.sameerasw.pixsl.ui.HomeScreen
import com.sameerasw.pixsl.ui.community.CommunityScreen
import com.sameerasw.pixsl.ui.components.PixeLKFloatingToolbar
import com.sameerasw.pixsl.ui.components.sheets.AboutAppSheet
import com.sameerasw.pixsl.ui.components.sheets.ProfileMenuSheet
import com.sameerasw.pixsl.ui.guides.GuidesScreen
import com.sameerasw.pixsl.ui.theme.PixeLKTheme
import com.sameerasw.pixsl.utils.HapticUtil
import com.sameerasw.pixsl.viewmodel.MainViewModel
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var isAppReady = false

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        HapticUtil.initialize(this)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { !isAppReady }

        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            try {
                val splashScreenView = splashScreenViewProvider.view
                val splashIcon = try {
                    splashScreenViewProvider.iconView
                } catch (e: Exception) {
                    null
                }

                val fadeOut = ObjectAnimator.ofFloat(splashScreenView, "alpha", 1f, 0f).apply {
                    interpolator = AnticipateInterpolator()
                    duration = 750
                }
                fadeOut.doOnEnd {
                    splashScreenViewProvider.remove()
                    enableEdgeToEdge()
                }

                try {
                    @Suppress("SENSELESS_COMPARISON")
                    if (splashIcon != null) {
                        val scaleDownX =
                            ObjectAnimator.ofFloat(splashIcon, "scaleX", 1f, 0f).apply {
                                interpolator = AnticipateInterpolator()
                                duration = 750
                            }

                        val scaleDownY =
                            ObjectAnimator.ofFloat(splashIcon, "scaleY", 1f, 0f).apply {
                                interpolator = AnticipateInterpolator()
                                duration = 750
                            }

                        scaleDownX.start()
                        scaleDownY.start()
                    } else {
                        Log.w("SplashScreen", "iconView is null - OEM device detected")
                    }
                } catch (e: NullPointerException) {
                    Log.w(
                        "SplashScreen",
                        "NullPointerException on iconView animation - likely OEM device",
                        e
                    )
                }

                fadeOut.start()
            } catch (e: Exception) {
                Log.e("SplashScreen", "Exception during splash screen animation", e)
                try {
                    splashScreenViewProvider.remove()
                } catch (e2: Exception) {
                    Log.e("SplashScreen", "Exception during splash screen removal", e2)
                }
            }
        }

        setContent {
            val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val pitchBlackTheme by viewModel.pitchBlackTheme.collectAsState()

            PixeLKTheme(pitchBlackTheme = pitchBlackTheme) {
                val authState by viewModel.authState.collectAsState()
                val deviceSpecs by viewModel.deviceSpecs.collectAsState()
                val isSpecsLoading by viewModel.isSpecsLoading.collectAsState()
                val tabs = PixeLKTabs.entries
                val pagerState = rememberPagerState(pageCount = { tabs.size })
                val view = LocalView.current
                val scope = rememberCoroutineScope()
                var showProfileMenu by remember { mutableStateOf(false) }
                var showAboutSheet by remember { mutableStateOf(false) }

                val context = androidx.compose.ui.platform.LocalContext.current
                val deviceInfo = remember { DeviceUtils.getDeviceInfo(context) }
                val googleSignInAction = supabase.composeAuth.rememberSignInWithGoogle(
                    onResult = { result ->
                        viewModel.onSignInResult(result, context as MainActivity)
                    }
                )

                Scaffold(
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(
                        0,
                        0,
                        0,
                        0
                    ),
                    modifier = Modifier
                        .fillMaxSize(),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
                    topBar = { }
                ) { innerPadding ->
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        isAppReady = true
                        viewModel.loadDeviceSpecs(context)
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        val signedInState =
                            authState as? com.sameerasw.pixsl.data.model.AuthState.SignedIn
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = -ScreenOffset - 12.dp)
                                .zIndex(1f)
                        ) {
                            PixeLKFloatingToolbar(
                                currentPage = pagerState.currentPage,
                                tabs = tabs,
                                onTabSelected = { index ->
                                    HapticUtil.performUIHaptic(view)
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                isSignedIn = signedInState != null,
                                avatarUrl = signedInState?.avatarUrl,
                                isExpert = signedInState?.profile?.isExpert ?: false,
                                username = signedInState?.profile?.username,
                                deviceInfo = deviceInfo,
                                onProfileClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    showProfileMenu = true
                                }
                            )

                            if (showProfileMenu) {
                                ProfileMenuSheet(
                                    authState = authState,
                                    pitchBlackTheme = pitchBlackTheme,
                                    onPitchBlackThemeChange = { viewModel.setPitchBlackTheme(it) },
                                    onAboutClick = { showAboutSheet = true },
                                    onDismissRequest = { showProfileMenu = false },
                                    onSignOutClick = { viewModel.signOut() },
                                    onSignInClick = { googleSignInAction.startFlow() }
                                )
                            }

                            if (showAboutSheet) {
                                AboutAppSheet(
                                    onDismissRequest = { showAboutSheet = false }
                                )
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.Top
                        ) { page ->
                            when (tabs[page]) {
                                PixeLKTabs.HOME -> {
                                    HomeScreen(
                                        authState = authState,
                                        deviceInfo = deviceInfo,
                                        deviceSpecs = deviceSpecs,
                                        isSpecsLoading = isSpecsLoading,
                                        hasRunStartupAnimation = viewModel.hasRunStartupAnimation,
                                        onAnimationRun = {
                                            viewModel.hasRunStartupAnimation = true
                                        },
                                        onSignInClick = { googleSignInAction.startFlow() },
                                        contentPadding = innerPadding
                                    )
                                }

                                PixeLKTabs.GUIDES -> {
                                    GuidesScreen(
                                        contentPadding = innerPadding,
                                        onGuideClick = { guide ->
                                            val intent = android.content.Intent(
                                                context,
                                                com.sameerasw.pixsl.ui.guides.ArticleDetailActivity::class.java
                                            ).apply {
                                                putExtra("title", guide.title)
                                                putExtra("content", guide.content)
                                                putExtra("date", guide.date)
                                                putExtra("readTime", guide.readTime)
                                            }
                                            context.startActivity(intent)
                                        }
                                    )
                                }

                                PixeLKTabs.COMMUNITY -> {
                                    CommunityScreen(
                                        contentPadding = innerPadding,
                                        currentUserId = (authState as? com.sameerasw.pixsl.data.model.AuthState.SignedIn)?.profile?.id,
                                        currentNostrPubKey = (authState as? com.sameerasw.pixsl.data.model.AuthState.SignedIn)?.profile?.nostrPubKey,
                                        onPostClick = { postId ->
                                            val intent = android.content.Intent(
                                                context,
                                                com.sameerasw.pixsl.ui.community.PostDetailActivity::class.java
                                            ).apply {
                                                putExtra("postId", postId)
                                            }
                                            context.startActivity(intent)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Removing previous extracted MainFeedScreen