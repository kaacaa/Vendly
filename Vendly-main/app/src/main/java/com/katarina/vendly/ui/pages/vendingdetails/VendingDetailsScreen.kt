package com.katarina.vendly.ui.pages.vendingdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.katarina.vendly.domain.model.vm.VendingStatus
import kotlinx.coroutines.launch

@Composable
fun VendingDetailsScreen(
    navController: NavController,
    vendingId: String,
    vm: VendingDetailsViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val ui = vm.ui.collectAsState().value

    LaunchedEffect(vendingId) { vm.start(vendingId) }

    val vending = ui?.vending
    var currentStatus by remember(ui?.vending?.status) {
        mutableStateOf(VendingStatus.fromCode(ui?.vending?.status).code)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        SnackbarHost(hostState = snack)

        Text("Vending details", style = MaterialTheme.typography.titleLarge)

        // IMAGE (read-only)
        AsyncImage(
            model = vending?.imageUrl,
            contentDescription = vending?.name ?: "Vending photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        // READ-ONLY INFO
        Text(vending?.name ?: "", style = MaterialTheme.typography.titleMedium)
        Text(vending?.productType ?: "", color = Color.Gray)
        Text("Status: ${VendingStatus.fromCode(currentStatus).label}")

        // STATUS PICKER adds **Low**
        StatusButtons(
            selected = currentStatus,
            onSelect = { currentStatus = it }
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    vm.updateStatus(currentStatus)
                    val err = vm.ui.value?.error
                    if (err == null) {
                        snack.showSnackbar(
                            "Status updated (+${com.katarina.vendly.domain.gamification.Points.PER_STATUS_UPDATE} pts)"
                        )
                        navController.popBackStack()
                    } else {
                        snack.showSnackbar("Failed: $err")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save status") }
    }
}

/** Simple status button group with Low included */
@Composable
private fun StatusButtons(
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { onSelect(VendingStatus.FULL.code) },
            enabled = selected != VendingStatus.FULL.code
        ) { Text("Full") }

        Button(
            onClick = { onSelect(VendingStatus.LOW.code) },
            enabled = selected != VendingStatus.LOW.code
        ) { Text("Low") }

        Button(
            onClick = { onSelect(VendingStatus.EMPTY.code) },
            enabled = selected != VendingStatus.EMPTY.code
        ) { Text("Empty") }

        Button(
            onClick = { onSelect(VendingStatus.OUT_OF_ORDER.code) },
            enabled = selected != VendingStatus.OUT_OF_ORDER.code
        ) { Text("Out of order") }
    }
}