package com.katarina.vendly.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance

@Composable
fun SetSystemBarsColor(
    color: Color,
    darkIcons: Boolean
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = color.toArgb()
            window.navigationBarColor = color.toArgb()

            val controller = WindowInsetsControllerCompat(window, view)
            controller.isAppearanceLightStatusBars = darkIcons
            controller.isAppearanceLightNavigationBars = darkIcons
        }
    }
}

/**
 * Automatically applies Vendly's system bar color.
 * Chooses icon color (light/dark) based on background luminance.
 */
@Composable
fun AppSystemBars() {
    val barColor = MaterialTheme.colorScheme.surfaceVariant
    val darkIcons = barColor.luminance() > 0.5f
    SetSystemBarsColor(color = barColor, darkIcons = darkIcons)
}