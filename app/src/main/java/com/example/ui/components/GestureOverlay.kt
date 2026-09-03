package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize

@Composable
fun GestureOverlay(
    isLocked: Boolean,
    gesturesEnabled: Boolean,
    seekSeconds: Int,
    onSingleTap: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onDoubleTapCenter: () -> Unit,
    onBrightnessDelta: (Float) -> Unit, // Brightness
    onVolumeDelta: (Int) -> Unit, // Volume
    onHorizontalSwipeSeek: (Long) -> Unit, // Seek in Ms
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLocked) {
        // When locked, only single tap to show/hide unlock button
        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onSingleTap() })
                }
        )
        return
    }

    var size by remember { mutableStateOf(IntSize.Zero) }
    var currentScale by remember { mutableFloatStateOf(1.0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(gesturesEnabled) {
                if (gesturesEnabled) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (zoom != 1.0f) {
                            currentScale = (currentScale * zoom).coerceIn(0.5f, 4.0f)
                            onZoomChange(currentScale)
                        } else if (size.width > 0 && size.height > 0) {
                            val absX = kotlin.math.abs(pan.x)
                            val absY = kotlin.math.abs(pan.y)

                            if (absX > absY && absX > 5f) {
                                // Horizontal swipe seek
                                val deltaSec = (pan.x / size.width.toFloat()) * 90f // up to 90s swipe span
                                onHorizontalSwipeSeek((deltaSec * 1000).toLong())
                            }
                        }
                    }
                }
            }
            .pointerInput(gesturesEnabled, seekSeconds) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { offset ->
                        val width = size.width.toFloat()
                        if (width > 0) {
                            when {
                                offset.x < width * 0.35f -> onDoubleTapLeft()
                                offset.x > width * 0.65f -> onDoubleTapRight()
                                else -> onDoubleTapCenter()
                            }
                        } else {
                            onDoubleTapCenter()
                        }
                    }
                )
            }
            .pointerInput(gesturesEnabled) {
                if (gesturesEnabled) {
                    var accumulatedY = 0f
                    detectVerticalDragGestures(
                        onDragStart = { accumulatedY = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedY += dragAmount // positive = down, negative = up
                            val threshold = 25f // pixels per step notch
                            if (kotlin.math.abs(accumulatedY) >= threshold) {
                                val steps = (accumulatedY / threshold).toInt()
                                accumulatedY %= threshold
                                val isLeft = change.position.x < size.width * 0.5f
                                if (isLeft) {
                                    // Swipe up (negative accumulatedY) increases brightness
                                    val brightnessDelta = -steps * 0.05f
                                    onBrightnessDelta(brightnessDelta)
                                } else {
                                    // Swipe up (negative accumulatedY) increases volume (+1 per step)
                                    val volumeDelta = -steps
                                    onVolumeDelta(volumeDelta)
                                }
                            }
                        }
                    )
                }
            }
    ) {
        // Keep track of size
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    size = this.size
                }
        )
    }
}
