package com.sameerasw.pixsl.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.pixsl.R
import com.sameerasw.pixsl.ui.components.containers.RoundedCardContainer
import com.sameerasw.pixsl.ui.theme.Shapes
import com.sameerasw.pixsl.utils.DeviceInfo
import com.sameerasw.pixsl.utils.DeviceUtils

@Composable
fun DeviceHeroCard(
    deviceInfo: DeviceInfo,
    modifier: Modifier = Modifier
) {
    RoundedCardContainer(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, shape = Shapes.extraSmall)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📱",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = MaterialTheme.typography.displayLarge.fontSize * 3
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // User-set Device Name
            Text(
                text = deviceInfo.deviceName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = com.sameerasw.pixsl.ui.theme.GoogleSansFlexRounded
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Manufacturer Model
            Text(
                text = "${deviceInfo.manufacturer.replaceFirstChar { it.uppercase() }} ${deviceInfo.model} (${deviceInfo.hardware})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

//            Spacer(modifier = Modifier.height(24.dp))

        }


        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, shape = Shapes.extraSmall)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Android Version: Android 15 (35)
            Text(
                text = "Android ${deviceInfo.androidVersion} (${deviceInfo.sdkInt})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
//            Spacer(modifier = Modifier.height(16.dp))
            
            
        }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, shape = Shapes.extraSmall)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Storage and Memory Info
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom =  8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.label_device_storage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${DeviceUtils.formatSize(deviceInfo.availableStorage)} / ${DeviceUtils.formatSize(deviceInfo.totalStorage)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.label_device_ram),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${DeviceUtils.formatSize(deviceInfo.availableRam)} / ${DeviceUtils.formatSize(deviceInfo.totalRam)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
