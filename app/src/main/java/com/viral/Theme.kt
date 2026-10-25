package com.viral.gamingenhancer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonBlue = Color(0xFF00B7FF)
val NeonPurple = Color(0xFF9B5CFF)

@Composable
fun VgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = NeonBlue, secondary = NeonPurple,
            background = Color(0xFF0B0B14), surface = Color(0xFF14141F),
            onPrimary = Color.Black
        ),
        content = content
    )
}
