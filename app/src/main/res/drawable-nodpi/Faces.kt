package com.example.futbolmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

// Caras de los jugadores del Atlético Candeleda (ids 200 a 221), todas en una sola imagen (caras.jpg)
// de 6 columnas x 4 filas con celdas de 160 px. La casilla 18 (Mario) queda vacía hasta tener permiso.
@Composable
fun PlayerFace(p: Player, boxSize: Dp) {
    val idx = p.id - 200
    if (idx in 0..21) {
        if (idx == 18) {
            Spacer(Modifier.size(boxSize))
        } else {
            val sheet = ImageBitmap.imageResource(R.drawable.caras)
            Canvas(Modifier.size(boxSize).clip(CircleShape)) {
                drawImage(
                    image = sheet,
                    srcOffset = IntOffset((idx % 6) * 160, (idx / 6) * 160),
                    srcSize = IntSize(160, 160),
                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                )
            }
        }
    }
}
