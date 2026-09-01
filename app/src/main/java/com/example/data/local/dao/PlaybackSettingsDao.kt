package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PlaybackSettingsEntity

@Dao
interface PlaybackSettingsDao {

    @Query("SELECT * FROM playback_settings WHERE videoId = :videoId LIMIT 1")
    suspend fun getSettingsForVideo(videoId: Long): PlaybackSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: PlaybackSettingsEntity)

    @Query("DELETE FROM playback_settings WHERE videoId = :videoId")
    suspend fun deleteSettings(videoId: Long)

    @Query("DELETE FROM playback_settings")
    suspend fun clearAllSettings()
}
