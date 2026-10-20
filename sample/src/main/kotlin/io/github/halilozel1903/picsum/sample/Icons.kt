package io.github.halilozel1903.picsum.sample

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/** Small drawn icons, so the sample needs no icon library. */
@Composable
fun GridIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = Stroke(width = s * 0.08f)
        val corner = CornerRadius(s * 0.06f)
        drawRoundRect(color, Offset(s * 0.14f, s * 0.14f), Size(s * 0.3f, s * 0.42f), corner, style = stroke)
        drawRoundRect(color, Offset(s * 0.56f, s * 0.14f), Size(s * 0.3f, s * 0.26f), corner, style = stroke)
        drawRoundRect(color, Offset(s * 0.14f, s * 0.68f), Size(s * 0.3f, s * 0.18f), corner, style = stroke)
        drawRoundRect(color, Offset(s * 0.56f, s * 0.52f), Size(s * 0.3f, s * 0.34f), corner, style = stroke)
    }
}

@Composable
fun PeopleIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = Stroke(width = s * 0.08f, cap = StrokeCap.Round)
        drawCircle(color, radius = s * 0.15f, center = Offset(s * 0.5f, s * 0.32f), style = stroke)
        val body = Path().apply {
            moveTo(s * 0.2f, s * 0.86f)
            cubicTo(s * 0.22f, s * 0.62f, s * 0.36f, s * 0.56f, s * 0.5f, s * 0.56f)
            cubicTo(s * 0.64f, s * 0.56f, s * 0.78f, s * 0.62f, s * 0.8f, s * 0.86f)
        }
        drawPath(body, color, style = stroke)
    }
}

@Composable
fun BackIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = s * 0.1f
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.82f, s * 0.5f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.46f, s * 0.24f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.46f, s * 0.76f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/** A chevron pointing left, or right when [right]. */
@Composable
fun ChevronIcon(color: Color, right: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val (tip, back) = if (right) 0.64f to 0.38f else 0.36f to 0.62f
        val path = Path().apply {
            moveTo(s * back, s * 0.22f)
            lineTo(s * tip, s * 0.5f)
            lineTo(s * back, s * 0.78f)
        }
        drawPath(path, color, style = Stroke(width = s * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
