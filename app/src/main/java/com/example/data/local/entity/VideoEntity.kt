package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "videos",
    indices = [
        Index(value = ["path"], unique = true),
        Index(value = ["folder"]),
        Index(value = ["lastPlayed"]),
        Index(value = ["isFavorite"])
    ]
)
data class VideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,
    val title: String,
    val duration: Long = 0L, // in milliseconds
    val size: Long = 0L, // in bytes
    val thumbnailUri: String? = null,
    val folder: String = "",
    val dateAdded: Long = 0L,
    val dateModified: Long = 0L,
    val lastPlayed: Long = 0L,
    val resumePosition: Long = 0L, // in milliseconds
    val watchedPercentage: Int = 0,
    val isFavorite: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = "video/*"
) {
    val resolutionLabel: String
        get() = when {
            width >= 3840 || height >= 2160 -> "4K UHD"
            width >= 2560 || height >= 1440 -> "2K QHD"
            width >= 1920 || height >= 1080 -> "1080p FHD"
            width >= 1280 || height >= 720 -> "720p HD"
            width > 0 && height > 0 -> "${width}x${height}"
            else -> "HD"
        }

    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.1f KB", kb)
            }
        }
}
