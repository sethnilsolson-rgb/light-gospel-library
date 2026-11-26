package com.thelightphone.gospellibrary.data

import com.thelightphone.gospellibrary.db.BookmarkEntity
import com.thelightphone.gospellibrary.db.ConferenceTalkEntity
import com.thelightphone.gospellibrary.db.DownloadedLanguageEntity
import com.thelightphone.gospellibrary.db.GospelDatabase
import com.thelightphone.gospellibrary.db.LastReadEntity
import com.thelightphone.gospellibrary.db.NoteEntity
import com.thelightphone.gospellibrary.db.SyncedHymnEntity
import com.thelightphone.gospellibrary.model.ConferencePeriod
import com.thelightphone.gospellibrary.model.ConferenceSessionGroup
import com.thelightphone.gospellibrary.model.ConferenceTalk
import com.thelightphone.gospellibrary.model.ContentChapter
import com.thelightphone.gospellibrary.model.ContentVerse
import com.thelightphone.gospellibrary.model.GospelLanguage
import com.thelightphone.gospellibrary.model.HymnBook
import com.thelightphone.gospellibrary.model.HymnItem
import com.thelightphone.gospellibrary.model.ReaderFontSize
import com.thelightphone.gospellibrary.model.ReaderPageMode
import com.thelightphone.gospellibrary.model.WordLookupResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

data class ConferenceSyncStatus(
    val message: String,
    val downloadedNew: Boolean,
)

data class HymnSyncStatus(
    val message: String,
    val downloadedCount: Int,
)

class GospelRepository(
    private val database: GospelDatabase,
    var assetStreamProvider: ((String) -> InputStream?)? = null,
    private var appFilesDir: File? = null,
) {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefsLock = Any()
    private val userModifiedPageMode = AtomicBoolean(false)
    private val userModifiedFontSize = AtomicBoolean(false)

    private val _pageMode = MutableStateFlow(ReaderPageMode.SCROLL)
    val pageMode: StateFlow<ReaderPageMode> = _pageMode.asStateFlow()

    private val _fontSize = MutableStateFlow(ReaderFontSize.REGULAR)
    val fontSize: StateFlow<ReaderFontSize> = _fontSize.asStateFlow()

    init {
        ioScope.launch {
            GeneralConferenceData.ensureBundledLoaded(assetStreamProvider)
            HymnsData.ensureBundledLoaded(assetStreamProvider)
            loadReaderPrefsFromDisk()
        }
    }

    internal fun attachFilesDir(dir: File?) {
        if (dir != null && appFilesDir == null) {
            appFilesDir = dir
            ioScope.launch {
                loadReaderPrefsFromDisk()
            }
        }
    }

    private fun prefsFile(): File? = appFilesDir?.let { File(it, "reader_prefs.txt") }

    private fun loadReaderPrefsFromDisk() {
        val file = prefsFile() ?: return
        synchronized(prefsLock) {
            if (!file.isFile) return
            runCatching {
                file.readLines(Charsets.UTF_8).forEach { line ->
                    val idx = line.indexOf('=')
                    if (idx > 0) {
                        val key = line.substring(0, idx).trim()
                        val value = line.substring(idx + 1).trim()
                        when (key) {
                            "pageMode" -> if (!userModifiedPageMode.get()) {
                                _pageMode.value = ReaderPageMode.fromName(value)
                            }
                            "fontSize" -> if (!userModifiedFontSize.get()) {
                                _fontSize.value = ReaderFontSize.fromName(value)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveReaderPrefsToDisk(mode: ReaderPageMode, size: ReaderFontSize) {
        val file = prefsFile() ?: return
        synchronized(prefsLock) {
            runCatching {
                file.parentFile?.mkdirs()
                file.writeText(
                    "pageMode=${mode.name}\nfontSize=${size.name}\n",
                    Charsets.UTF_8,
                )
            }
        }
    }

    fun setPageMode(mode: ReaderPageMode) {
        userModifiedPageMode.set(true)
        _pageMode.value = mode
        val sizeSnapshot = _fontSize.value
        ioScope.launch { saveReaderPrefsToDisk(mode, sizeSnapshot) }
    }

    fun setFontSize(size: ReaderFontSize) {
        userModifiedFontSize.set(true)
        _fontSize.value = size
        val modeSnapshot = _pageMode.value
        ioScope.launch { saveReaderPrefsToDisk(modeSnapshot, size) }
    }

    val bookmarks: Flow<List<BookmarkEntity>> = database.bookmarkDao().getAllBookmarks()
    val bookmarkCount: Flow<Int> = database.bookmarkDao().getBookmarkCount()
    val allNotes: Flow<List<NoteEntity>> = database.noteDao().getAllNotes()
    val noteCount: Flow<Int> = database.noteDao().getNoteCount()
    val lastRead: Flow<LastReadEntity?> = database.lastReadDao().getLastRead()
    val conferenceTalks: Flow<List<ConferenceTalkEntity>> = database.conferenceTalkDao().getAllTalks()
    val syncedHymns: Flow<List<SyncedHymnEntity>> = database.hymnDao().getAllSyncedHymns()
    val downloadedLanguages: Flow<List<DownloadedLanguageEntity>> =
        database.languageDao().getDownloadedLanguages()

    val activeLanguageCode: Flow<String> = downloadedLanguages.map { list ->
        list.firstOrNull { it.isActive }?.langCode ?: GospelLanguage.ENG.code
    }

    fun ensureConferenceAssetLoaded() {
        GeneralConferenceData.ensureBundledLoaded(assetStreamProvider)
    }

    fun ensureHymnsLoaded() {
        HymnsData.ensureBundledLoaded(assetStreamProvider)
    }

    fun isChapterBookmarked(contentId: String): Flow<Boolean> =
        database.bookmarkDao().isChapterBookmarked(contentId)

    fun getBookmarksForContent(contentId: String): Flow<List<BookmarkEntity>> =
        database.bookmarkDao().getBookmarksForContent(contentId)

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
                    scrollPermille = 0,
                )
            )
        }
    }

    suspend fun toggleScrollBookmark(
        chapter: ContentChapter,
        scrollPermille: Int,
        visibleVerseNumber: Int?,
        visibleSnippet: String?,
        matchingBookmark: BookmarkEntity?,
    ) {
        if (matchingBookmark != null) {
            database.bookmarkDao().deleteBookmark(matchingBookmark)
        } else {
            val clamped = scrollPermille.coerceIn(0, 1000)
            val pct = ((clamped + 5) / 10).coerceIn(0, 100)
            val title = when {
                clamped > 15 && visibleVerseNumber != null -> "${chapter.title}:$visibleVerseNumber"
                clamped > 15 -> "${chapter.title} • $pct% in"
                else -> chapter.title
            }
            val snippet = visibleSnippet?.trim()?.take(90)?.ifBlank { null }
                ?: chapter.verses.firstOrNull()?.text?.trim()?.take(90)
                ?: ""
            database.bookmarkDao().insertBookmark(
                BookmarkEntity(
                    contentId = chapter.id,
                    title = title,
                    snippet = snippet,
                    verseNumber = if (clamped > 15) visibleVerseNumber else null,
                    scrollPermille = clamped,
                )
            )
        }
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) {
        database.bookmarkDao().deleteBookmark(bookmark)
    }

    fun getNotesForContent(contentId: String): Flow<List<NoteEntity>> =
        database.noteDao().getNotesForContent(contentId)

    suspend fun saveNote(note: NoteEntity): Long = withContext(Dispatchers.IO) {
        database.noteDao().insertNote(note)
    }

    suspend fun deleteNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        database.noteDao().deleteNote(note)
    }

    suspend fun deleteNoteById(id: Long) = withContext(Dispatchers.IO) {
        database.noteDao().deleteNoteById(id)
    }

    suspend fun cleanupOctober2024() {
        database.bookmarkDao().deleteOctober2024Bookmarks()
        database.highlightDao().deleteOctober2024Highlights()
        database.conferenceTalkDao().deleteOctober2024Talks()
    }

    suspend fun recordLastRead(chapter: ContentChapter, scrollPermille: Int = 0) {
        database.lastReadDao().saveLastRead(
            LastReadEntity(
                contentId = chapter.id,
                title = chapter.title,
                subtitle = chapter.subtitle,
                scrollPermille = scrollPermille.coerceIn(0, 1000),
            )
        )
    }

    suspend fun updateLastReadScroll(contentId: String, scrollPermille: Int) {
        database.lastReadDao().updateScrollPermille(
            contentId = contentId,
            scrollPermille = scrollPermille.coerceIn(0, 1000),
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
                LanguageDictionaryService.isDictionaryComplete(
                    language.dictKey,
                    database.languageDictionaryDao(),
                )
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
        try {
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
            ).getOrThrow()

            // 2. General Conference (Oct 2026) in target language
            onProgress("Downloading October 2026 Conference (${language.nativeName})...")
            val confTalks = LanguageContentService.fetchFullConferenceInLanguage(
                langCode = language.code,
                year = 2026,
                monthNum = 10,
                onProgress = onProgress,
            ).getOrThrow()

            if (confTalks.isEmpty()) {
                throw IOException("No conference talks downloaded for ${language.englishName}")
            }
            langDao.insertTranslatedConferenceTalks(confTalks)

            val cachedChapters = langDao.countTranslatedChapters(language.code)
            val entity = DownloadedLanguageEntity(
                langCode = language.code,
                dictionaryDownloaded = true,
                dictionaryEntryCount = dictCount,
                conferenceDownloaded = true,
                scriptureChaptersCached = cachedChapters,
                isActive = false,
            )
            langDao.upsertLanguage(entity)
            onProgress("${language.englishName} ready!")
            Result.success(entity)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
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
        try {
            val books = StandardWorksData.getBooksForVolume(volumeId)
            val allTargets = books.flatMap { book ->
                (1..book.chapterCount).map { ch -> book.id to ch }
            }
            val total = allTargets.size
            val completed = AtomicInteger(0)
            val failed = AtomicInteger(0)
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
                                }.onFailure {
                                    failed.incrementAndGet()
                                }
                            }
                            val done = completed.incrementAndGet()
                            if (done % 5 == 0 || done == total) {
                                onProgress("Downloading: $done / $total chapters")
                            }
                        }
                    }
                }.awaitAll()
            }

            val totalCached = langDao.countTranslatedChapters(language.code)
            langDao.getLanguageSync(language.code)?.let { existing ->
                langDao.upsertLanguage(existing.copy(scriptureChaptersCached = totalCached))
            }

            val failedCount = failed.get()
            if (failedCount > 0) {
                return@withContext Result.failure(
                    IOException("Failed to download $failedCount of $total chapters")
                )
            }

            onProgress("Offline download complete ($total chapters)")
            Result.success(totalCached)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
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
                .filterValues { it.size >= 30 }
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

    suspend fun getHymnsForBook(book: HymnBook): List<HymnItem> = withContext(Dispatchers.IO) {
        ensureHymnsLoaded()
        val synced = database.hymnDao().getAllSyncedHymnsSync()
        HymnsData.getHymnsForBook(book, synced)
    }

    suspend fun syncHomeAndChurchHymns(): HymnSyncStatus = withContext(Dispatchers.IO) {
        ensureHymnsLoaded()
        val currentSynced = database.hymnDao().getAllSyncedHymnsSync()
        val existingHymns = HymnsData.getHymnsForBook(HymnBook.HOME_AND_CHURCH, currentSynced)
        val existingSlugs = existingHymns.mapNotNull { it.slug.takeIf(String::isNotBlank) }.toSet()
        val existingNumbers = existingHymns.map { it.number }.toSet()

        val result = HymnSyncService.fetchNewHomeAndChurchHymns(
            existingSlugs = existingSlugs,
            existingNumbers = existingNumbers,
        )
        if (result.downloadedHymns.isNotEmpty()) {
            database.hymnDao().insertHymns(result.downloadedHymns)
            val total = existingHymns.size + result.downloadedHymns.size
            val n = result.downloadedHymns.size
            return@withContext HymnSyncStatus(
                message = "Downloaded $n new ${if (n == 1) "hymn" else "hymns"} ($total total)",
                downloadedCount = n,
            )
        }
        if (result.hadNetworkError) {
            return@withContext HymnSyncStatus(
                message = "Unable to connect. Check internet and try again.",
                downloadedCount = 0,
            )
        }
        HymnSyncStatus(
            message = "Up to date (${existingHymns.size} hymns)",
            downloadedCount = 0,
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

    suspend fun getContent(contentId: String): ContentChapter? = getEnglishContent(contentId)

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

            val canonicalId = if (contentId.startsWith("bofm/")) {
                "scripture/$contentId"
            } else {
                contentId
            }
            val langDao = database.languageDao()

            // 1. Check translated conference talks table first for GC talks
            if (canonicalId.startsWith("gc/")) {
                val translatedTalk = langDao.getTranslatedConferenceTalk("$langCode|$canonicalId")
                if (translatedTalk != null) {
                    val verses = translatedTalk.paragraphs
                        .split("\n\n")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .map { ContentVerse(number = null, text = it) }
                    val subtitle = buildString {
                        if (translatedTalk.speaker.isNotBlank()) append(translatedTalk.speaker)
                        if (translatedTalk.periodName.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append(translatedTalk.periodName)
                        }
                    }
                    return@withContext baseEnglish.copy(
                        title = translatedTalk.title,
                        subtitle = subtitle,
                        verses = verses,
                        langCode = langCode,
                    )
                }
            }

            // 2. Check cached translated chapters table
            val cacheKey = "$langCode|$canonicalId"
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
                canonicalId.startsWith("scripture/") -> {
                    val parts = canonicalId.split("/")
                    if (parts.size == 4) {
                        LanguageContentService.fetchScriptureChapter(
                            langCode = langCode,
                            volumeId = parts[1],
                            bookId = parts[2],
                            chapterNum = parts[3].toIntOrNull() ?: 1,
                        ).getOrNull()
                    } else null
                }
                canonicalId.startsWith("gc/") -> {
                    LanguageContentService.fetchConferenceTalk(langCode, canonicalId).getOrNull()
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

    private suspend fun getEnglishContent(contentId: String): ContentChapter? = withContext(Dispatchers.IO) {
        when {
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
                val dbTalks = database.conferenceTalkDao().getAllTalksSync()
                GeneralConferenceData.getTalkContent(contentId, dbTalks)
            }
            contentId.startsWith("hymn/") -> {
                ensureHymnsLoaded()
                val synced = database.hymnDao().getAllSyncedHymnsSync()
                HymnsData.getHymnContent(contentId, synced)
            }
            else -> null
        }
    }

    suspend fun searchGospel(
        query: String,
        scope: SearchScope = SearchScope.ALL,
        isCancelled: () -> Boolean = { false },
    ): GospelSearchResponse = withContext(Dispatchers.IO) {
        ensureConferenceAssetLoaded()
        ensureHymnsLoaded()
        val dbTalks = database.conferenceTalkDao().getAllTalksSync()
        val mergedById = LinkedHashMap<String, ConferenceTalkEntity>()
        GeneralConferenceData.getBundledTalkEntities().forEach { mergedById[it.talkId] = it }
        dbTalks
            .filter { it.periodId >= "2026-10" && !it.talkId.contains("2024-10") }
            .forEach { mergedById[it.talkId] = it }

        val syncedHymns = database.hymnDao().getAllSyncedHymnsSync()
        val allHymns = HymnsData.getAllMergedHymns(syncedHymns)

        GospelSearchEngine.search(
            rawQuery = query,
            scope = scope,
            assetStreamProvider = assetStreamProvider,
            conferenceTalks = mergedById.values.toList(),
            hymns = allHymns,
            isCancelled = isCancelled,
        )
    }

    // endregion

    companion object {
        const val DATABASE_NAME = "gospel_library.db"

        @Volatile
        private var instance: GospelRepository? = null

        fun getInstance(
            dbProvider: () -> GospelDatabase,
            assetStreamProvider: ((String) -> InputStream?)? = null,
            filesDir: File? = null,
        ): GospelRepository {
            return instance ?: synchronized(this) {
                instance ?: GospelRepository(dbProvider(), assetStreamProvider, filesDir).also { instance = it }
            }.apply {
                if (assetStreamProvider != null && this.assetStreamProvider == null) {
                    this.assetStreamProvider = assetStreamProvider
                    ioScope.launch {
                        ensureConferenceAssetLoaded()
                        ensureHymnsLoaded()
                    }
                }
                if (filesDir != null) {
                    attachFilesDir(filesDir)
                }
            }
        }
    }
}
