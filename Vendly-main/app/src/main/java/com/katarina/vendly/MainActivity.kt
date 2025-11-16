package com.katarina.vendly

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.katarina.vendly.ui.auth.AuthViewModel
import com.katarina.vendly.ui.theme.VendlyTheme
import com.katarina.vendly.ui.theme.AppSystemBars

class MainActivity : ComponentActivity() {
    //ako smo kliknuli na notifikaciju preko id dobijamo detalje
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authViewModel: AuthViewModel by viewModels()
        val vendingIdFromNotif = intent.getStringExtra("vending_id")

        setContent {
            VendlyTheme {
                AppSystemBars()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LocationPermissionGate {
                        MyAppNavigation(
                            modifier = Modifier.padding(innerPadding),
                            authViewModel = authViewModel,
                            startVendingId = vendingIdFromNotif
                        )
                    }
                }
            }
        }
    }
}

//trazenje dozvvola za lokaciju
@Composable
private fun LocationPermissionGate(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.ACCESS_FINE_LOCATION //koje dozvole su potrebne
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok -> granted = ok }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    if (granted) content()
}