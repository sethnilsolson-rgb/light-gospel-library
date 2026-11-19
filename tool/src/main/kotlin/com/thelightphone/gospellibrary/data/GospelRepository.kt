package com.thelightphone.gospellibrary.data

import com.thelightphone.gospellibrary.db.BookmarkEntity
import com.thelightphone.gospellibrary.db.ConferenceTalkEntity
import com.thelightphone.gospellibrary.db.GospelDatabase
import com.thelightphone.gospellibrary.db.LastReadEntity
import com.thelightphone.gospellibrary.model.ConferencePeriod
import com.thelightphone.gospellibrary.model.ContentChapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.io.InputStream

data class ConferenceSyncStatus(
    val message: String,
    val downloadedNew: Boolean,
)

class GospelRepository(
    private val database: GospelDatabase,
    var assetStreamProvider: ((String) -> InputStream?)? = null,
) {
    init {
        GeneralConferenceData.ensureBundledLoaded(assetStreamProvider)
    }

    val bookmarks: Flow<List<BookmarkEntity>> = database.bookmarkDao().getAllBookmarks()
    val bookmarkCount: Flow<Int> = database.bookmarkDao().getBookmarkCount()
    val lastRead: Flow<LastReadEntity?> = database.lastReadDao().getLastRead()
    val conferenceTalks: Flow<List<ConferenceTalkEntity>> = database.conferenceTalkDao().getAllTalks()

    fun ensureConferenceAssetLoaded() {
        GeneralConferenceData.ensureBundledLoaded(assetStreamProvider)
    }

    fun isChapterBookmarked(contentId: String): Flow<Boolean> =
        database.bookmarkDao().isChapterBookmarked(contentId)

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

    /**
     * Checks churchofjesuschrist.org for any new General Conference sessions released after
     * the ones already bundled or stored locally, downloads them in full, and saves to Room.
     */
    suspend fun syncConferenceIfOnline(): ConferenceSyncStatus {
        ensureConferenceAssetLoaded()
        val dbTalks = database.conferenceTalkDao().getAllTalksSync()
        val existingPeriodIds = buildSet {
            if (GeneralConferenceData.getBundledTalkEntities().isNotEmpty()) {
                add("2026-10")
            }
            dbTalks
                .groupBy { it.periodId }
                .filterValues { it.size >= 10 }
                .keys
                .forEach { add(it) }
        }

        val syncResult = ConferenceSyncService.fetchMissingConferences(existingPeriodIds)
        if (syncResult.downloadedTalks.isNotEmpty()) {
            database.conferenceTalkDao().insertTalks(syncResult.downloadedTalks)
            val names = syncResult.downloadedPeriods.joinToString(", ")
            return ConferenceSyncStatus(
                message = "Downloaded $names (${syncResult.downloadedTalks.size} talks)",
                downloadedNew = true,
            )
        }

        if (syncResult.hadNetworkError) {
            return ConferenceSyncStatus(
                message = "Unable to connect. Check internet and try again.",
                downloadedNew = false,
            )
        }

        val latestName = GeneralConferenceData.getConferencePeriods(dbTalks).firstOrNull()?.name ?: "October 2026"
        return ConferenceSyncStatus(
            message = "Up to date ($latestName)",
            downloadedNew = false,
        )
    }

    suspend fun getConferencePeriods(): List<ConferencePeriod> {
        ensureConferenceAssetLoaded()
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
                ensureConferenceAssetLoaded()
                val dbTalks = runBlocking {
                    database.conferenceTalkDao().getAllTalksSync()
                }
                GeneralConferenceData.getTalkContent(contentId, dbTalks)
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
                    ensureConferenceAssetLoaded()
                }
            }
        }
    }
}
