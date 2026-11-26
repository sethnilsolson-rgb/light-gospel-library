package com.thelightphone.gospellibrary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.thelightphone.gospellibrary.data.GospelRepository
import com.thelightphone.gospellibrary.data.GospelSearchEngine
import com.thelightphone.gospellibrary.data.GospelSearchResult
import com.thelightphone.gospellibrary.data.SearchMatchKind
import com.thelightphone.gospellibrary.data.SearchScope
import com.thelightphone.gospellibrary.db.GospelDatabase
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.buildDatabase
import com.thelightphone.sdk.rememberKeyboardOptions
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextField
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

enum class ResultKindFilter(val label: String) {
    ALL("All"),
    DIRECT("Direct"),
    RELATED("Related"),
}

class SearchViewModel(
    private val repository: GospelRepository,
) : LightViewModel<Unit>() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _isEditing = MutableStateFlow(true)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _scope = MutableStateFlow(SearchScope.ALL)
    val scope: StateFlow<SearchScope> = _scope.asStateFlow()

    private val _kindFilter = MutableStateFlow(ResultKindFilter.ALL)
    val kindFilter: StateFlow<ResultKindFilter> = _kindFilter.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _matchedTopics = MutableStateFlow<List<String>>(emptyList())
    val matchedTopics: StateFlow<List<String>> = _matchedTopics.asStateFlow()

    private val _results = MutableStateFlow<List<GospelSearchResult>>(emptyList())
    val results: StateFlow<List<GospelSearchResult>> = _results.asStateFlow()

    private var searchJob: Job? = null
    private var activeCancel = AtomicBoolean(false)

    fun openEditor() {
        _isEditing.value = true
    }

    fun closeEditor() {
        _isEditing.value = false
    }

    fun selectScope(newScope: SearchScope) {
        if (_scope.value == newScope) return
        _scope.value = newScope
        if (_query.value.trim().length >= 2) {
            runSearch(_query.value)
        }
    }

    fun selectKindFilter(filter: ResultKindFilter) {
        _kindFilter.value = filter
    }

    fun runSearch(rawQuery: String) {
        val trimmed = rawQuery.trim()
        _query.value = trimmed
        _isEditing.value = false
        _kindFilter.value = ResultKindFilter.ALL

        activeCancel.set(true)
        searchJob?.cancel()

        if (trimmed.length < 2 && GospelSearchEngine.parseHymnNumberQuery(trimmed) == null) {
            _results.value = emptyList()
            _matchedTopics.value = emptyList()
            _isSearching.value = false
            return
        }

        val cancelFlag = AtomicBoolean(false)
        activeCancel = cancelFlag
        val currentScope = _scope.value

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            try {
                val response = repository.searchGospel(
                    query = trimmed,
                    scope = currentScope,
                    isCancelled = { cancelFlag.get() },
                )
                if (!cancelFlag.get()) {
                    _matchedTopics.value = response.matchedTopics
                    _results.value = response.results
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (!cancelFlag.get()) {
                    _matchedTopics.value = emptyList()
                    _results.value = emptyList()
                }
            } finally {
                if (!cancelFlag.get()) {
                    _isSearching.value = false
                }
            }
        }
    }

    override fun onCleared() {
        activeCancel.set(true)
        super.onCleared()
    }
}

class SearchScreen(
    sealedActivity: SealedLightActivity,
) : LightScreen<Unit, SearchViewModel>(sealedActivity) {

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
                    GospelDatabase.MIGRATION_5_6,
                )
                fallbackToDestructiveMigration()
            }
        },
        assetStreamProvider = { path ->
            lightContext.readAsset(path).inputStream()
        },
        filesDir = lightContext.filesDir,
    )

    override val viewModelClass: Class<SearchViewModel>
        get() = SearchViewModel::class.java

    override fun createViewModel() = SearchViewModel(repository)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val query by viewModel.query.collectAsState()
        val isEditing by viewModel.isEditing.collectAsState()
        val scope by viewModel.scope.collectAsState()
        val kindFilter by viewModel.kindFilter.collectAsState()
        val isSearching by viewModel.isSearching.collectAsState()
        val matchedTopics by viewModel.matchedTopics.collectAsState()
        val results by viewModel.results.collectAsState()

        val textFieldState = rememberTextFieldState(query)
        val keyboardOptionsFlow = rememberKeyboardOptions()

        val directCount = results.count { it.matchKind != SearchMatchKind.CONCEPT }
        val relatedCount = results.count { it.matchKind == SearchMatchKind.CONCEPT }
        val filteredResults = when (kindFilter) {
            ResultKindFilter.ALL -> results
            ResultKindFilter.DIRECT -> results.filter { it.matchKind != SearchMatchKind.CONCEPT }
            ResultKindFilter.RELATED -> results.filter { it.matchKind == SearchMatchKind.CONCEPT }
        }

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
            ) {
                if (isEditing) {
                    LightTextInputEditor(
                        title = "Search Scriptures, Talks & Hymns",
                        state = textFieldState,
                        keyboardOptionsFlow = keyboardOptionsFlow,
                        submitIcon = LightIcons.SEARCH,
                        singleLine = true,
                        onSubmit = { charSeq ->
                            val q = charSeq.toString().trim()
                            if (q.isNotBlank()) {
                                viewModel.runSearch(q)
                            } else {
                                viewModel.closeEditor()
                            }
                        },
                        onBack = {
                            if (query.isBlank()) {
                                goBack()
                            } else {
                                viewModel.closeEditor()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val edgeBackPx = with(LocalDensity.current) { 44.dp.toPx() }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .swipeToBack(sideOnly = true, edgeThresholdPx = edgeBackPx) { goBack() }
                    ) {
                        LightTopBar(
                            leftButton = LightBarButton.LightIcon(
                                icon = LightIcons.BACK,
                                onClick = { goBack() },
                            ),
                            center = LightTopBarCenter.Text("Search"),
                            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
                        )

                        // Search Query Field
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 4.dp)
                        ) {
                            LightTextField(
                                label = "",
                                value = query,
                                placeholder = "Word, phrase, topic, or Alma 32:21...",
                                onClick = {
                                    textFieldState.setTextAndPlaceCursorAtEnd(query)
                                    viewModel.openEditor()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Scope Filter Chips (All / BofM / NT / OT / D&C / PGP / Conf.)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SearchScope.entries.forEach { s ->
                                SearchChip(
                                    label = s.label,
                                    selected = scope == s,
                                    onClick = { viewModel.selectScope(s) },
                                )
                            }
                        }

                        // Direct vs Related Ideas filter bar (shown when both exist)
                        if (!isSearching && relatedCount > 0 && directCount > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SearchChip(
                                    label = "All (${results.size})",
                                    selected = kindFilter == ResultKindFilter.ALL,
                                    onClick = { viewModel.selectKindFilter(ResultKindFilter.ALL) },
                                )
                                SearchChip(
                                    label = "Direct ($directCount)",
                                    selected = kindFilter == ResultKindFilter.DIRECT,
                                    onClick = { viewModel.selectKindFilter(ResultKindFilter.DIRECT) },
                                )
                                SearchChip(
                                    label = "Related ($relatedCount)",
                                    selected = kindFilter == ResultKindFilter.RELATED,
                                    onClick = { viewModel.selectKindFilter(ResultKindFilter.RELATED) },
                                )
                            }
                        }

                        // Body: Loading / Suggestions / Results
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .swipeToBack { goBack() }
                        ) {
                            when {
                                isSearching -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        LightText(
                                            text = "Searching scriptures & talks...",
                                            variant = LightTextVariant.Subheading,
                                        )
                                    }
                                }

                                query.isBlank() -> {
                                    LightScrollView(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 1.5f.gridUnitsAsDp()),
                                    ) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        LightText(
                                            text = "TRY SEARCHING AN IDEA OR PHRASE",
                                            variant = LightTextVariant.Detail,
                                            lighten = true,
                                            modifier = Modifier.padding(end = 1.5f.gridUnitsAsDp(), bottom = 8.dp),
                                        )
                                        GospelSearchEngine.SUGGESTED_TOPICS.forEach { topic ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .lightClickable {
                                                        textFieldState.setTextAndPlaceCursorAtEnd(topic)
                                                        viewModel.runSearch(topic)
                                                    }
                                                    .padding(end = 1.5f.gridUnitsAsDp(), top = 8.dp, bottom = 8.dp),
                                            ) {
                                                LightText(
                                                    text = topic,
                                                    variant = LightTextVariant.Paragraph,
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        LightText(
                                            text = "Tip: Jump straight to any verse (e.g. Alma 32:21, D&C 121:7) or type how you're feeling to find related teachings.",
                                            variant = LightTextVariant.Detail,
                                            lighten = true,
                                            modifier = Modifier.padding(end = 1.5f.gridUnitsAsDp(), bottom = 16.dp),
                                        )
                                    }
                                }

                                filteredResults.isEmpty() -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(2f.gridUnitsAsDp()),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            LightText(
                                                text = "No matches for \"$query\"",
                                                variant = LightTextVariant.Subheading,
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            LightText(
                                                text = "Try fewer words, a topic (like peace or faith), or switch scope to All.",
                                                variant = LightTextVariant.Detail,
                                                lighten = true,
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    LightScrollView(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 1.5f.gridUnitsAsDp()),
                                    ) {
                                        if (matchedTopics.isNotEmpty()) {
                                            LightText(
                                                text = "Topics: ${matchedTopics.joinToString(" • ")}",
                                                variant = LightTextVariant.Detail,
                                                lighten = true,
                                                modifier = Modifier.padding(end = 1.5f.gridUnitsAsDp(), top = 4.dp, bottom = 6.dp),
                                            )
                                        }

                                        filteredResults.forEachIndexed { idx, item ->
                                            SearchResultRow(
                                                item = item,
                                                onClick = {
                                                    navigateTo(screenFactory = { act ->
                                                        ReaderScreen(act, item.contentId, item.scrollPermille)
                                                    })
                                                },
                                                modifier = Modifier.padding(end = 1.5f.gridUnitsAsDp()),
                                            )
                                            if (idx < filteredResults.lastIndex) {
                                                HorizontalDivider(
                                                    thickness = 0.5.dp,
                                                    color = LightThemeTokens.colors.content.copy(alpha = 0.2f),
                                                    modifier = Modifier.padding(end = 1.5f.gridUnitsAsDp()),
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
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

@Composable
private fun SearchResultRow(
    item: GospelSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LightText(
                text = item.title,
                variant = LightTextVariant.Subheading,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = LightThemeTokens.colors.content.copy(
                            alpha = if (item.matchKind == SearchMatchKind.CONCEPT) 0.45f else 0.75f
                        ),
                        shape = RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                LightText(
                    text = item.matchKind.badge,
                    variant = LightTextVariant.Detail,
                    lighten = item.matchKind == SearchMatchKind.CONCEPT,
                )
            }
        }

        val subLine = buildString {
            append(item.subtitle)
            if (item.matchKind == SearchMatchKind.CONCEPT && !item.topicLabel.isNullOrBlank()) {
                append(" • ")
                append(item.topicLabel)
            }
        }
        LightText(
            text = subLine,
            variant = LightTextVariant.Detail,
            lighten = true,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )

        if (item.snippet.isNotBlank()) {
            LightText(
                text = item.snippet,
                variant = LightTextVariant.Detail,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SearchChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val content = LightThemeTokens.colors.content
    val bg = if (selected) content else LightThemeTokens.colors.background
    val fg = if (selected) LightThemeTokens.colors.background else content
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .border(
                width = 1.dp,
                color = content.copy(alpha = 0.55f),
                shape = RoundedCornerShape(4.dp),
            )
            .lightClickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        LightText(
            text = label,
            variant = LightTextVariant.Detail,
            color = fg,
        )
    }
}
