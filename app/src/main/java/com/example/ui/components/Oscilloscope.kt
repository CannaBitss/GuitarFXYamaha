package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngineStats

@Composable
fun OscilloscopeView(
    waveform: FloatArray,
    stats: AudioEngineStats,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1114))
            .border(1.5.dp, Color(0xFF282C34), RoundedCornerShape(12.dp))
            .padding(10.dp)
            .testTag("oscilloscope_view")
    ) {
        // Stats bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (stats.isRunning) Color(0xFF00E676) else Color(0xFFFF5252),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (stats.isRunning) "DSP RUNNING" else "DSP OFF",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${stats.sampleRate} Hz | ${stats.bufferSizeFrames} f",
                    color = Color(0xFF8B949E),
                    fontSize = 10.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format("LATENCY: %.1f ms", stats.measuredLatencyMs),
                    color = Color(0xFF64B5F6),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (stats.isClipping) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CLIP!",
                        color = Color(0xFFFF1744),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Waveform Screen & Meters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Oscilloscope Screen
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF07090C), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF1E242B), RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val midY = h / 2f

                    // Grid lines
                    drawLine(
                        color = Color(0xFF16202A),
                        start = Offset(0f, midY),
                        end = Offset(w, midY),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0xFF16202A),
                        start = Offset(w / 2f, 0f),
                        end = Offset(w / 2f, h),
                        strokeWidth = 1f
                    )

                    if (waveform.isNotEmpty()) {
                        val path = Path()
                        val stepX = w / (waveform.size - 1).coerceAtLeast(1)
                        for (i in waveform.indices) {
                            val raw = waveform[i]
                            val safe = if (raw.isNaN() || raw.isInfinite()) 0f else raw.coerceIn(-1.5f, 1.5f)
                            val x = i * stepX
                            val y = midY - (safe * (h * 0.42f))
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        // Glow shadow
                        drawPath(
                            path = path,
                            color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Sharp wave line
                        drawPath(
                            path = path,
                            color = Color(0xFF00E5FF),
                            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // VU Meter Levels
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                VuBar(label = "IN", level = stats.inputLevelRms * 2.5f)
                VuBar(label = "OUT", level = stats.outputLevelRms * 2.2f)
            }
        }
    }
}

@Composable
fun VuBar(label: String, level: Float) {
    val clamped = if (level.isNaN() || level.isInfinite() || level <= 0f) {
        0.001f
    } else {
        level.coerceIn(0.001f, 1.0f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B949E),
            modifier = Modifier.width(22.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .background(Color(0xFF1F242D), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF00E676), Color(0xFFFFEB3B), Color(0xFFFF1744))
                        ),
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}
