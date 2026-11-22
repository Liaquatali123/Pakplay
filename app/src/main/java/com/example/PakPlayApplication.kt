package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import coil.disk.DiskCache
import coil.decode.VideoFrameDecoder
import com.example.data.local.AppDatabase
import com.example.data.preferences.AppSettings
import com.example.data.repository.VideoRepository
import com.example.data.scanner.MediaScanner

class PakPlayApplication : Application(), ImageLoaderFactory {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: VideoRepository
        private set

    lateinit var appSettings: AppSettings
        private set

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("video_thumbnails"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(false)
            .allowHardware(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        appSettings = AppSettings(this)
        val mediaScanner = MediaScanner(this, database.videoDao())
        repository = VideoRepository(
            videoDao = database.videoDao(),
            playlistDao = database.playlistDao(),
            playbackSettingsDao = database.playbackSettingsDao(),
            mediaScanner = mediaScanner
        )

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackChannel = NotificationChannel(
                CHANNEL_PLAYBACK_ID,
                "Video Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls and status for background video playback"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(playbackChannel)
        }
    }

    companion object {
        const val CHANNEL_PLAYBACK_ID = "pakplay_playback_channel"
        lateinit var instance: PakPlayApplication
            private set
    }
}
