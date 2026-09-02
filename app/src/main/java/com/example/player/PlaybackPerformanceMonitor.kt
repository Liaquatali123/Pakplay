package com.example.player

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

data class PlaybackPerformanceMetrics(
    val droppedFramesCount: Int = 0,
    val renderTimeMs: Long = 0L,
    val isHardwareDecoded: Boolean = true,
    val activeCodecName: String = "Hardware Decoder"
)

@OptIn(UnstableApi::class)
class PlaybackPerformanceMonitor(private val player: ExoPlayer) {
    private val tag = "PlaybackPerfMonitor"
    private var initStartTime: Long = System.currentTimeMillis()

    fun getInitializationDuration(): Long {
        return System.currentTimeMillis() - initStartTime
    }

    fun logMetrics() {
        try {
            val videoFormat = player.videoFormat
            val codec = videoFormat?.sampleMimeType ?: "unknown"
            val width = videoFormat?.width ?: 0
            val height = videoFormat?.height ?: 0
            val bitrate = videoFormat?.bitrate ?: 0
            Log.d(tag, "Active Video: codec=$codec, res=${width}x{height}, bitrate=$bitrate")
        } catch (e: Exception) {
            Log.e(tag, "Error logging performance metrics", e)
        }
    }
}
