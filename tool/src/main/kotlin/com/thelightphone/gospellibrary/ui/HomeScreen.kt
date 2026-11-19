package com.thelightphone.gospellibrary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.thelightphone.gospellibrary.data.GeneralConferenceData
import com.thelightphone.gospellibrary.data.GospelRepository
import com.thelightphone.gospellibrary.data.StandardWorksData
import com.thelightphone.gospellibrary.db.GospelDatabase
import com.thelightphone.gospellibrary.db.LastReadEntity
import com.thelightphone.sdk.InitialScreen
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeScreenViewModel(
    private val repository: GospelRepository,
) : LightViewModel<Unit>() {

    private val _lastRead = MutableStateFlow<LastReadEntity?>(null)
    val lastRead: StateFlow<LastReadEntity?> = _lastRead.asStateFlow()

    private val _bookmarkCount = MutableStateFlow(0)
    val bookmarkCount: StateFlow<Int> = _bookmarkCount.asStateFlow()

    private val _latestConferenceTitle = MutableStateFlow("October 2026 Conference")
    val latestConferenceTitle: StateFlow<String> = _latestConferenceTitle.asStateFlow()

    private val _latestConferenceSubtitle = MutableStateFlow("38 complete talks • Tap to read or update")
    val latestConferenceSubtitle: StateFlow<String> = _latestConferenceSubtitle.asStateFlow()

    private var lastReadJob: Job? = null
    private var bookmarkJob: Job? = null
    private var conferenceJob: Job? = null
    private var hasSyncedOnLaunch = false

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        repository.ensureConferenceAssetLoaded()

        lastReadJob?.cancel()
        lastReadJob = viewModelScope.launch {
            repository.lastRead.collect { entity ->
                _lastRead.value = entity
            }
        }

        bookmarkJob?.cancel()
        bookmarkJob = viewModelScope.launch {
            repository.bookmarkCount.collect { count ->
                _bookmarkCount.value = count
            }
        }

        conferenceJob?.cancel()
        conferenceJob = viewModelScope.launch {
            repository.conferenceTalks.collect { talks ->
                val periods = GeneralConferenceData.getConferencePeriods(talks)
                val topPeriod = periods.firstOrNull()
                if (topPeriod != null) {
                    _latestConferenceTitle.value = "${topPeriod.name} Conference"
                    val talkCount = topPeriod.sessions.sumOf { it.talks.size }
                    _latestConferenceSubtitle.value = "$talkCount complete talks • Tap to read or update"
                }
            }
        }

        if (!hasSyncedOnLaunch) {
            hasSyncedOnLaunch = true
            viewModelScope.launch {
                repository.cleanupOctober2024()
                repository.syncConferenceIfOnline()
            }
        }
    }
}

@InitialScreen
class HomeScreen(
    sealedActivity: SealedLightActivity,
) : LightScreen<Unit, HomeScreenViewModel>(sealedActivity) {

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

    override val viewModelClass: Class<HomeScreenViewModel>
        get() = HomeScreenViewModel::class.java

    override fun createViewModel() = HomeScreenViewModel(repository)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val lastRead by viewModel.lastRead.collectAsState()
        val bookmarkCount by viewModel.bookmarkCount.collectAsState()
        val latestConferenceTitle by viewModel.latestConferenceTitle.collectAsState()
        val latestConferenceSubtitle by viewModel.latestConferenceSubtitle.collectAsState()

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Minimal Top Bar
                    LightTopBar(
                        center = LightTopBarCenter.Text("Gospel Library"),
                        rightButton = LightBarButton.LightIcon(
                            icon = LightIcons.SETTINGS,
                            contentDescription = "Settings",
                            onClick = {
                                navigateTo(screenFactory = { act ->
                                    SettingsScreen(act)
                                })
                            }
                        ),
                        modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp())
                    )

                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 1.5f.gridUnitsAsDp())
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Continue Reading (if present)
                        lastRead?.let { recent ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                                    .lightClickable {
                                        navigateTo(screenFactory = { act ->
                                            ReaderScreen(act, recent.contentId)
                                        })
                                    }
                            ) {
                                LightText(
                                    text = "CONTINUE READING",
                                    variant = LightTextVariant.Detail,
                                    lighten = true,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                LightText(
                                    text = recent.title,
                                    variant = LightTextVariant.Subheading
                                )
                                recent.subtitle?.let { sub ->
                                    LightText(
                                        text = sub,
                                        variant = LightTextVariant.Detail,
                                        lighten = true,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Bookmarks Quick Entry
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                                .lightClickable {
                                    navigateTo(screenFactory = { act ->
                                        BookmarksScreen(act)
                                    })
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                LightText(
                                    text = "Bookmarks",
                                    variant = LightTextVariant.Subheading
                                )
                                LightText(
                                    text = if (bookmarkCount == 1) "1 saved bookmark" else "$bookmarkCount saved bookmarks",
                                    variant = LightTextVariant.Detail,
                                    lighten = true,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Standard Works Section
                        LightText(
                            text = "STANDARD WORKS",
                            variant = LightTextVariant.Detail,
                            lighten = true,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        StandardWorksData.volumes.forEach { volume ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .lightClickable {
                                        if (volume.id == "dc") {
                                            val dcBook = StandardWorksData.getBooksForVolume("dc").first()
                                            navigateTo(screenFactory = { act ->
                                                ChapterPickerScreen(act, dcBook)
                                            })
                                        } else {
                                            navigateTo(screenFactory = { act ->
                                                BookListScreen(act, volume)
                                            })
                                        }
                                    }
                            ) {
                                LightText(
                                    text = volume.shortTitle,
                                    variant = LightTextVariant.Subheading
                                )
                                val count = if (volume.id == "dc") "138 sections" else "${volume.bookCount} books"
                                LightText(
                                    text = count,
                                    variant = LightTextVariant.Detail,
                                    lighten = true,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // General Conference Section
                        LightText(
                            text = "GENERAL CONFERENCE",
                            variant = LightTextVariant.Detail,
                            lighten = true,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .lightClickable {
                                    navigateTo(screenFactory = { act ->
                                        ConferenceListScreen(act)
                                    })
                                }
                        ) {
                            LightText(
                                text = latestConferenceTitle,
                                variant = LightTextVariant.Subheading
                            )
                            LightText(
                                text = latestConferenceSubtitle,
                                variant = LightTextVariant.Detail,
                                lighten = true,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
