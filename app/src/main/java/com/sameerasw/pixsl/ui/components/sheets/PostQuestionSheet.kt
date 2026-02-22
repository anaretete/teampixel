package com.sameerasw.pixsl.ui.components.sheets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sameerasw.pixsl.R
import com.sameerasw.pixsl.utils.HapticUtil
import com.sameerasw.pixsl.utils.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostQuestionSheet(
    sheetTitle: String? = null,
    actionLabel: String? = null,
    isUploading: Boolean = false,
    uploadedImageUrl: String? = null,
    onDismiss: () -> Unit,
    onPost: (String) -> Unit,
    onMediaPick: (android.net.Uri) -> Unit,
    onClearMedia: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current
    val view = LocalView.current
    var showPermissionSheet by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onMediaPick(uri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launcher.launch("image/*")
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                sheetTitle ?: stringResource(R.string.sheet_title_post),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.placeholder_post_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            // Image Preview Area
            if (isUploading || uploadedImageUrl != null) {
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(8.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else if (uploadedImageUrl != null) {
                        AsyncImage(
                            model = uploadedImageUrl,
                            contentDescription = stringResource(R.string.content_desc_preview),
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(200.dp)
                                .align(Alignment.CenterStart)
                                .padding(4.dp),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = onClearMedia,
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.content_desc_remove_media),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        if (PermissionUtils.hasMediaPermission(context)) {
                            launcher.launch("image/*")
                        } else {
                            showPermissionSheet = true
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = stringResource(R.string.content_desc_add_media),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        val finalContent =
                            if (uploadedImageUrl != null) "$text\n$uploadedImageUrl" else text
                        if (finalContent.isNotBlank()) onPost(finalContent)
                    },
                    enabled = (text.isNotBlank() || uploadedImageUrl != null) && !isUploading
                ) {
                    Text(actionLabel ?: stringResource(R.string.label_post))
                }
            }

            if (showPermissionSheet) {
                PermissionsBottomSheet(
                    onDismissRequest = { showPermissionSheet = false },
                    featureTitle = R.string.feat_media_upload_title,
                    permissions = listOf(
                        PermissionItem(
                            icon = Icons.Outlined.PhotoLibrary,
                            title = stringResource(R.string.perm_title_media),
                            dependentFeatures = listOf(R.string.feat_media_upload_title),
                            isGranted = false,
                            action = {
                                permissionLauncher.launch(PermissionUtils.getMediaPermission())
                                showPermissionSheet = false
                            }
                        )
                    )
                )
            }
        }
    }
}

