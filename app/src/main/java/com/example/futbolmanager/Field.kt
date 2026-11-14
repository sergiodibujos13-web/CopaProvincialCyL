package com.example.futbolmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Color(0xFFFFC107), onPrimary = Color.Black)
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Box(Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxSize()) {
                    val stripes = 12
                    val h = size.height / stripes
                    for (i in 0 until stripes) {
                        drawRect(
                            color = if (i % 2 == 0) Color(0xFF1B7F3B) else Color(0xFF23954A),
                            topLeft = Offset(0f, i * h),
                            size = Size(size.width, h + 1f)
                        )
                    }
                    drawRect(Color.Black.copy(alpha = 0.25f))
                    val line = Color.White.copy(alpha = 0.35f)
                    val w = 4f
                    drawRect(
                        line,
                        topLeft = Offset(24f, 24f),
                        size = Size(size.width - 48f, size.height - 48f),
                        style = Stroke(w)
                    )
                    drawLine(line, Offset(24f, size.height / 2), Offset(size.width - 24f, size.height / 2), w)
                    drawCircle(line, radius = size.width * 0.22f, center = center, style = Stroke(w))
                }
                content()
            }
        }
    }
}
