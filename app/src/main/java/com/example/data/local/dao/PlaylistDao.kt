package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.PlaylistItemEntity
import com.example.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

data class PlaylistWithVideos(
    val playlist: PlaylistEntity,
    val videos: List<VideoEntity>
)

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :newName WHERE id = :id")
    suspend fun updatePlaylistName(id: Long, newName: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity): Long

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: Long)

    @Query("""
        SELECT v.* FROM videos v
        INNER JOIN playlist_items pi ON v.id = pi.videoId
        WHERE pi.playlistId = :playlistId
        ORDER BY pi.position ASC
    """)
    fun getVideosInPlaylist(playlistId: Long): Flow<List<VideoEntity>>

    @Query("""
        SELECT v.* FROM videos v
        INNER JOIN playlist_items pi ON v.id = pi.videoId
        WHERE pi.playlistId = :playlistId
        ORDER BY pi.position ASC
    """)
    suspend fun getVideosInPlaylistSync(playlistId: Long): List<VideoEntity>

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId")
    fun getPlaylistVideoCount(playlistId: Long): Flow<Int>

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clearPlaylistItems(playlistId: Long)
}
