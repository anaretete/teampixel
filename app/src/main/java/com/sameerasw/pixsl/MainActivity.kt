package com.sameerasw.pixsl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.Bottom
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.pixsl.data.supabase
import com.sameerasw.pixsl.ui.HomeScreen
import com.sameerasw.pixsl.ui.community.CommunityScreen
import com.sameerasw.pixsl.ui.components.PixeLKTopAppBar
import com.sameerasw.pixsl.ui.components.PixeLKFloatingToolbar
import com.sameerasw.pixsl.domain.PixeLKTabs
import com.sameerasw.pixsl.ui.theme.PixeLKTheme
import com.sameerasw.pixsl.utils.HapticUtil
import com.sameerasw.pixsl.viewmodel.MainViewModel
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HapticUtil.initialize(this)
        enableEdgeToEdge()

        setContent {
            PixeLKTheme {
                val viewModel: MainViewModel = viewModel()
                val authState by viewModel.authState.collectAsState()
                val scope = rememberCoroutineScope()
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                    rememberTopAppBarState()
                )
                val exitAlwaysScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = Bottom)
                val tabs = PixeLKTabs.entries
                val pagerState = rememberPagerState(pageCount = { tabs.size })
                val view = LocalView.current

                val context = androidx.compose.ui.platform.LocalContext.current
                val googleSignInAction = supabase.composeAuth.rememberSignInWithGoogle(
                    onResult = { result ->
                        viewModel.onSignInResult(result, context as MainActivity)
                    }
                )

                Scaffold(
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .nestedScroll(exitAlwaysScrollBehavior),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        PixeLKTopAppBar(
                            title = tabs[pagerState.currentPage].title,
                            scrollBehavior = scrollBehavior,
                            isSignedIn = authState is com.sameerasw.pixsl.data.model.AuthState.SignedIn,
                            avatarUrl = (authState as? com.sameerasw.pixsl.data.model.AuthState.SignedIn)?.avatarUrl,
                            onSignInClick = { googleSignInAction.startFlow() },
                            onSignOutClick = { viewModel.signOut() }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        PixeLKFloatingToolbar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = -ScreenOffset - 12.dp)
                                .zIndex(1f),
                            currentPage = pagerState.currentPage,
                            tabs = tabs,
                            onTabSelected = { index ->
                                HapticUtil.performUIHaptic(view)
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            scrollBehavior = exitAlwaysScrollBehavior
                        )

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.Top
                        ) { page ->
                            when (tabs[page]) {
                                PixeLKTabs.HOME -> {
                                    HomeScreen(
                                        authState = authState,
                                        onSignInClick = { googleSignInAction.startFlow() },
                                        contentPadding = innerPadding
                                    )
                                }
                                PixeLKTabs.COMMUNITY -> {
                                    CommunityScreen(
                                        contentPadding = innerPadding,
                                        currentUserId = (authState as? com.sameerasw.pixsl.data.model.AuthState.SignedIn)?.profile?.id,
                                        currentNostrPubKey = (authState as? com.sameerasw.pixsl.data.model.AuthState.SignedIn)?.profile?.nostrPubKey,
                                        onPostClick = { postId ->
                                            val intent = android.content.Intent(context, com.sameerasw.pixsl.ui.community.PostDetailActivity::class.java).apply {
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