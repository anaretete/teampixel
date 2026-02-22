package com.sameerasw.pixsl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sameerasw.pixsl.R
import com.sameerasw.pixsl.data.model.AuthState
import com.sameerasw.pixsl.ui.components.containers.RoundedCardContainer
import com.sameerasw.pixsl.utils.HapticUtil
import androidx.compose.ui.platform.LocalView

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import com.sameerasw.pixsl.utils.DeviceUtils

import com.sameerasw.pixsl.ui.components.home.DeviceHeroCard
import com.sameerasw.pixsl.ui.components.home.DeviceSpecsCard
import com.sameerasw.pixsl.ui.components.home.SignInPromptCard

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    authState: AuthState,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 88.dp,
                start = 16.dp,
                end = 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val deviceInfo = remember { DeviceUtils.getDeviceInfo(context) }
        
        DeviceHeroCard(deviceInfo = deviceInfo)

        DeviceSpecsCard()

        if (authState is AuthState.Loading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        }

        if (authState is AuthState.SignedOut) {
            SignInPromptCard(onSignInClick = onSignInClick)
        }
    }
}

