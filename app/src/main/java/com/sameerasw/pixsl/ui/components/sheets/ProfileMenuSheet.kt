package com.sameerasw.pixsl.ui.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.pixsl.R
import com.sameerasw.pixsl.data.model.AuthState
import com.sameerasw.pixsl.ui.components.PixeLKAvatar
import com.sameerasw.pixsl.ui.components.containers.RoundedCardContainer
import com.sameerasw.pixsl.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMenuSheet(
    authState: AuthState,
    pitchBlackTheme: Boolean,
    onPitchBlackThemeChange: (Boolean) -> Unit,
    useGSMArena: Boolean,
    onUseGSMArenaChange: (Boolean) -> Unit,
    onAboutClick: () -> Unit,
    onDismissRequest: () -> Unit,
    onSignOutClick: () -> Unit,
    onSignInClick: () -> Unit
) {
    val view = LocalView.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val signedInState = authState as? AuthState.SignedIn

            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixeLKAvatar(
                    avatarUrl = signedInState?.avatarUrl,
                    username = signedInState?.profile?.username,
                    isExpert = signedInState?.profile?.isExpert ?: false,
                    size = 56.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = signedInState?.profile?.username
                            ?: stringResource(R.string.label_guest),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (signedInState?.email != null) {
                        Text(
                            text = signedInState.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Menu Items in Rounded Container
            RoundedCardContainer(
                spacing = 2.dp
            ) {
                if (signedInState != null) {
                    ProfileMenuItem(
                        icon = R.drawable.rounded_person_24,
                        label = stringResource(R.string.action_profile),
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            // TODO: Navigate to profile
                            onDismissRequest()
                        }
                    )

                    ProfileMenuItem(
                        icon = R.drawable.rounded_logout_24,
                        label = stringResource(R.string.action_sign_out),
                        labelColor = MaterialTheme.colorScheme.error,
                        iconColor = MaterialTheme.colorScheme.error,
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onSignOutClick()
                            onDismissRequest()
                        }
                    )
                } else {
                    ProfileMenuItem(
                        icon = R.drawable.rounded_person_24,
                        label = stringResource(R.string.action_sign_in),
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            onSignInClick()
                            onDismissRequest()
                        }
                    )
                }
            }

            // Settings Container
            RoundedCardContainer(
                spacing = 2.dp
            ) {
                ProfileMenuToggleItem(
                    icon = R.drawable.rounded_dark_mode_24,
                    label = stringResource(R.string.label_pitch_black_theme),
                    checked = pitchBlackTheme,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onPitchBlackThemeChange(it)
                    }
                )

                ProfileMenuToggleItem(
                    icon = R.drawable.ic_launcher_foreground, // Fallback icon, or pick a better one if available
                    label = stringResource(R.string.label_use_gsmarena),
                    checked = useGSMArena,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onUseGSMArenaChange(it)
                    }
                )

                ProfileMenuItem(
                    icon = R.drawable.rounded_info_24,
                    label = stringResource(R.string.label_about),
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onDismissRequest() // Close profile sheet
                        onAboutClick()     // Open about sheet
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: Int,
    label: String,
    labelColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    iconColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.extraSmall, // Essentials style: sharper corners inside container
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = labelColor,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(id = R.drawable.rounded_chevron_forward_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ProfileMenuToggleItem(
    icon: Int,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        shape = MaterialTheme.shapes.extraSmall,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
