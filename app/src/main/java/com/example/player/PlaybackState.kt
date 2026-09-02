package com.example.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi

enum class AspectRatioMode(val label: String) {
    FIT("Fit to Screen"),
    CROP("Crop / Fill"),
    STRETCH("Stretch"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3"),
    ORIGINAL("Original 100%")
}

data class TrackInfo(
    val id: String,
    val index: Int,
    val name: String,
    val language: String?,
    val isSelected: Boolean,
    val format: Format? = null
)

data class VideoQualityInfo(
    val index: Int,
    val resolution: String,
    val bitrate: Long,
    val frameRate: Float,
    val codec: String?,
    val isSelected: Boolean
)

data class SubtitleCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

data class PlayerUiState(
    val currentVideoId: Long = 0L,
    val videoTitle: String = "",
    val videoUri: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val isLocked: Boolean = false,
    val brightness: Float = 0.5f,
    val volume: Float = 0.5f,
    val maxVolume: Int = 15,
    val currentVolumeLevel: Int = 7,
    val zoomScale: Float = 1.0f,
    val showControls: Boolean = true,
    val audioTracks: List<TrackInfo> = emptyList(),
    val selectedAudioTrackIndex: Int = -1,
    val subtitleTracks: List<TrackInfo> = emptyList(),
    val selectedSubtitleTrackIndex: Int = -1,
    val externalSubtitles: List<SubtitleCue> = emptyList(),
    val activeSubtitleText: String = "",
    val videoQualities: List<VideoQualityInfo> = emptyList(),
    val selectedVideoQualityIndex: Int = -1,
    val errorMessage: String? = null,
    val isPiPActive: Boolean = false,
    val isBackgroundPlayEnabled: Boolean = false,
    val autoPlayNext: Boolean = true,
    val gestureNoticeText: String? = null,
    val gestureNoticeIcon: String? = null,
    val isScreenAwake: Boolean = true,
    val decoderInfo: String = "Hardware Decoding"
)
