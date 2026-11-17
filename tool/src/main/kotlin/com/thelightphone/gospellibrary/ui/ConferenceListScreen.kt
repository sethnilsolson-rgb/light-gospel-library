package com.thelightphone.gospellibrary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.thelightphone.gospellibrary.data.GeneralConferenceData
import com.thelightphone.gospellibrary.data.GospelRepository
import com.thelightphone.gospellibrary.db.GospelDatabase
import com.thelightphone.gospellibrary.model.ConferencePeriod
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

class ConferenceListViewModel(
    private val repository: GospelRepository,
) : LightViewModel<Unit>() {

    private val _periods = MutableStateFlow(GeneralConferenceData.conferencePeriods)
    val periods: StateFlow<List<ConferencePeriod>> = _periods.asStateFlow()

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        viewModelScope.launch {
            repository.conferenceTalks.collect { talks ->
                _periods.value = GeneralConferenceData.getConferencePeriods(talks)
            }
        }
        viewModelScope.launch {
            repository.syncConferenceIfOnline()
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
                fallbackToDestructiveMigration()
            }
        },
        assetStreamProvider = { path ->
            lightContext.readAsset(path).inputStream()
        }
    )

    override val viewModelClass: Class<ConferenceListViewModel>
        get() = ConferenceListViewModel::class.java

    override fun createViewModel() = ConferenceListViewModel(repository)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val periods by viewModel.periods.collectAsState()
        val conference = periods.firstOrNull()

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(
                            icon = LightIcons.BACK,
                            onClick = { goBack() }
                        ),
                        center = LightTopBarCenter.Text(conference?.name ?: "General Conference"),
                        modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp())
                    )

                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 1.5f.gridUnitsAsDp())
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        conference?.sessions?.forEachIndexed { index, session ->
                            val isLast = index == conference.sessions.lastIndex
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = if (isLast) 8.dp else 16.dp)
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
