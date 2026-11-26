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
import com.thelightphone.gospellibrary.data.StandardWorksData
import com.thelightphone.gospellibrary.model.ScriptureVolume
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

class BookListViewModel : LightViewModel<Unit>()

class BookListScreen(
    sealedActivity: SealedLightActivity,
    private val volume: ScriptureVolume,
) : LightScreen<Unit, BookListViewModel>(sealedActivity) {

    override val viewModelClass: Class<BookListViewModel>
        get() = BookListViewModel::class.java

    override fun createViewModel() = BookListViewModel()

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val books = StandardWorksData.getBooksForVolume(volume.id)

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .swipeToBack { goBack() }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    BatteryTopBar(
                        leftButton = LightBarButton.LightIcon(
                            icon = LightIcons.BACK,
                            onClick = { goBack() }
                        ),
                        center = LightTopBarCenter.Text(volume.shortTitle),
                        modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp())
                    )

                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 1.5f.gridUnitsAsDp())
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        books.forEach { book ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                                    .lightClickable {
                                        navigateTo(screenFactory = { act ->
                                            ChapterPickerScreen(act, book)
                                        })
                                    }
                            ) {
                                LightText(
                                    text = book.shortTitle,
                                    variant = LightTextVariant.Subheading
                                )
                                val countText = if (volume.id == "dc") {
                                    "${book.chapterCount} sections"
                                } else {
                                    "${book.chapterCount} ${if (book.chapterCount == 1) "chapter" else "chapters"}"
                                }
                                LightText(
                                    text = countText,
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
