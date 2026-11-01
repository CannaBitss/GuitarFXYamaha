package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.dsp.TunerResult
import kotlin.math.abs

@Composable
fun GuitarTunerModal(
    result: TunerResult?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val note = result?.noteName ?: "-"
    val octave = result?.octave ?: 0
    val cents = result?.centsDeviation ?: 0.0f
    val isLocked = result?.isLocked ?: false
    val freq = result?.frequency ?: 0.0f
    val targetFreq = result?.targetFreq ?: 0.0f

    val haloColor by animateColorAsState(
        targetValue = when {
            isLocked -> Color(0xFF00E676)
            abs(cents) < 15f -> Color(0xFFFFEB3B)
            else -> Color(0xFFFF5252)
        },
        label = "tunerHalo"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14171C)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, Color(0xFF2C313C), RoundedCornerShape(18.dp))
            .testTag("guitar_tuner_modal")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CHROMATIC GUITAR TUNER",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F),
                    letterSpacing = 1.sp
                )
                TextButton(onClick = onClose) {
                    Text("DONE", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Big Note Readout Circle with Glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF232730), Color(0xFF111317))
                        )
                    )
                    .border(3.dp, haloColor, CircleShape)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = note,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isLocked) Color(0xFF00E676) else Color.White
                        )
                        if (octave > 0) {
                            Text(
                                text = "$octave",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                    if (isLocked) {
                        Text(
                            text = "IN TUNE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00E676),
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cents Deviation Needle Meter (-50 cents to +50 cents)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF0C0E11), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF22262E), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val midX = w / 2f

                    // Draw tick marks
                    val numTicks = 11
                    for (i in 0 until numTicks) {
                        val tickCents = -50f + i * 10f
                        val x = midX + (tickCents / 50f) * (w * 0.45f)
                        val isCenter = i == 5
                        drawLine(
                            color = if (isCenter) Color(0xFF00E676) else Color(0xFF4B5362),
                            start = Offset(x, if (isCenter) 2.dp.toPx() else 8.dp.toPx()),
                            end = Offset(x, h - (if (isCenter) 2.dp.toPx() else 8.dp.toPx())),
                            strokeWidth = if (isCenter) 2.5.dp.toPx() else 1.2.dp.toPx()
                        )
                    }

                    // Tuning Indicator Needle
                    if (result != null) {
                        val needleX = (midX + (cents / 50f) * (w * 0.45f)).coerceIn(8.dp.toPx(), w - 8.dp.toPx())
                        drawLine(
                            color = haloColor,
                            start = Offset(needleX, 0f),
                            end = Offset(needleX, h),
                            strokeWidth = 3.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("♭ FLAT (-50¢)", fontSize = 10.sp, color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                Text(
                    text = String.format("%+.1f cents", cents),
                    fontSize = 11.sp,
                    color = haloColor,
                    fontWeight = FontWeight.Bold
                )
                Text("SHARP (+50¢) ♯", fontSize = 10.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Frequency Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = String.format("DETECTED: %.1f Hz", freq),
                    fontSize = 11.sp,
                    color = Color(0xFF90A4AE),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = String.format("TARGET: %.1f Hz", targetFreq),
                    fontSize = 11.sp,
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick String Target Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("E2 (82.4)", "A2 (110)", "D3 (146.8)", "G3 (196)", "B3 (246.9)", "E4 (329.6)").forEach { str ->
                    val isCurrent = str.startsWith(note)
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = str,
                                fontSize = 9.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) Color(0xFF00E676) else Color.Gray
                            )
                        }
                    )
                }
            }
        }
    }
}
