package com.example.data.repository

import com.example.data.local.dao.FolderSummary
import com.example.data.local.dao.PlaybackSettingsDao
import com.example.data.local.dao.PlaylistDao
import com.example.data.local.dao.VideoDao
import com.example.data.local.entity.PlaybackSettingsEntity
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.PlaylistItemEntity
import com.example.data.local.entity.VideoEntity
import com.example.data.scanner.MediaScanner
import kotlinx.coroutines.flow.Flow

class VideoRepository(
    private val videoDao: VideoDao,
    private val playlistDao: PlaylistDao,
    private val playbackSettingsDao: PlaybackSettingsDao,
    private val mediaScanner: MediaScanner
) {
    val allVideos: Flow<List<VideoEntity>> = videoDao.getAllVideos()
    val recentlyPlayed: Flow<List<VideoEntity>> = videoDao.getRecentlyPlayed()
    val recentlyAdded: Flow<List<VideoEntity>> = videoDao.getRecentlyAdded()
    val continueWatching: Flow<List<VideoEntity>> = videoDao.getContinueWatching()
    val favorites: Flow<List<VideoEntity>> = videoDao.getFavorites()
    val folderSummaries: Flow<List<FolderSummary>> = videoDao.getFolderSummaries()
    val playlists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun scanMedia(): Int = mediaScanner.scanMedia()

    suspend fun cleanStaleRecords(): Int = mediaScanner.cleanStaleRecords()

    fun getVideosByFolder(folder: String): Flow<List<VideoEntity>> = videoDao.getVideosByFolder(folder)

    fun searchVideos(query: String): Flow<List<VideoEntity>> = videoDao.searchVideos(query)

    suspend fun getVideoById(id: Long): VideoEntity? = videoDao.getVideoById(id)

    suspend fun getVideoByPath(path: String): VideoEntity? = videoDao.getVideoByPath(path)

    suspend fun toggleFavorite(id: Long, currentStatus: Boolean) {
        videoDao.setFavorite(id, !currentStatus)
    }

    suspend fun updatePlaybackProgress(id: Long, position: Long, duration: Long) {
        val percentage = if (duration > 0) ((position * 100) / duration).toInt().coerceIn(0, 100) else 0
        videoDao.updatePlaybackProgress(
            id = id,
            position = position,
            percentage = percentage,
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun updateVideo(video: VideoEntity) {
        videoDao.updateVideo(video)
    }

    suspend fun deleteVideo(id: Long) {
        videoDao.deleteVideoById(id)
    }

    suspend fun clearHistory() {
        videoDao.clearHistory()
    }

    suspend fun clearResumePositions() {
        videoDao.clearResumePositions()
    }

    // Playlist operations
    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun renamePlaylist(id: Long, newName: String) {
        playlistDao.updatePlaylistName(id, newName)
    }

    suspend fun deletePlaylist(id: Long) {
        playlistDao.deletePlaylist(id)
    }

    suspend fun addVideoToPlaylist(playlistId: Long, videoId: Long) {
        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                videoId = videoId
            )
        )
    }

    suspend fun addVideosToPlaylist(playlistId: Long, videoIds: List<Long>) {
        videoIds.forEach { vid ->
            addVideoToPlaylist(playlistId, vid)
        }
    }


    suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: Long) {
        playlistDao.removeVideoFromPlaylist(playlistId, videoId)
    }

    fun getVideosInPlaylist(playlistId: Long): Flow<List<VideoEntity>> =
        playlistDao.getVideosInPlaylist(playlistId)

    suspend fun getPlaylistById(id: Long): PlaylistEntity? =
        playlistDao.getPlaylistById(id)

    // Playback settings
    suspend fun getPlaybackSettings(videoId: Long): PlaybackSettingsEntity? =
        playbackSettingsDao.getSettingsForVideo(videoId)

    suspend fun savePlaybackSettings(settings: PlaybackSettingsEntity) {
        playbackSettingsDao.saveSettings(settings)
    }
}
