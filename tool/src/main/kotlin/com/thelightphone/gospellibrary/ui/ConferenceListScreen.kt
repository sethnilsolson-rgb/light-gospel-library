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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.thelightphone.gospellibrary.data.GeneralConferenceData
import com.thelightphone.gospellibrary.data.GospelRepository
import com.thelightphone.gospellibrary.db.GospelDatabase
import com.thelightphone.gospellibrary.model.ConferencePeriod
import com.thelightphone.gospellibrary.model.GospelLanguage
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ConferenceListViewModel(
    private val repository: GospelRepository,
) : LightViewModel<Unit>() {

    private val _periods = MutableStateFlow<List<ConferencePeriod>>(emptyList())
    val periods: StateFlow<List<ConferencePeriod>> = _periods.asStateFlow()

    private val _selectedPeriodId = MutableStateFlow<String?>(null)
    val selectedPeriodId: StateFlow<String?> = _selectedPeriodId.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _availableLanguages = MutableStateFlow<List<GospelLanguage>>(listOf(GospelLanguage.ENG))
    val availableLanguages: StateFlow<List<GospelLanguage>> = _availableLanguages.asStateFlow()

    private val _currentLanguage = MutableStateFlow(GospelLanguage.ENG)
    val currentLanguage: StateFlow<GospelLanguage> = _currentLanguage.asStateFlow()

    private var collectJob: Job? = null
    private var languagesJob: Job? = null
    private var syncJob: Job? = null
    private var refreshJob: Job? = null

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        _periods.value = GeneralConferenceData.getConferencePeriods()
        if (_selectedPeriodId.value == null) {
            _selectedPeriodId.value = _periods.value.firstOrNull()?.id
        }

        languagesJob?.cancel()
        languagesJob = viewModelScope.launch {
            repository.downloadedLanguages.collect { entities ->
                val others = entities
                    .filter { it.langCode != GospelLanguage.ENG.code }
                    .map { GospelLanguage.fromCode(it.langCode) }
                    .distinct()
                    .sortedBy { GospelLanguage.entries.indexOf(it) }
                _availableLanguages.value = listOf(GospelLanguage.ENG) + others
            }
        }

        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            val activeLang = GospelLanguage.fromCode(repository.getActiveLanguageCodeSync())
            _currentLanguage.value = activeLang
            combine(repository.conferenceTalks, _currentLanguage) { _, lang -> lang }
                .collectLatest { lang ->
                    refreshPeriodsForLanguage(lang)
                }
        }
    }

    override fun onScreenHide(screen: SimpleLightScreen<Unit>) {
        super.onScreenHide(screen)
        collectJob?.cancel()
        languagesJob?.cancel()
        syncJob?.cancel()
        refreshJob?.cancel()
    }

    private suspend fun refreshPeriodsForLanguage(language: GospelLanguage) {
        val updated = repository.getConferencePeriods(language.code)
        _periods.value = updated
        if (_selectedPeriodId.value == null || updated.none { it.id == _selectedPeriodId.value }) {
            _selectedPeriodId.value = updated.firstOrNull()?.id
        }
    }

    fun cycleLanguage() {
        val list = _availableLanguages.value
        if (list.size <= 1) return
        val currentIdx = list.indexOf(_currentLanguage.value).coerceAtLeast(0)
        val nextLang = list[(currentIdx + 1) % list.size]
        _currentLanguage.value = nextLang
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            repository.setActiveLanguage(nextLang.code)
        }
    }

    fun selectPeriod(periodId: String) {
        _selectedPeriodId.value = periodId
    }

    fun checkForNewConference() {
        if (_isSyncing.value) return
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Checking for new conference talks..."
            try {
                val outcome = repository.syncConferenceIfOnline()
                _syncMessage.value = outcome.message
                if (outcome.downloadedNew) {
                    refreshPeriodsForLanguage(_currentLanguage.value)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _syncMessage.value = "Unable to check right now. Try again later."
            } finally {
                _isSyncing.value = false
            }
        }
    }
}

class ConferenceListScreen(
    sealedActivity: SealedLightActivity,
) : LightScreen<Unit, ConferenceListViewModel>(sealedActivity) {

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

    override val viewModelClass: Class<ConferenceListViewModel>
        get() = ConferenceListViewModel::class.java

    override fun createViewModel() = ConferenceListViewModel(repository)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val periods by viewModel.periods.collectAsState()
        val selectedPeriodId by viewModel.selectedPeriodId.collectAsState()
        val isSyncing by viewModel.isSyncing.collectAsState()
        val syncMessage by viewModel.syncMessage.collectAsState()
        val availableLanguages by viewModel.availableLanguages.collectAsState()
        val currentLanguage by viewModel.currentLanguage.collectAsState()

        val conference = periods.firstOrNull { it.id == selectedPeriodId } ?: periods.firstOrNull()
        val showLanguageToggle = availableLanguages.size > 1

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .swipeToBack { goBack() }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 0.5f.gridUnitsAsDp())
                    ) {
                        BatteryTopBar(
                            leftButton = LightBarButton.LightIcon(
                                icon = LightIcons.BACK,
                                onClick = { goBack() }
                            ),
                            center = if (showLanguageToggle) {
                                null
                            } else {
                                LightTopBarCenter.Text(conference?.name ?: "General Conference")
                            },
                            centerTitleForMeasure = conference?.name ?: "General Conference",
                            centerExtraWidthDp = if (showLanguageToggle) 44.dp else 0.dp,
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
                                    text = conference?.name ?: "General Conference",
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

                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 1.5f.gridUnitsAsDp())
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Easy one-tap sync for new General Conferences
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 1.5f.gridUnitsAsDp(), bottom = 14.dp)
                                .border(
                                    width = 1.dp,
                                    color = LightThemeTokens.colors.content.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .lightClickable { viewModel.checkForNewConference() }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LightText(
                                    text = if (isSyncing) "Checking for New Talks..." else "Check for New Conference",
                                    variant = LightTextVariant.Detail,
                                    modifier = Modifier.weight(1f)
                                )
                                LightText(
                                    text = if (isSyncing) "..." else "↻",
                                    variant = LightTextVariant.Detail
                                )
                            }
                            syncMessage?.let { msg ->
                                LightText(
                                    text = msg,
                                    variant = LightTextVariant.Detail,
                                    lighten = true,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        // Period selector if more than one conference is available (Oct 2026+)
                        if (periods.size > 1) {
                            LightText(
                                text = "CONFERENCES",
                                variant = LightTextVariant.Detail,
                                lighten = true,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            periods.forEach { period ->
                                val isSelected = period.id == conference?.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 1.5f.gridUnitsAsDp(), bottom = 6.dp)
                                        .lightClickable { viewModel.selectPeriod(period.id) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LightText(
                                        text = if (isSelected) "• ${period.name}" else period.name,
                                        variant = if (isSelected) LightTextVariant.Subheading else LightTextVariant.Paragraph,
                                        lighten = !isSelected
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        conference?.sessions?.forEachIndexed { index, session ->
                            val isLast = index == conference.sessions.lastIndex
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 1.5f.gridUnitsAsDp(), bottom = if (isLast) 12.dp else 18.dp)
                            ) {
                                LightText(
                                    text = session.sessionName.uppercase(),
                                    variant = LightTextVariant.Detail,
                                    lighten = true,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                session.talks.forEach { talk ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .lightClickable {
                                                navigateTo(screenFactory = { act ->
                                                    ReaderScreen(act, talk.id)
                                                })
                                            }
                                    ) {
                                        LightText(
                                            text = talk.title,
                                            variant = LightTextVariant.Subheading
                                        )
                                        if (talk.speaker.isNotBlank()) {
                                            LightText(
                                                text = talk.speaker,
                                                variant = LightTextVariant.Detail,
                                                lighten = true,
                                                modifier = Modifier.padding(top = 2.dp)
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
}
