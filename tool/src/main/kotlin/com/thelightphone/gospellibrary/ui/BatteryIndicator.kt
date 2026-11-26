package com.thelightphone.gospellibrary.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarButton
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.designVerticalPxToSp
import com.thelightphone.sdk.ui.gridUnitsAsDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Immutable
internal data class BatteryStatus(
    val percent: Int,
    val isCharging: Boolean,
)

/**
 * Ultra-low-power battery reader compliant with the Light SDK sandbox.
 *
 * Reads the standard Linux power-supply sysfs capacity node once on foreground resume
 * and at most once per 60 seconds while the screen is visible. If sysfs is restricted
 * on the current OS image, caches unavailability immediately and performs zero further I/O.
 */
internal object BatteryStatusReader {
    private val knownDirs = listOf(
        "/sys/class/power_supply/battery",
        "/sys/class/power_supply/bms",
        "/sys/class/power_supply/BAT0",
        "/sys/class/power_supply/BAT1",
    )

    @Volatile
    private var resolvedCapacityFile: File? = null

    @Volatile
    private var resolvedStatusFile: File? = null

    @Volatile
    private var probed = false

    @Volatile
    private var unavailable = false

    fun readNow(): BatteryStatus? {
        if (unavailable) return null
        if (!probed) {
            probe()
            if (unavailable) return null
        }
        val capFile = resolvedCapacityFile ?: return null
        val pct = runCatching { capFile.readText().trim().toIntOrNull() }.getOrNull()
        if (pct == null) {
            unavailable = true
            return null
        }
        val statusText = resolvedStatusFile?.let { f ->
            runCatching { f.readText().trim() }.getOrNull()
        }.orEmpty()
        val charging = statusText.equals("Charging", ignoreCase = true)
        return BatteryStatus(
            percent = pct.coerceIn(0, 100),
            isCharging = charging,
        )
    }

    private fun probe() {
        probed = true
        for (dirPath in knownDirs) {
            val cap = File(dirPath, "capacity")
            val parsed = runCatching {
                if (cap.canRead()) cap.readText().trim().toIntOrNull() else null
            }.getOrNull()
            if (parsed != null) {
                resolvedCapacityFile = cap
                resolvedStatusFile = File(dirPath, "status").takeIf { it.canRead() }
                return
            }
        }
        val psRoot = File("/sys/class/power_supply")
        val children = runCatching { psRoot.listFiles() }.getOrNull().orEmpty()
        for (child in children) {
            val typeStr = runCatching { File(child, "type").readText().trim() }.getOrNull()
            if (typeStr.equals("Battery", ignoreCase = true)) {
                val cap = File(child, "capacity")
                val parsed = runCatching {
                    if (cap.canRead()) cap.readText().trim().toIntOrNull() else null
                }.getOrNull()
                if (parsed != null) {
                    resolvedCapacityFile = cap
                    resolvedStatusFile = File(child, "status").takeIf { it.canRead() }
                    return
                }
            }
        }
        unavailable = true
    }
}

@Composable
internal fun rememberBatteryStatus(isForeground: Boolean = true): BatteryStatus? {
    var status by remember { mutableStateOf<BatteryStatus?>(null) }

    LaunchedEffect(isForeground) {
        if (!isForeground) return@LaunchedEffect
        while (true) {
            val next = withContext(Dispatchers.IO) { BatteryStatusReader.readNow() }
            status = next
            if (next == null) break
            delay(60_000L)
        }
    }

    return status
}

/**
 * Drop-in wrapper around [LightTopBar] that shows the device battery percentage
 * cleanly in the top-right corner when there is comfortable space, and automatically
 * hides it when space is needed (e.g., wide text right button or long center title).
 */
@Composable
internal fun BatteryTopBar(
    leftButton: LightTopBarButton? = null,
    center: LightTopBarCenter? = null,
    rightButton: LightTopBarButton? = null,
    modifier: Modifier = Modifier,
    isForeground: Boolean = true,
    showBattery: Boolean = true,
    centerTitleForMeasure: String? = null,
    centerExtraWidthDp: Dp = 0.dp,
) {
    val battery = rememberBatteryStatus(isForeground = isForeground && showBattery)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val colors = LightThemeTokens.colors
    val typography = LightThemeTokens.typography

    val barHeight = 3f.gridUnitsAsDp()
    val horizontalPad = 1f.gridUnitsAsDp()
    val centerMaxWidth = 18f.gridUnitsAsDp()
    val iconSlotReserved = 2.4f.gridUnitsAsDp()

    val titleStyle = typography.fine.copy(
        fontSize = typography.fine.fontSize.value.designVerticalPxToSp(),
    )
    val badgeStyle = typography.detail.copy(
        fontSize = (typography.detail.fontSize.value * 0.88f).designVerticalPxToSp(),
        fontWeight = if ((battery?.percent ?: 100) <= 15) FontWeight.Medium else FontWeight.Normal,
        color = colors.content.copy(alpha = if ((battery?.percent ?: 100) <= 15) 1f else 0.72f),
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        LightTopBar(
            leftButton = leftButton,
            center = center,
            rightButton = rightButton,
        )

        val canConsiderBattery = showBattery && battery != null && rightButton !is LightBarButton.Text
        if (canConsiderBattery && battery != null) {
            val badgeText = if (battery.isCharging) "${battery.percent}%+" else "${battery.percent}%"
            val titleText = centerTitleForMeasure ?: when (center) {
                is LightTopBarCenter.Text -> center.text
                is LightTopBarCenter.TwoLineDetail ->
                    if (center.line1.length >= center.line2.length) center.line1 else center.line2
                null -> ""
            }

            val barWidthPx = with(density) { maxWidth.toPx() }
            val centerMaxWidthPx = with(density) { centerMaxWidth.toPx() }
            val measuredTitlePx = if (titleText.isNotEmpty()) {
                textMeasurer.measure(titleText, style = titleStyle, maxLines = 1).size.width.toFloat()
            } else {
                0f
            }
            val effectiveCenterPx = (measuredTitlePx + with(density) { centerExtraWidthDp.toPx() })
                .coerceAtMost(centerMaxWidthPx)
            val badgeWidthPx = textMeasurer.measure(badgeText, style = badgeStyle, maxLines = 1).size.width.toFloat()

            val endOffsetDp = if (rightButton == null) horizontalPad else horizontalPad + iconSlotReserved
            val endOffsetPx = with(density) { endOffsetDp.toPx() }
            val minGapPx = with(density) { 8.dp.toPx() }
            val availableRightPx = (barWidthPx - effectiveCenterPx) / 2f - endOffsetPx

            if (availableRightPx >= badgeWidthPx + minGapPx) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(barHeight)
                        .padding(end = endOffsetDp)
                        .zIndex(1f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(
                        text = badgeText,
                        style = badgeStyle,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
