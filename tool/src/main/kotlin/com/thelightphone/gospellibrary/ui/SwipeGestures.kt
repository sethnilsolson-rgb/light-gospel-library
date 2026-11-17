package com.thelightphone.gospellibrary.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Modifier that triggers [onBack] when the user performs a horizontal swipe from the side (left to right).
 *
 * @param sideOnly if true, requires the touch to start within [edgeThresholdPx] from the left edge.
 * @param edgeThresholdPx maximum x-coordinate (in pixels) from the left edge considered "from the side".
 * @param swipeDistanceThresholdPx minimum rightward drag distance to trigger the back action.
 */
fun Modifier.swipeToBack(
    sideOnly: Boolean = false,
    edgeThresholdPx: Float = 300f,
    swipeDistanceThresholdPx: Float = 70f,
    onBack: () -> Unit,
): Modifier = pointerInput(Unit) {
    var startX = 0f
    var horizontalDragAccumulator = 0f

    detectHorizontalDragGestures(
        onDragStart = { offset ->
            startX = offset.x
            horizontalDragAccumulator = 0f
        },
        onDragEnd = {
            val startedAtSide = !sideOnly || startX <= edgeThresholdPx
            if (startedAtSide && horizontalDragAccumulator >= swipeDistanceThresholdPx) {
                onBack()
            }
            horizontalDragAccumulator = 0f
        },
        onDragCancel = {
            horizontalDragAccumulator = 0f
        },
        onHorizontalDrag = { _, dragAmount ->
            horizontalDragAccumulator += dragAmount
        }
    )
}
