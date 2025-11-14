package com.katarina.vendly.ui.pages.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/* ============================= UI ============================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    vm: HomeViewModel = viewModel()
) {
    LaunchedEffect(Unit) { vm.start() }
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(64.dp),
                title = {
                    Text(
                        "Your vending machines",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                ui.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Loading your machines…")
                    }
                }

                !ui.isSignedIn -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Please log in to see your vending machines.")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { navController.navigate("login") }) {
                            Text("Go to login")
                        }
                    }
                }

                ui.error != null -> {
                    // Show the error and a retry. If rules/App Check are the cause,
                    // the message helps you diagnose from UI instead of a silent failure.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = ui.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.restart() }) {
                            Text("Retry")
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "If this says 'insufficient permissions', check Firestore rules or App Check.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ui.items.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("You haven’t added any vending machines yet.")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { navController.navigate("map") }) {
                            Text("Open map")
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(ui.items, key = { it.id }) { v ->
                            VendingRow(
                                item = v,
                                onClick = {
                                    // 🔧 FIXED: route must match your NavHost "vendingDetails/{vendingId}"
                                    navController.navigate("vendingDetails/${v.id}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VendingRow(item: VendingItem, onClick: () -> Unit) {
    val matchBottomBar = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = matchBottomBar,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Type: ${item.productType}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Status: ${item.status}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.createdAt > 0) {
                    val dateStr by remember(item.createdAt) {
                        mutableStateOf(
                            SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
                                .format(Date(item.createdAt))
                        )
                    }
                    Text(
                        dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

