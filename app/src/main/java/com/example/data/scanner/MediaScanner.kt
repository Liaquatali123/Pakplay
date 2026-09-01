package com.example.data.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.dao.VideoDao
import com.example.data.local.entity.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaScanner(
    private val context: Context,
    private val videoDao: VideoDao
) {

    suspend fun scanMedia(): Int = withContext(Dispatchers.IO) {
        val existingVideos = videoDao.getAllVideosList().associateBy { it.path }
        val scannedVideos = mutableListOf<VideoEntity>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            } else {
                MediaStore.Video.Media.DATA
            }
        )

        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val displayNameColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val titleColumn = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val dataColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val widthColumn = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val mimeColumn = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val bucketColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                } else -1

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mediaId).toString()
                    val path = if (dataColumn != -1) cursor.getString(dataColumn) ?: contentUri else contentUri
                    val title = (if (displayNameColumn != -1) cursor.getString(displayNameColumn) else null)
                        ?: (if (titleColumn != -1) cursor.getString(titleColumn) else null)
                        ?: File(path).nameWithoutExtension
                    val duration = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L
                    val size = if (sizeColumn != -1) cursor.getLong(sizeColumn) else 0L
                    val dateAdded = if (dateAddedColumn != -1) cursor.getLong(dateAddedColumn) * 1000 else System.currentTimeMillis()
                    val dateModified = if (dateModifiedColumn != -1) cursor.getLong(dateModifiedColumn) * 1000 else System.currentTimeMillis()
                    val width = if (widthColumn != -1) cursor.getInt(widthColumn) else 0
                    val height = if (heightColumn != -1) cursor.getInt(heightColumn) else 0
                    val mimeType = if (mimeColumn != -1) cursor.getString(mimeColumn) ?: "video/*" else "video/*"

                    val folder = if (bucketColumn != -1 && bucketColumn < cursor.columnCount) {
                        cursor.getString(bucketColumn) ?: extractFolderFromPath(path)
                    } else {
                        extractFolderFromPath(path)
                    }

                    val existing = existingVideos[path]
                    val videoEntity = VideoEntity(
                        id = existing?.id ?: 0L,
                        path = path,
                        title = title,
                        duration = if (duration > 0) duration else (existing?.duration ?: 0L),
                        size = size,
                        thumbnailUri = contentUri,
                        folder = folder.ifEmpty { "Videos" },
                        dateAdded = dateAdded,
                        dateModified = dateModified,
                        lastPlayed = existing?.lastPlayed ?: 0L,
                        resumePosition = existing?.resumePosition ?: 0L,
                        watchedPercentage = existing?.watchedPercentage ?: 0,
                        isFavorite = existing?.isFavorite ?: false,
                        width = width,
                        height = height,
                        mimeType = mimeType
                    )
                    scannedVideos.add(videoEntity)
                }
            }
        } catch (e: Exception) {
            Log.e("MediaScanner", "Error scanning MediaStore", e)
        }

        // If no local videos found (e.g. in development or clean device), seed high quality sample cinema videos
        if (scannedVideos.isEmpty() && existingVideos.isEmpty()) {
            val sampleVideos = getDemoVideos()
            videoDao.insertVideos(sampleVideos)
            return@withContext sampleVideos.size
        }

        if (scannedVideos.isNotEmpty()) {
            videoDao.insertVideos(scannedVideos)
        }

        return@withContext scannedVideos.size
    }

    private fun extractFolderFromPath(path: String): String {
        return try {
            val file = File(path)
            file.parentFile?.name ?: "Videos"
        } catch (e: Exception) {
            "Videos"
        }
    }

    private fun getDemoVideos(): List<VideoEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            VideoEntity(
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                title = "Big Buck Bunny (4K Animation)",
                duration = 596000L,
                size = 158008374L,
                thumbnailUri = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800&auto=format&fit=crop&q=80",
                folder = "Movies",
                dateAdded = now - 86400000L * 2,
                dateModified = now - 86400000L * 2,
                lastPlayed = now - 3600000L,
                resumePosition = 145000L,
                watchedPercentage = 24,
                isFavorite = true,
                width = 3840,
                height = 2160,
                mimeType = "video/mp4"
            ),
            VideoEntity(
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                title = "Elephants Dream (Sci-Fi CGI)",
                duration = 653000L,
                size = 140234500L,
                thumbnailUri = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=80",
                folder = "Sci-Fi",
                dateAdded = now - 86400000L * 4,
                dateModified = now - 86400000L * 4,
                lastPlayed = now - 7200000L,
                resumePosition = 320000L,
                watchedPercentage = 49,
                isFavorite = false,
                width = 1920,
                height = 1080,
                mimeType = "video/mp4"
            ),
            VideoEntity(
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                title = "Tears of Steel (VFX Action)",
                duration = 734000L,
                size = 180556200L,
                thumbnailUri = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800&auto=format&fit=crop&q=80",
                folder = "Movies",
                dateAdded = now - 86400000L * 5,
                dateModified = now - 86400000L * 5,
                lastPlayed = 0L,
                resumePosition = 0L,
                watchedPercentage = 0,
                isFavorite = true,
                width = 1920,
                height = 800,
                mimeType = "video/mp4"
            ),
            VideoEntity(
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                title = "Sintel (Dragon Fantasy Short)",
                duration = 888000L,
                size = 210043120L,
                thumbnailUri = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800&auto=format&fit=crop&q=80",
                folder = "Downloads",
                dateAdded = now - 86400000L * 7,
                dateModified = now - 86400000L * 7,
                lastPlayed = now - 18000000L,
                resumePosition = 620000L,
                watchedPercentage = 70,
                isFavorite = true,
                width = 1920,
                height = 818,
                mimeType = "video/mp4"
            ),
            VideoEntity(
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                title = "Nature Showcase - Wild Elements",
                duration = 15000L,
                size = 15400000L,
                thumbnailUri = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=800&auto=format&fit=crop&q=80",
                folder = "Camera",
                dateAdded = now - 86400000L,
                dateModified = now - 86400000L,
                lastPlayed = 0L,
                resumePosition = 0L,
                watchedPercentage = 0,
                isFavorite = false,
                width = 1920,
                height = 1080,
                mimeType = "video/mp4"
            ),
            VideoEntity(
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                title = "Escapes & Wanderlust 4K",
                duration = 15000L,
                size = 16200000L,
                thumbnailUri = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800&auto=format&fit=crop&q=80",
                folder = "WhatsApp",
                dateAdded = now - 3600000L * 4,
                dateModified = now - 3600000L * 4,
                lastPlayed = 0L,
                resumePosition = 0L,
                watchedPercentage = 0,
                isFavorite = false,
                width = 1920,
                height = 1080,
                mimeType = "video/mp4"
            )
        )
    }
}
