package com.sameerasw.pixsl.ui.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.pixsl.R
import com.sameerasw.pixsl.ui.components.cards.PermissionCard
import com.sameerasw.pixsl.ui.components.containers.RoundedCardContainer

data class PermissionItem(
    val iconRes: Int,
    val title: Any,
    val dependentFeatures: List<Any> = emptyList(),
    val actionLabel: Any? = null,
    val action: (() -> Unit)? = null,
    val isGranted: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsBottomSheet(
    onDismissRequest: () -> Unit,
    featureTitle: Any,
    permissions: List<PermissionItem>
) {
    val resolvedTitle = when (featureTitle) {
        is Int -> stringResource(id = featureTitle)
        is String -> featureTitle
        else -> ""
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.requires_following_permissions, resolvedTitle),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            RoundedCardContainer {
                permissions.forEach { perm ->
                    PermissionCard(
                        iconRes = perm.iconRes,
                        title = perm.title,
                        dependentFeatures = perm.dependentFeatures,
                        actionLabel = perm.actionLabel ?: R.string.perm_action_enable,
                        isGranted = perm.isGranted,
                        onActionClick = { perm.action?.invoke() }
                    )
                }
            }
        }
    }
}
