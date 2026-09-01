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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

data class MediaScanResult(
    val addedCount: Int = 0,
    val updatedCount: Int = 0,
    val deletedCount: Int = 0,
    val totalCount: Int = 0,
    val isUnchanged: Boolean = true
)

class MediaScanner(
    private val context: Context,
    private val videoDao: VideoDao
) {

    private val scanMutex = Mutex()

    companion object {
        private const val TAG = "MediaScanner"

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

    /**
     * Incrementally scans MediaStore in background (Dispatchers.IO).
     *
     * 1. Fetches lightweight sync info (id, path, dateModified, size) from Room.
     * 2. Scans MediaStore cursor without performing disk/file descriptor checks for existing items.
     * 3. Classifies items into:
     *    - NEW: Videos in MediaStore but not in Room -> insert.
     *    - MODIFIED: dateModified/size/duration changed -> update, keeping user state (favorites/progress).
     *    - UNCHANGED: untouched, 0 database writes.
     *    - DELETED: in Room but no longer in MediaStore -> delete.
     * 4. Applies all changes in a single Room transaction.
     */
    suspend fun scanMedia(forceFullScan: Boolean = false): MediaScanResult = withContext(Dispatchers.IO) {
        scanMutex.withLock {
            val startTime = System.currentTimeMillis()
            val existingList = videoDao.getAllVideoSyncInfo()
            val existingMap = existingList.associateBy { it.path }

            val toInsert = mutableListOf<VideoEntity>()
            val toUpdate = mutableListOf<VideoEntity>()
            val mediaStorePaths = HashSet<String>(existingList.size + 32)

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

                        mediaStorePaths.add(path)
                        mediaStorePaths.add(contentUri)

                        val duration = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L
                        val size = if (sizeColumn != -1) cursor.getLong(sizeColumn) else 0L
                        val dateAdded = if (dateAddedColumn != -1) cursor.getLong(dateAddedColumn) * 1000L else System.currentTimeMillis()
                        val dateModified = if (dateModifiedColumn != -1) cursor.getLong(dateModifiedColumn) * 1000L else System.currentTimeMillis()
                        val width = if (widthColumn != -1) cursor.getInt(widthColumn) else 0
                        val height = if (heightColumn != -1) cursor.getInt(heightColumn) else 0
                        val mimeType = if (mimeColumn != -1) cursor.getString(mimeColumn) ?: "video/*" else "video/*"

                        val title = (if (displayNameColumn != -1) cursor.getString(displayNameColumn) else null)
                            ?: (if (titleColumn != -1) cursor.getString(titleColumn) else null)
                            ?: try { File(path).nameWithoutExtension } catch (e: Exception) { "Video_$mediaId" }

                        val folder = if (bucketColumn != -1 && bucketColumn < cursor.columnCount) {
                            cursor.getString(bucketColumn) ?: extractFolderFromPath(path)
                        } else {
                            extractFolderFromPath(path)
                        }

                        val existing = existingMap[path] ?: existingMap[contentUri]

                        if (existing == null) {
                            // Brand new video found
                            val videoEntity = VideoEntity(
                                id = 0L,
                                path = path,
                                title = title,
                                duration = duration,
                                size = size,
                                thumbnailUri = contentUri,
                                folder = folder.ifEmpty { "Videos" },
                                dateAdded = dateAdded,
                                dateModified = dateModified,
                                lastPlayed = 0L,
                                resumePosition = 0L,
                                watchedPercentage = 0,
                                isFavorite = false,
                                width = width,
                                height = height,
                                mimeType = mimeType
                            )
                            toInsert.add(videoEntity)
                        } else if (forceFullScan ||
                            existing.dateModified != dateModified ||
                            existing.size != size ||
                            (existing.duration == 0L && duration > 0)
                        ) {
                            // Video metadata modified on device
                            val updatedEntity = VideoEntity(
                                id = existing.id,
                                path = path,
                                title = title,
                                duration = if (duration > 0) duration else existing.duration,
                                size = size,
                                thumbnailUri = contentUri,
                                folder = folder.ifEmpty { "Videos" },
                                dateAdded = dateAdded,
                                dateModified = dateModified,
                                lastPlayed = existing.lastPlayed,
                                resumePosition = existing.resumePosition,
                                watchedPercentage = existing.watchedPercentage,
                                isFavorite = existing.isFavorite,
                                width = width,
                                height = height,
                                mimeType = mimeType
                            )
                            toUpdate.add(updatedEntity)
                        }
                        // If unchanged: do nothing!
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying MediaStore", e)
            }

            // Identify removed or deleted videos (in DB but not in MediaStore)
            val toDeleteIds = mutableListOf<Long>()
            for (existing in existingList) {
                if (!mediaStorePaths.contains(existing.path)) {
                    toDeleteIds.add(existing.id)
                }
            }

            val hasChanges = toInsert.isNotEmpty() || toUpdate.isNotEmpty() || toDeleteIds.isNotEmpty()
            if (hasChanges) {
                videoDao.applyIncrementalSync(
                    toInsert = toInsert,
                    toUpdate = toUpdate,
                    toDeleteIds = toDeleteIds
                )
            }

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "Incremental sync finished in ${elapsed}ms: +${toInsert.size}, ~${toUpdate.size}, -${toDeleteIds.size}, total=${mediaStorePaths.size}")

            return@withLock MediaScanResult(
                addedCount = toInsert.size,
                updatedCount = toUpdate.size,
                deletedCount = toDeleteIds.size,
                totalCount = (existingList.size + toInsert.size - toDeleteIds.size).coerceAtLeast(0),
                isUnchanged = !hasChanges
            )
        }
    }

    suspend fun cleanStaleRecords(): Int = withContext(Dispatchers.IO) {
        val allVideos = videoDao.getAllVideoSyncInfo()
        val staleIds = mutableListOf<Long>()
        for (video in allVideos) {
            if (!verifyFileExists(context, video.path)) {
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
