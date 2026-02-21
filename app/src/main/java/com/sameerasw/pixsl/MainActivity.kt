package com.sameerasw.pixsl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.pixsl.data.supabase
import com.sameerasw.pixsl.ui.community.CommunityScreen
import com.sameerasw.pixsl.ui.components.PixeLKTopAppBar
import com.sameerasw.pixsl.ui.theme.PixeLKTheme
import com.sameerasw.pixsl.utils.HapticUtil
import com.sameerasw.pixsl.viewmodel.MainViewModel
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
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

                val googleSignInAction = supabase.composeAuth.rememberSignInWithGoogle(
                    onResult = { result ->
                        viewModel.onSignInResult(result, this@MainActivity)
                    }
                )

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        PixeLKTopAppBar(
                            title = R.string.label_community,
                            scrollBehavior = scrollBehavior,
                            isSignedIn = authState is com.sameerasw.pixsl.data.model.AuthState.SignedIn,
                            avatarUrl = (authState as? com.sameerasw.pixsl.data.model.AuthState.SignedIn)?.avatarUrl,
                            onSignInClick = {
                                googleSignInAction.startFlow()
                            },
                            onSignOutClick = {
                                viewModel.signOut()
                            }
                        )
                    }
                ) { innerPadding ->
                    CommunityScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}