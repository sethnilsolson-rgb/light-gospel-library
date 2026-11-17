package com.thelightphone.gospellibrary.data

import com.thelightphone.gospellibrary.db.BookmarkEntity
import com.thelightphone.gospellibrary.db.ConferenceTalkEntity
import com.thelightphone.gospellibrary.db.GospelDatabase
import com.thelightphone.gospellibrary.db.HighlightEntity
import com.thelightphone.gospellibrary.db.LastReadEntity
import com.thelightphone.gospellibrary.model.ConferencePeriod
import com.thelightphone.gospellibrary.model.ContentChapter
import com.thelightphone.gospellibrary.model.ContentType
import com.thelightphone.gospellibrary.model.ContentVerse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.io.InputStream

class GospelRepository(
    private val database: GospelDatabase,
    var assetStreamProvider: ((String) -> InputStream?)? = null,
) {
    val bookmarks: Flow<List<BookmarkEntity>> = database.bookmarkDao().getAllBookmarks()
    val bookmarkCount: Flow<Int> = database.bookmarkDao().getBookmarkCount()
    val lastRead: Flow<LastReadEntity?> = database.lastReadDao().getLastRead()
    val conferenceTalks: Flow<List<ConferenceTalkEntity>> = database.conferenceTalkDao().getAllTalks()

    fun isChapterBookmarked(contentId: String): Flow<Boolean> =
        database.bookmarkDao().isChapterBookmarked(contentId)

    fun getHighlightedVerses(contentId: String): Flow<List<Int>> =
        database.highlightDao().getHighlightedVerseNumbers(contentId)

    suspend fun toggleChapterBookmark(chapter: ContentChapter, isCurrentlyBookmarked: Boolean) {
        if (isCurrentlyBookmarked) {
            database.bookmarkDao().deleteChapterBookmark(chapter.id)
        } else {
            val preview = chapter.verses.firstOrNull()?.text?.take(80) ?: ""
            database.bookmarkDao().insertBookmark(
                BookmarkEntity(
                    contentId = chapter.id,
                    title = chapter.title,
                    snippet = preview,
                    verseNumber = null,
                )
            )
        }
    }

    suspend fun toggleVerseHighlight(contentId: String, verseNumber: Int, currentHighlights: List<Int>) {
        if (verseNumber in currentHighlights) {
            database.highlightDao().removeHighlight(contentId, verseNumber)
        } else {
            database.highlightDao().insertHighlight(
                HighlightEntity(
                    contentId = contentId,
                    verseNumber = verseNumber,
                )
            )
        }
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) {
        database.bookmarkDao().deleteBookmark(bookmark)
    }

    suspend fun cleanupOctober2024() {
        database.bookmarkDao().deleteOctober2024Bookmarks()
        database.highlightDao().deleteOctober2024Highlights()
        database.conferenceTalkDao().deleteOctober2024Talks()
    }

    suspend fun recordLastRead(chapter: ContentChapter) {
        database.lastReadDao().saveLastRead(
            LastReadEntity(
                contentId = chapter.id,
                title = chapter.title,
                subtitle = chapter.subtitle,
            )
        )
    }

    suspend fun syncConferenceIfOnline(customUrl: String? = null): Boolean {
        val url = customUrl ?: ConferenceSyncService.DEFAULT_UPDATE_URL
        val result = ConferenceSyncService.fetchRemoteTalks(url)
        return result.map { talks ->
            if (talks.isNotEmpty()) {
                database.conferenceTalkDao().insertTalks(talks)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    suspend fun getConferencePeriods(): List<ConferencePeriod> {
        val dbTalks = database.conferenceTalkDao().getAllTalksSync()
        return GeneralConferenceData.getConferencePeriods(dbTalks)
    }

    fun getContent(contentId: String): ContentChapter? {
        return when {
            contentId.startsWith("scripture/") -> {
                val parts = contentId.split("/")
                if (parts.size == 4) {
                    val volumeId = parts[1]
                    val bookId = parts[2]
                    val chapter = parts[3].toIntOrNull() ?: 1
                    val book = StandardWorksData.findBook(volumeId, bookId)
                    val loaded = if (book != null && assetStreamProvider != null) {
                        ScriptureAssetLoader.loadVerses(assetStreamProvider, volumeId, book.shortTitle, chapter).ifEmpty { null }
                    } else null
                    StandardWorksData.getChapter(volumeId, bookId, chapter, loaded)
                } else null
            }
            contentId.startsWith("bofm/") -> {
                val parts = contentId.split("/")
                if (parts.size == 3) {
                    val bookId = parts[1]
                    val chapter = parts[2].toIntOrNull() ?: 1
                    val book = StandardWorksData.findBook("bofm", bookId)
                    val loaded = if (book != null && assetStreamProvider != null) {
                        ScriptureAssetLoader.loadVerses(assetStreamProvider, "bofm", book.shortTitle, chapter).ifEmpty { null }
                    } else null
                    StandardWorksData.getChapter("bofm", bookId, chapter, loaded)
                } else null
            }
            contentId.startsWith("gc/") -> {
                val dbTalk = runBlocking {
                    database.conferenceTalkDao().getTalk(contentId)
                }
                if (dbTalk != null) {
                    val verses = dbTalk.paragraphs.split("\n\n")
                        .filter { it.isNotBlank() }
                        .mapIndexed { idx, txt -> ContentVerse(idx + 1, txt) }

                    ContentChapter(
                        id = dbTalk.talkId,
                        type = ContentType.GENERAL_CONFERENCE,
                        title = dbTalk.title,
                        subtitle = "${dbTalk.speaker} • ${dbTalk.periodName}",
                        verses = verses,
                        prevId = null,
                        nextId = null,
                    )
                } else {
                    GeneralConferenceData.getTalkContent(contentId)
                }
            }
            else -> null
        }
    }

    companion object {
        const val DATABASE_NAME = "gospel_library.db"

        @Volatile
        private var instance: GospelRepository? = null

        fun getInstance(
            dbProvider: () -> GospelDatabase,
            assetStreamProvider: ((String) -> InputStream?)? = null,
        ): GospelRepository {
            return instance ?: synchronized(this) {
                instance ?: GospelRepository(dbProvider(), assetStreamProvider).also { instance = it }
            }.apply {
                if (assetStreamProvider != null && this.assetStreamProvider == null) {
                    this.assetStreamProvider = assetStreamProvider
                }
            }
        }
    }
}
