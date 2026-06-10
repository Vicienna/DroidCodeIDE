package com.droidcode.ide.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    primaryContainer = Color(0xFF3700B3),
    secondary = Color(0xFF03DAC6),
    secondaryContainer = Color(0xFF006D6A),
    tertiary = Color(0xFFCF6679),
    background = Color(0xFF1E1E1E),
    surface = Color(0xFF252526),
    surfaceVariant = Color(0xFF2D2D2D),
    surfaceContainerHighest = Color(0xFF3C3C3C),
    onBackground = Color(0xFFCCCCCC),
    onSurface = Color(0xFFCCCCCC),
    onPrimary = Color(0xFF000000),
    onSecondary = Color(0xFF000000),
    outline = Color(0xFF444444),
    outlineVariant = Color(0xFF333333),
    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF1E1E1E),
    scrim = Color(0xFF000000)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    primaryContainer = Color(0xFFBB86FC),
    secondary = Color(0xFF018786),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF3F3F3),
    onBackground = Color(0xFF1E1E1E),
    onSurface = Color(0xFF1E1E1E)
)

@Composable
fun Theme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val context = androidx.compose.ui.platform.LocalContext.current

    SideEffect {
        val activity = (context as? Activity) ?: return@SideEffect
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = colorScheme.surfaceContainer.toArgb()
        window.navigationBarColor = colorScheme.background.toArgb()
        val isDark = colorScheme.background.luminance() < 0.5
        window.setStatusBarContrastEnforced(true)
        window.setNavigationBarContrastEnforced(true)
        window.decorView.systemUiVisibility = if (isDark) 0 else android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
} 