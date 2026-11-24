package com.thelightphone.gospellibrary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.thelightphone.gospellibrary.data.GospelRepository
import com.thelightphone.gospellibrary.db.BookmarkEntity
import com.thelightphone.gospellibrary.db.GospelDatabase
import com.thelightphone.gospellibrary.model.ContentChapter
import com.thelightphone.gospellibrary.model.GospelLanguage
import com.thelightphone.gospellibrary.model.ReaderFontSize
import com.thelightphone.gospellibrary.model.ReaderPageMode
import com.thelightphone.gospellibrary.model.WordLookupResult
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
import com.thelightphone.sdk.ui.designVerticalPxToSp
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

private const val RECENT_SCROLL_TTL_MS = 180_000L // 3 minutes brief flip-back memory
private const val BOOKMARK_PROXIMITY_PERMILLE = 55 // ~5.5% of page height

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
    private val initialScrollPermille: Int?,
    private val repository: GospelRepository,
) : LightViewModel<Unit>() {

    private data class RecentScrollEntry(
        val permille: Int,
        val savedAtMs: Long,
    )

    private val _contentId = MutableStateFlow(initialContentId)
    val contentId: StateFlow<String> = _contentId.asStateFlow()

    private val _chapter = MutableStateFlow<ContentChapter?>(null)
    val chapter: StateFlow<ContentChapter?> = _chapter.asStateFlow()

    private val _chapterBookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    val chapterBookmarks: StateFlow<List<BookmarkEntity>> = _chapterBookmarks.asStateFlow()

    private val _pendingScrollRestore = MutableStateFlow<Pair<Int, Int>?>(null)
    val pendingScrollRestore: StateFlow<Pair<Int, Int>?> = _pendingScrollRestore.asStateFlow()

    val fontSize: StateFlow<ReaderFontSize> = repository.fontSize
    val pageMode: StateFlow<ReaderPageMode> = repository.pageMode

    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()

    private val _availableLanguages = MutableStateFlow<List<GospelLanguage>>(listOf(GospelLanguage.ENG))
    val availableLanguages: StateFlow<List<GospelLanguage>> = _availableLanguages.asStateFlow()

    private val _currentLanguage = MutableStateFlow(GospelLanguage.ENG)
    val currentLanguage: StateFlow<GospelLanguage> = _currentLanguage.asStateFlow()

    private val _isLoadingLanguage = MutableStateFlow(false)
    val isLoadingLanguage: StateFlow<Boolean> = _isLoadingLanguage.asStateFlow()

    private val _languageNotice = MutableStateFlow<String?>(null)
    val languageNotice: StateFlow<String?> = _languageNotice.asStateFlow()

    private val _lookupEnabled = MutableStateFlow(false)
    val lookupEnabled: StateFlow<Boolean> = _lookupEnabled.asStateFlow()

    private val _isLookingUp = MutableStateFlow(false)
    val isLookingUp: StateFlow<Boolean> = _isLookingUp.asStateFlow()

    private val _lookupResult = MutableStateFlow<WordLookupResult?>(null)
    val lookupResult: StateFlow<WordLookupResult?> = _lookupResult.asStateFlow()

    private val recentScrollPositions = mutableMapOf<String, RecentScrollEntry>()
    private var currentScrollPermille: Int = 0
    private var restoreToken: Int = 0
    private var hasInitialized = false

    private var bookmarkJob: Job? = null
    private var loadJob: Job? = null
    private var languagesJob: Job? = null
    private var lookupJob: Job? = null
    private var scrollSaveJob: Job? = null

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)

        languagesJob?.cancel()
        languagesJob = viewModelScope.launch {
            repository.downloadedLanguages.collect { entities ->
                val others = entities
                    .filter { it.langCode != GospelLanguage.ENG.code }
                    .map { GospelLanguage.fromCode(it.langCode) }
                    .distinct()
                    .sortedBy { GospelLanguage.entries.indexOf(it) }
                _availableLanguages.value = listOf(GospelLanguage.ENG) + others

                // Refresh whether press-and-hold dictionary is active for current language
                _lookupEnabled.value = repository.isDictionaryReadyForLanguage(_currentLanguage.value)
            }
        }

        if (!hasInitialized) {
            hasInitialized = true
            viewModelScope.launch {
                val activeCode = repository.getActiveLanguageCodeSync()
                val activeLang = GospelLanguage.fromCode(activeCode)
                _currentLanguage.value = activeLang
                _lookupEnabled.value = repository.isDictionaryReadyForLanguage(activeLang)
                loadContent(
                    id = _contentId.value,
                    language = activeLang,
                    explicitScrollPermille = initialScrollPermille ?: 0,
                    saveCurrentScroll = false,
                )
            }
        }
    }

    fun loadContent(
        id: String,
        language: GospelLanguage = _currentLanguage.value,
        explicitScrollPermille: Int? = null,
        saveCurrentScroll: Boolean = true,
    ) {
        val previousId = _contentId.value
        val now = System.currentTimeMillis()

        // Briefly remember where we were on the chapter we're leaving (for flip-back comparisons)
        if (saveCurrentScroll && previousId != id && _chapter.value?.id == previousId) {
            recentScrollPositions[previousId] = RecentScrollEntry(
                permille = currentScrollPermille.coerceIn(0, 1000),
                savedAtMs = now,
            )
            recentScrollPositions.entries.removeAll { (_, entry) ->
                now - entry.savedAtMs > RECENT_SCROLL_TTL_MS
            }
        }

        val targetPermille = when {
            explicitScrollPermille != null -> explicitScrollPermille.coerceIn(0, 1000)
            previousId != id -> {
                val recent = recentScrollPositions[id]
                if (recent != null && now - recent.savedAtMs <= RECENT_SCROLL_TTL_MS) {
                    recent.permille
                } else {
                    0 // New chapter always starts cleanly at the top
                }
            }
            else -> currentScrollPermille.coerceIn(0, 1000)
        }

        _contentId.value = id
        _languageNotice.value = null
        dismissLookup()
        scrollSaveJob?.cancel()

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val isNonEnglish = language != GospelLanguage.ENG
            if (isNonEnglish) {
                _isLoadingLanguage.value = true
            }
            val loaded = repository.getContentInLanguage(id, language.code)
            _isLoadingLanguage.value = false

            val resolvedChapter = if (loaded != null) {
                _lookupEnabled.value = repository.isDictionaryReadyForLanguage(language)
                loaded
            } else if (isNonEnglish) {
                // Gracefully fall back to built-in English while offline if chapter isn't cached yet
                _lookupEnabled.value = repository.isDictionaryReadyForLanguage(GospelLanguage.ENG)
                _languageNotice.value =
                    "${language.nativeName} chapter isn't cached offline yet. Connect to internet once or pre-download in Settings."
                repository.getContentInLanguage(id, GospelLanguage.ENG.code)
            } else {
                null
            }

            currentScrollPermille = targetPermille
            _pendingScrollRestore.value = (++restoreToken) to targetPermille
            _chapter.value = resolvedChapter

            if (resolvedChapter != null) {
                repository.recordLastRead(resolvedChapter, targetPermille)
            }

            bookmarkJob?.cancel()
            bookmarkJob = viewModelScope.launch {
                repository.getBookmarksForContent(id).collect { list ->
                    _chapterBookmarks.value = list
                }
            }
        }
    }

    fun consumePendingScrollRestore() {
        _pendingScrollRestore.value = null
    }

    fun onScrollPositionChanged(renderedChapterId: String, scrollPermille: Int) {
        if (_pendingScrollRestore.value != null) return
        if (_chapter.value?.id != renderedChapterId) return

        val clamped = scrollPermille.coerceIn(0, 1000)
        currentScrollPermille = clamped

        scrollSaveJob?.cancel()
        scrollSaveJob = viewModelScope.launch {
            delay(400L)
            repository.updateLastReadScroll(renderedChapterId, clamped)
        }
    }

    fun cycleLanguage() {
        val list = _availableLanguages.value
        if (list.size <= 1) return
        val currentIdx = list.indexOf(_currentLanguage.value).coerceAtLeast(0)
        val nextLang = list[(currentIdx + 1) % list.size]
        _currentLanguage.value = nextLang
        val keepPermille = currentScrollPermille
        viewModelScope.launch {
            repository.setActiveLanguage(nextLang.code)
            _lookupEnabled.value = repository.isDictionaryReadyForLanguage(nextLang)
        }
        loadContent(
            id = _contentId.value,
            language = nextLang,
            explicitScrollPermille = keepPermille,
            saveCurrentScroll = false,
        )
    }

    fun lookupWordAt(text: String, charIndex: Int, verseNumber: Int?) {
        if (!_lookupEnabled.value) return
        val activeLangCode = _chapter.value?.langCode ?: _currentLanguage.value.code
        val contentId = _contentId.value

        lookupJob?.cancel()
        lookupJob = viewModelScope.launch {
            _isLookingUp.value = true
            _lookupResult.value = null
            val result = repository.lookupWordInLanguage(
                text = text,
                charIndex = charIndex,
                langCode = activeLangCode,
                contentId = contentId,
                verseNumber = verseNumber,
            )
            _isLookingUp.value = false
            _lookupResult.value = result
        }
    }

    fun dismissLookup() {
        lookupJob?.cancel()
        _isLookingUp.value = false
        _lookupResult.value = null
    }

    fun toggleBookmark(
        scrollPermille: Int,
        visibleVerseNumber: Int?,
        visibleSnippet: String?,
    ) {
        val current = _chapter.value ?: return
        val clamped = scrollPermille.coerceIn(0, 1000)
        val matching = _chapterBookmarks.value
            .filter { abs(it.scrollPermille - clamped) <= BOOKMARK_PROXIMITY_PERMILLE }
            .minByOrNull { abs(it.scrollPermille - clamped) }

        viewModelScope.launch {
            repository.toggleScrollBookmark(
                chapter = current,
                scrollPermille = clamped,
                visibleVerseNumber = visibleVerseNumber,
                visibleSnippet = visibleSnippet,
                matchingBookmark = matching,
            )
        }
    }

    fun cycleFontSize(preservePermille: Int) {
        currentScrollPermille = preservePermille.coerceIn(0, 1000)
        _pendingScrollRestore.value = (++restoreToken) to currentScrollPermille
        val next = when (fontSize.value) {
            ReaderFontSize.COMPACT -> ReaderFontSize.REGULAR
            ReaderFontSize.REGULAR -> ReaderFontSize.LARGE
            ReaderFontSize.LARGE -> ReaderFontSize.COMPACT
        }
        repository.setFontSize(next)
    }

    fun togglePageMode(preservePermille: Int) {
        currentScrollPermille = preservePermille.coerceIn(0, 1000)
        _pendingScrollRestore.value = (++restoreToken) to currentScrollPermille
        repository.setPageMode(pageMode.value.toggle())
    }

    fun toggleFullScreen() {
        _isFullScreen.value = !_isFullScreen.value
    }
}

class ReaderScreen(
    sealedActivity: SealedLightActivity,
    private val contentId: String,
    private val initialScrollPermille: Int? = null,
) : LightScreen<Unit, ReaderViewModel>(sealedActivity) {

    private val repository = GospelRepository.getInstance(
        dbProvider = {
            lightContext.buildDatabase(
                GospelDatabase::class.java,
                GospelRepository.DATABASE_NAME
            ) {
                addMigrations(
                    GospelDatabase.MIGRATION_2_3,
                    GospelDatabase.MIGRATION_3_4,
                    GospelDatabase.MIGRATION_4_5,
                )
                fallbackToDestructiveMigration()
            }
        },
        assetStreamProvider = { path ->
            lightContext.readAsset(path).inputStream()
        },
        filesDir = lightContext.filesDir,
    )

    override val viewModelClass: Class<ReaderViewModel>
        get() = ReaderViewModel::class.java

    override fun createViewModel() = ReaderViewModel(contentId, initialScrollPermille, repository)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val chapter by viewModel.chapter.collectAsState()
        val chapterBookmarks by viewModel.chapterBookmarks.collectAsState()
        val pendingScrollRestore by viewModel.pendingScrollRestore.collectAsState()
        val fontSize by viewModel.fontSize.collectAsState()
        val pageMode by viewModel.pageMode.collectAsState()
        val isFullScreen by viewModel.isFullScreen.collectAsState()
        val availableLanguages by viewModel.availableLanguages.collectAsState()
        val currentLanguage by viewModel.currentLanguage.collectAsState()
        val isLoadingLanguage by viewModel.isLoadingLanguage.collectAsState()
        val languageNotice by viewModel.languageNotice.collectAsState()
        val lookupEnabled by viewModel.lookupEnabled.collectAsState()
        val isLookingUp by viewModel.isLookingUp.collectAsState()
        val lookupResult by viewModel.lookupResult.collectAsState()

        val textVariant = when (fontSize) {
            ReaderFontSize.COMPACT -> LightTextVariant.Detail
            ReaderFontSize.REGULAR -> LightTextVariant.Paragraph
            ReaderFontSize.LARGE -> LightTextVariant.Copy
        }

        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val bodyStyle = readerScaledTextStyle(textVariant)
        val scrollState = rememberScrollState()
        val verseOffsets = remember(chapter?.id, chapter?.langCode, fontSize) { mutableMapOf<Int, Int>() }

        // Paged mode viewport measurement & pagination
        var pagedViewportSize by remember { mutableStateOf(IntSize.Zero) }
        var currentPageIdx by remember(chapter?.id, chapter?.langCode) { mutableIntStateOf(0) }
        var pagedAnchorPermille by remember(chapter?.id, chapter?.langCode) { mutableIntStateOf(0) }

        val sidePadDp = 1.5f.gridUnitsAsDp()
        val sidePadPx = with(density) { sidePadDp.roundToPx() }
        val pagedVerticalPadPx = with(density) { (if (isFullScreen) 32.dp else 16.dp).roundToPx() }

        val pages = remember(
            pageMode,
            chapter?.id,
            chapter?.langCode,
            chapter?.verses,
            bodyStyle,
            pagedViewportSize,
            isFullScreen,
        ) {
            val ch = chapter
            if (pageMode != ReaderPageMode.PAGED || ch == null || pagedViewportSize.width <= 0 || pagedViewportSize.height <= 0) {
                emptyList()
            } else {
                GospelReaderPaginator.paginateVerses(
                    verses = ch.verses,
                    textMeasurer = textMeasurer,
                    bodyStyle = bodyStyle,
                    viewportWidthPx = (pagedViewportSize.width - sidePadPx * 2).coerceAtLeast(100),
                    viewportHeightPx = (pagedViewportSize.height - pagedVerticalPadPx).coerceAtLeast(100),
                    density = density,
                )
            }
        }

        // Keep paged reading position anchored when pages recalculate (e.g. Full Screen / font size toggle)
        LaunchedEffect(pages) {
            if (pageMode == ReaderPageMode.PAGED && pages.isNotEmpty()) {
                val pending = pendingScrollRestore
                val target = pending?.second ?: pagedAnchorPermille
                val mapped = GospelReaderPaginator.pageIndexForPermille(pages, target)
                currentPageIdx = mapped.coerceIn(0, pages.lastIndex)
                pagedAnchorPermille = pages[currentPageIdx].permilleStart
                if (pending != null) {
                    viewModel.consumePendingScrollRestore()
                }
            }
        }

        fun readCurrentPermille(): Int {
            return if (pageMode == ReaderPageMode.PAGED) {
                pages.getOrNull(currentPageIdx)?.permilleStart ?: pagedAnchorPermille
            } else {
                val maxVal = scrollState.maxValue
                if (maxVal > 0) {
                    ((scrollState.value.toLong() * 1000L) / maxVal.toLong()).toInt().coerceIn(0, 1000)
                } else {
                    0
                }
            }
        }

        val isBookmarked by remember(scrollState, chapterBookmarks, pageMode, currentPageIdx, pages) {
            derivedStateOf {
                val permille = readCurrentPermille()
                chapterBookmarks.any {
                    abs(it.scrollPermille - permille) <= BOOKMARK_PROXIMITY_PERMILLE
                }
            }
        }

        // Restore target scroll / page position (0 for new chapters, saved permille for bookmarks/flip-backs/mode/font changes)
        LaunchedEffect(pendingScrollRestore, chapter?.id, chapter?.langCode, fontSize, pageMode, pages.size) {
            val pending = pendingScrollRestore ?: return@LaunchedEffect
            if (chapter == null) return@LaunchedEffect
            val (_, targetPermille) = pending
            if (pageMode == ReaderPageMode.PAGED) {
                pagedAnchorPermille = targetPermille
                if (pages.isNotEmpty()) {
                    val mapped = GospelReaderPaginator.pageIndexForPermille(pages, targetPermille)
                    currentPageIdx = mapped.coerceIn(0, pages.lastIndex)
                    pagedAnchorPermille = pages[currentPageIdx].permilleStart
                    viewModel.consumePendingScrollRestore()
                }
            } else {
                if (targetPermille <= 0) {
                    scrollState.scrollTo(0)
                    withFrameNanos { }
                    if (scrollState.value != 0) {
                        scrollState.scrollTo(0)
                    }
                } else {
                    withFrameNanos { }
                    val maxVal = withTimeoutOrNull(800L) {
                        snapshotFlow { scrollState.maxValue }.first { it > 0 }
                    } ?: scrollState.maxValue
                    val targetPx = if (maxVal > 0) {
                        ((targetPermille.toLong() * maxVal.toLong()) / 1000L).toInt().coerceIn(0, maxVal)
                    } else {
                        0
                    }
                    scrollState.scrollTo(targetPx)
                }
                viewModel.consumePendingScrollRestore()
            }
        }

        // Track scroll position for Continue Reading and brief flip-back memory (Scroll mode)
        LaunchedEffect(scrollState, chapter?.id, pageMode) {
            if (pageMode != ReaderPageMode.SCROLL) return@LaunchedEffect
            val renderedChapterId = chapter?.id ?: return@LaunchedEffect
            snapshotFlow { scrollState.value to scrollState.maxValue }
                .collect { (value, maxVal) ->
                    val permille = if (maxVal > 0) {
                        ((value.toLong() * 1000L) / maxVal.toLong()).toInt().coerceIn(0, 1000)
                    } else {
                        0
                    }
                    viewModel.onScrollPositionChanged(renderedChapterId, permille)
                }
        }

        // Track page position for Continue Reading and flip-back memory (Paged mode)
        LaunchedEffect(currentPageIdx, pages, chapter?.id, pageMode) {
            if (pageMode != ReaderPageMode.PAGED) return@LaunchedEffect
            val renderedChapterId = chapter?.id ?: return@LaunchedEffect
            val page = pages.getOrNull(currentPageIdx) ?: return@LaunchedEffect
            pagedAnchorPermille = page.permilleStart
            viewModel.onScrollPositionChanged(renderedChapterId, page.permilleStart)
        }

        fun goNextPageOrChapter() {
            val ch = chapter ?: return
            if (pageMode == ReaderPageMode.PAGED && pages.isNotEmpty() && currentPageIdx < pages.lastIndex) {
                currentPageIdx += 1
            } else {
                ch.nextId?.let { viewModel.loadContent(it, explicitScrollPermille = 0) }
            }
        }

        fun goPrevPageOrChapter() {
            val ch = chapter ?: return
            if (pageMode == ReaderPageMode.PAGED && pages.isNotEmpty() && currentPageIdx > 0) {
                currentPageIdx -= 1
            } else if (ch.prevId != null) {
                val target = if (pageMode == ReaderPageMode.PAGED) 1000 else null
                viewModel.loadContent(ch.prevId, explicitScrollPermille = target)
            } else {
                goBack()
            }
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

        val haptics = LocalHapticFeedback.current
        val showLanguageToggle = availableLanguages.size > 1
        val isOverlayOpen = isLookingUp || lookupResult != null

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .readerHorizontalSwipes(
                        enabled = !isOverlayOpen,
                        onEdgeSwipeBack = { goBack() },
                        onSwipeRight = { goPrevPageOrChapter() },
                        onSwipeLeft = { goNextPageOrChapter() },
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!isFullScreen) {
                        // Top Bar with optional seamless Language Toggle right up by the title ("1 Nephi 2")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 0.5f.gridUnitsAsDp())
                        ) {
                            LightTopBar(
                                leftButton = LightBarButton.LightIcon(
                                    icon = LightIcons.BACK,
                                    onClick = { goBack() }
                                ),
                                center = if (showLanguageToggle) {
                                    null
                                } else {
                                    LightTopBarCenter.Text(text = chapter?.title ?: "Reader")
                                },
                                rightButton = LightBarButton.Icon(
                                    painter = bookmarkPainter,
                                    contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark",
                                    onClick = {
                                        val currentChapter = chapter ?: return@Icon
                                        if (pageMode == ReaderPageMode.PAGED) {
                                            val curPage = pages.getOrNull(currentPageIdx)
                                            viewModel.toggleBookmark(
                                                scrollPermille = readCurrentPermille(),
                                                visibleVerseNumber = curPage?.firstVerseNumber,
                                                visibleSnippet = curPage?.previewSnippet,
                                            )
                                        } else {
                                            val visibleIdx = verseOffsets.entries
                                                .filter { it.value <= scrollState.value + 60 }
                                                .maxByOrNull { it.value }
                                                ?.key
                                                ?: verseOffsets.entries
                                                    .minByOrNull { abs(it.value - scrollState.value) }
                                                    ?.key
                                                ?: 0
                                            val visibleVerse = currentChapter.verses.getOrNull(visibleIdx)
                                            viewModel.toggleBookmark(
                                                scrollPermille = readCurrentPermille(),
                                                visibleVerseNumber = visibleVerse?.number,
                                                visibleSnippet = visibleVerse?.text,
                                            )
                                        }
                                    }
                                ),
                            )

                            if (showLanguageToggle) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(horizontal = 4.2f.gridUnitsAsDp()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    LightText(
                                        text = chapter?.title ?: "Reader",
                                        variant = LightTextVariant.Fine,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .border(
                                                width = 1.dp,
                                                color = LightThemeTokens.colors.content.copy(alpha = 0.85f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .lightClickable { viewModel.cycleLanguage() }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LightText(
                                            text = currentLanguage.shortBadge,
                                            variant = LightTextVariant.Detail
                                        )
                                    }
                                }
                            }
                        }

                        // Secondary Sub-header: Subtitle, Page/Scroll Toggle, and Font Size Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = sidePadDp, end = sidePadDp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val subText = when {
                                isLoadingLanguage -> "Loading ${currentLanguage.nativeName}..."
                                pageMode == ReaderPageMode.PAGED && pages.isNotEmpty() -> {
                                    val baseSub = chapter?.subtitle?.takeIf { it.isNotBlank() }
                                    val pageTag = "P. ${currentPageIdx + 1}/${pages.size}"
                                    if (baseSub != null) "$pageTag • $baseSub" else pageTag
                                }
                                !chapter?.subtitle.isNullOrBlank() -> chapter?.subtitle
                                lookupEnabled -> "Hold word to define • 2× tap full"
                                else -> "Double-tap text for full screen"
                            }
                            LightText(
                                text = subText.orEmpty(),
                                variant = LightTextVariant.Detail,
                                lighten = true,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .border(
                                        width = 1.dp,
                                        color = LightThemeTokens.colors.content.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .lightClickable { viewModel.togglePageMode(readCurrentPermille()) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                LightText(
                                    text = pageMode.chipLabel,
                                    variant = LightTextVariant.Detail
                                )
                            }

                            LightText(
                                text = fontSize.displayLabel,
                                variant = LightTextVariant.Detail,
                                lighten = true,
                                modifier = Modifier
                                    .padding(start = 10.dp)
                                    .lightClickable { viewModel.cycleFontSize(readCurrentPermille()) }
                            )
                        }

                        languageNotice?.let { notice ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = sidePadDp,
                                        end = sidePadDp,
                                        top = 4.dp,
                                        bottom = 6.dp
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = LightThemeTokens.colors.content.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                LightText(
                                    text = notice,
                                    variant = LightTextVariant.Detail,
                                    lighten = true
                                )
                            }
                        }
                    }

                    // Reading Content Surface (Continuous Scroll OR Book-Style Pages)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .onSizeChanged { pagedViewportSize = it }
                            .pointerInput(isOverlayOpen) {
                                if (isOverlayOpen) return@pointerInput
                                detectTapGestures(
                                    onDoubleTap = { viewModel.toggleFullScreen() }
                                )
                            }
                    ) {
                        val currentChapter = chapter
                        if (currentChapter == null) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                LightText(
                                    text = if (isLoadingLanguage) "Loading ${currentLanguage.nativeName}..." else "Content not found",
                                    variant = LightTextVariant.Subheading
                                )
                            }
                        } else if (pageMode == ReaderPageMode.PAGED) {
                            val activePage = pages.getOrNull(currentPageIdx)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        horizontal = sidePadDp,
                                        vertical = if (isFullScreen) 16.dp else 8.dp,
                                    )
                            ) {
                                activePage?.slices?.forEach { slice ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                top = if (slice.showVerseNumber) 6.dp else 2.dp,
                                                bottom = 8.dp,
                                            ),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        if (slice.verseNumber != null) {
                                            LightText(
                                                text = if (slice.showVerseNumber) "${slice.verseNumber} " else "",
                                                variant = LightTextVariant.Detail,
                                                lighten = true,
                                                modifier = Modifier.width(32.dp),
                                            )
                                        }
                                        LookupableVerseText(
                                            text = slice.text,
                                            variant = textVariant,
                                            lookupEnabled = lookupEnabled,
                                            onDoubleTap = { viewModel.toggleFullScreen() },
                                            onLongPressChar = { charIndex ->
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.lookupWordAt(
                                                    text = slice.text,
                                                    charIndex = charIndex,
                                                    verseNumber = slice.verseNumber,
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        } else {
                            LightScrollView(
                                scrollState = scrollState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = sidePadDp)
                            ) {
                                Spacer(modifier = Modifier.height(if (isFullScreen) 18.dp else 12.dp))

                                currentChapter.verses.forEachIndexed { idx, verse ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { coords ->
                                                verseOffsets[idx] = coords.positionInParent().y.toInt()
                                            }
                                            .padding(end = sidePadDp, top = 6.dp, bottom = 8.dp),
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
                                        LookupableVerseText(
                                            text = verse.text,
                                            variant = textVariant,
                                            lookupEnabled = lookupEnabled,
                                            onDoubleTap = { viewModel.toggleFullScreen() },
                                            onLongPressChar = { charIndex ->
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.lookupWordAt(
                                                    text = verse.text,
                                                    charIndex = charIndex,
                                                    verseNumber = verse.number,
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (!isFullScreen) {
                                    // Quiet Footer & Swipe Affordance
                                    Spacer(modifier = Modifier.height(20.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(end = sidePadDp, top = 8.dp, bottom = 12.dp),
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
                                } else {
                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                            }
                        }
                    }

                    // Quiet Bottom Page Bar in Paged Mode (hidden automatically in Full Screen)
                    if (!isFullScreen && pageMode == ReaderPageMode.PAGED && chapter != null) {
                        val canGoBack = currentPageIdx > 0 || chapter?.prevId != null
                        val canGoForward = (pages.isNotEmpty() && currentPageIdx < pages.lastIndex) || chapter?.nextId != null
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = sidePadDp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (canGoBack) {
                                LightText(
                                    text = if (currentPageIdx > 0) "← Prev" else "← ${chapter?.prevLabel ?: "Prev"}",
                                    variant = LightTextVariant.Detail,
                                    lighten = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .lightClickable { goPrevPageOrChapter() },
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            if (pages.isNotEmpty()) {
                                LightText(
                                    text = "${currentPageIdx + 1} / ${pages.size}",
                                    variant = LightTextVariant.Detail,
                                    lighten = true,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                            }

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                if (canGoForward) {
                                    LightText(
                                        text = if (pages.isNotEmpty() && currentPageIdx < pages.lastIndex) {
                                            "Next →"
                                        } else {
                                            "${chapter?.nextLabel ?: "Next"} →"
                                        },
                                        variant = LightTextVariant.Detail,
                                        lighten = true,
                                        modifier = Modifier.lightClickable { goNextPageOrChapter() },
                                    )
                                }
                            }
                        }
                    }
                }

                // Press-and-Hold Word / Character Dictionary Overlay Card
                if (isOverlayOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LightThemeTokens.colors.background.copy(alpha = 0.6f))
                            .pointerInput(Unit) {
                                detectTapGestures { viewModel.dismissLookup() }
                            }
                    ) {
                        DictionaryLookupCard(
                            isLookingUp = isLookingUp,
                            result = lookupResult,
                            onDismiss = { viewModel.dismissLookup() },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp)
                                .pointerInput(Unit) {
                                    // Consume taps inside the card so they don't dismiss the scrim
                                    detectTapGestures {}
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LookupableVerseText(
    text: String,
    variant: LightTextVariant,
    lookupEnabled: Boolean,
    onDoubleTap: () -> Unit,
    onLongPressChar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = readerScaledTextStyle(variant)
    val color = LightThemeTokens.colors.content
    var layoutResult by remember(text, variant) { mutableStateOf<TextLayoutResult?>(null) }

    val gestureModifier = modifier.pointerInput(text, lookupEnabled) {
        detectTapGestures(
            onDoubleTap = { onDoubleTap() },
            onLongPress = if (lookupEnabled) {
                { position ->
                    layoutResult?.let { layout ->
                        val offset = layout.getOffsetForPosition(position)
                        onLongPressChar(offset)
                    }
                }
            } else null,
        )
    }

    Text(
        text = text,
        color = color,
        style = style,
        onTextLayout = { layoutResult = it },
        modifier = gestureModifier,
    )
}

@Composable
private fun readerScaledTextStyle(variant: LightTextVariant): TextStyle {
    val t = LightThemeTokens.typography
    val base = when (variant) {
        LightTextVariant.Detail -> t.detail
        LightTextVariant.Copy -> t.copy
        else -> t.paragraph
    }
    return base.copy(
        fontSize = base.fontSize.scaledTextUnit(),
        lineHeight = base.lineHeight.scaledTextUnit(),
        letterSpacing = base.letterSpacing.scaledTextUnit(),
    )
}

@Composable
private fun TextUnit.scaledTextUnit(): TextUnit {
    if (this == TextUnit.Unspecified) return this
    return value.designVerticalPxToSp()
}

@Composable
private fun DictionaryLookupCard(
    isLookingUp: Boolean,
    result: WordLookupResult?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightThemeTokens.colors
    var showEnglishVerse by remember(result) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .background(colors.background, RoundedCornerShape(6.dp))
            .border(width = 1.5.dp, color = colors.content, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .heightIn(max = 310.dp)
    ) {
        if (isLookingUp || result == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LightText(
                    text = "Looking up...",
                    variant = LightTextVariant.Subheading,
                    modifier = Modifier.weight(1f)
                )
                LightText(
                    text = "✕",
                    variant = LightTextVariant.Subheading,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .lightClickable { onDismiss() }
                )
            }
            return@Column
        }

        // Header: Word + Reading/Pinyin + Close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LightText(
                        text = result.headword,
                        variant = LightTextVariant.Subheading
                    )
                    if (result.reading.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        LightText(
                            text = result.reading,
                            variant = LightTextVariant.Paragraph,
                            lighten = true
                        )
                    }
                }
                LightText(
                    text = result.sourceLabel,
                    variant = LightTextVariant.Detail,
                    lighten = true,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            LightText(
                text = "✕",
                variant = LightTextVariant.Subheading,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .lightClickable { onDismiss() }
            )
        }

        HorizontalDivider(
            color = colors.content.copy(alpha = 0.25f),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            LightText(
                text = result.definition,
                variant = LightTextVariant.Paragraph
            )

            result.characterBreakdown?.let { ch ->
                HorizontalDivider(
                    color = colors.content.copy(alpha = 0.2f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                val readingPart = if (ch.reading.isNotBlank()) " (${ch.reading})" else ""
                LightText(
                    text = "Character: ${ch.headword}$readingPart",
                    variant = LightTextVariant.Detail,
                    lighten = true
                )
                LightText(
                    text = ch.definition,
                    variant = LightTextVariant.Detail,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            result.englishVersePreview?.let { engVerse ->
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = colors.content.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .lightClickable { showEnglishVerse = !showEnglishVerse }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    LightText(
                        text = if (showEnglishVerse) "Hide English Verse" else "Compare English Verse",
                        variant = LightTextVariant.Detail
                    )
                }
                if (showEnglishVerse) {
                    LightText(
                        text = engVerse,
                        variant = LightTextVariant.Detail,
                        lighten = true,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}
