package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.dsp.RiffStyle
import com.example.model.EffectType
import com.example.model.PresetRepository
import com.example.ui.components.*
import com.example.viewmodel.GuitarFxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedalboardScreen(
    viewModel: GuitarFxViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.startEngine()
        }
    }

    val engineStats by viewModel.audioEngine.engineStats.collectAsStateWithLifecycle()
    val waveform by viewModel.audioEngine.waveformFlow.collectAsStateWithLifecycle()
    val tunerResult by viewModel.audioEngine.tunerResultFlow.collectAsStateWithLifecycle()

    val currentPreset by viewModel.currentPreset.collectAsStateWithLifecycle()
    val presetsList by viewModel.presetsList.collectAsStateWithLifecycle()
    val effectsChain by viewModel.effectsChain.collectAsStateWithLifecycle()

    val isTunerOpen by viewModel.isTunerOpen.collectAsStateWithLifecycle()
    val isVirtualRiffEnabled by viewModel.isVirtualRiffEnabled.collectAsStateWithLifecycle()
    val selectedRiffStyle by viewModel.selectedRiffStyle.collectAsStateWithLifecycle()
    val showUsbSheet by viewModel.showUsbSheet.collectAsStateWithLifecycle()
    val connectedDevices by viewModel.connectedDevices.collectAsStateWithLifecycle()
    val hardwareSpecs by viewModel.hardwareSpecs.collectAsStateWithLifecycle()
    val masterVolume by viewModel.masterVolume.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()

    val isIrigConnected by viewModel.isIrigConnected.collectAsStateWithLifecycle()
    val isIrigModeEnabled by viewModel.isIrigModeEnabled.collectAsStateWithLifecycle()
    val irigGain by viewModel.irigInputGain.collectAsStateWithLifecycle()
    val irigAntiFeedback by viewModel.irigAntiFeedback.collectAsStateWithLifecycle()
    val irigHumReduction by viewModel.irigHumReduction.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Pedals, 1: Amp Head, 2: All
    var showSaveDialog by remember { mutableStateOf(false) }
    var showJsonDialog by remember { mutableStateOf(false) }
    var showRiffMenu by remember { mutableStateOf(false) }

    val jewelGlowColor by animateColorAsState(
        targetValue = if (engineStats.isRunning) Color(0xFFFF9800) else Color(0xFF422A00),
        label = "jewelGlow"
    )

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131518))
                    .statusBarsPadding()
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Logo & Pilot Jewel Lamp
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pilot Jewel Lamp
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(jewelGlowColor)
                                .border(2.dp, Color(0xFF6B5833), CircleShape)
                                .clickable {
                                    if (!hasAudioPermission) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        viewModel.toggleEnginePower()
                                    }
                                }
                                .testTag("power_jewel_lamp")
                        ) {
                            if (engineStats.isRunning) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFFFFD54F), CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "GUITAR FX",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PRO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB300)
                                )
                            }
                            Text(
                                text = "Yamaha Multi-Effects & THR Engine",
                                fontSize = 9.sp,
                                color = Color(0xFF90A4AE)
                            )
                        }
                    }

                    // Action Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Virtual Riff Audition Toggle
                        Box {
                            FilledTonalButton(
                                onClick = { showRiffMenu = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isVirtualRiffEnabled) Color(0xFF00E5FF) else Color(0xFF262C38),
                                    contentColor = if (isVirtualRiffEnabled) Color.Black else Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("virtual_riff_button")
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isVirtualRiffEnabled) "RIFF: ON" else "DEMO RIFF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            DropdownMenu(
                                expanded = showRiffMenu,
                                onDismissRequest = { showRiffMenu = false },
                                modifier = Modifier.background(Color(0xFF1A1D24))
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isVirtualRiffEnabled) "Stop Virtual Riff" else "Start Virtual Riff", color = Color.White) },
                                    onClick = {
                                        viewModel.toggleVirtualRiff()
                                        showRiffMenu = false
                                    }
                                )
                                HorizontalDivider(color = Color(0xFF333842))
                                RiffStyle.entries.forEach { style ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = style.displayName,
                                                color = if (selectedRiffStyle == style) Color(0xFFFFD54F) else Color.LightGray,
                                                fontWeight = if (selectedRiffStyle == style) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.setRiffStyle(style)
                                            if (!isVirtualRiffEnabled) viewModel.toggleVirtualRiff()
                                            showRiffMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Tuner Toggle Button
                        IconButton(
                            onClick = {
                                if (!hasAudioPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                                viewModel.toggleTuner()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    if (isTunerOpen) Color(0xFF00E676) else Color(0xFF262C38),
                                    CircleShape
                                )
                                .testTag("tuner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Tuner",
                                tint = if (isTunerOpen) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // iRig 3.5mm Earphone Jack & USB Audio Button
                        IconButton(
                            onClick = { viewModel.setShowUsbSheet(true) },
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    if (isIrigConnected) Color(0xFF00E676) else Color(0xFF262C38),
                                    CircleShape
                                )
                                .testTag("irig_audio_button")
                        ) {
                            Icon(
                                imageVector = if (isIrigConnected) Icons.Default.HeadsetMic else Icons.Default.Headphones,
                                contentDescription = "iRig & Audio Interfaces",
                                tint = if (isIrigConnected) Color.Black else Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Mute button
                        IconButton(
                            onClick = { viewModel.toggleMute() },
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    if (isMuted) Color(0xFFFF1744) else Color(0xFF262C38),
                                    CircleShape
                                )
                                .testTag("mute_button")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Mute",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Permission Warning Banner if not granted
                if (!hasAudioPermission) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3E2723)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Microphone/USB audio permission required for real-time guitar input.",
                                fontSize = 11.sp,
                                color = Color(0xFFFFCCBC),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("GRANT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Oscilloscope & Signal Meters
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    OscilloscopeView(
                        waveform = waveform,
                        stats = engineStats
                    )
                }

                // View Tabs (Pedals / Amp Head / All)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF131518),
                    contentColor = Color(0xFFFFB300),
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("PEDALBOARD", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("YAMAHA AMP", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("FULL STACK", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        },
        bottomBar = {
            // Preset Bar & Master Output Strip
            Surface(
                color = Color(0xFF101215),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("MASTER OUTPUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90A4AE))
                        Text(String.format("%.2fx", masterVolume), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                    }
                    Slider(
                        value = masterVolume,
                        onValueChange = { viewModel.setMasterVolume(it) },
                        valueRange = 0.5f..2.5f,
                        modifier = Modifier.height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFB300),
                            activeTrackColor = Color(0xFFFFB300)
                        )
                    )

                    // Quick Preset Buttons Carousel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PRESET: ${currentPreset.name}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Tap Tempo button for delay
                            FilledTonalButton(
                                onClick = { viewModel.tapTempo() },
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF2A313D)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("tap_tempo_button")
                            ) {
                                Text("TAP TEMPO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6))
                            }

                            // Save Preset Button
                            IconButton(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "Save Preset", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }

                            // JSON Share Button
                            IconButton(
                                onClick = { showJsonDialog = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Preset JSON", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetsList) { p ->
                            val isSelected = p.id == currentPreset.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectPreset(p) },
                                label = { Text(p.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFB300),
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.testTag("preset_chip_${p.id}")
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0D0E11),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(14.dp)
            ) {
                // Tuner Modal when active
                AnimatedVisibility(visible = isTunerOpen) {
                    Column {
                        GuitarTunerModal(
                            result = tunerResult,
                            onClose = { viewModel.toggleTuner() }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Amp Head View (Visible in Amp tab or Full Stack tab)
                if (selectedTab == 1 || selectedTab == 2) {
                    val ampFx = effectsChain.firstOrNull { it.type == EffectType.AMP_SIM }
                    val cabFx = effectsChain.firstOrNull { it.type == EffectType.CAB_SIM }

                    YamahaAmpHeadView(
                        ampEffect = ampFx,
                        cabEffect = cabFx,
                        inputRms = engineStats.inputLevelRms,
                        onAmpParamChange = { p, v -> viewModel.updateParam(EffectType.AMP_SIM, p, v) },
                        onCabParamChange = { p, v -> viewModel.updateParam(EffectType.CAB_SIM, p, v) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Stompbox Pedalboard Chain (Visible in Pedals tab or Full Stack tab)
                if (selectedTab == 0 || selectedTab == 2) {
                    Text(
                        text = "EFFECTS SIGNAL CHAIN (ROUTING)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90A4AE),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val horizontalScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScrollState),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        effectsChain.forEachIndexed { idx, fx ->
                            StompboxCard(
                                effect = fx,
                                index = idx,
                                totalCount = effectsChain.size,
                                onToggleBypass = { viewModel.toggleEffectBypass(fx.type) },
                                onParamChange = { p, v -> viewModel.updateParam(fx.type, p, v) },
                                onMoveLeft = { viewModel.moveEffect(idx, idx - 1) },
                                onMoveRight = { viewModel.moveEffect(idx, idx + 1) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Bottom Sheet for iRig & USB Audio interfaces
        if (showUsbSheet) {
            UsbAudioBottomSheet(
                devices = connectedDevices,
                specs = hardwareSpecs,
                isIrigConnected = isIrigConnected,
                isIrigModeEnabled = isIrigModeEnabled,
                irigGain = irigGain,
                irigAntiFeedback = irigAntiFeedback,
                irigHumReduction = irigHumReduction,
                onToggleIrigMode = { viewModel.setIrigModeEnabled(it) },
                onIrigGainChange = { viewModel.setIrigInputGain(it) },
                onToggleAntiFeedback = { viewModel.toggleIrigAntiFeedback() },
                onToggleHumReduction = { viewModel.toggleIrigHumReduction() },
                onDismiss = { viewModel.setShowUsbSheet(false) },
                onRefresh = { viewModel.refreshAudioDevices() }
            )
        }

        // Save preset dialog
        if (showSaveDialog) {
            SavePresetDialog(
                onDismiss = { showSaveDialog = false },
                onSave = { name, desc ->
                    viewModel.saveCurrentAsNewPreset(name, desc)
                    showSaveDialog = false
                }
            )
        }

        // JSON Export / Import dialog
        if (showJsonDialog) {
            val json = PresetRepository.presetToJson(currentPreset)
            PresetJsonDialog(
                jsonContent = json,
                onDismiss = { showJsonDialog = false },
                onImport = { importedJson ->
                    val preset = PresetRepository.jsonToPreset(importedJson)
                    if (preset != null) {
                        viewModel.selectPreset(preset)
                    }
                    showJsonDialog = false
                }
            )
        }
    }
}
