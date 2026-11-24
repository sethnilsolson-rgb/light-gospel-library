package com.thelightphone.gospellibrary.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.thelightphone.gospellibrary.model.ContentVerse
import kotlin.math.ceil

data class GospelPageSlice(
    val sourceIndex: Int,
    val verseNumber: Int?,
    val showVerseNumber: Boolean,
    val text: String,
)

data class GospelReaderPage(
    val pageIndex: Int,
    val slices: List<GospelPageSlice>,
    val permilleStart: Int,
) {
    val firstVerseNumber: Int?
        get() = slices.firstNotNullOfOrNull { it.verseNumber }

    val previewSnippet: String
        get() = slices.firstOrNull()?.text.orEmpty()
}

/**
 * Exact-fit Compose [TextMeasurer] paginator for scripture chapters and conference talks.
 * Splits long verses cleanly on exact visual line boundaries so zero lines are ever clipped,
 * and computes character-weighted [GospelReaderPage.permilleStart] values (0..1000) so
 * bookmarks, Scroll <-> Pages toggles, font size changes, and Full-Screen toggles preserve
 * the user's exact reading position.
 */
object GospelReaderPaginator {

    fun paginateVerses(
        verses: List<ContentVerse>,
        textMeasurer: TextMeasurer,
        bodyStyle: TextStyle,
        viewportWidthPx: Int,
        viewportHeightPx: Int,
        density: Density,
    ): List<GospelReaderPage> {
        if (verses.isEmpty() || viewportWidthPx <= 0 || viewportHeightPx <= 0) return emptyList()

        val verseNumberWidthPx = with(density) { 32.dp.roundToPx() }
        val sliceTopPadPx = with(density) { 6.dp.roundToPx() }
        val sliceBottomPadPx = with(density) { 8.dp.roundToPx() }
        val contTopPadPx = with(density) { 2.dp.roundToPx() }
        val safetyMarginPx = with(density) { 10.dp.roundToPx() }

        val usableHeightPx = (viewportHeightPx - safetyMarginPx).coerceAtLeast(80)
        val totalChars = verses.sumOf { it.text.length }.coerceAtLeast(1)

        val pages = ArrayList<GospelReaderPage>()
        var currentSlices = ArrayList<GospelPageSlice>()
        var currentY = 0
        var pageStartChars = 0
        var consumedChars = 0

        fun flushPage() {
            if (currentSlices.isEmpty()) return
            val permille = if (pages.isEmpty()) {
                0
            } else {
                ((pageStartChars.toLong() * 1000L) / totalChars.toLong()).toInt().coerceIn(1, 999)
            }
            pages.add(
                GospelReaderPage(
                    pageIndex = pages.size,
                    slices = currentSlices,
                    permilleStart = permille,
                )
            )
            currentSlices = ArrayList()
            currentY = 0
            pageStartChars = consumedChars
        }

        for ((idx, verse) in verses.withIndex()) {
            val rawText = verse.text
            if (rawText.isBlank()) continue

            val hasNumber = verse.number != null
            val textWidthPx = (viewportWidthPx - if (hasNumber) verseNumberWidthPx else 0).coerceAtLeast(80)
            val layout = textMeasurer.measure(
                text = AnnotatedString(rawText),
                style = bodyStyle,
                constraints = Constraints(maxWidth = textWidthPx),
            )
            val lineCount = layout.lineCount
            if (lineCount <= 0) {
                consumedChars += rawText.length
                continue
            }

            var startLine = 0
            while (startLine < lineCount) {
                val isFirstSlice = startLine == 0
                val topPad = if (isFirstSlice) sliceTopPadPx else contTopPadPx
                val available = usableHeightPx - currentY - topPad

                var endLine = startLine - 1
                val topY = layout.getLineTop(startLine)
                for (line in startLine until lineCount) {
                    val h = ceil(layout.getLineBottom(line) - topY).toInt()
                    val needBottom = if (line == lineCount - 1) sliceBottomPadPx else 0
                    if (h + needBottom <= available) {
                        endLine = line
                    } else {
                        break
                    }
                }

                // If nothing fits on the current non-empty page, start a new page and retry
                if (endLine < startLine) {
                    if (currentSlices.isNotEmpty()) {
                        flushPage()
                        continue
                    } else {
                        // Single line taller than viewport: take at least 1 line to guarantee progress
                        endLine = startLine
                    }
                }

                // Avoid an isolated 1-line stub at the bottom of a page when a multi-line verse starts
                val linesTaken = endLine - startLine + 1
                if (isFirstSlice && linesTaken == 1 && lineCount >= 3 && currentSlices.isNotEmpty()) {
                    flushPage()
                    continue
                }

                val charStart = layout.getLineStart(startLine).coerceIn(0, rawText.length)
                val charEnd = layout.getLineEnd(endLine, visibleEnd = true).coerceIn(charStart, rawText.length)
                val sliceText = rawText.substring(charStart, charEnd).trimEnd()
                val sliceHeight = ceil(layout.getLineBottom(endLine) - topY).toInt()
                val isLastSlice = endLine == lineCount - 1

                if (sliceText.isNotEmpty()) {
                    currentSlices.add(
                        GospelPageSlice(
                            sourceIndex = idx,
                            verseNumber = verse.number,
                            showVerseNumber = isFirstSlice && hasNumber,
                            text = sliceText,
                        )
                    )
                    currentY += topPad + sliceHeight + if (isLastSlice) sliceBottomPadPx else 0
                }
                consumedChars += (charEnd - charStart).coerceAtLeast(0)
                startLine = endLine + 1

                if (!isLastSlice) {
                    flushPage()
                }
            }
        }

        flushPage()
        return pages
    }

    fun pageIndexForPermille(pages: List<GospelReaderPage>, permille: Int): Int {
        if (pages.isEmpty()) return 0
        if (permille <= 0) return 0
        if (permille >= 1000) return pages.lastIndex
        var match = 0
        for (i in pages.indices) {
            if (pages[i].permilleStart <= permille) {
                match = i
            } else {
                break
            }
        }
        return match
    }
}
