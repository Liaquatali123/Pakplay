package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.VideoEntity
import com.example.player.AspectRatioMode
import com.example.player.TrackInfo
import com.example.player.VideoQualityInfo
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PurpleElectric

@Composable
fun ResumePlaybackDialog(
    video: VideoEntity,
    onConfirm: (Boolean) -> Unit
) {
    val totalSec = video.resumePosition / 1000
    val formattedPos = String.format("%02d:%02d", (totalSec % 3600) / 60, totalSec % 60)

    AlertDialog(
        onDismissRequest = { onConfirm(true) },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Replay, contentDescription = null, tint = CyanNeon)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Resume Playback?", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(
                text = "Would you like to continue playing \"${video.title}\" from $formattedPos?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(true) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                modifier = Modifier.testTag("resume_confirm_button")
            ) {
                Text("Resume", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onConfirm(false) },
                modifier = Modifier.testTag("start_over_button")
            ) {
                Text("Start Over")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = CyanNeon)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Playback Speed", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(modifier = Modifier.height(280.dp)) {
                items(speeds) { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeedSelected(speed) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                            fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal,
                            color = if (speed == currentSpeed) CyanNeon else MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                        RadioButton(
                            selected = speed == currentSpeed,
                            onClick = { onSpeedSelected(speed) },
                            colors = RadioButtonDefaults.colors(selectedColor = CyanNeon)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun AspectRatioDialog(
    currentMode: AspectRatioMode,
    onModeSelected: (AspectRatioMode) -> Unit,
    onDismiss: () -> Unit
) {
    val modes = AspectRatioMode.entries

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aspect Ratio", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                modes.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = mode.label,
                            fontWeight = if (mode == currentMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (mode == currentMode) CyanNeon else MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                        RadioButton(
                            selected = mode == currentMode,
                            onClick = { onModeSelected(mode) },
                            colors = RadioButtonDefaults.colors(selectedColor = CyanNeon)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun AudioTrackSelectorDialog(
    audioTracks: List<TrackInfo>,
    selectedIndex: Int,
    onTrackSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Audio Tracks",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CyanNeon.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (audioTracks.isEmpty()) "1 Stream" else "${audioTracks.size} Track${if (audioTracks.size > 1) "s" else ""}",
                        color = CyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (audioTracks.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Default Audio (Single Stream)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "This video contains a single audio stream.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Tap to switch audio stream instantly during playback.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        items(audioTracks) { track ->
                            val isSelected = track.isSelected || track.index == selectedIndex
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onTrackSelected(track.index) }
                                    .testTag("audio_track_${track.index}"),
                                color = if (isSelected) CyanNeon.copy(alpha = 0.15f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                                border = if (isSelected) {
                                    BorderStroke(1.dp, CyanNeon)
                                } else {
                                    BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = null,
                                            tint = if (isSelected) CyanNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = track.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) CyanNeon else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp
                                            )
                                            if (!track.language.isNullOrBlank()) {
                                                Text(
                                                    text = "Language: ${track.language}",
                                                    fontSize = 11.sp,
                                                    color = if (isSelected) CyanNeon.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Active Track",
                                            tint = CyanNeon,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_audio_dialog_button")
            ) {
                Text("Close", color = CyanNeon, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun TracksSelectorDialog(
    audioTracks: List<TrackInfo>,
    selectedAudioIndex: Int,
    onAudioTrackSelected: (Int) -> Unit,
    subtitleTracks: List<TrackInfo>,
    selectedSubtitleIndex: Int,
    onSubtitleTrackSelected: (Int) -> Unit,
    videoQualities: List<VideoQualityInfo>,
    onLoadExternalSubtitle: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onLoadExternalSubtitle(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyanNeon
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Audio", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Subtitles", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Quality", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.HighQuality, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        },
        text = {
            Box(modifier = Modifier.height(260.dp)) {
                when (selectedTab) {
                    0 -> {
                        // Audio Tracks
                        if (audioTracks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text("Default Audio Track (Single Stream)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn {
                                items(audioTracks) { track ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onAudioTrackSelected(track.index) }
                                            .padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = track.name,
                                                fontWeight = if (track.isSelected || track.index == selectedAudioIndex) FontWeight.Bold else FontWeight.Normal,
                                                color = if (track.isSelected || track.index == selectedAudioIndex) CyanNeon else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (!track.language.isNullOrBlank()) {
                                                Text(
                                                    text = "Language: ${track.language}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (track.isSelected || track.index == selectedAudioIndex) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = CyanNeon)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Subtitle Tracks
                        Column {
                            // Option: Load External Subtitle (.srt / .vtt)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        filePickerLauncher.launch(arrayOf("*/*", "text/*", "application/x-subrip"))
                                    },
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.FileOpen, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Load Subtitle File (.srt, .vtt)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanNeon)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyColumn {
                                // Off option
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSubtitleTrackSelected(-1) }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Off / None",
                                            fontWeight = if (selectedSubtitleIndex == -1) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedSubtitleIndex == -1) CyanNeon else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (selectedSubtitleIndex == -1) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = CyanNeon)
                                        }
                                    }
                                }

                                items(subtitleTracks) { track ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSubtitleTrackSelected(track.index) }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = track.name,
                                            fontWeight = if (track.isSelected || track.index == selectedSubtitleIndex) FontWeight.Bold else FontWeight.Normal,
                                            color = if (track.isSelected || track.index == selectedSubtitleIndex) CyanNeon else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (track.isSelected || track.index == selectedSubtitleIndex) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = CyanNeon)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Video Qualities
                        if (videoQualities.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text("Auto Quality (Source Stream)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn {
                                items(videoQualities) { qual ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = qual.resolution,
                                                fontWeight = if (qual.isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (qual.isSelected) CyanNeon else MaterialTheme.colorScheme.onSurface
                                            )
                                            val fps = if (qual.frameRate > 0) "${qual.frameRate.toInt()} fps" else ""
                                            val codec = qual.codec ?: ""
                                            Text(
                                                text = "$codec $fps".trim(),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (qual.isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = CyanNeon)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}
