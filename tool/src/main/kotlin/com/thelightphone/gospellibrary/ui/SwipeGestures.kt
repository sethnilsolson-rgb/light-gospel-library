package com.thelightphone.gospellibrary.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Reliable horizontal swipe-to-go-back that coexists with clickable rows, scroll views,
 * and tap/long-press text detectors by observing [PointerEventPass.Initial] and locking
 * only when a clear horizontal drag exceeds touch slop.
 */
fun Modifier.swipeToBack(
    sideOnly: Boolean = false,
    edgeThresholdPx: Float = 300f,
    swipeDistanceThresholdPx: Float = 110f,
    onBack: () -> Unit,
): Modifier = composed {
    val currentOnBack by rememberUpdatedState(onBack)

    this.pointerInput(sideOnly, edgeThresholdPx, swipeDistanceThresholdPx) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val startX = down.position.x
            var totalDx = 0f
            var totalDy = 0f
            var horizontalLocked = false
            var verticalLocked = false
            val slop = viewConfiguration.touchSlop

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } > 1) {
                    horizontalLocked = false
                    break
                }
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    if (horizontalLocked) change.consume()
                    break
                }

                val delta = change.position - change.previousPosition
                totalDx += delta.x
                totalDy += delta.y

                if (!horizontalLocked && !verticalLocked) {
                    val absX = abs(totalDx)
                    val absY = abs(totalDy)
                    if (absX > slop && absX > absY * 1.25f) {
                        horizontalLocked = true
                    } else if (absY > slop && absY >= absX) {
                        verticalLocked = true
                    }
                }

                if (horizontalLocked) {
                    change.consume()
                }
            }

            val startedAtSide = !sideOnly || startX <= edgeThresholdPx
            if (horizontalLocked && startedAtSide && totalDx >= swipeDistanceThresholdPx) {
                currentOnBack()
            }
        }
    }
}

/**
 * Reader-wide horizontal swipe handler that coexists seamlessly with double-tap Full Screen,
 * press-and-hold dictionary lookup, and vertical scrolling.
 */
fun Modifier.readerHorizontalSwipes(
    enabled: Boolean = true,
    edgeBackWidth: Dp = 44.dp,
    swipeThreshold: Dp = 42.dp,
    onEdgeSwipeBack: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
): Modifier = composed {
    val currentEnabled by rememberUpdatedState(enabled)
    val currentEdgeBack by rememberUpdatedState(onEdgeSwipeBack)
    val currentSwipeRight by rememberUpdatedState(onSwipeRight)
    val currentSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val density = LocalDensity.current
    val edgeBackPx = with(density) { edgeBackWidth.toPx() }
    val swipeThresholdPx = with(density) { swipeThreshold.toPx() }

    this.pointerInput(edgeBackPx, swipeThresholdPx) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (!currentEnabled) return@awaitEachGesture

            val startX = down.position.x
            var totalDx = 0f
            var totalDy = 0f
            var horizontalLocked = false
            var verticalLocked = false
            val slop = viewConfiguration.touchSlop

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } > 1) {
                    horizontalLocked = false
                    break
                }
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    if (horizontalLocked) change.consume()
                    break
                }

                val delta = change.position - change.previousPosition
                totalDx += delta.x
                totalDy += delta.y

                if (!horizontalLocked && !verticalLocked) {
                    val absX = abs(totalDx)
                    val absY = abs(totalDy)
                    if (absX > slop && absX > absY * 1.25f) {
                        horizontalLocked = true
                    } else if (absY > slop && absY >= absX) {
                        verticalLocked = true
                    }
                }

                if (horizontalLocked) {
                    change.consume()
                }
            }

            if (horizontalLocked && abs(totalDx) >= swipeThresholdPx) {
                if (totalDx > 0f) {
                    if (startX <= edgeBackPx) {
                        currentEdgeBack()
                    } else {
                        currentSwipeRight()
                    }
                } else {
                    currentSwipeLeft()
                }
            }
        }
    }
}
