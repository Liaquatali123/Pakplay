package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PakPlayApplication
import com.example.data.local.entity.VideoEntity
import com.example.data.preferences.AppSettings
import com.example.player.AspectRatioMode
import com.example.player.PlayerUiState
import com.example.player.SubtitleParser
import com.example.player.VideoPlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PakPlayApplication
    val repository = app.repository
    val appSettings: AppSettings = app.appSettings

    val playerController = VideoPlayerController(
        context = application.applicationContext,
        repository = repository,
        scope = viewModelScope,
        appSettings = appSettings
    )

    val playerUiState: StateFlow<PlayerUiState> = playerController.uiState

    private val _playlistVideos = MutableStateFlow<List<VideoEntity>>(emptyList())
    val playlistVideos: StateFlow<List<VideoEntity>> = _playlistVideos.asStateFlow()

    private val _currentPlaylistIndex = MutableStateFlow(0)
    val currentPlaylistIndex: StateFlow<Int> = _currentPlaylistIndex.asStateFlow()

    private val _showResumeDialog = MutableStateFlow<VideoEntity?>(null)
    val showResumeDialog: StateFlow<VideoEntity?> = _showResumeDialog.asStateFlow()

    private val _showSpeedDialog = MutableStateFlow(false)
    val showSpeedDialog: StateFlow<Boolean> = _showSpeedDialog.asStateFlow()

    private val _showAudioTrackDialog = MutableStateFlow(false)
    val showAudioTrackDialog: StateFlow<Boolean> = _showAudioTrackDialog.asStateFlow()

    private val _showTrackDialog = MutableStateFlow(false)
    val showTrackDialog: StateFlow<Boolean> = _showTrackDialog.asStateFlow()

    private val _showAspectRatioDialog = MutableStateFlow(false)
    val showAspectRatioDialog: StateFlow<Boolean> = _showAspectRatioDialog.asStateFlow()

    private val _showVideoInfoDialog = MutableStateFlow(false)
    val showVideoInfoDialog: StateFlow<Boolean> = _showVideoInfoDialog.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _isRepeatEnabled = MutableStateFlow(false)
    val isRepeatEnabled: StateFlow<Boolean> = _isRepeatEnabled.asStateFlow()

    fun playVideo(video: VideoEntity, playlist: List<VideoEntity> = emptyList()) {
        val list = if (playlist.isNotEmpty()) playlist else listOf(video)
        _playlistVideos.value = list
        val index = list.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
        _currentPlaylistIndex.value = index

        val resumeMode = appSettings.settings.value.resumeMode

        if (video.resumePosition > 3000L && video.resumePosition < (video.duration - 5000L)) {
            when (resumeMode) {
                "ALWAYS" -> {
                    startPlayback(video, video.resumePosition)
                }
                "NEVER" -> {
                    startPlayback(video, 0L)
                }
                else -> { // "ASK"
                    _showResumeDialog.value = video
                }
            }
        } else {
            startPlayback(video, 0L)
        }
    }

    fun onResumeDialogConfirm(resume: Boolean) {
        val video = _showResumeDialog.value ?: return
        _showResumeDialog.value = null
        val startPos = if (resume) video.resumePosition else 0L
        startPlayback(video, startPos)
    }

    private fun startPlayback(video: VideoEntity, startPositionMs: Long) {
        val defaultSpeed = appSettings.settings.value.defaultPlaybackSpeed
        playerController.prepareAndPlay(video, startPositionMs)
        if (defaultSpeed != 1.0f) {
            playerController.setPlaybackSpeed(defaultSpeed)
        }
    }

    fun playNext() {
        val list = _playlistVideos.value
        if (list.isEmpty()) return
        val nextIdx = if (_isShuffleEnabled.value) {
            (0 until list.size).random()
        } else {
            (_currentPlaylistIndex.value + 1) % list.size
        }
        _currentPlaylistIndex.value = nextIdx
        val nextVideo = list[nextIdx]
        startPlayback(nextVideo, 0L)
    }

    fun playPrevious() {
        val list = _playlistVideos.value
        if (list.isEmpty()) return
        val prevIdx = if (_currentPlaylistIndex.value > 0) _currentPlaylistIndex.value - 1 else list.size - 1
        _currentPlaylistIndex.value = prevIdx
        val prevVideo = list[prevIdx]
        startPlayback(prevVideo, 0L)
    }

    fun togglePlayPause() = playerController.togglePlayPause()

    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    fun seekRelative(offsetMs: Long) = playerController.seekRelative(offsetMs)

    fun setPlaybackSpeed(speed: Float) {
        playerController.setPlaybackSpeed(speed)
        _showSpeedDialog.value = false
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        playerController.setAspectRatio(mode)
        _showAspectRatioDialog.value = false
    }

    fun toggleLock() = playerController.toggleLock()

    fun toggleControls() = playerController.toggleControls()

    fun adjustVolume(deltaPercent: Float) = playerController.adjustVolume(deltaPercent)

    fun adjustBrightness(activity: Activity, deltaPercent: Float) = playerController.adjustBrightness(activity, deltaPercent)

    fun setZoomScale(scale: Float) = playerController.setZoomScale(scale)

    fun resetZoom() = playerController.resetZoom()

    fun selectAudioTrack(index: Int) {
        playerController.selectAudioTrack(index)
        _showAudioTrackDialog.value = false
        _showTrackDialog.value = false
    }

    fun selectSubtitleTrack(index: Int) {
        playerController.selectSubtitleTrack(index)
        _showTrackDialog.value = false
    }

    fun loadExternalSubtitle(uri: Uri, context: Context) {
        viewModelScope.launch {
            val cues = SubtitleParser.parseSubtitleFromUri(
                context = context,
                uri = uri,
                charsetName = appSettings.settings.value.subtitleEncoding
            )
            playerController.loadExternalSubtitle(cues)
            _showTrackDialog.value = false
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        val stateText = if (_isShuffleEnabled.value) "Shuffle On" else "Shuffle Off"
        playerController.showGestureNotice(stateText, "shuffle")
    }

    fun toggleRepeat() {
        _isRepeatEnabled.value = !_isRepeatEnabled.value
        val stateText = if (_isRepeatEnabled.value) "Repeat On" else "Repeat Off"
        playerController.showGestureNotice(stateText, "repeat")
    }

    fun openSpeedDialog() { _showSpeedDialog.value = true }
    fun closeSpeedDialog() { _showSpeedDialog.value = false }

    fun openAudioTrackDialog() { _showAudioTrackDialog.value = true }
    fun closeAudioTrackDialog() { _showAudioTrackDialog.value = false }

    fun openTrackDialog() { _showTrackDialog.value = true }
    fun closeTrackDialog() { _showTrackDialog.value = false }

    fun openAspectRatioDialog() { _showAspectRatioDialog.value = true }
    fun closeAspectRatioDialog() { _showAspectRatioDialog.value = false }

    fun openVideoInfoDialog() { _showVideoInfoDialog.value = true }
    fun closeVideoInfoDialog() { _showVideoInfoDialog.value = false }

    fun toggleFavorite(video: VideoEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(video.id, video.isFavorite)
        }
    }

    fun deleteVideo(video: VideoEntity) {
        viewModelScope.launch {
            repository.deleteVideo(video.id)
            closeVideoInfoDialog()
            playNext()
        }
    }

    fun shareVideo(context: Context, video: VideoEntity) {
        try {
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = video.mimeType.ifEmpty { "video/*" }
                val uri = android.net.Uri.parse(video.path)
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Video"))
        } catch (e: Exception) {
            // ignore
        }
    }

    fun enterPictureInPicture(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                if (activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                    val params = android.app.PictureInPictureParams.Builder().build()
                    activity.enterPictureInPictureMode(params)
                }
            } catch (e: Exception) {
                // Ignore or handle
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerController.release()
    }
}
