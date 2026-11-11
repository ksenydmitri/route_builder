package com.furiqd.route_builder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RoutePrimary,
    onPrimary = RouteSurface,
    primaryContainer = RoutePrimaryContainer,
    onPrimaryContainer = RouteOnSurface,
    secondary = RouteSecondary,
    secondaryContainer = RouteSecondaryContainer,
    onSecondaryContainer = RouteOnSurface,
    background = Color(0xFF20292F),
    onBackground = RouteSurface,
    surface = Color(0xFF20292F),
    onSurface = RouteSurface,
    outline = RouteOutline,
)

private val LightColorScheme = lightColorScheme(
    primary = RoutePrimary,
    onPrimary = Color.White,
    primaryContainer = RoutePrimaryContainer,
    onPrimaryContainer = RouteOnSurface,
    secondary = RouteSecondary,
    onSecondary = Color.White,
    secondaryContainer = RouteSecondaryContainer,
    onSecondaryContainer = RouteOnSurface,
    background = RouteBackground,
    onBackground = RouteOnSurface,
    surface = RouteSurface,
    onSurface = RouteOnSurface,
    outline = RouteOutline,
)

@Composable
fun Route_builderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}