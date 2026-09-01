package com.example.player

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.Charset

object SubtitleParser {

    suspend fun parseSubtitleFromUri(
        context: Context,
        uri: Uri,
        charsetName: String = "UTF-8"
    ): List<SubtitleCue> = withContext(Dispatchers.IO) {
        val cues = mutableListOf<SubtitleCue>()
        try {
            val charset = try {
                Charset.forName(charsetName)
            } catch (e: Exception) {
                Charsets.UTF_8
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, charset)).use { reader ->
                    var line: String?
                    var state = 0 // 0: looking for index/time, 1: reading time, 2: reading text
                    var startTime = 0L
                    var endTime = 0L
                    val textBuilder = StringBuilder()

                    while (reader.readLine().also { line = it } != null) {
                        val trimmed = line?.trim() ?: ""

                        if (trimmed.isEmpty()) {
                            if (textBuilder.isNotEmpty() && endTime > startTime) {
                                cues.add(
                                    SubtitleCue(
                                        startTimeMs = startTime,
                                        endTimeMs = endTime,
                                        text = cleanSubtitleHtml(textBuilder.toString().trim())
                                    )
                                )
                                textBuilder.setLength(0)
                            }
                            state = 0
                            continue
                        }

                        // Check if time code line (e.g. 00:01:20,000 --> 00:01:23,400)
                        if (trimmed.contains("-->")) {
                            val times = parseTimeCodeLine(trimmed)
                            if (times != null) {
                                startTime = times.first
                                endTime = times.second
                                state = 2
                            }
                        } else if (state == 2) {
                            if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                            textBuilder.append(trimmed)
                        }
                    }

                    // Flush any remaining cue
                    if (textBuilder.isNotEmpty() && endTime > startTime) {
                        cues.add(
                            SubtitleCue(
                                startTimeMs = startTime,
                                endTimeMs = endTime,
                                text = cleanSubtitleHtml(textBuilder.toString().trim())
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SubtitleParser", "Error parsing subtitle from URI $uri", e)
        }
        return@withContext cues
    }

    private fun parseTimeCodeLine(line: String): Pair<Long, Long>? {
        try {
            val parts = line.split("-->")
            if (parts.size != 2) return null

            val startStr = parts[0].trim().replace(',', '.')
            val endStr = parts[1].trim().split(" ")[0].replace(',', '.')

            val startMs = parseTimestampToMs(startStr)
            val endMs = parseTimestampToMs(endStr)

            return Pair(startMs, endMs)
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseTimestampToMs(timeStr: String): Long {
        val colonParts = timeStr.split(":")
        var hours = 0L
        var minutes = 0L
        var seconds = 0.0

        if (colonParts.size == 3) {
            hours = colonParts[0].toLongOrNull() ?: 0L
            minutes = colonParts[1].toLongOrNull() ?: 0L
            seconds = colonParts[2].toDoubleOrNull() ?: 0.0
        } else if (colonParts.size == 2) {
            minutes = colonParts[0].toLongOrNull() ?: 0L
            seconds = colonParts[1].toDoubleOrNull() ?: 0.0
        }

        return (hours * 3600000) + (minutes * 60000) + (seconds * 1000).toLong()
    }

    private fun cleanSubtitleHtml(text: String): String {
        return text.replace(Regex("<[^>]*>"), "")
            .replace("{\\an8}", "")
            .replace("{\\an7}", "")
            .replace("{\\b1}", "")
            .replace("{\\b0}", "")
            .replace("{\\i1}", "")
            .replace("{\\i0}", "")
    }

    fun findMatchingSubtitleFile(videoPath: String): File? {
        try {
            val videoFile = File(videoPath)
            if (!videoFile.exists()) return null
            val parent = videoFile.parentFile ?: return null
            val nameWithoutExt = videoFile.nameWithoutExtension

            val possibleExtensions = listOf("srt", "vtt", "ass", "ssa", "en.srt", "sub")
            for (ext in possibleExtensions) {
                val candidate = File(parent, "$nameWithoutExt.$ext")
                if (candidate.exists()) return candidate
            }
        } catch (e: Exception) {
            Log.e("SubtitleParser", "Error looking for matching subtitle", e)
        }
        return null
    }
}
