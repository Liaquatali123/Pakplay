package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

data class FolderSummary(
    val folder: String,
    val videoCount: Int,
    val totalSize: Long,
    val latestDateAdded: Long
)

data class VideoSyncInfo(
    val id: Long,
    val path: String,
    val dateModified: Long,
    val size: Long,
    val duration: Long,
    val isFavorite: Boolean,
    val lastPlayed: Long,
    val resumePosition: Long,
    val watchedPercentage: Int
)

@Dao
interface VideoDao {

    @Query("SELECT * FROM videos ORDER BY dateAdded DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT id, path, dateModified, size, duration, isFavorite, lastPlayed, resumePosition, watchedPercentage FROM videos")
    suspend fun getAllVideoSyncInfo(): List<VideoSyncInfo>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: Long): VideoEntity?

    @Query("SELECT * FROM videos WHERE path = :path LIMIT 1")
    suspend fun getVideoByPath(path: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE lastPlayed > 0 ORDER BY lastPlayed DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 20): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY dateAdded DESC LIMIT :limit")
    fun getRecentlyAdded(limit: Int = 20): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE resumePosition > 1000 AND resumePosition < (duration * 0.95) ORDER BY lastPlayed DESC LIMIT :limit")
    fun getContinueWatching(limit: Int = 20): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavorites(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE folder = :folderName ORDER BY title ASC")
    fun getVideosByFolder(folderName: String): Flow<List<VideoEntity>>

    @Query("""
        SELECT folder, COUNT(*) as videoCount, SUM(size) as totalSize, MAX(dateAdded) as latestDateAdded 
        FROM videos 
        WHERE folder != '' 
        GROUP BY folder 
        ORDER BY folder ASC
    """)
    fun getFolderSummaries(): Flow<List<FolderSummary>>

    @Query("""
        SELECT * FROM videos 
        WHERE title LIKE '%' || :query || '%' OR folder LIKE '%' || :query || '%'
        ORDER BY title ASC
    """)
    fun searchVideos(query: String): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("UPDATE videos SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("""
        UPDATE videos 
        SET resumePosition = :position, 
            watchedPercentage = :percentage, 
            lastPlayed = :timestamp 
        WHERE id = :id
    """)
    suspend fun updatePlaybackProgress(id: Long, position: Long, percentage: Int, timestamp: Long)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideoById(id: Long)

    @Query("DELETE FROM videos WHERE id IN (:ids)")
    suspend fun deleteVideosByIds(ids: List<Long>)

    @Query("DELETE FROM videos WHERE path NOT IN (:currentPaths)")
    suspend fun deleteStaleVideos(currentPaths: List<String>)

    @Query("DELETE FROM videos")
    suspend fun deleteAllVideos()

    @Query("SELECT * FROM videos")
    suspend fun getAllVideosList(): List<VideoEntity>

    @Query("UPDATE videos SET resumePosition = 0, watchedPercentage = 0, lastPlayed = 0")
    suspend fun clearHistory()

    @Query("UPDATE videos SET resumePosition = 0, watchedPercentage = 0")
    suspend fun clearResumePositions()

    @Transaction
    suspend fun applyIncrementalSync(
        toInsert: List<VideoEntity>,
        toUpdate: List<VideoEntity>,
        toDeleteIds: List<Long>
    ) {
        if (toDeleteIds.isNotEmpty()) {
            deleteVideosByIds(toDeleteIds)
        }
        if (toInsert.isNotEmpty()) {
            insertVideos(toInsert)
        }
        if (toUpdate.isNotEmpty()) {
            insertVideos(toUpdate)
        }
    }
}
