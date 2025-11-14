package com.katarina.vendly

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.katarina.vendly.ui.auth.AuthState
import com.katarina.vendly.ui.auth.AuthViewModel
import com.katarina.vendly.ui.components.BottomBar
import com.katarina.vendly.ui.components.BrandTopBar
import com.katarina.vendly.ui.pages.addvending.AddVendingScreen
import com.katarina.vendly.ui.pages.home.HomeScreen
import com.katarina.vendly.ui.pages.leaderboard.LeaderboardScreen
import com.katarina.vendly.ui.pages.login.LoginScreen
import com.katarina.vendly.ui.pages.map.MapScreen
import com.katarina.vendly.ui.pages.profile.ProfileScreen
import com.katarina.vendly.ui.pages.signup.SignupScreen
import com.katarina.vendly.ui.pages.vendingdetails.VendingDetailsScreen

@Composable
fun MyAppNavigation(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    startVendingId: String? = null // passed from MainActivity (notification)
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentDest = backStack?.destination
    val currentRoute = currentDest?.route.orEmpty()

    // Top-level tabs (for bottom bar)
    val topLevelRoutes = listOf("home", "map", "leaderboard", "profile")

    // Observe auth state to decide start destination and handle redirects
    val authState by authViewModel.authState.observeAsState(initial = AuthState.Loading)

    // Consume notification deep-link exactly once
    var deepLinkConsumed by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            // Hide top bar on auth screens only
            val showTopBar = currentRoute !in listOf("login", "signup")
            if (showTopBar) {
                // Show back button on non-top-level destinations
                val isTopLevel = currentDest?.hierarchy?.any { it.route in topLevelRoutes } == true
                BrandTopBar(
                    appName = "Vendly",
                    showBack = !isTopLevel,
                    onBack = { navController.popBackStack() }
                )
            }
        },
        bottomBar = {
            val isTopLevel = currentDest?.hierarchy?.any { it.route in topLevelRoutes } == true
            if (isTopLevel) {
                BottomBar(
                    currentRoute = topLevelRoutes.firstOrNull { tab ->
                        currentDest?.hierarchy?.any { it.route == tab } == true
                    } ?: currentRoute,
                    onHome = {
                        navController.navigate("home") {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    },
                    onMap = {
                        navController.navigate("map") {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    },
                    onLeaderboard = {
                        navController.navigate("leaderboard") {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    },
                    onProfile = {
                        navController.navigate("profile") {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        // Choose a start destination based on auth state
        val startDestination = when (authState) {
            AuthState.Authenticated -> "home"
            AuthState.Unauthenticated -> "login"
            AuthState.Error("...") -> "login" // any error should land on login
            else -> "login"
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Auth
            composable("login") { LoginScreen(modifier, navController, authViewModel) }
            composable("signup") { SignupScreen(modifier, navController, authViewModel) }

            // Tabs
            composable("home") { HomeScreen(navController) }
            composable("map") { MapScreen(navController) }
            composable("leaderboard") { LeaderboardScreen(navController) }
            composable("profile") { ProfileScreen(modifier, navController, authViewModel) }

            // Add vending
            composable("addVending") { AddVendingScreen(navController) }

            // Details
            composable(
                route = "vendingDetails/{vendingId}",
                arguments = listOf(navArgument("vendingId") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("vendingId").orEmpty()
                VendingDetailsScreen(navController, id)
            }
        }

        // Handle auth-driven redirects (e.g., logout -> go to login)
        LaunchedEffect(authState) {
            when (authState) {
                AuthState.Authenticated -> {
                    if (currentRoute in listOf("login", "signup") || currentRoute.isBlank()) {
                        navController.navigate("home") {
                            popUpTo(0) { inclusive = true } // clear stack
                            launchSingleTop = true
                        }
                    }
                }
                is AuthState.Error, AuthState.Unauthenticated -> {
                    if (currentRoute !in listOf("login", "signup")) {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                else -> Unit
            }
        }

        // Trigger deep-link to details once when authenticated
        LaunchedEffect(startVendingId, authState) {
            if (!deepLinkConsumed && !startVendingId.isNullOrBlank() && authState == AuthState.Authenticated) {
                deepLinkConsumed = true
                navController.navigate("vendingDetails/$startVendingId") {
                    launchSingleTop = true
                }
            }
        }
    }
}