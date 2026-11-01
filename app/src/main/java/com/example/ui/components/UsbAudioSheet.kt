package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.ConnectedAudioDevice
import com.example.audio.HardwareAudioSpecs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsbAudioBottomSheet(
    devices: List<ConnectedAudioDevice>,
    specs: HardwareAudioSpecs?,
    isIrigConnected: Boolean,
    isIrigModeEnabled: Boolean,
    irigGain: Float,
    irigAntiFeedback: Boolean,
    irigHumReduction: Boolean,
    onToggleIrigMode: (Boolean) -> Unit,
    onIrigGainChange: (Float) -> Unit,
    onToggleAntiFeedback: () -> Unit,
    onToggleHumReduction: () -> Unit,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14171D),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF4A5160)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .testTag("usb_audio_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Audio Interfaces",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "AUDIO INTERFACE & IRIG INPUT",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dedicated iRig 3.5mm Earphone Jack Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isIrigConnected) Color(0xFF1A3326) else Color(0xFF1B202A)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        if (isIrigConnected) Color(0xFF00E676) else Color(0xFF37474F),
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("irig_controls_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HeadsetMic,
                                contentDescription = null,
                                tint = if (isIrigConnected) Color(0xFF00E676) else Color(0xFFFFB300),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "iRig 3.5mm Earphone Jack",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isIrigConnected) "Status: Connected & Active" else "Ready (Plug into 3.5mm jack)",
                                    fontSize = 10.sp,
                                    color = if (isIrigConnected) Color(0xFF69F0AE) else Color.Gray
                                )
                            }
                        }

                        Switch(
                            checked = isIrigModeEnabled,
                            onCheckedChange = onToggleIrigMode,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFF00E676)
                            )
                        )
                    }

                    if (isIrigModeEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // iRig Input Gain Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pickup Input Gain Trim",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = String.format("%.1fx (+%.1fdB)", irigGain, (irigGain - 1f) * 6f),
                                fontSize = 11.sp,
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = irigGain,
                            onValueChange = onIrigGainChange,
                            valueRange = 0.5f..3.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFB300),
                                activeTrackColor = Color(0xFFFFB300)
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Anti-Feedback and Hum Filter toggles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = irigAntiFeedback,
                                onClick = onToggleAntiFeedback,
                                label = { Text("Anti-Crosstalk Squeal", fontSize = 10.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = irigHumReduction,
                                onClick = onToggleHumReduction,
                                label = { Text("50/60Hz Hum Cut", fontSize = 10.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.FilterAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hardware Latency Specs Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B202A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "HARDWARE AUDIO SPECS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Sample Rate", fontSize = 10.sp, color = Color.Gray)
                            Text("${specs?.optimalSampleRate ?: 48000} Hz", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column {
                            Text("Burst Buffer", fontSize = 10.sp, color = Color.Gray)
                            Text("${specs?.framesPerBuffer ?: 192} frames", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column {
                            Text("Hardware Latency", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                String.format("%.1f ms", specs?.estimatedHardwareLatencyMs ?: 5.3f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "DETECTED AUDIO PORTS (${devices.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF90A4AE)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(devices) { dev ->
                    DeviceItemCard(device = dev)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // iRig Connection Tips Card
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101B24)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Tips",
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Cara Hubungkan iRig:\n1. Colok jack 3.5mm iRig ke port earphone HP.\n2. Colok kabel gitar ke jack input 1/4\" gitar iRig.\n3. Colok headphone Anda ke port 3.5mm headphone di iRig.\n4. Fitur Anti-Crosstalk aktif secara otomatis untuk mencegah feedback melengking.",
                        fontSize = 10.sp,
                        color = Color(0xFFBBDEFB),
                        lineHeight = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
fun DeviceItemCard(device: ConnectedAudioDevice) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                device.isIrigJack -> Color(0xFF1E3328)
                device.isUsb -> Color(0xFF1A2A38)
                else -> Color(0xFF1A1C22)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        device.isIrigJack -> Icons.Default.HeadsetMic
                        device.isUsb -> Icons.Default.Usb
                        device.isInput -> Icons.Default.Mic
                        else -> Icons.Default.VolumeUp
                    },
                    contentDescription = null,
                    tint = when {
                        device.isIrigJack -> Color(0xFF00E676)
                        device.isUsb -> Color(0xFF00E5FF)
                        else -> Color.Gray
                    },
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = device.productName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${device.typeName} • ${if (device.isInput) "INPUT" else "OUTPUT"}",
                        fontSize = 9.sp,
                        color = Color(0xFF8B949E)
                    )
                }
            }

            if (device.isIrigJack) {
                AssistChip(
                    onClick = {},
                    label = { Text("iRig 3.5mm", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676)) }
                )
            } else if (device.isUsb) {
                AssistChip(
                    onClick = {},
                    label = { Text("USB PRO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF)) }
                )
            }
        }
    }
}
