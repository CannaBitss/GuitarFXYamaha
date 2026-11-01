package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AmpModel
import com.example.model.CabType
import com.example.model.EffectSetting
import com.example.model.EffectType

@Composable
fun YamahaAmpHeadView(
    ampEffect: EffectSetting?,
    cabEffect: EffectSetting?,
    inputRms: Float,
    onAmpParamChange: (String, Float) -> Unit,
    onCabParamChange: (String, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val ampParams = ampEffect?.params ?: emptyMap()
    val cabParams = cabEffect?.params ?: emptyMap()

    val currentModelIdx = (ampParams["model"] ?: 0.0f).toInt().coerceIn(0, AmpModel.entries.size - 1)
    val currentCabIdx = (cabParams["cab_type"] ?: 1.0f).toInt().coerceIn(0, CabType.entries.size - 1)

    val currentModel = AmpModel.entries[currentModelIdx]
    val currentCab = CabType.entries[currentCabIdx]

    val gain = ampParams["gain"] ?: 0.5f
    val bass = ampParams["bass"] ?: 0.5f
    val mid = ampParams["mid"] ?: 0.5f
    val treble = ampParams["treble"] ?: 0.5f
    val presence = ampParams["presence"] ?: 0.5f
    val master = ampParams["master"] ?: 0.8f

    // Tube glow intensity responds dynamically to gain and playing RMS
    val tubeGlowAlpha = (0.35f + gain * 0.40f + inputRms * 1.5f).coerceIn(0.2f, 1.0f)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141518)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, Color(0xFF38332A), RoundedCornerShape(14.dp))
            .testTag("yamaha_amp_head")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Grille & Glowing Tubes Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(Color(0xFF16171A))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Vintage speaker/amp mesh pattern
                    val gridStep = 8.dp.toPx()
                    for (x in 0..(size.width / gridStep).toInt()) {
                        drawLine(
                            color = Color(0xFF222428),
                            start = Offset(x * gridStep, 0f),
                            end = Offset(x * gridStep, size.height),
                            strokeWidth = 1f
                        )
                    }
                    for (y in 0..(size.height / gridStep).toInt()) {
                        drawLine(
                            color = Color(0xFF222428),
                            start = Offset(0f, y * gridStep),
                            end = Offset(size.width, y * gridStep),
                            strokeWidth = 1f
                        )
                    }

                    // 3 Vacuum Tubes Glowing Behind Grille (Yamaha THR amber tube lights)
                    val tubePositions = listOf(0.35f, 0.50f, 0.65f)
                    for (pos in tubePositions) {
                        val centerX = size.width * pos
                        val centerY = size.height * 0.55f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF9800).copy(alpha = tubeGlowAlpha),
                                    Color(0xFFFF5722).copy(alpha = tubeGlowAlpha * 0.6f),
                                    Color.Transparent
                                ),
                                center = Offset(centerX, centerY),
                                radius = 45.dp.toPx()
                            )
                        )
                        // Tube glass silhouette
                        drawRoundRect(
                            color = Color(0xFFFFD54F).copy(alpha = tubeGlowAlpha * 0.9f),
                            topLeft = Offset(centerX - 8.dp.toPx(), centerY - 18.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 34.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }

                // Brand Emblem
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YAMAHA",
                        color = Color(0xFFE5C07B),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "THR VCM TUBE SIMULATOR",
                        color = Color(0xFF9E9E9E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Brushed Brass / Gold Faceplate
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF423724), Color(0xFF2E2619), Color(0xFF1E1911))
                        )
                    )
                    .padding(12.dp)
            ) {
                // Amp Model Selector Buttons
                Text(
                    text = "AMP MODEL",
                    color = Color(0xFFFFD54F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AmpModel.entries.forEachIndexed { idx, model ->
                        val isSelected = idx == currentModelIdx
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Color(0xFFFFB300) else Color(0xFF1E1F24),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onAmpParamChange("model", idx.toFloat()) }
                                .testTag("amp_model_${model.name}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
                            ) {
                                Text(
                                    text = model.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF1A1A1A) else Color(0xFFBDBDBD)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tone Stack Chicken-Head Knobs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RotaryKnob(
                        label = "GAIN",
                        value = gain,
                        onValueChange = { onAmpParamChange("gain", it) },
                        size = 56.dp,
                        indicatorColor = Color(0xFFFFC107),
                        ringTrackColor = Color(0xFF141518)
                    )
                    RotaryKnob(
                        label = "BASS",
                        value = bass,
                        onValueChange = { onAmpParamChange("bass", it) },
                        size = 56.dp,
                        indicatorColor = Color(0xFFFFC107),
                        ringTrackColor = Color(0xFF141518)
                    )
                    RotaryKnob(
                        label = "MID",
                        value = mid,
                        onValueChange = { onAmpParamChange("mid", it) },
                        size = 56.dp,
                        indicatorColor = Color(0xFFFFC107),
                        ringTrackColor = Color(0xFF141518)
                    )
                    RotaryKnob(
                        label = "TREBLE",
                        value = treble,
                        onValueChange = { onAmpParamChange("treble", it) },
                        size = 56.dp,
                        indicatorColor = Color(0xFFFFC107),
                        ringTrackColor = Color(0xFF141518)
                    )
                    RotaryKnob(
                        label = "PRESENCE",
                        value = presence,
                        onValueChange = { onAmpParamChange("presence", it) },
                        size = 56.dp,
                        indicatorColor = Color(0xFFFFC107),
                        ringTrackColor = Color(0xFF141518)
                    )
                    RotaryKnob(
                        label = "MASTER",
                        value = master,
                        onValueChange = { onAmpParamChange("master", it) },
                        size = 56.dp,
                        indicatorColor = Color(0xFFFF7043),
                        ringTrackColor = Color(0xFF141518)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cabinet IR Module Strip
                HorizontalDivider(color = Color(0xFF53452E), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CABINET IR: ${currentCab.displayName}",
                            color = Color(0xFFFFF9C4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentCab.speakerSize,
                            color = Color(0xFFB0BEC5),
                            fontSize = 10.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CabType.entries.forEachIndexed { cIdx, cab ->
                            val isSel = cIdx == currentCabIdx
                            FilterChip(
                                selected = isSel,
                                onClick = { onCabParamChange("cab_type", cIdx.toFloat()) },
                                label = { Text(cab.name.removePrefix("CAB_"), fontSize = 10.sp) },
                                modifier = Modifier.testTag("cab_chip_${cab.name}")
                            )
                        }
                    }
                }
            }
        }
    }
}
