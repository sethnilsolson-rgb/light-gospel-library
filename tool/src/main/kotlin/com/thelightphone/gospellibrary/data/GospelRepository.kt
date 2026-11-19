package com.thelightphone.gospellibrary.data

import com.thelightphone.gospellibrary.db.BookmarkEntity
import com.thelightphone.gospellibrary.db.ConferenceTalkEntity
import com.thelightphone.gospellibrary.db.DownloadedLanguageEntity
import com.thelightphone.gospellibrary.db.GospelDatabase
import com.thelightphone.gospellibrary.db.LastReadEntity
import com.thelightphone.gospellibrary.model.ConferencePeriod
import com.thelightphone.gospellibrary.model.ConferenceSessionGroup
import com.thelightphone.gospellibrary.model.ConferenceTalk
import com.thelightphone.gospellibrary.model.ContentChapter
import com.thelightphone.gospellibrary.model.ContentVerse
import com.thelightphone.gospellibrary.model.GospelLanguage
import com.thelightphone.gospellibrary.model.WordLookupResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
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
    val downloadedLanguages: Flow<List<DownloadedLanguageEntity>> =
        database.languageDao().getDownloadedLanguages()

    val activeLanguageCode: Flow<String> = downloadedLanguages.map { list ->
        list.firstOrNull { it.isActive }?.langCode ?: GospelLanguage.ENG.code
    }

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

    // region Language & Offline Dictionary Management

    suspend fun getDownloadedLanguagesSync(): List<DownloadedLanguageEntity> = withContext(Dispatchers.IO) {
        database.languageDao().getDownloadedLanguagesSync()
    }

    suspend fun getActiveLanguageCodeSync(): String = withContext(Dispatchers.IO) {
        database.languageDao().getDownloadedLanguagesSync()
            .firstOrNull { it.isActive }?.langCode ?: GospelLanguage.ENG.code
    }

    suspend fun setActiveLanguage(langCode: String) = withContext(Dispatchers.IO) {
        val dao = database.languageDao()
        if (langCode == GospelLanguage.ENG.code && dao.getLanguageSync("eng") == null) {
            dao.upsertLanguage(DownloadedLanguageEntity(langCode = "eng", isActive = true))
        }
        dao.setActiveLanguage(langCode)
    }

    suspend fun isDictionaryReadyForLanguage(language: GospelLanguage): Boolean = withContext(Dispatchers.IO) {
        val langEntity = database.languageDao().getLanguageSync(language.code)
        if (language == GospelLanguage.ENG) {
            langEntity?.dictionaryDownloaded == true ||
                database.languageDictionaryDao().countEntries(language.dictKey) > 100
        } else {
            langEntity?.dictionaryDownloaded == true
        }
    }

    suspend fun downloadEnglish1828Dictionary(
        onProgress: (String) -> Unit,
    ): Result<Int> = withContext(Dispatchers.IO) {
        val result = LanguageDictionaryService.downloadOfflineDictionary(
            language = GospelLanguage.ENG,
            dao = database.languageDictionaryDao(),
            onProgress = onProgress,
        )
        result.onSuccess { count ->
            val existing = database.languageDao().getLanguageSync("eng")
            database.languageDao().upsertLanguage(
                (existing ?: DownloadedLanguageEntity(langCode = "eng", isActive = true)).copy(
                    dictionaryDownloaded = true,
                    dictionaryEntryCount = count,
                )
            )
        }
        result
    }

    suspend fun removeEnglish1828Dictionary() = withContext(Dispatchers.IO) {
        database.languageDictionaryDao().deleteDictionary(GospelLanguage.ENG.dictKey)
        val existing = database.languageDao().getLanguageSync("eng")
        if (existing != null) {
            database.languageDao().upsertLanguage(
                existing.copy(dictionaryDownloaded = false, dictionaryEntryCount = 0)
            )
        }
    }

    /**
     * Downloads a language pack:
     * 1. Downloads and indexes the language's offline word-lookup dictionary (e.g. CC-CEDICT for Chinese, EDICT2 for Japanese, FreeDict for Spanish/Portuguese/French).
     * 2. Downloads the complete October 2026 General Conference in that language.
     * 3. Registers the language so the Reader header shows the seamless language toggle button.
     */
    suspend fun downloadLanguagePack(
        language: GospelLanguage,
        onProgress: (String) -> Unit,
    ): Result<DownloadedLanguageEntity> = withContext(Dispatchers.IO) {
        runCatching {
            // Ensure English base row exists
            val langDao = database.languageDao()
            if (langDao.getLanguageSync("eng") == null) {
                langDao.upsertLanguage(DownloadedLanguageEntity(langCode = "eng", isActive = true))
            }

            // 1. Offline Dictionary
            val dictCount = LanguageDictionaryService.downloadOfflineDictionary(
                language = language,
                dao = database.languageDictionaryDao(),
                onProgress = onProgress,
            ).getOrDefault(0)

            // 2. General Conference (Oct 2026) in target language
            onProgress("Downloading October 2026 Conference (${language.nativeName})...")
            val confTalks = LanguageContentService.fetchFullConferenceInLanguage(
                langCode = language.code,
                year = 2026,
                monthNum = 10,
                onProgress = onProgress,
            ).getOrDefault(emptyList())

            if (confTalks.isNotEmpty()) {
                langDao.insertTranslatedConferenceTalks(confTalks)
            }

            val cachedChapters = langDao.countTranslatedChapters(language.code)
            val entity = DownloadedLanguageEntity(
                langCode = language.code,
                dictionaryDownloaded = true,
                dictionaryEntryCount = dictCount,
                conferenceDownloaded = confTalks.isNotEmpty(),
                scriptureChaptersCached = cachedChapters,
                isActive = false,
            )
            langDao.upsertLanguage(entity)
            onProgress("${language.englishName} ready!")
            entity
        }
    }

    /**
     * Downloads an entire Standard Works volume offline for [language] (e.g., Book of Mormon = 239 chapters).
     */
    suspend fun downloadVolumeOffline(
        language: GospelLanguage,
        volumeId: String,
        onProgress: (String) -> Unit,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val books = StandardWorksData.getBooksForVolume(volumeId)
            val allTargets = books.flatMap { book ->
                (1..book.chapterCount).map { ch -> book.id to ch }
            }
            val total = allTargets.size
            var completed = 0
            val semaphore = Semaphore(6)
            val langDao = database.languageDao()

            coroutineScope {
                allTargets.map { (bookId, ch) ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            val cacheKey = "${language.code}|scripture/$volumeId/$bookId/$ch"
                            if (langDao.getTranslatedChapter(cacheKey) == null) {
                                LanguageContentService.fetchScriptureChapter(
                                    langCode = language.code,
                                    volumeId = volumeId,
                                    bookId = bookId,
                                    chapterNum = ch,
                                ).onSuccess { entity ->
                                    langDao.upsertTranslatedChapter(entity)
                                }
                            }
                            completed++
                            if (completed % 5 == 0 || completed == total) {
                                onProgress("Downloading: $completed / $total chapters")
                            }
                        }
                    }
                }.awaitAll()
            }

            val totalCached = langDao.countTranslatedChapters(language.code)
            langDao.getLanguageSync(language.code)?.let { existing ->
                langDao.upsertLanguage(existing.copy(scriptureChaptersCached = totalCached))
            }
            onProgress("Offline download complete ($total chapters)")
            totalCached
        }
    }

    suspend fun removeLanguagePack(language: GospelLanguage) = withContext(Dispatchers.IO) {
        if (language == GospelLanguage.ENG) return@withContext
        val langDao = database.languageDao()
        val wasActive = langDao.getLanguageSync(language.code)?.isActive == true
        langDao.deleteLanguage(language.code)
        langDao.deleteTranslatedChaptersForLanguage(language.code)
        langDao.deleteTranslatedConferenceTalksForLanguage(language.code)

        // Only delete shared Chinese dictionary if NEITHER Traditional nor Simplified remains installed
        val remainingSharingDict = langDao.getDownloadedLanguagesSync().any {
            GospelLanguage.fromCode(it.langCode).dictKey == language.dictKey
        }
        if (!remainingSharingDict) {
            database.languageDictionaryDao().deleteDictionary(language.dictKey)
        }

        if (wasActive) {
            setActiveLanguage(GospelLanguage.ENG.code)
        }
    }

    suspend fun lookupWordInLanguage(
        text: String,
        charIndex: Int,
        langCode: String,
        contentId: String,
        verseNumber: Int?,
    ): WordLookupResult? = withContext(Dispatchers.IO) {
        val language = GospelLanguage.fromCode(langCode)
        val englishPreview = if (language != GospelLanguage.ENG && verseNumber != null && contentId.startsWith("scripture/")) {
            runCatching {
                getEnglishContent(contentId)?.verses?.firstOrNull { it.number == verseNumber }?.text
            }.getOrNull()
        } else null

        LanguageDictionaryService.lookupAtOffset(
            text = text,
            charIndex = charIndex,
            language = language,
            dao = database.languageDictionaryDao(),
            englishVersePreview = englishPreview,
        )
    }

    // endregion

    // region Conference & Content Loading

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

    suspend fun getConferencePeriods(langCode: String = GospelLanguage.ENG.code): List<ConferencePeriod> =
        withContext(Dispatchers.IO) {
            ensureConferenceAssetLoaded()
            if (langCode != GospelLanguage.ENG.code) {
                val translated = database.languageDao().getTranslatedConferenceTalksSync(langCode)
                if (translated.isNotEmpty()) {
                    return@withContext translated
                        .groupBy { it.periodId }
                        .entries
                        .sortedByDescending { it.key }
                        .map { (periodId, list) ->
                            val first = list.first()
                            val sessions = list
                                .groupBy { it.sessionOrder to it.sessionName }
                                .entries
                                .sortedBy { it.key.first }
                                .map { (_, sTalks) ->
                                    val sorted = sTalks.sortedBy { it.talkOrder }
                                    ConferenceSessionGroup(
                                        sessionName = sorted.first().sessionName,
                                        talks = sorted.map { t ->
                                            ConferenceTalk(
                                                id = t.talkId,
                                                title = t.title,
                                                speaker = t.speaker,
                                                speakerTitle = t.speakerTitle,
                                                session = t.sessionName,
                                                period = t.periodName,
                                                summary = t.summary,
                                            )
                                        }
                                    )
                                }
                            ConferencePeriod(
                                id = periodId,
                                name = first.periodName,
                                year = first.year,
                                month = first.month,
                                sessions = sessions,
                            )
                        }
                }
            }
            val dbTalks = database.conferenceTalkDao().getAllTalksSync()
            GeneralConferenceData.getConferencePeriods(dbTalks)
        }

    fun getContent(contentId: String): ContentChapter? = getEnglishContent(contentId)

    /**
     * Loads a scripture chapter or conference talk in [langCode].
     * English loads immediately from bundled assets/Room; other languages load from local
     * Room cache if available, or fetch & cache permanently from churchofjesuschrist.org.
     */
    suspend fun getContentInLanguage(contentId: String, langCode: String): ContentChapter? =
        withContext(Dispatchers.IO) {
            val baseEnglish = getEnglishContent(contentId) ?: return@withContext null
            if (langCode == GospelLanguage.ENG.code) {
                return@withContext baseEnglish.copy(langCode = GospelLanguage.ENG.code)
            }

            val langDao = database.languageDao()

            // 1. Check translated conference talks table first for GC talks
            if (contentId.startsWith("gc/")) {
                val translatedTalk = langDao.getTranslatedConferenceTalk("$langCode|$contentId")
                if (translatedTalk != null) {
                    val verses = translatedTalk.paragraphs
                        .split("\n\n")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .map { ContentVerse(number = null, text = it) }
                    return@withContext baseEnglish.copy(
                        title = translatedTalk.title,
                        subtitle = "${translatedTalk.speaker} • ${translatedTalk.periodName}",
                        verses = verses,
                        langCode = langCode,
                    )
                }
            }

            // 2. Check cached translated chapters table
            val cacheKey = "$langCode|$contentId"
            val cached = langDao.getTranslatedChapter(cacheKey)
            if (cached != null) {
                val verses = LanguageContentService.decodeVerses(cached.versesEncoded)
                if (verses.isNotEmpty()) {
                    return@withContext baseEnglish.copy(
                        title = cached.title,
                        subtitle = cached.subtitle.ifBlank { baseEnglish.subtitle },
                        verses = verses,
                        langCode = langCode,
                    )
                }
            }

            // 3. Fetch on demand from churchofjesuschrist.org and cache permanently for offline use
            val fetched = when {
                contentId.startsWith("scripture/") -> {
                    val parts = contentId.split("/")
                    if (parts.size == 4) {
                        LanguageContentService.fetchScriptureChapter(
                            langCode = langCode,
                            volumeId = parts[1],
                            bookId = parts[2],
                            chapterNum = parts[3].toIntOrNull() ?: 1,
                        ).getOrNull()
                    } else null
                }
                contentId.startsWith("bofm/") -> {
                    val parts = contentId.split("/")
                    if (parts.size == 3) {
                        LanguageContentService.fetchScriptureChapter(
                            langCode = langCode,
                            volumeId = "bofm",
                            bookId = parts[1],
                            chapterNum = parts[2].toIntOrNull() ?: 1,
                        ).getOrNull()
                    } else null
                }
                contentId.startsWith("gc/") -> {
                    LanguageContentService.fetchConferenceTalk(langCode, contentId).getOrNull()
                }
                else -> null
            }

            if (fetched != null) {
                langDao.upsertTranslatedChapter(fetched)
                val count = langDao.countTranslatedChapters(langCode)
                langDao.getLanguageSync(langCode)?.let { existing ->
                    langDao.upsertLanguage(existing.copy(scriptureChaptersCached = count))
                }
                val verses = LanguageContentService.decodeVerses(fetched.versesEncoded)
                if (verses.isNotEmpty()) {
                    return@withContext baseEnglish.copy(
                        title = fetched.title,
                        subtitle = fetched.subtitle.ifBlank { baseEnglish.subtitle },
                        verses = verses,
                        langCode = langCode,
                    )
                }
            }

            null
        }

    private fun getEnglishContent(contentId: String): ContentChapter? {
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

    // endregion

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
