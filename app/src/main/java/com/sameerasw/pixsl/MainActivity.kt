package com.sameerasw.pixsl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.Bottom
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
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
                val tabs = PixeLKTabs.entries
                val pagerState = rememberPagerState(pageCount = { tabs.size })
                val view = LocalView.current
                val scope = rememberCoroutineScope()
                var showProfileMenu by remember { mutableStateOf(false) }

                val context = androidx.compose.ui.platform.LocalContext.current
                val googleSignInAction = supabase.composeAuth.rememberSignInWithGoogle(
                    onResult = { result ->
                        viewModel.onSignInResult(result, context as MainActivity)
                    }
                )

                Scaffold(
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                    modifier = Modifier
                        .fillMaxSize(),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
                    topBar = { }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        val signedInState = authState as? com.sameerasw.pixsl.data.model.AuthState.SignedIn
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
                                onProfileClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    if (signedInState != null) {
                                        showProfileMenu = true
                                    } else {
                                        googleSignInAction.startFlow()
                                    }
                                }
                            )

                            if (showProfileMenu && signedInState != null) {
                                androidx.compose.material3.ModalBottomSheet(
                                    onDismissRequest = { showProfileMenu = false },
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    tonalElevation = 8.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Header
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            com.sameerasw.pixsl.ui.components.PixeLKAvatar(
                                                avatarUrl = signedInState.avatarUrl,
                                                username = signedInState.profile?.username,
                                                isExpert = signedInState.profile?.isExpert ?: false,
                                                size = 48.dp
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text = signedInState.profile?.username ?: stringResource(R.string.label_guest),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                )
                                                if (signedInState.email != null) {
                                                    Text(
                                                        text = signedInState.email,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                        // Profile Option
                                        androidx.compose.material3.ListItem(
                                            headlineContent = { Text(stringResource(R.string.action_profile)) },
                                            leadingContent = {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.Person,
                                                    contentDescription = null
                                                )
                                            },
                                            modifier = Modifier.clickable { /* Handle profile */ }
                                        )

                                        // Sign Out Option
                                        androidx.compose.material3.ListItem(
                                            headlineContent = { 
                                                Text(
                                                    stringResource(R.string.action_sign_out),
                                                    color = MaterialTheme.colorScheme.error
                                                ) 
                                            },
                                            leadingContent = {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ExitToApp,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            modifier = Modifier.clickable {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                viewModel.signOut()
                                                showProfileMenu = false
                                            }
                                        )
                                    }
                                }
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