package com.katarina.vendly.ui.pages.addvending

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.rememberAsyncImagePainter
import com.katarina.vendly.components.ImagePicker
import com.katarina.vendly.domain.model.vm.VendingStatus
import com.katarina.vendly.ui.pages.map.MapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVendingScreen(
    navController: NavController,
    vm: AddVendingViewModel = viewModel()
) {
    val currentEntry by navController.currentBackStackEntryAsState()
    val mapBackEntry = remember(currentEntry) { navController.getBackStackEntry("map") }
    val mapVm: MapViewModel = viewModel(mapBackEntry)

    val ui by vm.ui.collectAsState()
    val ctx = LocalContext.current
    val mapUi by mapVm.uiState.collectAsState()

    LaunchedEffect(ui.done) {
        if (ui.done) {
            navController.popBackStack()
            vm.resetDone()
        }
    }

    val hasLocation = mapUi.latitude != null && mapUi.longitude != null
    val canAdd = hasLocation &&
            !ui.isSaving &&
            ui.photo != null &&
            ui.name.isNotBlank() &&
            ui.productType.isNotBlank()

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Surface(color = Color.Transparent, tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        enabled = !ui.isSaving,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }

                    Button(
                        onClick = { vm.submit(ctx, mapVm) },
                        enabled = canAdd,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (ui.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Add")
                        }
                    }
                }
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasLocation) {
                item {
                    AssistChip(
                        onClick = {},
                        label = { Text("Waiting for GPS fix… step outside or enable location") }
                    )
                }
            }

            if (ui.photo != null) {
                item {
                    Image(
                        painter = rememberAsyncImagePainter(ui.photo),
                        contentDescription = "Vending machine photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }

            item {
                ImagePicker(
                    selectedImage = ui.photo,
                    onImagePicked = vm::onPhotoPicked,
                    showPreview = false,
                    triggerContent = { open ->
                        OutlinedButton(onClick = open) {
                            Text(if (ui.photo == null) "Pick a photo" else "Change photo")
                        }
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = ui.name,
                    onValueChange = vm::onNameChanged,
                    label = { Text("Machine name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = ui.productType,
                    onValueChange = vm::onProductTypeChanged,
                    label = { Text("Product type (e.g. drinks, snacks)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 🔽 Radio group for status (canonical codes; pretty labels)
            item {
                StatusRadioGroup(
                    selectedCode = ui.status.ifBlank { VendingStatus.FULL.code },
                    onSelect = vm::onStatusChanged
                )
            }

            if (ui.error != null) {
                item { Text(ui.error ?: "Error", color = MaterialTheme.colorScheme.error) }
            }

            item { Spacer(Modifier.height(64.dp)) }
        }
    }
}

@Composable
private fun StatusRadioGroup(
    selectedCode: String,
    onSelect: (String) -> Unit
) {
    val items = VendingStatus.values()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Status", style = MaterialTheme.typography.labelLarge)
        items.forEach { st ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(st.code) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedCode.equals(st.code, ignoreCase = true),
                    onClick = { onSelect(st.code) }
                )
                Spacer(Modifier.width(8.dp))
                Text(st.label)
            }
        }
    }
}