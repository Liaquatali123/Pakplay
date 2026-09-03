package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PurpleElectric
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showResumeModeDialog by remember { mutableStateOf(false) }
    var showSeekSecDialog by remember { mutableStateOf(false) }
    var showSubtitleSizeDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearPositionsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("settings_screen"),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Playback Section
            SettingsSectionCard(title = "Playback", icon = Icons.Default.PlayCircleOutline) {
                SettingsClickableItem(
                    title = "Resume Playback",
                    subtitle = when (settings.resumeMode) {
                        "ALWAYS" -> "Always resume from last position"
                        "NEVER" -> "Always start from beginning"
                        else -> "Ask each time"
                    },
                    onClick = { showResumeModeDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                SettingsSwitchItem(
                    title = "Auto-play Next Video",
                    subtitle = "Automatically play the next video in queue when playback ends",
                    checked = settings.autoPlayNext,
                    onCheckedChange = { viewModel.appSettings.updateAutoPlayNext(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                SettingsSwitchItem(
                    title = "Hardware Acceleration",
                    subtitle = "Use hardware decoders for ultra-smooth 60fps playback",
                    checked = settings.hardwareAcceleration,
                    onCheckedChange = { viewModel.appSettings.updateHardwareAcceleration(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                SettingsSwitchItem(
                    title = "Audio Boost",
                    subtitle = "Enhance dialog clarity and volume on built-in speakers",
                    checked = settings.audioBoost,
                    onCheckedChange = { viewModel.appSettings.updateAudioBoost(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                SettingsSwitchItem(
                    title = "Keep Screen Awake",
                    subtitle = "Prevent display from dimming or sleeping while video is playing",
                    checked = settings.keepScreenOn,
                    onCheckedChange = { viewModel.appSettings.updateKeepScreenOn(it) }
                )
            }

            // Gestures Section
            SettingsSectionCard(title = "Gestures & Controls", icon = Icons.Default.Gesture) {
                SettingsSwitchItem(
                    title = "Gesture Controls",
                    subtitle = "Swipe vertically for brightness/volume and horizontally for seek",
                    checked = settings.gesturesEnabled,
                    onCheckedChange = { viewModel.appSettings.updateGesturesEnabled(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                SettingsClickableItem(
                    title = "Double Tap Seek Duration",
                    subtitle = "${settings.doubleTapSeekSeconds} seconds",
                    onClick = { showSeekSecDialog = true }
                )
            }

            // Subtitles Section
            SettingsSectionCard(title = "Subtitles", icon = Icons.Default.Subtitles) {
                SettingsClickableItem(
                    title = "Subtitle Text Size",
                    subtitle = "${settings.subtitleFontSize} sp",
                    onClick = { showSubtitleSizeDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                SettingsClickableItem(
                    title = "Subtitle Encoding",
                    subtitle = settings.subtitleEncoding,
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Standard UTF-8 subtitle encoding is active.")
                        }
                    }
                )
            }

            // Media Library Section
            SettingsSectionCard(title = "Media Library & Storage", icon = Icons.Default.Tune) {
                SettingsClickableItem(
                    title = "Rescan Device Videos",
                    subtitle = "Search internal and external storage for newly added videos",
                    onClick = {
                        viewModel.scanMedia()
                        scope.launch {
                            snackbarHostState.showSnackbar("Scanning device media files...")
                        }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                SettingsClickableItem(
                    title = "Clear Playback History",
                    subtitle = "Reset recent playback log",
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { showClearHistoryDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                SettingsClickableItem(
                    title = "Clear Resume Positions",
                    subtitle = "Reset all video progress positions",
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { showClearPositionsDialog = true }
                )
            }

            // About Veyra Section
            SettingsSectionCard(title = "About Veyra", icon = Icons.Default.Info) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Veyra Player v1.0.0",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Developer: LiaquatAli\nEngineered with ExoPlayer Media3, Jetpack Compose, and Hardware Acceleration. Supports MKV, MP4, AVI, WebM, FLV, TS, Subtitles, and PiP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPrivacyDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Privacy Policy",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "100% offline & local data security",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy - Veyra Player", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Developer: LiaquatAli\nLast updated: September 2026",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Veyra Player is built with a strict privacy-first, offline-first architecture. We respect your privacy completely:\n\n" +
                                "1. Local Data Only: All videos, media thumbnails, playlists, favorites, and playback history are stored locally on your device using a secure local Room database.\n\n" +
                                "2. No Data Collection: Veyra Player does not collect, track, store, or transmit any personal information, media contents, or analytics to external servers.\n\n" +
                                "3. Permissions: Storage permission is used solely to scan and play local media files selected by you. Internet permission (if requested by system components) is never used for telemetry or data collection.\n\n" +
                                "4. Contact: For inquiries regarding this privacy policy, contact developer LiaquatAli.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close", color = CyanNeon, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Resume mode dialog
    if (showResumeModeDialog) {
        val modes = listOf("ASK" to "Ask every time", "ALWAYS" to "Always resume", "NEVER" to "Always start over")
        AlertDialog(
            onDismissRequest = { showResumeModeDialog = false },
            title = { Text("Resume Playback Mode", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    modes.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.appSettings.updateResumeMode(key)
                                    showResumeModeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.resumeMode == key,
                                onClick = {
                                    viewModel.appSettings.updateResumeMode(key)
                                    showResumeModeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = CyanNeon)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showResumeModeDialog = false }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Double tap seek seconds dialog
    if (showSeekSecDialog) {
        val seconds = listOf(5, 10, 15, 20, 30)
        AlertDialog(
            onDismissRequest = { showSeekSecDialog = false },
            title = { Text("Double Tap Seek Duration", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    seconds.forEach { sec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.appSettings.updateDoubleTapSeekSeconds(sec)
                                    showSeekSecDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.doubleTapSeekSeconds == sec,
                                onClick = {
                                    viewModel.appSettings.updateDoubleTapSeekSeconds(sec)
                                    showSeekSecDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = CyanNeon)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$sec seconds", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSeekSecDialog = false }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Subtitle font size dialog
    if (showSubtitleSizeDialog) {
        val sizes = listOf(14 to "Small (14sp)", 18 to "Normal (18sp)", 22 to "Large (22sp)", 28 to "Extra Large (28sp)")
        AlertDialog(
            onDismissRequest = { showSubtitleSizeDialog = false },
            title = { Text("Subtitle Font Size", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    sizes.forEach { (sz, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.appSettings.updateSubtitleFontSize(sz)
                                    showSubtitleSizeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.subtitleFontSize == sz,
                                onClick = {
                                    viewModel.appSettings.updateSubtitleFontSize(sz)
                                    showSubtitleSizeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = CyanNeon)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSubtitleSizeDialog = false }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Clear history confirm dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Playback History?", fontWeight = FontWeight.Bold) },
            text = { Text("This will clear the list of recently played videos.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearHistoryDialog = false
                        scope.launch { snackbarHostState.showSnackbar("History cleared") }
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Clear positions confirm dialog
    if (showClearPositionsDialog) {
        AlertDialog(
            onDismissRequest = { showClearPositionsDialog = false },
            title = { Text("Clear Resume Positions?", fontWeight = FontWeight.Bold) },
            text = { Text("This will reset playback progress for all videos back to the beginning.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearResumePositions()
                        showClearPositionsDialog = false
                        scope.launch { snackbarHostState.showSnackbar("Resume positions cleared") }
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearPositionsDialog = false }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CyanNeon
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    titleColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (titleColor != Color.Unspecified) titleColor else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = CyanNeon
            )
        )
    }
}
