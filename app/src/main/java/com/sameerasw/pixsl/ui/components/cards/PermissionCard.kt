package com.sameerasw.pixsl.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.pixsl.R
import com.sameerasw.pixsl.utils.HapticUtil

@Composable
fun PermissionCard(
    iconRes: Int,
    title: Any,
    dependentFeatures: List<Any>,
    actionLabel: Any = R.string.perm_action_grant,
    isGranted: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val grantedGreen = Color(0xFF4CAF50)
    val view = LocalView.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = if (isGranted) grantedGreen else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val resolvedTitle = when (title) {
                        is Int -> stringResource(id = title)
                        is String -> title
                        else -> ""
                    }
                    Text(text = resolvedTitle, style = MaterialTheme.typography.titleMedium)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Required for:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    dependentFeatures.forEach { f ->
                        val resolvedFeature = when (f) {
                            is Int -> stringResource(id = f)
                            is String -> f
                            else -> ""
                        }
                        Text(
                            text = "• $resolvedFeature",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val resolvedActionLabel = when (actionLabel) {
                is Int -> stringResource(id = actionLabel)
                is String -> actionLabel
                else -> ""
            }

            if (isGranted) {
                OutlinedButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onActionClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                ) {
                    Text("Permission Granted")
                }
            } else {
                Button(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onActionClick()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(resolvedActionLabel)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_arrow_forward_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
