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
import com.thelightphone.gospellibrary.model.ScriptureBook
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
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

class ChapterPickerViewModel : LightViewModel<Unit>()

class ChapterPickerScreen(
    sealedActivity: SealedLightActivity,
    private val book: ScriptureBook,
) : LightScreen<Unit, ChapterPickerViewModel>(sealedActivity) {

    override val viewModelClass: Class<ChapterPickerViewModel>
        get() = ChapterPickerViewModel::class.java

    override fun createViewModel() = ChapterPickerViewModel()

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .swipeToBack { goBack() }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    val screenTitle = if (book.volumeId == "dc") "Doctrine & Covenants" else book.shortTitle
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(
                            icon = LightIcons.BACK,
                            onClick = { goBack() }
                        ),
                        center = LightTopBarCenter.Text(screenTitle),
                        modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp())
                    )

                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 1.5f.gridUnitsAsDp())
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        val chapterPrefix = if (book.volumeId == "dc") "Section" else "Chapter"

                        (1..book.chapterCount).forEach { chapterNum ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                                    .lightClickable {
                                        val contentId = "scripture/${book.volumeId}/${book.id}/$chapterNum"
                                        navigateTo(screenFactory = { act ->
                                            ReaderScreen(act, contentId)
                                        })
                                    }
                            ) {
                                LightText(
                                    text = "$chapterPrefix $chapterNum",
                                    variant = LightTextVariant.Subheading
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
