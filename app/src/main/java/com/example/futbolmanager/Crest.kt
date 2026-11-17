package com.example.futbolmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

@Composable
fun Crest(team: Team, boxSize: Dp) {
    if (team.id == REAL_ID) {
        Image(
            painter = painterResource(R.drawable.escudo_candeleda),
            contentDescription = team.name,
            modifier = Modifier.size(boxSize)
        )
        return
    }
    Box(Modifier.size(boxSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val shield = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.55f)
                cubicTo(w, h * 0.8f, w * 0.75f, h * 0.95f, w / 2f, h)
                cubicTo(w * 0.25f, h * 0.95f, 0f, h * 0.8f, 0f, h * 0.55f)
                close()
            }
            drawPath(shield, Color(team.c1))
            clipPath(shield) {
                when (team.pattern) {
                    0 -> drawRect(Color(team.c2), Offset(w * 0.38f, 0f), Size(w * 0.24f, h))
                    1 -> drawRect(Color(team.c2), Offset(0f, 0f), Size(w, h * 0.3f))
                    else -> drawRect(Color(team.c2), Offset(0f, h * 0.6f), Size(w, h * 0.4f))
                }
            }
            drawPath(shield, Color(0xFF222222), style = Stroke(w * 0.06f))
        }
        Text(
            team.initials,
            style = TextStyle(
                color = Color.White,
                fontSize = (boxSize.value * 0.3f).sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(Color.Black, blurRadius = 6f)
            )
        )
    }
}
