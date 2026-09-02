package com.example.player

import androidx.media3.common.PlaybackException

object PlaybackRecoveryManager {

    data class ErrorClassification(
        val userFriendlyMessage: String,
        val canRetry: Boolean,
        val isCodecError: Boolean
    )

    fun classifyError(error: PlaybackException): ErrorClassification {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> ErrorClassification(
                userFriendlyMessage = "Video file not found, moved, or deleted.",
                canRetry = false,
                isCodecError = false
            )
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> ErrorClassification(
                userFriendlyMessage = "Network connection failed. Please check your connection.",
                canRetry = true,
                isCodecError = false
            )
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> ErrorClassification(
                userFriendlyMessage = "Your device may not support this video codec or resolution.",
                canRetry = false,
                isCodecError = true
            )
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> ErrorClassification(
                userFriendlyMessage = "This video container format appears to be damaged or unsupported.",
                canRetry = false,
                isCodecError = false
            )
            else -> ErrorClassification(
                userFriendlyMessage = "Playback error: ${error.localizedMessage ?: "Unknown error occurred"}",
                canRetry = true,
                isCodecError = false
            )
        }
    }
}
