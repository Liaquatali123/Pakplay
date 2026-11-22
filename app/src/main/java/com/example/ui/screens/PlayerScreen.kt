package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.local.entity.VideoEntity
import com.example.player.AspectRatioMode
import com.example.ui.components.AspectRatioDialog
import com.example.ui.components.AudioTrackSelectorDialog
import com.example.ui.components.GestureOverlay
import com.example.ui.components.PlaybackSpeedDialog
import com.example.ui.components.ResumePlaybackDialog
import com.example.ui.components.TracksSelectorDialog
import com.example.ui.components.VideoInfoDialog
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PurpleElectric
import com.example.ui.viewmodel.PlayerViewModel

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    videoId: Long,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val playerUiState by playerViewModel.playerUiState.collectAsStateWithLifecycle()
    val settings by playerViewModel.appSettings.settings.collectAsStateWithLifecycle()
    val playlistVideos by playerViewModel.playlistVideos.collectAsStateWithLifecycle()
    val isShuffleEnabled by playerViewModel.isShuffleEnabled.collectAsStateWithLifecycle()
    val isRepeatEnabled by playerViewModel.isRepeatEnabled.collectAsStateWithLifecycle()

    val showResumeDialog by playerViewModel.showResumeDialog.collectAsStateWithLifecycle()
    val showSpeedDialog by playerViewModel.showSpeedDialog.collectAsStateWithLifecycle()
    val showAudioTrackDialog by playerViewModel.showAudioTrackDialog.collectAsStateWithLifecycle()
    val showTrackDialog by playerViewModel.showTrackDialog.collectAsStateWithLifecycle()
    val showAspectRatioDialog by playerViewModel.showAspectRatioDialog.collectAsStateWithLifecycle()
    val showVideoInfoDialog by playerViewModel.showVideoInfoDialog.collectAsStateWithLifecycle()

    var isLandscape by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isUserDraggingSlider by remember { mutableStateOf(false) }

    // Immersive Fullscreen, Screen On, and Volume Control Stream
    DisposableEffect(Unit) {
        playerViewModel.playerController.syncSystemVolume()
        val window = activity?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        activity?.volumeControlStream = android.media.AudioManager.STREAM_MUSIC
        if (settings.keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val window = activity?.window
            if (window != null) {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Handle back button (unlock first if locked)
    BackHandler {
        if (playerUiState.isLocked) {
            playerViewModel.toggleLock()
        } else {
            playerViewModel.playerController.savePlaybackProgress()
            onBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen")
    ) {
        // ExoPlayer View
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .scale(playerUiState.zoomScale),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playerViewModel.playerController.getPlayer()
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    resizeMode = when (playerUiState.aspectRatioMode) {
                        AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        AspectRatioMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                        AspectRatioMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                        AspectRatioMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                }
            },
            update = { view ->
                if (view.player == null) {
                    view.player = playerViewModel.playerController.getPlayer()
                }
                view.resizeMode = when (playerUiState.aspectRatioMode) {
                    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    AspectRatioMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                    AspectRatioMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            onReset = { view ->
                view.player = null
            }
        )

        // Gesture Overlay
        GestureOverlay(
            isLocked = playerUiState.isLocked,
            gesturesEnabled = settings.gesturesEnabled,
            seekSeconds = settings.doubleTapSeekSeconds,
            onSingleTap = { playerViewModel.toggleControls() },
            onDoubleTapLeft = { playerViewModel.seekRelative(-settings.doubleTapSeekSeconds * 1000L) },
            onDoubleTapRight = { playerViewModel.seekRelative(settings.doubleTapSeekSeconds * 1000L) },
            onDoubleTapCenter = { playerViewModel.togglePlayPause() },
            onBrightnessDelta = { delta ->
                activity?.let { playerViewModel.changeBrightnessBy(it, delta) }
            },
            onVolumeDelta = { delta -> playerViewModel.changeVolumeBy(delta) },
            onHorizontalSwipeSeek = { deltaMs -> playerViewModel.seekRelative(deltaMs) },
            onZoomChange = { scale -> playerViewModel.setZoomScale(scale) }
        )

        // Subtitle Overlay
        if (playerUiState.activeSubtitleText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (playerUiState.showControls) 90.dp else 24.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = playerUiState.activeSubtitleText,
                        color = Color.White,
                        fontSize = settings.subtitleFontSize.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Center Gesture Notice (Volume / Brightness / Seek Feedback)
        playerUiState.gestureNoticeText?.let { notice ->
            Box(
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val iconVector = when (playerUiState.gestureNoticeIcon) {
                            "volume_up" -> Icons.AutoMirrored.Filled.VolumeUp
                            "volume_off" -> Icons.AutoMirrored.Filled.VolumeMute
                            "brightness" -> Icons.Default.Brightness6
                            "forward" -> Icons.Default.FastForward
                            "rewind" -> Icons.Default.FastRewind
                            "speed" -> Icons.Default.Speed
                            "lock" -> Icons.Default.Lock
                            "lock_open" -> Icons.Default.LockOpen
                            "aspect_ratio" -> Icons.Default.AspectRatio
                            "zoom" -> Icons.Default.ZoomOutMap
                            "shuffle" -> Icons.Default.Shuffle
                            "repeat" -> Icons.Default.Repeat
                            else -> Icons.Default.PlayArrow
                        }
                        Icon(imageVector = iconVector, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(24.dp))
                        Text(
                            text = notice,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Buffering Indicator in Center
        if (playerUiState.isBuffering) {
            Box(
                modifier = Modifier.align(Alignment.Center)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = CyanNeon,
                    strokeWidth = 4.dp
                )
            }
        }

        // Error message banner
        playerUiState.errorMessage?.let { error ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Playback Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                            IconButton(onClick = { playerViewModel.playVideo(playlistVideos.getOrNull(playerViewModel.currentPlaylistIndex.value) ?: return@IconButton) }) {
                                Icon(Icons.Default.Replay, contentDescription = "Retry", tint = CyanNeon)
                            }
                        }
                    }
                }
            }
        }

        // Locked mode indicator / Floating unlock button
        if (playerUiState.isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .clickable { playerViewModel.toggleLock() }
                        .testTag("unlock_screen_button"),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeon)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Unlock",
                            tint = CyanNeon,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Main Player Controls Overlay (Top bar, Center controls, Bottom bar)
        AnimatedVisibility(
            visible = playerUiState.showControls && !playerUiState.isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        playerViewModel.toggleControls()
                    }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // Top Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                playerViewModel.playerController.savePlaybackProgress()
                                onBack()
                            },
                            modifier = Modifier.testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = playerUiState.videoTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Playback Speed Button
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier
                                .clickable { playerViewModel.openSpeedDialog() }
                                .padding(horizontal = 4.dp)
                                .testTag("speed_button")
                        ) {
                            Text(
                                text = "${playerUiState.playbackSpeed}x",
                                color = CyanNeon,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        // Audio Tracks Button (Dedicated Multi-Audio Track Switching)
                        IconButton(
                            onClick = { playerViewModel.openAudioTrackDialog() },
                            modifier = Modifier.size(36.dp).testTag("audio_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = "Audio Tracks",
                                tint = if (playerUiState.audioTracks.size > 1) CyanNeon else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Subtitles & Track Settings
                        IconButton(
                            onClick = { playerViewModel.openTrackDialog() },
                            modifier = Modifier.size(36.dp).testTag("tracks_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = "Subtitles & Tracks",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Aspect Ratio Button
                        IconButton(
                            onClick = { playerViewModel.openAspectRatioDialog() },
                            modifier = Modifier.size(36.dp).testTag("aspect_ratio_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Aspect Ratio",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Picture in Picture Button
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(
                                onClick = {
                                    activity?.let { playerViewModel.enterPictureInPicture(it) }
                                },
                                modifier = Modifier.size(36.dp).testTag("pip_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = "PiP",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Video Info Button
                        IconButton(
                            onClick = { playerViewModel.openVideoInfoDialog() },
                            modifier = Modifier.size(36.dp).testTag("info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Stop / Close Video Button
                        IconButton(
                            onClick = {
                                playerViewModel.stopPlayback()
                                onBack()
                            },
                            modifier = Modifier.size(36.dp).testTag("stop_video_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop Video and Exit",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Center Large Playback Controls (Rewind 10s, Play/Pause, Forward 10s)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { playerViewModel.seekRelative(-settings.doubleTapSeekSeconds * 1000L) },
                        modifier = Modifier.size(52.dp).testTag("center_rewind_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable { playerViewModel.togglePlayPause() }
                            .testTag("center_play_pause_button"),
                        shape = CircleShape,
                        color = CyanNeon
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (playerUiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerUiState.isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { playerViewModel.seekRelative(settings.doubleTapSeekSeconds * 1000L) },
                        modifier = Modifier.size(52.dp).testTag("center_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Bottom Control Bar (Time scrubber, Lock, Prev, Next, Fullscreen)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // Scrubber with timestamps
                    val currentPos = if (isUserDraggingSlider) sliderPosition.toLong() else playerUiState.currentPositionMs
                    val totalDur = playerUiState.durationMs

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPos),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Slider(
                            value = if (totalDur > 0) currentPos.toFloat() else 0f,
                            onValueChange = {
                                isUserDraggingSlider = true
                                sliderPosition = it
                            },
                            onValueChangeFinished = {
                                playerViewModel.seekTo(sliderPosition.toLong())
                                isUserDraggingSlider = false
                            },
                            valueRange = 0f..(if (totalDur > 0) totalDur.toFloat() else 1f),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .testTag("player_seek_bar"),
                            colors = SliderDefaults.colors(
                                thumbColor = CyanNeon,
                                activeTrackColor = CyanNeon,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )

                        Text(
                            text = formatTime(totalDur),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Bottom Action Icons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Lock button
                        IconButton(
                            onClick = { playerViewModel.toggleLock() },
                            modifier = Modifier.size(40.dp).testTag("lock_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Lock Screen",
                                tint = Color.White
                            )
                        }

                        // Center: Prev, Play, Next
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { playerViewModel.playPrevious() },
                                modifier = Modifier.size(40.dp).testTag("prev_video_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous Video",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = { playerViewModel.togglePlayPause() },
                                modifier = Modifier.size(40.dp).testTag("bottom_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (playerUiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playerUiState.isPlaying) "Pause" else "Play",
                                    tint = CyanNeon
                                )
                            }

                            IconButton(
                                onClick = { playerViewModel.playNext() },
                                modifier = Modifier.size(40.dp).testTag("next_video_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Video",
                                    tint = Color.White
                                )
                            }
                        }

                        // Right: Repeat, Shuffle, Orientation
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { playerViewModel.toggleRepeat() },
                                modifier = Modifier.size(36.dp).testTag("repeat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "Repeat",
                                    tint = if (isRepeatEnabled) CyanNeon else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { playerViewModel.toggleShuffle() },
                                modifier = Modifier.size(36.dp).testTag("shuffle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (isShuffleEnabled) CyanNeon else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    isLandscape = !isLandscape
                                    activity?.requestedOrientation = if (isLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                },
                                modifier = Modifier.size(36.dp).testTag("orientation_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = "Rotate Screen",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    showResumeDialog?.let { video ->
        ResumePlaybackDialog(
            video = video,
            onConfirm = { resume ->
                playerViewModel.onResumeDialogConfirm(resume)
            }
        )
    }

    if (showSpeedDialog) {
        PlaybackSpeedDialog(
            currentSpeed = playerUiState.playbackSpeed,
            onSpeedSelected = { speed -> playerViewModel.setPlaybackSpeed(speed) },
            onDismiss = { playerViewModel.closeSpeedDialog() }
        )
    }

    if (showAudioTrackDialog) {
        AudioTrackSelectorDialog(
            audioTracks = playerUiState.audioTracks,
            selectedIndex = playerUiState.selectedAudioTrackIndex,
            onTrackSelected = { playerViewModel.selectAudioTrack(it) },
            onDismiss = { playerViewModel.closeAudioTrackDialog() }
        )
    }

    if (showAspectRatioDialog) {
        AspectRatioDialog(
            currentMode = playerUiState.aspectRatioMode,
            onModeSelected = { mode -> playerViewModel.setAspectRatio(mode) },
            onDismiss = { playerViewModel.closeAspectRatioDialog() }
        )
    }

    if (showTrackDialog) {
        TracksSelectorDialog(
            audioTracks = playerUiState.audioTracks,
            selectedAudioIndex = playerUiState.selectedAudioTrackIndex,
            onAudioTrackSelected = { playerViewModel.selectAudioTrack(it) },
            subtitleTracks = playerUiState.subtitleTracks,
            selectedSubtitleIndex = playerUiState.selectedSubtitleTrackIndex,
            onSubtitleTrackSelected = { playerViewModel.selectSubtitleTrack(it) },
            videoQualities = playerUiState.videoQualities,
            onLoadExternalSubtitle = { uri -> playerViewModel.loadExternalSubtitle(uri, context) },
            onDismiss = { playerViewModel.closeTrackDialog() }
        )
    }

    if (showVideoInfoDialog) {
        val currentVid = playlistVideos.getOrNull(playerViewModel.currentPlaylistIndex.value)
        if (currentVid != null) {
            VideoInfoDialog(
                video = currentVid,
                onDismiss = { playerViewModel.closeVideoInfoDialog() },
                onPlay = {
                    playerViewModel.closeVideoInfoDialog()
                    playerViewModel.playVideo(currentVid, playlistVideos)
                },
                onShare = {
                    playerViewModel.shareVideo(context, currentVid)
                },
                onRename = {},
                onFavoriteToggle = {
                    playerViewModel.toggleFavorite(currentVid)
                },
                onAddToPlaylist = {},
                onDelete = {
                    playerViewModel.deleteVideo(currentVid)
                }
            )
        }
    }
}

private fun formatTime(millis: Long): String {
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
