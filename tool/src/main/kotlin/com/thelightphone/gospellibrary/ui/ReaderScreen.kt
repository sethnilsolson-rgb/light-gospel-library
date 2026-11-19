package com.thelightphone.gospellibrary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.thelightphone.gospellibrary.data.GospelRepository
import com.thelightphone.gospellibrary.db.GospelDatabase
import com.thelightphone.gospellibrary.model.ContentChapter
import com.thelightphone.gospellibrary.model.GospelLanguage
import com.thelightphone.gospellibrary.model.ReaderFontSize
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

    private val _fontSize = MutableStateFlow(ReaderFontSize.REGULAR)
    val fontSize: StateFlow<ReaderFontSize> = _fontSize.asStateFlow()

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

    private var bookmarkJob: Job? = null
    private var loadJob: Job? = null
    private var languagesJob: Job? = null
    private var lookupJob: Job? = null

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

        viewModelScope.launch {
            val activeCode = repository.getActiveLanguageCodeSync()
            val activeLang = GospelLanguage.fromCode(activeCode)
            _currentLanguage.value = activeLang
            _lookupEnabled.value = repository.isDictionaryReadyForLanguage(activeLang)
            loadContent(_contentId.value, activeLang)
        }
    }

    fun loadContent(id: String, language: GospelLanguage = _currentLanguage.value) {
        _contentId.value = id
        _languageNotice.value = null
        dismissLookup()

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val isNonEnglish = language != GospelLanguage.ENG
            if (isNonEnglish) {
                _isLoadingLanguage.value = true
            }
            val loaded = repository.getContentInLanguage(id, language.code)
            _isLoadingLanguage.value = false

            if (loaded != null) {
                _chapter.value = loaded
                _lookupEnabled.value = repository.isDictionaryReadyForLanguage(language)
                repository.recordLastRead(loaded)
            } else if (isNonEnglish) {
                // Gracefully fall back to built-in English while offline if chapter isn't cached yet
                val englishFallback = repository.getContentInLanguage(id, GospelLanguage.ENG.code)
                _chapter.value = englishFallback
                _lookupEnabled.value = repository.isDictionaryReadyForLanguage(GospelLanguage.ENG)
                _languageNotice.value =
                    "${language.nativeName} chapter isn't cached offline yet. Connect to internet once or pre-download in Settings."
            } else {
                _chapter.value = null
            }

            bookmarkJob?.cancel()
            bookmarkJob = viewModelScope.launch {
                repository.isChapterBookmarked(id).collect { bookmarked ->
                    _isBookmarked.value = bookmarked
                }
            }
        }
    }

    fun cycleLanguage() {
        val list = _availableLanguages.value
        if (list.size <= 1) return
        val currentIdx = list.indexOf(_currentLanguage.value).coerceAtLeast(0)
        val nextLang = list[(currentIdx + 1) % list.size]
        _currentLanguage.value = nextLang
        viewModelScope.launch {
            repository.setActiveLanguage(nextLang.code)
            _lookupEnabled.value = repository.isDictionaryReadyForLanguage(nextLang)
        }
        loadContent(_contentId.value, nextLang)
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

    fun toggleBookmark() {
        val current = _chapter.value ?: return
        viewModelScope.launch {
            repository.toggleChapterBookmark(current, _isBookmarked.value)
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
            ) {
                addMigrations(GospelDatabase.MIGRATION_2_3)
                fallbackToDestructiveMigration()
            }
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
        val fontSize by viewModel.fontSize.collectAsState()
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
        var horizontalDragAccumulator by remember(chapter?.id) { mutableFloatStateOf(0f) }
        var dragStartX by remember(chapter?.id) { mutableFloatStateOf(0f) }
        val showLanguageToggle = availableLanguages.size > 1
        val isOverlayOpen = isLookingUp || lookupResult != null

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .pointerInput(chapter?.id, isOverlayOpen) {
                        if (isOverlayOpen) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                dragStartX = offset.x
                                horizontalDragAccumulator = 0f
                            },
                            onDragEnd = {
                                if (horizontalDragAccumulator > 70f) {
                                    // Fast swipe from the side (or if no previous chapter) navigates back
                                    if (dragStartX <= 300f || chapter?.prevId == null) {
                                        goBack()
                                    } else {
                                        chapter?.prevId?.let { viewModel.loadContent(it) }
                                    }
                                } else if (horizontalDragAccumulator < -80f) {
                                    chapter?.nextId?.let { viewModel.loadContent(it) }
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
                                onClick = { viewModel.toggleBookmark() }
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

                    // Secondary Sub-header: Subtitle and Font Size Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 1.5f.gridUnitsAsDp(), end = 1.5f.gridUnitsAsDp(), top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val subText = when {
                            isLoadingLanguage -> "Loading ${currentLanguage.nativeName}..."
                            !chapter?.subtitle.isNullOrBlank() -> chapter?.subtitle
                            lookupEnabled -> "Hold any word to define"
                            else -> null
                        }
                        if (subText != null) {
                            LightText(
                                text = subText,
                                variant = LightTextVariant.Detail,
                                lighten = true,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        LightText(
                            text = fontSize.displayLabel,
                            variant = LightTextVariant.Detail,
                            lighten = true,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .lightClickable { viewModel.cycleFontSize() }
                        )
                    }

                    languageNotice?.let { notice ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 1.5f.gridUnitsAsDp(),
                                    end = 1.5f.gridUnitsAsDp(),
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
                                    text = if (isLoadingLanguage) "Loading ${currentLanguage.nativeName}..." else "Content not found",
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
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(end = 1.5f.gridUnitsAsDp(), top = 6.dp, bottom = 8.dp),
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

                                // Quiet Footer & Swipe Affordance
                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 1.5f.gridUnitsAsDp(), top = 8.dp, bottom = 12.dp),
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
    onLongPressChar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = readerScaledTextStyle(variant)
    val color = LightThemeTokens.colors.content
    var layoutResult by remember(text, variant) { mutableStateOf<TextLayoutResult?>(null) }

    val gestureModifier = if (lookupEnabled) {
        modifier.pointerInput(text, lookupEnabled) {
            detectTapGestures(
                onLongPress = { position ->
                    layoutResult?.let { layout ->
                        val offset = layout.getOffsetForPosition(position)
                        onLongPressChar(offset)
                    }
                }
            )
        }
    } else {
        modifier
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
