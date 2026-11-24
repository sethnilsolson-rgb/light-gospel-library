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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.thelightphone.gospellibrary.data.GospelRepository
import com.thelightphone.gospellibrary.db.DownloadedLanguageEntity
import com.thelightphone.gospellibrary.db.GospelDatabase
import com.thelightphone.gospellibrary.model.GospelLanguage
import com.thelightphone.gospellibrary.model.ReaderPageMode
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
import com.thelightphone.sdk.ui.LightThemeColors
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
import java.text.NumberFormat
import java.util.Locale

class SettingsViewModel(
    private val repository: GospelRepository,
) : LightViewModel<Unit>() {

    private val _downloadedLanguages = MutableStateFlow<List<DownloadedLanguageEntity>>(emptyList())
    val downloadedLanguages: StateFlow<List<DownloadedLanguageEntity>> = _downloadedLanguages.asStateFlow()

    private val _activeLanguageCode = MutableStateFlow(GospelLanguage.ENG.code)
    val activeLanguageCode: StateFlow<String> = _activeLanguageCode.asStateFlow()

    val pageMode: StateFlow<ReaderPageMode> = repository.pageMode

    private val _busyKey = MutableStateFlow<String?>(null)
    val busyKey: StateFlow<String?> = _busyKey.asStateFlow()

    private val _progressText = MutableStateFlow<String?>(null)
    val progressText: StateFlow<String?> = _progressText.asStateFlow()

    private val _errorText = MutableStateFlow<String?>(null)
    val errorText: StateFlow<String?> = _errorText.asStateFlow()

    private var languagesJob: Job? = null
    private var activeLangJob: Job? = null
    private var workJob: Job? = null

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)

        languagesJob?.cancel()
        languagesJob = viewModelScope.launch {
            repository.downloadedLanguages.collect { list ->
                _downloadedLanguages.value = list
            }
        }

        activeLangJob?.cancel()
        activeLangJob = viewModelScope.launch {
            repository.activeLanguageCode.collect { code ->
                _activeLanguageCode.value = code
            }
        }
    }

    fun downloadEnglish1828() {
        if (_busyKey.value != null) return
        workJob = viewModelScope.launch {
            _busyKey.value = "eng_1828"
            _errorText.value = null
            _progressText.value = "Connecting to Webster's 1828 Dictionary..."
            val result = repository.downloadEnglish1828Dictionary { progress ->
                _progressText.value = progress
            }
            result.onFailure {
                _errorText.value = "Couldn't download Webster's 1828. Check Wi-Fi/internet and try again."
            }
            _busyKey.value = null
            _progressText.value = null
        }
    }

    fun removeEnglish1828() {
        if (_busyKey.value != null) return
        viewModelScope.launch {
            _errorText.value = null
            repository.removeEnglish1828Dictionary()
        }
    }

    fun downloadLanguage(language: GospelLanguage) {
        if (_busyKey.value != null) return
        workJob = viewModelScope.launch {
            _busyKey.value = "lang:${language.code}"
            _errorText.value = null
            _progressText.value = "Preparing ${language.englishName}..."
            val result = repository.downloadLanguagePack(language) { progress ->
                _progressText.value = progress
            }
            result.onFailure {
                _errorText.value = "Couldn't download ${language.englishName}. Check internet and try again."
            }
            _busyKey.value = null
            _progressText.value = null
        }
    }

    fun removeLanguage(language: GospelLanguage) {
        if (_busyKey.value != null) return
        viewModelScope.launch {
            _errorText.value = null
            repository.removeLanguagePack(language)
        }
    }

    fun selectActiveLanguage(langCode: String) {
        viewModelScope.launch {
            repository.setActiveLanguage(langCode)
        }
    }

    fun downloadVolumeOffline(language: GospelLanguage, volumeId: String, label: String) {
        if (_busyKey.value != null) return
        workJob = viewModelScope.launch {
            _busyKey.value = "vol:${language.code}:$volumeId"
            _errorText.value = null
            _progressText.value = "Downloading $label (${language.nativeName})..."
            val result = repository.downloadVolumeOffline(language, volumeId) { progress ->
                _progressText.value = "$label: $progress"
            }
            result.onFailure {
                _errorText.value = "Offline download interrupted. Tap again to resume."
            }
            _busyKey.value = null
            _progressText.value = null
        }
    }
    fun togglePageMode() {
        repository.setPageMode(pageMode.value.toggle())
    }
}

class SettingsScreen(
    sealedActivity: SealedLightActivity,
) : LightScreen<Unit, SettingsViewModel>(sealedActivity) {

    private val repository = GospelRepository.getInstance(
        dbProvider = {
            lightContext.buildDatabase(
                GospelDatabase::class.java,
                GospelRepository.DATABASE_NAME
            ) {
                addMigrations(GospelDatabase.MIGRATION_2_3, GospelDatabase.MIGRATION_3_4)
                fallbackToDestructiveMigration()
            }
        },
        assetStreamProvider = { path ->
            lightContext.readAsset(path).inputStream()
        },
        filesDir = lightContext.filesDir,
    )

    override val viewModelClass: Class<SettingsViewModel>
        get() = SettingsViewModel::class.java

    override fun createViewModel() = SettingsViewModel(repository)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val downloadedLanguages by viewModel.downloadedLanguages.collectAsState()
        val activeLanguageCode by viewModel.activeLanguageCode.collectAsState()
        val pageMode by viewModel.pageMode.collectAsState()
        val busyKey by viewModel.busyKey.collectAsState()
        val progressText by viewModel.progressText.collectAsState()
        val errorText by viewModel.errorText.collectAsState()

        val isDark = themeColors == LightThemeColors.Dark
        val engEntity = downloadedLanguages.firstOrNull { it.langCode == GospelLanguage.ENG.code }
        val nonEnglishDownloaded = downloadedLanguages
            .filter { it.langCode != GospelLanguage.ENG.code }
            .sortedBy { GospelLanguage.entries.indexOf(GospelLanguage.fromCode(it.langCode)) }
        val downloadedCodeSet = nonEnglishDownloaded.map { it.langCode }.toSet()
        val availableToAdd = GospelLanguage.downloadableLanguages.filter { it.code !in downloadedCodeSet }

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .swipeToBack { goBack() }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(
                            icon = LightIcons.BACK,
                            onClick = { goBack() }
                        ),
                        center = LightTopBarCenter.Text("Settings"),
                        modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp())
                    )

                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 1.5f.gridUnitsAsDp())
                    ) {
                        val rightPad = 1.5f.gridUnitsAsDp()

                        Spacer(modifier = Modifier.height(10.dp))

                        // Error banner if any download failed
                        errorText?.let { err ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = rightPad, bottom = 14.dp)
                                    .border(
                                        width = 1.dp,
                                        color = LightThemeTokens.colors.content,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                LightText(
                                    text = err,
                                    variant = LightTextVariant.Detail
                                )
                            }
                        }

                        // 1. APPEARANCE (Light / Dark Theme)
                        LightText(
                            text = "APPEARANCE",
                            variant = LightTextVariant.Detail,
                            lighten = true,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = rightPad, bottom = 10.dp)
                                .border(
                                    width = 1.dp,
                                    color = LightThemeTokens.colors.content.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .lightClickable { LightThemeController.toggle() }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                LightText(
                                    text = if (isDark) "Dark Theme" else "Light Theme",
                                    variant = LightTextVariant.Subheading
                                )
                                LightText(
                                    text = if (isDark) "Tap to switch to Light Theme" else "Tap to switch to Dark Theme",
                                    variant = LightTextVariant.Detail,
                                    lighten = true,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            SettingsActionPill(
                                label = if (isDark) "Light" else "Dark",
                                highlighted = false,
                                onClick = { LightThemeController.toggle() }
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = rightPad, bottom = 18.dp)
                                .border(
                                    width = 1.dp,
                                    color = LightThemeTokens.colors.content.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .lightClickable { viewModel.togglePageMode() }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                LightText(
                                    text = "Reading Format: ${pageMode.label}",
                                    variant = LightTextVariant.Subheading
                                )
                                LightText(
                                    text = if (pageMode == ReaderPageMode.PAGED) {
                                        "Swipe on words to turn pages like a book. Double-tap for full screen."
                                    } else {
                                        "Continuous chapter scroll. Double-tap text for full screen."
                                    },
                                    variant = LightTextVariant.Detail,
                                    lighten = true,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            SettingsActionPill(
                                label = pageMode.chipLabel,
                                highlighted = pageMode == ReaderPageMode.PAGED,
                                onClick = { viewModel.togglePageMode() }
                            )
                        }

                        // 2. ENGLISH STUDY DICTIONARY (Webster's 1828)
                        LightText(
                            text = "ENGLISH STUDY DICTIONARY",
                            variant = LightTextVariant.Detail,
                            lighten = true,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val eng1828Ready = engEntity?.dictionaryDownloaded == true
                        val eng1828Busy = busyKey == "eng_1828"

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = rightPad, bottom = 20.dp)
                                .border(
                                    width = 1.dp,
                                    color = LightThemeTokens.colors.content.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    LightText(
                                        text = "Webster's 1828 Dictionary",
                                        variant = LightTextVariant.Subheading
                                    )
                                    val subtitle = when {
                                        eng1828Busy -> progressText ?: "Downloading..."
                                        eng1828Ready -> {
                                            val countStr = formatEntryCount(engEntity.dictionaryEntryCount)
                                            "Ready • $countStr entries • Press & hold any English word"
                                        }
                                        else -> "Offline historic definitions for press-and-hold scripture lookup."
                                    }
                                    LightText(
                                        text = subtitle,
                                        variant = LightTextVariant.Detail,
                                        lighten = !eng1828Busy,
                                        modifier = Modifier.padding(top = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (eng1828Busy) {
                                    SettingsActionPill(label = "...", highlighted = false, onClick = {})
                                } else if (eng1828Ready) {
                                    SettingsActionPill(
                                        label = "Remove",
                                        highlighted = false,
                                        onClick = { viewModel.removeEnglish1828() }
                                    )
                                } else {
                                    SettingsActionPill(
                                        label = "Download",
                                        highlighted = true,
                                        enabled = busyKey == null,
                                        onClick = { viewModel.downloadEnglish1828() }
                                    )
                                }
                            }
                        }

                        // 3. DOWNLOADED LANGUAGES
                        if (nonEnglishDownloaded.isNotEmpty()) {
                            LightText(
                                text = "DOWNLOADED LANGUAGES",
                                variant = LightTextVariant.Detail,
                                lighten = true,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            LightText(
                                text = "Use the language button beside any chapter title to switch seamlessly.",
                                variant = LightTextVariant.Detail,
                                lighten = true,
                                modifier = Modifier.padding(end = rightPad, bottom = 10.dp)
                            )

                            // English row so user can switch default back to English anytime
                            val engActive = activeLanguageCode == GospelLanguage.ENG.code
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = rightPad, bottom = 10.dp)
                                    .border(
                                        width = 1.dp,
                                        color = LightThemeTokens.colors.content.copy(alpha = if (engActive) 0.9f else 0.35f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .lightClickable { viewModel.selectActiveLanguage(GospelLanguage.ENG.code) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    LightText(
                                        text = "English [EN]",
                                        variant = LightTextVariant.Subheading
                                    )
                                    LightText(
                                        text = "Built-in Standard Works & Conference",
                                        variant = LightTextVariant.Detail,
                                        lighten = true,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                SettingsActionPill(
                                    label = if (engActive) "Active" else "Set Active",
                                    highlighted = engActive,
                                    onClick = { viewModel.selectActiveLanguage(GospelLanguage.ENG.code) }
                                )
                            }

                            nonEnglishDownloaded.forEach { entity ->
                                val lang = GospelLanguage.fromCode(entity.langCode)
                                val isActive = activeLanguageCode == lang.code
                                val volBusy = busyKey?.startsWith("vol:${lang.code}:") == true

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = rightPad, bottom = 12.dp)
                                        .border(
                                            width = 1.dp,
                                            color = LightThemeTokens.colors.content.copy(alpha = if (isActive) 0.9f else 0.4f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            LightText(
                                                text = "${lang.nativeName} [${lang.shortBadge}]",
                                                variant = LightTextVariant.Subheading
                                            )
                                            LightText(
                                                text = lang.englishName,
                                                variant = LightTextVariant.Detail,
                                                lighten = true,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                        SettingsActionPill(
                                            label = if (isActive) "Active" else "Use",
                                            highlighted = isActive,
                                            onClick = { viewModel.selectActiveLanguage(lang.code) }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        SettingsActionPill(
                                            label = "Remove",
                                            highlighted = false,
                                            enabled = busyKey == null,
                                            onClick = { viewModel.removeLanguage(lang) }
                                        )
                                    }

                                    val dictSummary = if (entity.dictionaryEntryCount > 0) {
                                        "${lang.dictLabel} (${formatEntryCount(entity.dictionaryEntryCount)} entries)"
                                    } else {
                                        "${lang.dictLabel} ready • Press & hold any word"
                                    }
                                    LightText(
                                        text = dictSummary,
                                        variant = LightTextVariant.Detail,
                                        lighten = true,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )

                                    val chapterSummary = buildString {
                                        if (entity.scriptureChaptersCached > 0) {
                                            append("${entity.scriptureChaptersCached} scripture chapters saved offline")
                                        } else {
                                            append("Scriptures cache automatically when opened")
                                        }
                                        if (entity.conferenceDownloaded) {
                                            append(" • Conference ready")
                                        }
                                    }
                                    LightText(
                                        text = chapterSummary,
                                        variant = LightTextVariant.Detail,
                                        lighten = true,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                    // Optional Offline Volume Pre-Download Buttons
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LightText(
                                        text = if (volBusy) (progressText ?: "Downloading...") else "PRE-DOWNLOAD SCRIPTURES OFFLINE",
                                        variant = LightTextVariant.Detail,
                                        lighten = !volBusy,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        VolumeChip("Book of Mormon", enabled = busyKey == null) {
                                            viewModel.downloadVolumeOffline(lang, "bofm", "Book of Mormon")
                                        }
                                        VolumeChip("D&C", enabled = busyKey == null) {
                                            viewModel.downloadVolumeOffline(lang, "dc", "D&C")
                                        }
                                        VolumeChip("PGP", enabled = busyKey == null) {
                                            viewModel.downloadVolumeOffline(lang, "pgp", "PGP")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        VolumeChip("New Testament", enabled = busyKey == null) {
                                            viewModel.downloadVolumeOffline(lang, "nt", "New Testament")
                                        }
                                        VolumeChip("Old Testament", enabled = busyKey == null) {
                                            viewModel.downloadVolumeOffline(lang, "ot", "Old Testament")
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // 4. ADD A LANGUAGE
                        if (availableToAdd.isNotEmpty()) {
                            LightText(
                                text = "ADD A LANGUAGE",
                                variant = LightTextVariant.Detail,
                                lighten = true,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            LightText(
                                text = "Downloads offline press-and-hold word dictionary & General Conference talks.",
                                variant = LightTextVariant.Detail,
                                lighten = true,
                                modifier = Modifier.padding(end = rightPad, bottom = 10.dp)
                            )

                            availableToAdd.forEach { lang ->
                                val isThisBusy = busyKey == "lang:${lang.code}"
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = rightPad, bottom = 10.dp)
                                        .border(
                                            width = 1.dp,
                                            color = LightThemeTokens.colors.content.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            LightText(
                                                text = "${lang.nativeName} [${lang.shortBadge}]",
                                                variant = LightTextVariant.Subheading
                                            )
                                            LightText(
                                                text = "${lang.englishName} • ${lang.dictLabel}",
                                                variant = LightTextVariant.Detail,
                                                lighten = true,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        SettingsActionPill(
                                            label = if (isThisBusy) "..." else "Download",
                                            highlighted = !isThisBusy,
                                            enabled = busyKey == null,
                                            onClick = { viewModel.downloadLanguage(lang) }
                                        )
                                    }

                                    if (isThisBusy && progressText != null) {
                                        LightText(
                                            text = progressText!!,
                                            variant = LightTextVariant.Detail,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsActionPill(
    label: String,
    highlighted: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val content = LightThemeTokens.colors.content
    val bg = if (highlighted) content else LightThemeTokens.colors.background
    val fg = if (highlighted) LightThemeTokens.colors.background else content
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .border(
                width = 1.dp,
                color = content.copy(alpha = if (enabled) 0.85f else 0.3f),
                shape = RoundedCornerShape(4.dp)
            )
            .let { mod -> if (enabled) mod.lightClickable(onClick = onClick) else mod }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        LightText(
            text = label,
            variant = LightTextVariant.Detail,
            color = if (enabled) fg else content.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun VolumeChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val content = LightThemeTokens.colors.content
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = content.copy(alpha = if (enabled) 0.45f else 0.2f),
                shape = RoundedCornerShape(4.dp)
            )
            .let { mod -> if (enabled) mod.lightClickable(onClick = onClick) else mod }
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        LightText(
            text = label,
            variant = LightTextVariant.Detail,
            lighten = !enabled
        )
    }
}

private fun formatEntryCount(count: Int): String =
    NumberFormat.getNumberInstance(Locale.US).format(count)
