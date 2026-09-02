package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.VideoEntity
import com.example.data.repository.VideoRepository
import com.example.data.scanner.MediaScanner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: VideoRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val mediaScanner = MediaScanner(context, database.videoDao())
        repository = VideoRepository(
            videoDao = database.videoDao(),
            playlistDao = database.playlistDao(),
            playbackSettingsDao = database.playbackSettingsDao(),
            mediaScanner = mediaScanner
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context returns Veyra Player`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Veyra Player", appName)
    }

    @Test
    fun `insert and retrieve video from repository`() = runBlocking {
        val sampleVideo = VideoEntity(
            id = 101L,
            title = "Nature Documentary 4K",
            path = "/storage/emulated/0/Movies/nature.mp4",
            duration = 125000L,
            size = 45000000L,
            width = 3840,
            height = 2160,
            folder = "Movies",
            mimeType = "video/mp4",
            dateAdded = System.currentTimeMillis()
        )

        database.videoDao().insertVideos(listOf(sampleVideo))

        val allVideos = repository.allVideos.first()
        assertEquals(1, allVideos.size)
        assertEquals("Nature Documentary 4K", allVideos.first().title)
        assertEquals("Movies", allVideos.first().folder)

        // Toggle favorite
        repository.toggleFavorite(101L, false)
        val favorites = repository.favorites.first()
        assertEquals(1, favorites.size)
        assertEquals(101L, favorites.first().id)

        // Update progress
        repository.updatePlaybackProgress(101L, 45000L, 125000L)
        val updated = repository.getVideoById(101L)
        assertNotNull(updated)
        assertEquals(45000L, updated?.resumePosition)
    }

    @Test
    fun `create playlist and add video`() = runBlocking {
        val plId = repository.createPlaylist("Action Movies")
        assertTrue(plId > 0)

        val playlists = repository.playlists.first()
        assertEquals(1, playlists.size)
        assertEquals("Action Movies", playlists.first().name)

        val sampleVideo = VideoEntity(
            id = 202L,
            title = "Action Trailer",
            path = "/storage/emulated/0/Download/action.mkv",
            duration = 60000L,
            size = 20000000L,
            width = 1920,
            height = 1080,
            folder = "Download",
            mimeType = "video/x-matroska",
            dateAdded = System.currentTimeMillis()
        )
        database.videoDao().insertVideos(listOf(sampleVideo))
        repository.addVideosToPlaylist(plId, listOf(sampleVideo.id))

        val playlistVideos = repository.getVideosInPlaylist(plId).first()
        assertEquals(1, playlistVideos.size)
        assertEquals("Action Trailer", playlistVideos.first().title)
    }

    @Test
    fun `test incremental sync and VideoSyncInfo retrieval`() = runBlocking {
        val v1 = VideoEntity(
            id = 301L,
            title = "Episode 1",
            path = "/storage/emulated/0/Anime/ep1.mkv",
            duration = 1440000L,
            size = 350000000L,
            dateModified = 1000L,
            folder = "Anime"
        )
        val v2 = VideoEntity(
            id = 302L,
            title = "Episode 2",
            path = "/storage/emulated/0/Anime/ep2.mkv",
            duration = 1440000L,
            size = 350000000L,
            dateModified = 1000L,
            folder = "Anime"
        )
        database.videoDao().insertVideos(listOf(v1, v2))

        val syncInfo = database.videoDao().getAllVideoSyncInfo()
        assertEquals(2, syncInfo.size)
        assertTrue(syncInfo.any { it.id == 301L && it.path == "/storage/emulated/0/Anime/ep1.mkv" })

        // Incremental sync updates: insert v3, update v1, delete v2
        val v3 = VideoEntity(
            id = 303L,
            title = "Episode 3",
            path = "/storage/emulated/0/Anime/ep3.mkv",
            duration = 1440000L,
            size = 350000000L,
            dateModified = 2000L,
            folder = "Anime"
        )
        val v1Updated = v1.copy(title = "Episode 1 (Director's Cut)", dateModified = 2000L)

        database.videoDao().applyIncrementalSync(
            toInsert = listOf(v3),
            toUpdate = listOf(v1Updated),
            toDeleteIds = listOf(302L)
        )

        val updatedVideos = repository.allVideos.first()
        assertEquals(2, updatedVideos.size)
        assertTrue(updatedVideos.any { it.title == "Episode 1 (Director's Cut)" })
        assertTrue(updatedVideos.any { it.title == "Episode 3" })
        assertTrue(updatedVideos.none { it.id == 302L })
    }
}
