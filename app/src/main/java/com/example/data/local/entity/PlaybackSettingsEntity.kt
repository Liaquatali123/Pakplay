package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_settings")
data class PlaybackSettingsEntity(
    @PrimaryKey
    val videoId: Long,
    val position: Long = 0L,
    val speed: Float = 1.0f,
    val audioTrackIndex: Int = -1,
    val subtitleTrackIndex: Int = -1,
    val aspectRatio: String = "FIT"
)
