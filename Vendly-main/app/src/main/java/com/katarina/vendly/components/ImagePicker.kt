package com.katarina.vendly.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePicker(
    selectedImage: Uri?,
    onImagePicked: (Uri) -> Unit,
    onRemove: (() -> Unit)? = null,
    size: Dp = 112.dp,
    ring: Brush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
        )
    ),
    showPreview: Boolean = true,
    triggerContent: (@Composable (open: () -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let(onImagePicked) }

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraImageUri?.let(onImagePicked)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val photoFile = File.createTempFile("profile_", ".jpg", context.cacheDir).apply {
                deleteOnExit()
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showPreview) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { showSheet = true },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(BorderStroke(2.dp, ring), CircleShape)
            )
            if (selectedImage != null) {
                AsyncImage(
                    model = selectedImage,
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    } else {
        if (triggerContent != null) {
            triggerContent { showSheet = true }
        } else {
            OutlinedButton(onClick = { showSheet = true }) {
                Text(if (selectedImage == null) "Pick a photo" else "Change photo")
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            scrimColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.32f),
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) }
        ) {
            ListItem(
                headlineContent = { Text("Take a photo") },
                supportingContent = { Text("Use your camera") },
                leadingContent = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) },
                modifier = Modifier.clickable {
                    showSheet = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        val photoFile = File.createTempFile("profile_", ".jpg", context.cacheDir).apply {
                            deleteOnExit()
                        }
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            photoFile
                        )
                        cameraImageUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )
            ListItem(
                headlineContent = { Text("Choose from gallery") },
                supportingContent = { Text("Pick an existing picture") },
                leadingContent = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
                modifier = Modifier.clickable {
                    showSheet = false
                    galleryLauncher.launch("image/*")
                }
            )
            AnimatedVisibility(visible = selectedImage != null && onRemove != null) {
                ListItem(
                    headlineContent = { Text("Remove photo") },
                    leadingContent = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    modifier = Modifier
                        .clickable {
                            showSheet = false
                            onRemove?.invoke()
                        }
                        .padding(bottom = 12.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}