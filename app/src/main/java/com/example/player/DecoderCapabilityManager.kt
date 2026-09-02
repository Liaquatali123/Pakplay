package com.example.player

import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceCapabilities(
    val androidVersion: Int = Build.VERSION.SDK_INT,
    val deviceModel: String = Build.MODEL,
    val hasHevcHardwareSupport: Boolean,
    val hasVp9HardwareSupport: Boolean,
    val hasAv1HardwareSupport: Boolean,
    val hasH264HardwareSupport: Boolean,
    val supportsHdr: Boolean,
    val maxSupportedResolution: String
)

@OptIn(UnstableApi::class)
object DecoderCapabilityManager {
    private const val TAG = "DecoderCapabilityMgr"
    private var cachedCapabilities: DeviceCapabilities? = null

    suspend fun getDeviceCapabilities(): DeviceCapabilities = withContext(Dispatchers.IO) {
        cachedCapabilities?.let { return@withContext it }

        val hasH264 = checkCodecSupport("video/avc")
        val hasHevc = checkCodecSupport("video/hevc")
        val hasVp9 = checkCodecSupport("video/x-vnd.on2.vp9")
        val hasAv1 = checkCodecSupport("video/av01")
        val supportsHdr = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && (hasHevc || hasVp9)

        val maxRes = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hasHevc -> "4K/8K Ultra HD"
            hasHevc || hasVp9 -> "4K Ultra HD"
            hasH264 -> "1080p Full HD"
            else -> "720p HD"
        }

        val caps = DeviceCapabilities(
            hasHevcHardwareSupport = hasHevc,
            hasVp9HardwareSupport = hasVp9,
            hasAv1HardwareSupport = hasAv1,
            hasH264HardwareSupport = hasH264,
            supportsHdr = supportsHdr,
            maxSupportedResolution = maxRes
        )

        cachedCapabilities = caps
        Log.d(TAG, "Device Capabilities: HEVC=$hasHevc, VP9=$hasVp9, AV1=$hasAv1, HDR=$supportsHdr, MaxRes=$maxRes")
        return@withContext caps
    }

    private fun checkCodecSupport(mimeType: String): Boolean {
        return try {
            val decoders = MediaCodecUtil.getDecoderInfos(mimeType, false, false)
            decoders.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
