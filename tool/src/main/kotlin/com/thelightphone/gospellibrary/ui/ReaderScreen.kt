package com.thelightphone.gospellibrary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.thelightphone.gospellibrary.data.GospelRepository
import com.thelightphone.gospellibrary.db.GospelDatabase
import com.thelightphone.gospellibrary.model.ContentChapter
import com.thelightphone.gospellibrary.model.ReaderFontSize
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.buildDatabase
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private fun createBookmarkFilledVector(color: Color): ImageVector =
    ImageVector.Builder(
        name = "BookmarkFilled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = listOf(
            PathNode.MoveTo(17f, 3f),
            PathNode.LineTo(7f, 3f),
            PathNode.CurveTo(5.9f, 3f, 5f, 3.9f, 5f, 5f),
            PathNode.VerticalTo(21f),
            PathNode.LineTo(12f, 18f),
            PathNode.LineTo(19f, 21f),
            PathNode.VerticalTo(5f),
            PathNode.CurveTo(19f, 3.9f, 18.1f, 3f, 17f, 3f),
            PathNode.Close,
        ),
        fill = SolidColor(color),
    ).build()

private fun createBookmarkOutlineVector(color: Color): ImageVector =
    ImageVector.Builder(
        name = "BookmarkOutline",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = listOf(
            PathNode.MoveTo(17f, 3f),
            PathNode.LineTo(7f, 3f),
            PathNode.CurveTo(5.9f, 3f, 5f, 3.9f, 5f, 5f),
            PathNode.VerticalTo(21f),
            PathNode.LineTo(12f, 18f),
            PathNode.LineTo(19f, 21f),
            PathNode.VerticalTo(5f),
            PathNode.CurveTo(19f, 3.9f, 18.1f, 3f, 17f, 3f),
            PathNode.Close,
            PathNode.MoveTo(17f, 18f),
            PathNode.LineTo(12f, 15.82f),
            PathNode.LineTo(7f, 18f),
            PathNode.VerticalTo(5f),
            PathNode.HorizontalTo(17f),
            PathNode.VerticalTo(18f),
            PathNode.Close,
        ),
        fill = SolidColor(color),
    ).build()

class ReaderViewModel(
    private val initialContentId: String,
    private val repository: GospelRepository,
) : LightViewModel<Unit>() {

    private val _contentId = MutableStateFlow(initialContentId)
    val contentId: StateFlow<String> = _contentId.asStateFlow()

    private val _chapter = MutableStateFlow<ContentChapter?>(null)
    val chapter: StateFlow<ContentChapter?> = _chapter.asStateFlow()

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private val _highlights = MutableStateFlow<List<Int>>(emptyList())
    val highlights: StateFlow<List<Int>> = _highlights.asStateFlow()

    private val _fontSize = MutableStateFlow(ReaderFontSize.REGULAR)
    val fontSize: StateFlow<ReaderFontSize> = _fontSize.asStateFlow()

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        loadContent(_contentId.value)
    }

    fun loadContent(id: String) {
        _contentId.value = id
        val loaded = repository.getContent(id)
        _chapter.value = loaded
        if (loaded != null) {
            viewModelScope.launch {
                repository.recordLastRead(loaded)
            }
            viewModelScope.launch {
                repository.isChapterBookmarked(id).collect { bookmarked ->
                    _isBookmarked.value = bookmarked
                }
            }
            viewModelScope.launch {
                repository.getHighlightedVerses(id).collect { list ->
                    _highlights.value = list
                }
            }
        }
    }

    fun toggleBookmark() {
        val current = _chapter.value ?: return
        viewModelScope.launch {
            repository.toggleChapterBookmark(current, _isBookmarked.value)
        }
    }

    fun toggleVerseHighlight(verseNumber: Int) {
        val id = _contentId.value
        viewModelScope.launch {
            repository.toggleVerseHighlight(id, verseNumber, _highlights.value)
        }
    }

    fun cycleFontSize() {
        _fontSize.value = when (_fontSize.value) {
            ReaderFontSize.COMPACT -> ReaderFontSize.REGULAR
            ReaderFontSize.REGULAR -> ReaderFontSize.LARGE
            ReaderFontSize.LARGE -> ReaderFontSize.COMPACT
        }
    }
}

class ReaderScreen(
    sealedActivity: SealedLightActivity,
    private val contentId: String,
) : LightScreen<Unit, ReaderViewModel>(sealedActivity) {

    private val repository = GospelRepository.getInstance(
        dbProvider = {
            lightContext.buildDatabase(
                GospelDatabase::class.java,
                GospelRepository.DATABASE_NAME
            )
        },
        assetStreamProvider = { path ->
            lightContext.readAsset(path).inputStream()
        }
    )

    override val viewModelClass: Class<ReaderViewModel>
        get() = ReaderViewModel::class.java

    override fun createViewModel() = ReaderViewModel(contentId, repository)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val chapter by viewModel.chapter.collectAsState()
        val isBookmarked by viewModel.isBookmarked.collectAsState()
        val highlights by viewModel.highlights.collectAsState()
        val fontSize by viewModel.fontSize.collectAsState()

        val textVariant = when (fontSize) {
            ReaderFontSize.COMPACT -> LightTextVariant.Detail
            ReaderFontSize.REGULAR -> LightTextVariant.Paragraph
            ReaderFontSize.LARGE -> LightTextVariant.Copy
        }

        val contentColor = LightThemeTokens.colors.content
        val bookmarkPainter = rememberVectorPainter(
            remember(isBookmarked, contentColor) {
                if (isBookmarked) {
                    createBookmarkFilledVector(contentColor)
                } else {
                    createBookmarkOutlineVector(contentColor)
                }
            }
        )

        var horizontalDragAccumulator by remember(chapter?.id) { mutableFloatStateOf(0f) }

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .pointerInput(chapter?.id) {
                        detectHorizontalDragGestures(
                            onDragStart = { horizontalDragAccumulator = 0f },
                            onDragEnd = {
                                if (horizontalDragAccumulator < -100f) {
                                    chapter?.nextId?.let { viewModel.loadContent(it) }
                                } else if (horizontalDragAccumulator > 100f) {
                                    chapter?.prevId?.let { viewModel.loadContent(it) }
                                }
                                horizontalDragAccumulator = 0f
                            },
                            onDragCancel = { horizontalDragAccumulator = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                horizontalDragAccumulator += dragAmount
                            }
                        )
                    }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(
                            icon = LightIcons.BACK,
                            onClick = { goBack() }
                        ),
                        center = LightTopBarCenter.Text(
                            text = chapter?.title ?: "Reader"
                        ),
                        rightButton = LightBarButton.Icon(
                            painter = bookmarkPainter,
                            contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark",
                            onClick = { viewModel.toggleBookmark() }
                        ),
                        modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp())
                    )

                    // Secondary Sub-header: Subtitle and Font Size Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 1.5f.gridUnitsAsDp(), end = 1.5f.gridUnitsAsDp(), top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        chapter?.subtitle?.let { subtitle ->
                            LightText(
                                text = subtitle,
                                variant = LightTextVariant.Detail,
                                lighten = true,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        } ?: Spacer(modifier = Modifier.weight(1f))

                        LightText(
                            text = fontSize.displayLabel,
                            variant = LightTextVariant.Detail,
                            lighten = true,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .lightClickable { viewModel.cycleFontSize() }
                        )
                    }

                    // Content as a Single Continuous Page
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val currentChapter = chapter
                        if (currentChapter == null) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                LightText(
                                    text = "Content not found",
                                    variant = LightTextVariant.Subheading
                                )
                            }
                        } else {
                            LightScrollView(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 1.5f.gridUnitsAsDp())
                            ) {
                                Spacer(modifier = Modifier.height(12.dp))

                                currentChapter.verses.forEach { verse ->
                                    val isHighlighted = verse.number != null && verse.number in highlights
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .lightClickable {
                                                verse.number?.let { num ->
                                                    viewModel.toggleVerseHighlight(num)
                                                }
                                            },
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        verse.number?.let { num ->
                                            LightText(
                                                text = "$num ",
                                                variant = LightTextVariant.Detail,
                                                lighten = true,
                                                modifier = Modifier.width(32.dp)
                                            )
                                        }
                                        LightText(
                                            text = verse.text,
                                            variant = textVariant,
                                            underline = isHighlighted,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // Quiet Footer & Swipe Affordance
                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (currentChapter.prevId != null) {
                                        LightText(
                                            text = "← ${currentChapter.prevLabel ?: "Previous"}",
                                            variant = LightTextVariant.Detail,
                                            lighten = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .lightClickable {
                                                    viewModel.loadContent(currentChapter.prevId)
                                                }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }

                                    if (currentChapter.nextId != null) {
                                        LightText(
                                            text = "${currentChapter.nextLabel ?: "Next"} →",
                                            variant = LightTextVariant.Detail,
                                            lighten = true,
                                            modifier = Modifier.lightClickable {
                                                viewModel.loadContent(currentChapter.nextId)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
