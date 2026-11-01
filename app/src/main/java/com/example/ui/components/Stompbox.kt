package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EffectSetting
import com.example.model.EffectType

@Composable
fun StompboxCard(
    effect: EffectSetting,
    index: Int,
    totalCount: Int,
    onToggleBypass: () -> Unit,
    onParamChange: (String, Float) -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = getStompboxTheme(effect.type)

    val ledGlowColor by animateColorAsState(
        targetValue = if (effect.isEnabled) theme.ledColor else Color(0xFF333333),
        animationSpec = tween(150),
        label = "ledGlow"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = theme.casingColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .width(220.dp)
            .shadow(10.dp, RoundedCornerShape(16.dp))
            .border(2.dp, Brush.verticalGradient(listOf(theme.highlightColor, Color(0xFF101114))), RoundedCornerShape(16.dp))
            .testTag("stompbox_${effect.type.name}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Bar: Re-order arrows, Title, and Category badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onMoveLeft,
                    enabled = index > 0,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Move Left",
                        tint = if (index > 0) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = effect.type.displayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.titleColor,
                        maxLines = 1
                    )
                    Text(
                        text = effect.type.category.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                IconButton(
                    onClick = onMoveRight,
                    enabled = index < totalCount - 1,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Move Right",
                        tint = if (index < totalCount - 1) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Knobs Section based on Effect Type
            StompboxKnobs(
                effect = effect,
                theme = theme,
                onParamChange = onParamChange
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Status LED & Footswitch section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // LED Jewel Light
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(ledGlowColor)
                        .border(1.5.dp, Color(0xFF222428), CircleShape)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (effect.isEnabled) {
                            // Radial halo glow
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.8f), ledGlowColor),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.minDimension / 2f
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Heavy-duty 3PDT Stomp Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFD0D4DC), Color(0xFF6B7280), Color(0xFF2B2F38))
                            )
                        )
                        .border(2.5.dp, Color(0xFF1E2024), CircleShape)
                        .clickable(onClick = onToggleBypass)
                        .testTag("footswitch_${effect.type.name}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF888E99), Color(0xFF4A4E57))
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Bypass Stomp",
                            tint = if (effect.isEnabled) theme.ledColor else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (effect.isEnabled) "ACTIVE" else "BYPASS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (effect.isEnabled) theme.ledColor else Color.Gray
                )
            }
        }
    }
}

@Composable
fun StompboxKnobs(
    effect: EffectSetting,
    theme: StompboxTheme,
    onParamChange: (String, Float) -> Unit
) {
    val params = effect.params
    when (effect.type) {
        EffectType.NOISE_GATE -> {
            val thresh = params["threshold"] ?: -50.0f
            val release = params["release"] ?: 40.0f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RotaryKnob(
                    label = "THRESH",
                    value = (thresh + 80f) / 80f,
                    onValueChange = { onParamChange("threshold", -80f + it * 80f) },
                    valueDisplay = "${thresh.toInt()} dB",
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "RELEASE",
                    value = (release - 10f) / 140f,
                    onValueChange = { onParamChange("release", 10f + it * 140f) },
                    valueDisplay = "${release.toInt()} ms",
                    indicatorColor = theme.knobIndicator
                )
            }
        }

        EffectType.OVERDRIVE -> {
            val drive = params["drive"] ?: 0.5f
            val tone = params["tone"] ?: 0.5f
            val level = params["level"] ?: 0.7f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RotaryKnob(
                    label = "DRIVE",
                    value = drive,
                    onValueChange = { onParamChange("drive", it) },
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "TONE",
                    value = tone,
                    onValueChange = { onParamChange("tone", it) },
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "LEVEL",
                    value = level,
                    onValueChange = { onParamChange("level", it) },
                    indicatorColor = theme.knobIndicator
                )
            }
        }

        EffectType.DISTORTION -> {
            val gain = params["gain"] ?: 0.7f
            val tone = params["tone"] ?: 0.5f
            val level = params["level"] ?: 0.7f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RotaryKnob(
                    label = "DIST",
                    value = gain,
                    onValueChange = { onParamChange("gain", it) },
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "FILTER",
                    value = tone,
                    onValueChange = { onParamChange("tone", it) },
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "VOL",
                    value = level,
                    onValueChange = { onParamChange("level", it) },
                    indicatorColor = theme.knobIndicator
                )
            }
        }

        EffectType.BOOSTER -> {
            val gain = params["gain"] ?: 2.0f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RotaryKnob(
                    label = "BOOST",
                    value = (gain - 1.0f) / 4.0f,
                    onValueChange = { onParamChange("gain", 1.0f + it * 4.0f) },
                    valueDisplay = String.format("+%.1fdB", (gain - 1.0f) * 6f),
                    indicatorColor = theme.knobIndicator
                )
            }
        }

        EffectType.FUZZ -> {
            val fuzz = params["fuzz"] ?: 0.8f
            val level = params["level"] ?: 0.7f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RotaryKnob(
                    label = "FUZZ",
                    value = fuzz,
                    onValueChange = { onParamChange("fuzz", it) },
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "OUTPUT",
                    value = level,
                    onValueChange = { onParamChange("level", it) },
                    indicatorColor = theme.knobIndicator
                )
            }
        }

        EffectType.AMP_SIM -> {
            val gain = params["gain"] ?: 0.5f
            val master = params["master"] ?: 0.8f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RotaryKnob(
                    label = "AMP GAIN",
                    value = gain,
                    onValueChange = { onParamChange("gain", it) },
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "MASTER",
                    value = master,
                    onValueChange = { onParamChange("master", it) },
                    indicatorColor = theme.knobIndicator
                )
            }
        }

        EffectType.CAB_SIM -> {
            val micDist = params["mic_distance"] ?: 0.25f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RotaryKnob(
                    label = "MIC DIST",
                    value = micDist,
                    onValueChange = { onParamChange("mic_distance", it) },
                    indicatorColor = theme.knobIndicator
                )
            }
        }

        EffectType.CHORUS -> {
            val rate = params["rate"] ?: 1.2f
            val depth = params["depth"] ?: 0.5f
            val mix = params["mix"] ?: 0.35f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RotaryKnob(
                    label = "SPEED",
                    value = (rate - 0.2f) / 4.8f,
                    onValueChange = { onParamChange("rate", 0.2f + it * 4.8f) },
                    valueDisplay = String.format("%.1fHz", rate),
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "DEPTH",
                    value = depth,
                    onValueChange = { onParamChange("depth", it) },
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "MIX",
                    value = mix,
                    onValueChange = { onParamChange("mix", it) },
                    indicatorColor = theme.knobIndicator
                )
            }
        }

        EffectType.FLANGER -> {
            val rate = params["rate"] ?: 0.35f
            val depth = params["depth"] ?: 0.7f
            val mix = params["mix"] ?: 0.35f
            val feedback = params["feedback"] ?: 0.2f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RotaryKnob(
                    label = "RATE",
                    value = (rate - 0.05f) / 4.95f,
                    onValueChange = { onParamChange("rate", 0.05f + it * 4.95f) },
                    valueDisplay = String.format("%.2fHz", rate),
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "DEPTH",
                    value = depth,
                    onValueChange = { onParamChange("depth", it) },
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "MIX",
                    value = mix,
                    onValueChange = { onParamChange("mix", it) },
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "FEEDBACK",
                    value = feedback / 0.75f,
                    onValueChange = { onParamChange("feedback", it * 0.75f) },
                    indicatorColor = theme.knobIndicator
                )
            }
        }

        EffectType.WAH -> {
            val position = params["position"] ?: 0.5f
            val q = params["q"] ?: 3.5f
            val mix = params["mix"] ?: 1.0f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RotaryKnob(
                    label = "PEDAL",
                    value = position,
                    onValueChange = { onParamChange("position", it) },
                    valueDisplay = "${(350f * Math.pow(6.0, position.toDouble())).toInt()}Hz",
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "RESONANCE",
                    value = (q - 0.7f) / 9.3f,
                    onValueChange = { onParamChange("q", 0.7f + it * 9.3f) },
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "MIX",
                    value = mix,
                    onValueChange = { onParamChange("mix", it) },
                    indicatorColor = theme.knobIndicator
                )
            }
        }

        EffectType.DELAY -> {
            val time = params["time"] ?: 350.0f
            val feedback = params["feedback"] ?: 0.4f
            val mix = params["mix"] ?: 0.3f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RotaryKnob(
                    label = "TIME",
                    value = (time - 20f) / 980f,
                    onValueChange = { onParamChange("time", 20f + it * 980f) },
                    valueDisplay = "${time.toInt()}ms",
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "F.BACK",
                    value = feedback,
                    onValueChange = { onParamChange("feedback", it) },
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "MIX",
                    value = mix,
                    onValueChange = { onParamChange("mix", it) },
                    indicatorColor = theme.knobIndicator
                )
            }
        }

        EffectType.REVERB -> {
            val decay = params["decay"] ?: 2.5f
            val damping = params["damping"] ?: 0.4f
            val mix = params["mix"] ?: 0.35f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RotaryKnob(
                    label = "REV-X DECAY",
                    value = (decay - 0.5f) / 5.5f,
                    onValueChange = { onParamChange("decay", 0.5f + it * 5.5f) },
                    valueDisplay = String.format("%.1fs", decay),
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "DAMP",
                    value = damping,
                    onValueChange = { onParamChange("damping", it) },
                    indicatorColor = theme.knobIndicator
                )
                RotaryKnob(
                    label = "WET MIX",
                    value = mix,
                    onValueChange = { onParamChange("mix", it) },
                    indicatorColor = theme.knobIndicator
                )
            }
        }

        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RotaryKnob(
                    label = "PARAM",
                    value = 0.5f,
                    onValueChange = {},
                    indicatorColor = theme.knobIndicator
                )
            }
        }
    }
}

data class StompboxTheme(
    val casingColor: Color,
    val highlightColor: Color,
    val titleColor: Color,
    val ledColor: Color,
    val knobIndicator: Color
)

fun getStompboxTheme(type: EffectType): StompboxTheme {
    return when (type) {
        EffectType.NOISE_GATE -> StompboxTheme(
            casingColor = Color(0xFF282A30),
            highlightColor = Color(0xFF4A4E5A),
            titleColor = Color(0xFFE0E0E0),
            ledColor = Color(0xFF00E676),
            knobIndicator = Color(0xFF69F0AE)
        )
        EffectType.OVERDRIVE -> StompboxTheme(
            casingColor = Color(0xFF1B4D2B), // TS Screamer Green
            highlightColor = Color(0xFF2E7D46),
            titleColor = Color(0xFFA5D6A7),
            ledColor = Color(0xFFFF1744),
            knobIndicator = Color(0xFF81C784)
        )
        EffectType.DISTORTION -> StompboxTheme(
            casingColor = Color(0xFF211915), // RAT / Dark Distortion
            highlightColor = Color(0xFFFF6D00),
            titleColor = Color(0xFFFF9E80),
            ledColor = Color(0xFFFF5252),
            knobIndicator = Color(0xFFFFAB40)
        )
        EffectType.BOOSTER -> StompboxTheme(
            casingColor = Color(0xFF37474F), // Brushed Aluminum
            highlightColor = Color(0xFF78909C),
            titleColor = Color(0xFFECEFF1),
            ledColor = Color(0xFF00B0FF),
            knobIndicator = Color(0xFF80D8FF)
        )
        EffectType.FUZZ -> StompboxTheme(
            casingColor = Color(0xFF3E2723), // Vintage Fuzz
            highlightColor = Color(0xFF8D6E63),
            titleColor = Color(0xFFFFCCBC),
            ledColor = Color(0xFFFF3D00),
            knobIndicator = Color(0xFFFFAB91)
        )
        EffectType.AMP_SIM -> StompboxTheme(
            casingColor = Color(0xFF332A15), // Yamaha Gold/Bronze
            highlightColor = Color(0xFFFFB300),
            titleColor = Color(0xFFFFE082),
            ledColor = Color(0xFFFFC107),
            knobIndicator = Color(0xFFFFD54F)
        )
        EffectType.CAB_SIM -> StompboxTheme(
            casingColor = Color(0xFF1A1D20), // Tolex Black
            highlightColor = Color(0xFF424242),
            titleColor = Color(0xFFB0BEC5),
            ledColor = Color(0xFF76FF03),
            knobIndicator = Color(0xFFB2FF59)
        )
        EffectType.CHORUS -> StompboxTheme(
            casingColor = Color(0xFF0D47A1), // SPX Chorus Blue
            highlightColor = Color(0xFF1E88E5),
            titleColor = Color(0xFF90CAF9),
            ledColor = Color(0xFF00E5FF),
            knobIndicator = Color(0xFF40C4FF)
        )
        EffectType.DELAY -> StompboxTheme(
            casingColor = Color(0xFF1A237E), // Space Blue Delay
            highlightColor = Color(0xFF3949AB),
            titleColor = Color(0xFFC5CAE9),
            ledColor = Color(0xFF651FFF),
            knobIndicator = Color(0xFF7C4DFF)
        )
        EffectType.REVERB -> StompboxTheme(
            casingColor = Color(0xFF4A148C), // Yamaha REV-X Deep Purple
            highlightColor = Color(0xFF7B1FA2),
            titleColor = Color(0xFFE1BEE7),
            ledColor = Color(0xFFFF4081),
            knobIndicator = Color(0xFFFF80AB)
        )
        else -> StompboxTheme(
            casingColor = Color(0xFF263238),
            highlightColor = Color(0xFF546E7A),
            titleColor = Color(0xFFECEFF1),
            ledColor = Color(0xFFFFEB3B),
            knobIndicator = Color(0xFFFFF59D)
        )
    }
}
