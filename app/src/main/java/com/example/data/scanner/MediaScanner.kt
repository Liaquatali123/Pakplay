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

    companion object {
        fun verifyFileExists(context: Context, path: String): Boolean {
            if (path.isBlank()) return false
            return try {
                if (path.startsWith("content://")) {
                    val uri = Uri.parse(path)
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                        it.length > 0
                    } ?: false
                } else if (path.startsWith("http://") || path.startsWith("https://")) {
                    false
                } else {
                    val file = File(path)
                    file.exists() && file.isFile && file.length() > 0
                }
            } catch (e: Exception) {
                false
            }
        }
    }

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
                    
                    // Verify file actually exists and is accessible
                    val fileExists = verifyFileExists(context, path) || verifyFileExists(context, contentUri)
                    if (!fileExists) {
                        continue
                    }

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

        // Synchronize Room database with real storage
        if (scannedVideos.isNotEmpty()) {
            val scannedPaths = scannedVideos.map { it.path }
            videoDao.deleteStaleVideos(scannedPaths)
            videoDao.insertVideos(scannedVideos)
        } else {
            videoDao.deleteAllVideos()
        }

        return@withContext scannedVideos.size
    }

    suspend fun cleanStaleRecords(): Int = withContext(Dispatchers.IO) {
        val allVideos = videoDao.getAllVideosList()
        val staleIds = mutableListOf<Long>()
        for (video in allVideos) {
            if (!verifyFileExists(context, video.path) && !verifyFileExists(context, video.thumbnailUri ?: "")) {
                staleIds.add(video.id)
            }
        }
        if (staleIds.isNotEmpty()) {
            videoDao.deleteVideosByIds(staleIds)
        }
        return@withContext staleIds.size
    }

    private fun extractFolderFromPath(path: String): String {
        return try {
            val file = File(path)
            file.parentFile?.name ?: "Videos"
        } catch (e: Exception) {
            "Videos"
        }
    }
}
