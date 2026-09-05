package com.example.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
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
            .onSizeChanged { size = it }
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
                if (!gesturesEnabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var isZooming = false
                    var isDragStarted = false
                    var isHorizontalSeek = false
                    var isVerticalDrag = false
                    var accumulatedY = 0f
                    var accumulatedX = 0f
                    val touchSlop = viewConfiguration.touchSlop
                    val isLeft = down.position.x < (size.width * 0.5f)

                    do {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.filter { it.pressed }

                        if (activePointers.size >= 2) {
                            // Two-finger pinch to zoom
                            isZooming = true
                            val zoom = event.calculateZoom()
                            if (zoom != 1.0f) {
                                currentScale = (currentScale * zoom).coerceIn(0.5f, 4.0f)
                                onZoomChange(currentScale)
                            }
                            event.changes.forEach { it.consume() }
                        } else if (activePointers.size == 1 && !isZooming) {
                            val change = activePointers[0]
                            val dragDelta = change.positionChange()

                            if (!isDragStarted) {
                                accumulatedX += dragDelta.x
                                accumulatedY += dragDelta.y
                                if (kotlin.math.hypot(accumulatedX, accumulatedY) > touchSlop) {
                                    isDragStarted = true
                                    if (kotlin.math.abs(accumulatedX) > kotlin.math.abs(accumulatedY)) {
                                        isHorizontalSeek = true
                                    } else {
                                        isVerticalDrag = true
                                        accumulatedY = 0f
                                    }
                                }
                            } else {
                                if (isHorizontalSeek && size.width > 0) {
                                    change.consume()
                                    val deltaSec = (dragDelta.x / size.width.toFloat()) * 90f
                                    onHorizontalSwipeSeek((deltaSec * 1000).toLong())
                                } else if (isVerticalDrag) {
                                    change.consume()
                                    accumulatedY += dragDelta.y
                                    val threshold = 25f
                                    if (kotlin.math.abs(accumulatedY) >= threshold) {
                                        val steps = (accumulatedY / threshold).toInt()
                                        accumulatedY %= threshold
                                        if (isLeft) {
                                            onBrightnessDelta(-steps * 0.05f)
                                        } else {
                                            onVolumeDelta(-steps)
                                        }
                                    }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    )
}
