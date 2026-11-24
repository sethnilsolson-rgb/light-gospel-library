package com.thelightphone.gospellibrary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.thelightphone.gospellibrary.data.GospelRepository
import com.thelightphone.gospellibrary.data.HymnsData
import com.thelightphone.gospellibrary.db.GospelDatabase
import com.thelightphone.gospellibrary.db.SyncedHymnEntity
import com.thelightphone.gospellibrary.model.HymnBook
import com.thelightphone.gospellibrary.model.HymnItem
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.buildDatabase
import com.thelightphone.sdk.rememberKeyboardOptions
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class HymnListViewModel(
    initialBook: HymnBook,
    private val repository: GospelRepository,
) : LightViewModel<Unit>() {

    private val _selectedBook = MutableStateFlow(initialBook)
    val selectedBook: StateFlow<HymnBook> = _selectedBook.asStateFlow()

    private val _hymns = MutableStateFlow<List<HymnItem>>(emptyList())
    val hymns: StateFlow<List<HymnItem>> = _hymns.asStateFlow()

    private val _filterQuery = MutableStateFlow("")
    val filterQuery: StateFlow<String> = _filterQuery.asStateFlow()

    private val _isFilterEditing = MutableStateFlow(false)
    val isFilterEditing: StateFlow<Boolean> = _isFilterEditing.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private var syncedCache: List<SyncedHymnEntity> = emptyList()
    private var collectJob: Job? = null
    private var syncJob: Job? = null

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        repository.ensureHymnsLoaded()
        refreshList()

        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            repository.syncedHymns.collect { synced ->
                syncedCache = synced
                refreshList()
            }
        }
    }

    fun selectBook(book: HymnBook) {
        if (_selectedBook.value == book) return
        _selectedBook.value = book
        _syncMessage.value = null
        refreshList()
    }

    fun openFilter() {
        _isFilterEditing.value = true
    }

    fun closeFilter() {
        _isFilterEditing.value = false
    }

    fun applyFilter(raw: String) {
        _filterQuery.value = raw.trim()
        _isFilterEditing.value = false
        refreshList()
    }

    fun clearFilter() {
        _filterQuery.value = ""
        refreshList()
    }

    fun checkForNewHymns() {
        if (_isSyncing.value) return
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Checking for new hymns..."
            try {
                val status = repository.syncHomeAndChurchHymns()
                _syncMessage.value = status.message
            } catch (_: Throwable) {
                _syncMessage.value = "Unable to check right now. Try again later."
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun refreshList() {
        val full = HymnsData.getHymnsForBook(_selectedBook.value, syncedCache)
        val q = _filterQuery.value.trim().lowercase(Locale.US)
        if (q.isEmpty()) {
            _hymns.value = full
            return
        }
        val numQuery = q.removePrefix("#").removePrefix("hymn").trim().toIntOrNull()
        _hymns.value = full.filter { h ->
            (numQuery != null && (h.number == numQuery || h.number.toString().startsWith(numQuery.toString()))) ||
                h.title.lowercase(Locale.US).contains(q) ||
                h.topic.lowercase(Locale.US).contains(q) ||
                h.stanzas.any { it.lowercase(Locale.US).contains(q) }
        }
    }
}

class HymnListScreen(
    sealedActivity: SealedLightActivity,
    private val initialBookId: String = HymnBook.HOME_AND_CHURCH.id,
) : LightScreen<Unit, HymnListViewModel>(sealedActivity) {

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

    override val viewModelClass: Class<HymnListViewModel>
        get() = HymnListViewModel::class.java

    override fun createViewModel() = HymnListViewModel(HymnBook.fromId(initialBookId), repository)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val selectedBook by viewModel.selectedBook.collectAsState()
        val hymns by viewModel.hymns.collectAsState()
        val filterQuery by viewModel.filterQuery.collectAsState()
        val isFilterEditing by viewModel.isFilterEditing.collectAsState()
        val isSyncing by viewModel.isSyncing.collectAsState()
        val syncMessage by viewModel.syncMessage.collectAsState()

        val filterFieldState = rememberTextFieldState(filterQuery)
        val keyboardOptionsFlow = rememberKeyboardOptions()

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
            ) {
                if (isFilterEditing) {
                    LightTextInputEditor(
                        title = "Jump to Hymn # or Title",
                        state = filterFieldState,
                        keyboardOptionsFlow = keyboardOptionsFlow,
                        submitIcon = LightIcons.SEARCH,
                        singleLine = true,
                        onSubmit = { seq ->
                            viewModel.applyFilter(seq.toString())
                        },
                        onBack = { viewModel.closeFilter() },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .swipeToBack { goBack() }
                    ) {
                        LightTopBar(
                            leftButton = LightBarButton.LightIcon(
                                icon = LightIcons.BACK,
                                onClick = { goBack() },
                            ),
                            center = LightTopBarCenter.Text("Hymns"),
                            rightButton = LightBarButton.LightIcon(
                                icon = LightIcons.SEARCH,
                                contentDescription = "Filter Hymns",
                                onClick = {
                                    filterFieldState.setTextAndPlaceCursorAtEnd(filterQuery)
                                    viewModel.openFilter()
                                },
                            ),
                            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
                        )

                        // Two-book switcher: Home & Church | Classic (1985)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            HymnBook.entries.forEach { book ->
                                val active = book == selectedBook
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (active) {
                                                LightThemeTokens.colors.content
                                            } else {
                                                LightThemeTokens.colors.background
                                            },
                                            shape = RoundedCornerShape(4.dp),
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = LightThemeTokens.colors.content.copy(
                                                alpha = if (active) 1f else 0.4f
                                            ),
                                            shape = RoundedCornerShape(4.dp),
                                        )
                                        .lightClickable { viewModel.selectBook(book) }
                                        .padding(vertical = 7.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    LightText(
                                        text = book.shortTitle,
                                        variant = LightTextVariant.Detail,
                                        color = if (active) {
                                            LightThemeTokens.colors.background
                                        } else {
                                            LightThemeTokens.colors.content
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        LightScrollView(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(start = 1.5f.gridUnitsAsDp())
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            // Refresh card for the continuously updated "Hymns—For Home and Church"
                            if (selectedBook.supportsSync) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 1.5f.gridUnitsAsDp(), bottom = 12.dp)
                                        .border(
                                            width = 1.dp,
                                            color = LightThemeTokens.colors.content.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(4.dp),
                                        )
                                        .lightClickable { viewModel.checkForNewHymns() }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        LightText(
                                            text = if (isSyncing) "Checking for New Hymns..." else "Check for New Hymns",
                                            variant = LightTextVariant.Detail,
                                            modifier = Modifier.weight(1f),
                                        )
                                        LightText(
                                            text = if (isSyncing) "..." else "↻",
                                            variant = LightTextVariant.Detail,
                                        )
                                    }
                                    syncMessage?.let { msg ->
                                        LightText(
                                            text = msg,
                                            variant = LightTextVariant.Detail,
                                            lighten = true,
                                            modifier = Modifier.padding(top = 4.dp),
                                        )
                                    }
                                }
                            }

                            // Active Filter Chip
                            if (filterQuery.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 1.5f.gridUnitsAsDp(), bottom = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    LightText(
                                        text = "Filter: \"$filterQuery\" (${hymns.size})",
                                        variant = LightTextVariant.Detail,
                                        lighten = true,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .border(
                                                width = 1.dp,
                                                color = LightThemeTokens.colors.content.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(4.dp),
                                            )
                                            .lightClickable {
                                                filterFieldState.setTextAndPlaceCursorAtEnd("")
                                                viewModel.clearFilter()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        LightText(
                                            text = "Clear",
                                            variant = LightTextVariant.Detail,
                                        )
                                    }
                                }
                            } else {
                                LightText(
                                    text = "${selectedBook.title.uppercase()} (${hymns.size})",
                                    variant = LightTextVariant.Detail,
                                    lighten = true,
                                    modifier = Modifier.padding(end = 1.5f.gridUnitsAsDp(), bottom = 8.dp),
                                )
                            }

                            if (hymns.isEmpty()) {
                                LightText(
                                    text = if (filterQuery.isNotBlank()) {
                                        "No hymns match \"$filterQuery\" in ${selectedBook.shortTitle}."
                                    } else {
                                        "No hymns loaded."
                                    },
                                    variant = LightTextVariant.Paragraph,
                                    lighten = true,
                                    modifier = Modifier.padding(end = 1.5f.gridUnitsAsDp(), top = 12.dp),
                                )
                            } else {
                                hymns.forEach { hymn ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(end = 1.5f.gridUnitsAsDp(), top = 7.dp, bottom = 7.dp)
                                            .lightClickable {
                                                navigateTo(screenFactory = { act ->
                                                    ReaderScreen(act, hymn.id)
                                                })
                                            },
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        LightText(
                                            text = hymn.number.toString(),
                                            variant = LightTextVariant.Detail,
                                            lighten = true,
                                            modifier = Modifier
                                                .width(44.dp)
                                                .padding(top = 3.dp),
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            LightText(
                                                text = hymn.title,
                                                variant = LightTextVariant.Subheading,
                                            )
                                            if (hymn.topic.isNotBlank()) {
                                                LightText(
                                                    text = hymn.topic,
                                                    variant = LightTextVariant.Detail,
                                                    lighten = true,
                                                    modifier = Modifier.padding(top = 1.dp),
                                                )
                                            }
                                        }
                                    }
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
