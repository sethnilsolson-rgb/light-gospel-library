package com.thelightphone.gospellibrary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.lp3Keyboard.ui.LayoutOptions
import com.thelightphone.lp3Keyboard.ui.SpecialKey
import com.thelightphone.lp3Keyboard.ui.viewmodel.EnQwertyLp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3RepeatableKeyboardCallback
import com.thelightphone.sdk.LightVibrator
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.LocalHapticsEnabled
import com.thelightphone.sdk.ui.designVerticalPxToSp
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.keyboard.LightEmbeddedLp3Keyboard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun noteScaledTextStyle(base: TextStyle, color: Color): TextStyle = base.copy(
    color = color,
    fontSize = base.fontSize.value.designVerticalPxToSp(),
    lineHeight = if (base.lineHeight == TextUnit.Unspecified) {
        TextUnit.Unspecified
    } else {
        base.lineHeight.value.designVerticalPxToSp()
    },
    letterSpacing = if (base.letterSpacing == TextUnit.Unspecified) {
        TextUnit.Unspecified
    } else {
        base.letterSpacing.value.designVerticalPxToSp()
    },
)

internal fun shouldAutoCapitalizeNote(text: CharSequence, cursorPos: Int): Boolean {
    val pos = cursorPos.coerceIn(0, text.length)
    if (pos == 0) return true
    val before = text.subSequence(0, pos)
    if (before.endsWith('\n')) return true
    val trimmedRight = before.trimEnd(' ')
    val trailingSpaces = before.length - trimmedRight.length
    if (trailingSpaces >= 1 && trimmedRight.isNotEmpty()) {
        val last = trimmedRight[trimmedRight.length - 1]
        if (last == '.' || last == '!' || last == '?') return true
    }
    return false
}

private class NoteKeyboardCallback(
    private val state: TextFieldState,
    private val onHaptic: () -> Unit,
    private val onSyncCaps: () -> Unit,
) : Lp3RepeatableKeyboardCallback {

    private var lastSpaceTapMs: Long = 0L

    override fun onKeyPressed(code: Int) {
        onHaptic()
    }

    override fun onSpecialKeyPressed(key: SpecialKey) {
        onHaptic()
        if (key == SpecialKey.Space) {
            val now = System.currentTimeMillis()
            val pos = state.selection.min
            val canDoubleSpace = (now - lastSpaceTapMs) <= 450L &&
                pos >= 2 &&
                state.text[pos - 1] == ' ' &&
                state.text[pos - 2].isLetterOrDigit()
            if (canDoubleSpace) {
                state.edit {
                    replace(pos - 1, pos, ". ")
                    selection = TextRange(pos + 1)
                }
                lastSpaceTapMs = 0L
            } else {
                insertAtCursor(" ")
                lastSpaceTapMs = now
            }
            onSyncCaps()
        }
    }

    override fun onKeyReleased(code: Int) {
        lastSpaceTapMs = 0L
        insertAtCursor(buildString { appendCodePoint(code) })
        onSyncCaps()
    }

    override fun onSpecialKeyReleased(key: SpecialKey) {
        when (key) {
            SpecialKey.Backspace -> {
                lastSpaceTapMs = 0L
                val before = state.text.subSequence(0, state.selection.min)
                val count = if (before.isEmpty()) 0 else if (Character.isLowSurrogate(before[before.length - 1])) 2 else 1
                deleteBeforeCursor(count)
                onSyncCaps()
            }
            SpecialKey.Return -> {
                lastSpaceTapMs = 0L
                insertAtCursor("\n")
                onSyncCaps()
            }
            else -> Unit
        }
    }

    override fun onKeyLongPressed(code: Int) = Unit

    override fun onSpecialKeyLongPressed(key: SpecialKey) {
        if (key == SpecialKey.Backspace) {
            lastSpaceTapMs = 0L
            val before = state.text.subSequence(0, state.selection.min)
            val trimmed = before.trimEnd()
            val lastSpace = trimmed.indexOfLast { it.isWhitespace() }
            val count = before.length - if (lastSpace >= 0) lastSpace + 1 else 0
            deleteBeforeCursor(count)
            onSyncCaps()
        }
    }

    override fun onKeyRepeated(code: Int) {
        insertAtCursor(buildString { appendCodePoint(code) })
    }

    override fun onSpecialKeyRepeated(specialKey: SpecialKey) {
        if (specialKey == SpecialKey.Space) insertAtCursor(" ")
    }

    override fun onSubmitWord(word: CharSequence) {
        lastSpaceTapMs = 0L
        insertAtCursor(word.toString())
        onSyncCaps()
    }

    private fun insertAtCursor(text: String) {
        state.edit {
            val start = selection.min
            val end = selection.max
            replace(start, end, text)
            selection = TextRange(start + text.length)
        }
    }

    private fun deleteBeforeCursor(count: Int) {
        if (count <= 0) return
        state.edit {
            val end = selection.min
            if (end == 0) return@edit
            val start = (end - count).coerceAtLeast(0)
            delete(start, end)
            selection = TextRange(start)
        }
    }
}

@Composable
private fun rememberNoteKeyboardViewModel(
    state: TextFieldState,
    keyboardOptionsFlow: StateFlow<KeyboardOptions>,
    vibrator: LightVibrator,
    editorKey: Any,
): Lp3KeyboardViewModel<*> {
    val hapticsEnabled = LocalHapticsEnabled.current
    val currentOnHaptic by rememberUpdatedState {
        if (hapticsEnabled) runCatching { vibrator.click() }
    }
    return remember(editorKey, state) {
        var vmRef: EnQwertyLp3KeyboardViewModel<Unit>? = null
        val callback = NoteKeyboardCallback(
            state = state,
            onHaptic = { currentOnHaptic() },
            onSyncCaps = {
                val wantCaps = shouldAutoCapitalizeNote(state.text, state.selection.min)
                vmRef?.setCapsMode(wantCaps)
            },
        )
        EnQwertyLp3KeyboardViewModel<Unit>(
            callback,
            keyboardOptionsFlow = keyboardOptionsFlow,
            optionsForLayout = { LayoutOptions(displayCloseButton = !it.isRootLayout) },
        ).also { vm ->
            vmRef = vm
            vm.setCapsMode(shouldAutoCapitalizeNote(state.text, state.selection.min))
        }
    }
}

/**
 * Full-screen edge-to-edge LightOS Notes composer for passage notes:
 * - Small crisp paragraph font using the entire canvas above the embedded LP3 keyboard
 * - Displays the underlined quoted words cleanly at the top
 * - Tap-anywhere / horizontal-drag cursor placement, auto-scroll, auto-caps, double-space period
 * - Clean LightOS word buttons ("Done" / "Save" / optional "Delete") — zero triangles
 */
@Composable
fun ReaderNoteComposer(
    title: String,
    quotedExcerpt: String,
    state: TextFieldState,
    keyboardOptionsFlow: StateFlow<KeyboardOptions>,
    vibrator: LightVibrator,
    onSave: (String) -> Unit,
    onBack: () -> Unit,
    onDelete: (() -> Unit)? = null,
    editorKey: Any = "reader-note",
) {
    val colors = LightThemeTokens.colors
    val content = colors.content
    val secondary = colors.contentSecondary
    val bg = colors.background

    val keyboardViewModel = rememberNoteKeyboardViewModel(
        state = state,
        keyboardOptionsFlow = keyboardOptionsFlow,
        vibrator = vibrator,
        editorKey = editorKey,
    )

    val textStr = state.text.toString()
    val cursorIndex = state.selection.min.coerceIn(0, textStr.length)

    val scrollState = rememberScrollState()
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    var cursorVisible by remember { mutableStateOf(true) }

    LaunchedEffect(textStr, cursorIndex) {
        cursorVisible = true
        while (true) {
            delay(550L)
            cursorVisible = !cursorVisible
        }
    }

    LaunchedEffect(textStr.length, cursorIndex, textLayout, viewportHeightPx) {
        val layout = textLayout ?: return@LaunchedEffect
        if (viewportHeightPx <= 0) return@LaunchedEffect
        val safePos = cursorIndex.coerceIn(0, layout.layoutInput.text.length)
        val rect = layout.getCursorRect(safePos)
        val curScroll = scrollState.value
        val pad = 32
        if (rect.bottom.roundToInt() + pad > curScroll + viewportHeightPx) {
            scrollState.scrollTo((rect.bottom.roundToInt() + pad - viewportHeightPx).coerceAtLeast(0))
        } else if (rect.top.roundToInt() - pad < curScroll) {
            scrollState.scrollTo((rect.top.roundToInt() - pad).coerceAtLeast(0))
        }
    }

    val bodyStyle = noteScaledTextStyle(LightThemeTokens.typography.paragraph, content)
    val quoteStyle = noteScaledTextStyle(LightThemeTokens.typography.detail, secondary).copy(
        textDecoration = TextDecoration.Underline,
    )
    val edgePad = 10.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
    ) {
        LightTopBar(
            leftButton = LightBarButton.LightIcon(
                icon = LightIcons.BACK,
                onClick = onBack,
            ),
            center = LightTopBarCenter.Text(title),
            rightButton = LightBarButton.Text(
                text = "Done",
                onClick = { onSave(state.text.toString()) },
            ),
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(content.copy(alpha = 0.18f)),
        )

        if (quotedExcerpt.isNotBlank()) {
            BasicText(
                text = "\u201C$quotedExcerpt\u201D",
                style = quoteStyle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = edgePad, vertical = 6.dp),
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(content.copy(alpha = 0.14f)),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { viewportHeightPx = it.height },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startPos = down.position
                            var moved = false
                            var draggedHorizontally = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    if (!moved) {
                                        val layout = textLayout
                                        if (layout != null) {
                                            val localX = (startPos.x - 10.dp.toPx()).coerceAtLeast(0f)
                                            val localY = (startPos.y - 8.dp.toPx()).coerceAtLeast(0f)
                                            val pos = if (localY > layout.size.height) {
                                                state.text.length
                                            } else {
                                                layout.getOffsetForPosition(Offset(localX, localY))
                                            }
                                            state.edit { selection = TextRange(pos.coerceIn(0, length)) }
                                        }
                                    }
                                    break
                                }
                                val dx = abs(change.position.x - startPos.x)
                                val dy = abs(change.position.y - startPos.y)
                                if (dx > viewConfiguration.touchSlop || dy > viewConfiguration.touchSlop) {
                                    moved = true
                                    if (!draggedHorizontally && dx > dy * 1.6f) {
                                        draggedHorizontally = true
                                    }
                                }
                                if (draggedHorizontally) {
                                    val layout = textLayout
                                    if (layout != null) {
                                        val localX = (change.position.x - 10.dp.toPx()).coerceAtLeast(0f)
                                        val localY = (change.position.y - 8.dp.toPx()).coerceAtLeast(0f)
                                        val pos = layout.getOffsetForPosition(Offset(localX, localY))
                                        state.edit { selection = TextRange(pos.coerceIn(0, length)) }
                                    }
                                }
                            }
                        }
                    }
                    .padding(horizontal = edgePad, vertical = 8.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (textStr.isEmpty()) {
                        BasicText(
                            text = "Write your note\u2026",
                            style = bodyStyle.copy(color = secondary.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    BasicText(
                        text = textStr,
                        style = bodyStyle,
                        onTextLayout = { textLayout = it },
                        softWrap = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (cursorVisible) {
                        textLayout?.let { layout ->
                            val safePos = cursorIndex.coerceIn(0, layout.layoutInput.text.length)
                            val rect = layout.getCursorRect(safePos)
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                                    .width(2.dp)
                                    .height(with(LocalDensity.current) { rect.height.toDp().coerceAtLeast(16.dp) })
                                    .background(content),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (scrollState.maxValue > 0 && viewportHeightPx > 0) {
                val totalContentPx = viewportHeightPx + scrollState.maxValue
                val thumbFraction = (viewportHeightPx.toFloat() / totalContentPx.toFloat()).coerceIn(0.14f, 0.85f)
                val thumbHeightPx = (viewportHeightPx * thumbFraction).roundToInt()
                val travelPx = (viewportHeightPx - thumbHeightPx).coerceAtLeast(0)
                val progress = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                val thumbTopPx = (travelPx * progress).roundToInt()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 3.dp)
                        .fillMaxHeight()
                        .width(2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(0, thumbTopPx) }
                            .width(2.dp)
                            .height(with(LocalDensity.current) { thumbHeightPx.toDp() })
                            .background(content.copy(alpha = 0.5f), RoundedCornerShape(1.dp)),
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(content.copy(alpha = 0.35f)),
        )

        LightEmbeddedLp3Keyboard(
            viewModel = keyboardViewModel,
            additionalBottomHeight = 5f.gridUnitsAsDp(),
            bottomBar = {
                val buttons = buildList {
                    if (onDelete != null) {
                        add(
                            LightBarButton.Text(
                                text = "Delete",
                                onClick = onDelete,
                            )
                        )
                    }
                    add(
                        LightBarButton.Text(
                            text = "Save",
                            onClick = { onSave(state.text.toString()) },
                        )
                    )
                }
                LightBottomBar(items = buttons)
            },
        )
    }
}
