package com.example.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.data.local.entity.PlaybackSettingsEntity
import com.example.data.local.entity.VideoEntity
import com.example.data.preferences.AppSettings
import com.example.data.repository.VideoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@OptIn(UnstableApi::class)
class VideoPlayerController(
    private val context: Context,
    private val repository: VideoRepository,
    private val scope: CoroutineScope,
    private val appSettings: AppSettings? = null
) {
    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var gestureNoticeJob: Job? = null
    private var controlsHideJob: Job? = null
    private var currentVideoEntity: VideoEntity? = null
    private var hasPendingAudioRestore = true

    private val maxStreamVolume: Int
        get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    private val currentStreamVolume: Int
        get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    init {
        val maxVol = maxStreamVolume
        val curVol = currentStreamVolume
        val volRatio = if (maxVol > 0) curVol.toFloat() / maxVol.toFloat() else 0.5f
        _uiState.value = _uiState.value.copy(
            maxVolume = maxVol,
            currentVolumeLevel = curVol,
            volume = volRatio
        )
    }

    fun getPlayer(): ExoPlayer {
        if (exoPlayer == null) {
            initializePlayer()
        }
        return exoPlayer!!
    }

    private fun initializePlayer() {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableDecoderFallback(true)
        }

        trackSelector = DefaultTrackSelector(context)

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector!!)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _uiState.value = _uiState.value.copy(isBuffering = true, errorMessage = null)
                    }
                    Player.STATE_READY -> {
                        _uiState.value = _uiState.value.copy(
                            isBuffering = false,
                            durationMs = player.duration.coerceAtLeast(0L),
                            errorMessage = null,
                            isEnded = false
                        )
                        updateTracks()
                    }
                    Player.STATE_ENDED -> {
                        _uiState.value = _uiState.value.copy(
                            isEnded = true,
                            isPlaying = false,
                            isBuffering = false
                        )
                        savePlaybackProgress()
                    }
                    Player.STATE_IDLE -> {
                        _uiState.value = _uiState.value.copy(isBuffering = false)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startProgressTracker()
                    scheduleControlsHide()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateTracks()
            }

            override fun onPlayerError(error: PlaybackException) {
                val errorMsg = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "Video file not found or inaccessible."
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Network connection failed."
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "Unable to decode video format on this device."
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "Unsupported media container format."
                    else -> "Playback error: ${error.localizedMessage ?: "Unknown error"}"
                }
                Log.e("VideoPlayerController", "Player error: $errorMsg", error)
                _uiState.value = _uiState.value.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = errorMsg
                )
            }
        })

        exoPlayer = player
    }

    fun prepareAndPlay(video: VideoEntity, startPositionMs: Long? = null) {
        val player = getPlayer()
        currentVideoEntity = video
        hasPendingAudioRestore = true

        _uiState.value = _uiState.value.copy(
            currentVideoId = video.id,
            videoTitle = video.title,
            videoUri = video.path,
            currentPositionMs = 0L,
            durationMs = video.duration,
            isPlaying = false,
            isBuffering = true,
            errorMessage = null,
            isEnded = false,
            zoomScale = 1.0f,
            activeSubtitleText = "",
            externalSubtitles = emptyList()
        )

        val uri = if (video.path.startsWith("http://") || video.path.startsWith("https://") || video.path.startsWith("content://")) {
            Uri.parse(video.path)
        } else {
            Uri.fromFile(File(video.path))
        }

        val mediaItemBuilder = MediaItem.Builder().setUri(uri)

        // Check for local subtitle file
        if (!video.path.startsWith("http")) {
            val subFile = SubtitleParser.findMatchingSubtitleFile(video.path)
            if (subFile != null) {
                val subUri = Uri.fromFile(subFile)
                scope.launch {
                    val cues = SubtitleParser.parseSubtitleFromUri(context, subUri)
                    _uiState.value = _uiState.value.copy(externalSubtitles = cues)
                }
            }
        }

        player.setMediaItem(mediaItemBuilder.build())
        player.prepare()

        val resumePos = startPositionMs ?: video.resumePosition
        if (resumePos > 1000L && resumePos < (video.duration - 2000L)) {
            player.seekTo(resumePos)
            _uiState.value = _uiState.value.copy(currentPositionMs = resumePos)
        } else {
            player.seekTo(0L)
        }

        player.playWhenReady = true
        startProgressTracker()
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
        savePlaybackProgress()
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                pause()
            } else {
                play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val validPos = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(validPos)
        _uiState.value = _uiState.value.copy(currentPositionMs = validPos)
        updateActiveSubtitle(validPos)
    }

    fun seekRelative(offsetMs: Long) {
        val player = exoPlayer ?: return
        val currentPos = player.currentPosition
        val targetPos = (currentPos + offsetMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(targetPos)
        _uiState.value = _uiState.value.copy(currentPositionMs = targetPos)
        updateActiveSubtitle(targetPos)

        val seconds = (offsetMs / 1000).toInt()
        val notice = if (seconds > 0) "+${seconds}s" else "${seconds}s"
        showGestureNotice(notice, if (seconds > 0) "forward" else "rewind")
    }

    fun setPlaybackSpeed(speed: Float) {
        val player = exoPlayer ?: return
        player.playbackParameters = PlaybackParameters(speed)
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
        showGestureNotice("${speed}x", "speed")
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _uiState.value = _uiState.value.copy(aspectRatioMode = mode)
        showGestureNotice(mode.label, "aspect_ratio")
    }

    fun setZoomScale(scale: Float) {
        val clamped = scale.coerceIn(0.5f, 4.0f)
        _uiState.value = _uiState.value.copy(zoomScale = clamped)
    }

    fun resetZoom() {
        _uiState.value = _uiState.value.copy(zoomScale = 1.0f)
        showGestureNotice("Zoom 100%", "zoom")
    }

    fun toggleLock() {
        val newLock = !_uiState.value.isLocked
        _uiState.value = _uiState.value.copy(isLocked = newLock)
        if (newLock) {
            _uiState.value = _uiState.value.copy(showControls = false)
            showGestureNotice("Screen Locked", "lock")
        } else {
            _uiState.value = _uiState.value.copy(showControls = true)
            showGestureNotice("Screen Unlocked", "lock_open")
            scheduleControlsHide()
        }
    }

    fun setControlsVisibility(visible: Boolean) {
        if (_uiState.value.isLocked) return
        _uiState.value = _uiState.value.copy(showControls = visible)
        if (visible && _uiState.value.isPlaying) {
            scheduleControlsHide()
        }
    }

    fun toggleControls() {
        if (_uiState.value.isLocked) return
        setControlsVisibility(!_uiState.value.showControls)
    }

    private fun scheduleControlsHide() {
        controlsHideJob?.cancel()
        controlsHideJob = scope.launch {
            delay(4000)
            if (_uiState.value.isPlaying && !_uiState.value.isLocked) {
                _uiState.value = _uiState.value.copy(showControls = false)
            }
        }
    }

    // Volume gesture adjustment (0f..1f delta)
    fun adjustVolume(deltaPercent: Float) {
        val maxVol = maxStreamVolume
        val curVol = currentStreamVolume
        val newLevel = (curVol + (deltaPercent * maxVol)).toInt().coerceIn(0, maxVol)
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newLevel, 0)
        } catch (e: Exception) {
            Log.e("VideoPlayerController", "Error setting stream volume", e)
        }
        val ratio = if (maxVol > 0) newLevel.toFloat() / maxVol.toFloat() else 0f
        _uiState.value = _uiState.value.copy(
            volume = ratio,
            currentVolumeLevel = newLevel,
            maxVolume = maxVol
        )
        val percentText = "${(ratio * 100).toInt()}%"
        showGestureNotice("Volume: $percentText", if (newLevel == 0) "volume_off" else "volume_up")
    }

    // Brightness gesture adjustment (0f..1f delta)
    fun adjustBrightness(activity: Activity, deltaPercent: Float) {
        try {
            val layoutParams = activity.window.attributes
            var currentBrightness = layoutParams.screenBrightness
            if (currentBrightness < 0) {
                currentBrightness = try {
                    Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255.0f
                } catch (e: Exception) {
                    0.5f
                }
            }
            val newBrightness = (currentBrightness + deltaPercent).coerceIn(0.01f, 1.0f)
            layoutParams.screenBrightness = newBrightness
            activity.window.attributes = layoutParams

            _uiState.value = _uiState.value.copy(brightness = newBrightness)
            val percentText = "${(newBrightness * 100).toInt()}%"
            showGestureNotice("Brightness: $percentText", "brightness")
        } catch (e: Exception) {
            Log.e("VideoPlayerController", "Error setting brightness", e)
        }
    }

    fun showGestureNotice(text: String, icon: String) {
        _uiState.value = _uiState.value.copy(gestureNoticeText = text, gestureNoticeIcon = icon)
        gestureNoticeJob?.cancel()
        gestureNoticeJob = scope.launch {
            delay(1200)
            _uiState.value = _uiState.value.copy(gestureNoticeText = null, gestureNoticeIcon = null)
        }
    }

    fun selectAudioTrack(index: Int) {
        val selector = trackSelector ?: return
        val tracks = exoPlayer?.currentTracks ?: return

        var globalIndex = 0
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (trackIndex in 0 until group.length) {
                    if (globalIndex == index) {
                        val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(trackIndex))
                        val parametersBuilder = selector.buildUponParameters()
                            .setOverrideForType(override)
                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                        selector.setParameters(parametersBuilder)

                        val updatedList = _uiState.value.audioTracks.mapIndexed { idx, track ->
                            track.copy(isSelected = (idx == index))
                        }
                        _uiState.value = _uiState.value.copy(
                            selectedAudioTrackIndex = index,
                            audioTracks = updatedList
                        )

                        // Save audio preference for this video
                        currentVideoEntity?.let { video ->
                            val track = updatedList.getOrNull(index)
                            if (track != null) {
                                appSettings?.saveVideoAudioTrack(video.path, track.name, track.language, index)
                            }
                        }

                        val trackName = updatedList.getOrNull(index)?.name ?: "Track ${index + 1}"
                        showGestureNotice("Audio: $trackName", "audiotrack")
                        return
                    }
                    globalIndex++
                }
            }
        }
    }

    private fun resolveAudioTrackName(format: Format, trackNumber: Int): Pair<String, String?> {
        val rawLabel = format.label?.trim()
        val rawLang = format.language?.trim()

        val readableLanguage = if (!rawLang.isNullOrBlank() && !rawLang.equals("und", ignoreCase = true) && !rawLang.equals("zxx", ignoreCase = true)) {
            try {
                val loc = if (rawLang.length == 2 || rawLang.length == 3) {
                    Locale(rawLang)
                } else {
                    Locale.forLanguageTag(rawLang)
                }
                val display = loc.getDisplayLanguage(Locale.ENGLISH)
                if (display.isNotBlank() && !display.equals("und", ignoreCase = true)) {
                    display.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
                } else {
                    rawLang.uppercase()
                }
            } catch (e: Exception) {
                rawLang.uppercase()
            }
        } else {
            null
        }

        val channelDesc = when (format.channelCount) {
            1 -> "Mono"
            2 -> "Stereo"
            6 -> "5.1"
            8 -> "7.1"
            else -> if (format.channelCount > 2) "${format.channelCount}ch" else null
        }

        val baseName = when {
            !rawLabel.isNullOrBlank() -> rawLabel
            !readableLanguage.isNullOrBlank() -> readableLanguage
            else -> "Audio Track $trackNumber"
        }

        val fullName = if (channelDesc != null && !baseName.contains(channelDesc, ignoreCase = true)) {
            "$baseName • $channelDesc"
        } else {
            baseName
        }

        return Pair(fullName, readableLanguage)
    }

    private fun restoreSavedAudioTrack(videoPath: String, audioList: List<TrackInfo>) {
        if (audioList.size <= 1) return
        val saved = appSettings?.getSavedVideoAudioTrack(videoPath) ?: return
        val (savedName, savedLang, savedIdx) = saved

        val targetIndex = audioList.indexOfFirst { it.name == savedName }
            .takeIf { it != -1 }
            ?: audioList.indexOfFirst { !it.language.isNullOrBlank() && it.language.equals(savedLang, ignoreCase = true) }
                .takeIf { it != -1 }
            ?: if (savedIdx in audioList.indices) savedIdx else -1

        if (targetIndex != -1 && targetIndex != _uiState.value.selectedAudioTrackIndex) {
            selectAudioTrack(targetIndex)
        }
    }

    fun selectSubtitleTrack(index: Int) {
        val selector = trackSelector ?: return
        val parametersBuilder = selector.buildUponParameters()

        if (index == -1) {
            // Disable subtitles
            parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            selector.setParameters(parametersBuilder)
            _uiState.value = _uiState.value.copy(
                selectedSubtitleTrackIndex = -1,
                activeSubtitleText = ""
            )
            showGestureNotice("Subtitles Off", "subtitles_off")
            return
        }

        parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        val tracks = exoPlayer?.currentTracks ?: return
        var subGroupIndex = 0
        for (trackGroup in tracks.groups) {
            if (trackGroup.type == C.TRACK_TYPE_TEXT) {
                if (subGroupIndex == index) {
                    parametersBuilder.setOverrideForType(
                        TrackSelectionOverride(trackGroup.mediaTrackGroup, 0)
                    )
                    selector.setParameters(parametersBuilder)
                    _uiState.value = _uiState.value.copy(selectedSubtitleTrackIndex = index)
                    showGestureNotice("Subtitle selected", "subtitles")
                    return
                }
                subGroupIndex++
            }
        }
    }

    fun loadExternalSubtitle(cues: List<SubtitleCue>) {
        _uiState.value = _uiState.value.copy(
            externalSubtitles = cues,
            selectedSubtitleTrackIndex = 999
        )
        showGestureNotice("External Subtitle Loaded (${cues.size} cues)", "subtitles")
    }

    private fun updateTracks() {
        val player = exoPlayer ?: return
        val tracks = player.currentTracks

        val audioList = mutableListOf<TrackInfo>()
        val subList = mutableListOf<TrackInfo>()
        val videoQualities = mutableListOf<VideoQualityInfo>()

        var audioIdx = 0
        var selectedAudioIdx = 0
        var subIdx = 0
        var vidIdx = 0

        for (group in tracks.groups) {
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val (label, language) = resolveAudioTrackName(format, audioIdx + 1)
                        val isTrackSel = group.isTrackSelected(i)
                        if (isTrackSel) {
                            selectedAudioIdx = audioIdx
                        }
                        audioList.add(
                            TrackInfo(
                                id = format.id ?: "audio_${audioIdx}_$i",
                                index = audioIdx,
                                name = label,
                                language = language,
                                isSelected = isTrackSel,
                                format = format
                            )
                        )
                        audioIdx++
                    }
                }
                C.TRACK_TYPE_TEXT -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val label = format.label?.takeIf { it.isNotBlank() }
                            ?: format.language?.takeIf { it.isNotBlank() && it != "und" }?.let {
                                try { Locale(it).getDisplayLanguage(Locale.ENGLISH).replaceFirstChar { c -> c.uppercase() } } catch (e: Exception) { it }
                            }
                            ?: "Subtitle ${subIdx + 1}"
                        subList.add(
                            TrackInfo(
                                id = format.id ?: "sub_${subIdx}_$i",
                                index = subIdx,
                                name = label,
                                language = format.language,
                                isSelected = group.isTrackSelected(i),
                                format = format
                            )
                        )
                        subIdx++
                    }
                }
                C.TRACK_TYPE_VIDEO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val res = if (format.width > 0 && format.height > 0) "${format.width}x${format.height}" else "Auto"
                        videoQualities.add(
                            VideoQualityInfo(
                                index = vidIdx,
                                resolution = res,
                                bitrate = format.bitrate.toLong(),
                                frameRate = format.frameRate,
                                codec = format.sampleMimeType,
                                isSelected = group.isTrackSelected(i)
                            )
                        )
                        vidIdx++
                    }
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            audioTracks = audioList,
            selectedAudioTrackIndex = selectedAudioIdx,
            subtitleTracks = subList,
            videoQualities = videoQualities
        )

        // Restore saved audio track if available and not yet applied
        if (hasPendingAudioRestore && audioList.isNotEmpty()) {
            hasPendingAudioRestore = false
            currentVideoEntity?.let { video ->
                restoreSavedAudioTrack(video.path, audioList)
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = exoPlayer
                if (player != null && player.playbackState == Player.STATE_READY) {
                    val curPos = player.currentPosition
                    val duration = player.duration.coerceAtLeast(0L)
                    _uiState.value = _uiState.value.copy(
                        currentPositionMs = curPos,
                        durationMs = duration
                    )
                    updateActiveSubtitle(curPos)
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updateActiveSubtitle(currentPosMs: Long) {
        val cues = _uiState.value.externalSubtitles
        if (cues.isEmpty()) return

        val matchingCue = cues.find { currentPosMs in it.startTimeMs..it.endTimeMs }
        val text = matchingCue?.text ?: ""
        if (_uiState.value.activeSubtitleText != text) {
            _uiState.value = _uiState.value.copy(activeSubtitleText = text)
        }
    }

    fun savePlaybackProgress() {
        val video = currentVideoEntity ?: return
        val player = exoPlayer ?: return
        val pos = player.currentPosition
        val dur = player.duration.coerceAtLeast(0L)
        if (dur > 0) {
            scope.launch {
                repository.updatePlaybackProgress(video.id, pos, dur)
            }
        }
    }

    fun release() {
        savePlaybackProgress()
        stopProgressTracker()
        controlsHideJob?.cancel()
        gestureNoticeJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        trackSelector = null
    }
}
