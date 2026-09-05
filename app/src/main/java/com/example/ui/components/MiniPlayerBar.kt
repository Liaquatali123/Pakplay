package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PurpleElectric
import com.example.ui.viewmodel.PlayerViewModel

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MiniPlayerBar(
    playerViewModel: PlayerViewModel,
    onExpandToFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerUiState by playerViewModel.playerUiState.collectAsStateWithLifecycle()

    // Show mini player whenever there is an active video in the player controller
    val hasActiveVideo = playerUiState.currentVideoId != 0L || playerUiState.videoUri.isNotBlank()

    AnimatedVisibility(
        visible = hasActiveVideo,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onExpandToFullScreen() }
                .testTag("mini_player_bar"),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Slim Progress Indicator
                val totalDur = playerUiState.durationMs
                val curPos = playerUiState.currentPositionMs
                val progress = if (totalDur > 0) (curPos.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f) else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = CyanNeon,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small Video Preview Box (Live ExoPlayer video rendering)
                    Box(
                        modifier = Modifier
                            .width(88.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black)
                            .testTag("mini_player_preview")
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = playerViewModel.playerController.getPlayer()
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                }
                            },
                            update = { view ->
                                if (view.player == null) {
                                    view.player = playerViewModel.playerController.getPlayer()
                                }
                            },
                            onReset = { view ->
                                view.player = null
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Subtle overlay tap to open
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Title & Time info
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onExpandToFullScreen() }
                    ) {
                        Text(
                            text = playerUiState.videoTitle.ifEmpty { "Playing Video" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = if (playerUiState.isPlaying) CyanNeon.copy(alpha = 0.2f) else PurpleElectric.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (playerUiState.isPlaying) "PLAYING" else "PAUSED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (playerUiState.isPlaying) CyanNeon else PurpleElectric,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = "${formatMiniTime(curPos)} / ${formatMiniTime(totalDur)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Controls: Play/Pause, Fullscreen, Close (Stop Video)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Play/Pause button
                        IconButton(
                            onClick = { playerViewModel.togglePlayPause() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("mini_player_play_pause")
                        ) {
                            Icon(
                                imageVector = if (playerUiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerUiState.isPlaying) "Pause" else "Play",
                                tint = CyanNeon,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Fullscreen expand button
                        IconButton(
                            onClick = { onExpandToFullScreen() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("mini_player_fullscreen")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Full Screen",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Close (Stop Video completely) button
                        IconButton(
                            onClick = { playerViewModel.stopPlayback() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("mini_player_close")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close and Stop Video",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatMiniTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
