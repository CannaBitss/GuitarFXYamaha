package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RotaryKnob(
    label: String,
    value: Float, // Normalized 0.0f to 1.0f
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    knobColor: Color = Color(0xFF2B2D31),
    indicatorColor: Color = Color(0xFFFFB74D),
    ringTrackColor: Color = Color(0xFF1E2024),
    valueDisplay: String = "${(value * 100).toInt()}%"
) {
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(size + 14.dp)
            .testTag("knob_$label")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            // Dragging upwards increases value, downwards decreases
                            val delta = -dragAmount / 250f
                            val newValue = (value + delta).coerceIn(0.0f, 1.0f)
                            onValueChange(newValue)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val radius = this.size.minDimension / 2f - 4.dp.toPx()

                // Draw outer tick marks
                val startAngle = 135f
                val sweepTotal = 270f
                val numTicks = 9
                for (i in 0 until numTicks) {
                    val angleDeg = startAngle + (i.toFloat() / (numTicks - 1)) * sweepTotal
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val innerR = radius + 1.dp.toPx()
                    val outerR = radius + 4.dp.toPx()
                    val p1 = Offset(
                        (center.x + innerR * cos(angleRad)).toFloat(),
                        (center.y + innerR * sin(angleRad)).toFloat()
                    )
                    val p2 = Offset(
                        (center.x + outerR * cos(angleRad)).toFloat(),
                        (center.y + outerR * sin(angleRad)).toFloat()
                    )
                    drawLine(
                        color = Color(0xFF5A5E68),
                        start = p1,
                        end = p2,
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                // Background track arc
                drawArc(
                    color = ringTrackColor,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Active level arc
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color(0xFFFFB74D),
                        1.0f to Color(0xFFFF7043)
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f * value,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Knob Body (metallic bevel shadow and highlight)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(knobColor.copy(alpha = 0.9f), Color(0xFF141518)),
                        center = center,
                        radius = radius - 3.dp.toPx()
                    ),
                    radius = radius - 3.dp.toPx(),
                    center = center
                )

                // Knurled rim stroke
                drawCircle(
                    color = Color(0xFF42464D),
                    radius = radius - 3.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Indicator pointer needle
                val currentAngleDeg = 135f + 270f * value
                val currentAngleRad = Math.toRadians(currentAngleDeg.toDouble())
                val needleLen = radius - 8.dp.toPx()
                val needleEnd = Offset(
                    (center.x + needleLen * cos(currentAngleRad)).toFloat(),
                    (center.y + needleLen * sin(currentAngleRad)).toFloat()
                )
                val needleStart = Offset(
                    (center.x + (radius * 0.25f) * cos(currentAngleRad)).toFloat(),
                    (center.y + (radius * 0.25f) * sin(currentAngleRad)).toFloat()
                )

                drawLine(
                    color = indicatorColor,
                    start = needleStart,
                    end = needleEnd,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Center cap
                drawCircle(
                    color = Color(0xFF181A1D),
                    radius = radius * 0.22f,
                    center = center
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFCFD3DC),
            maxLines = 1
        )
        Text(
            text = valueDisplay,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFFFB74D),
            maxLines = 1
        )
    }
}
