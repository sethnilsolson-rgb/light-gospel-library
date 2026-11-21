package com.thelightphone.gospellibrary.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE contentId = :contentId ORDER BY createdAt DESC")
    fun getBookmarksForContent(contentId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE contentId = :contentId AND verseNumber IS NULL)")
    fun isChapterBookmarked(contentId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE contentId = :contentId AND verseNumber = :verseNumber)")
    fun isVerseBookmarked(contentId: String, verseNumber: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE contentId = :contentId AND verseNumber IS NULL")
    suspend fun deleteChapterBookmark(contentId: String)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("SELECT COUNT(*) FROM bookmarks")
    fun getBookmarkCount(): Flow<Int>

    @Query("DELETE FROM bookmarks WHERE contentId LIKE '%2024-10%'")
    suspend fun deleteOctober2024Bookmarks()
}

@Dao
interface HighlightDao {
    @Query("SELECT verseNumber FROM highlights WHERE contentId = :contentId")
    fun getHighlightedVerseNumbers(contentId: String): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity): Long

    @Query("DELETE FROM highlights WHERE contentId = :contentId AND verseNumber = :verseNumber")
    suspend fun removeHighlight(contentId: String, verseNumber: Int)

    @Query("DELETE FROM highlights WHERE contentId LIKE '%2024-10%'")
    suspend fun deleteOctober2024Highlights()
}

@Dao
interface LastReadDao {
    @Query("SELECT * FROM last_read WHERE recordId = 'singleton' LIMIT 1")
    fun getLastRead(): Flow<LastReadEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLastRead(entity: LastReadEntity)

    @Query("UPDATE last_read SET scrollPermille = :scrollPermille, updatedAt = :updatedAt WHERE recordId = 'singleton' AND contentId = :contentId")
    suspend fun updateScrollPermille(contentId: String, scrollPermille: Int, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface ConferenceTalkDao {
    @Query("SELECT * FROM conference_talks ORDER BY year DESC, sessionOrder ASC, talkOrder ASC")
    fun getAllTalks(): Flow<List<ConferenceTalkEntity>>

    @Query("SELECT * FROM conference_talks WHERE talkId = :talkId LIMIT 1")
    suspend fun getTalk(talkId: String): ConferenceTalkEntity?

    @Query("SELECT * FROM conference_talks ORDER BY year DESC, sessionOrder ASC, talkOrder ASC")
    suspend fun getAllTalksSync(): List<ConferenceTalkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTalks(talks: List<ConferenceTalkEntity>)

    @Query("SELECT COUNT(*) FROM conference_talks")
    suspend fun getCount(): Int

    @Query("DELETE FROM conference_talks WHERE periodId = '2024-10' OR talkId LIKE '%2024-10%'")
    suspend fun deleteOctober2024Talks()
}

@Dao
interface LanguageDao {
    @Query("SELECT * FROM downloaded_languages ORDER BY downloadedAt ASC")
    fun getDownloadedLanguages(): Flow<List<DownloadedLanguageEntity>>

    @Query("SELECT * FROM downloaded_languages ORDER BY downloadedAt ASC")
    suspend fun getDownloadedLanguagesSync(): List<DownloadedLanguageEntity>

    @Query("SELECT * FROM downloaded_languages WHERE langCode = :langCode LIMIT 1")
    suspend fun getLanguageSync(langCode: String): DownloadedLanguageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLanguage(entity: DownloadedLanguageEntity)

    @Query("UPDATE downloaded_languages SET isActive = (langCode = :langCode)")
    suspend fun setActiveLanguage(langCode: String)

    @Query("DELETE FROM downloaded_languages WHERE langCode = :langCode")
    suspend fun deleteLanguage(langCode: String)

    // Translated chapters cache
    @Query("SELECT * FROM translated_chapters WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getTranslatedChapter(cacheKey: String): TranslatedChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTranslatedChapter(chapter: TranslatedChapterEntity)

    @Query("SELECT COUNT(*) FROM translated_chapters WHERE langCode = :langCode")
    suspend fun countTranslatedChapters(langCode: String): Int

    @Query("DELETE FROM translated_chapters WHERE langCode = :langCode")
    suspend fun deleteTranslatedChaptersForLanguage(langCode: String)

    // Translated conference talks cache
    @Query("SELECT * FROM translated_conference_talks WHERE langCode = :langCode ORDER BY year DESC, sessionOrder ASC, talkOrder ASC")
    fun getTranslatedConferenceTalks(langCode: String): Flow<List<TranslatedConferenceTalkEntity>>

    @Query("SELECT * FROM translated_conference_talks WHERE langCode = :langCode ORDER BY year DESC, sessionOrder ASC, talkOrder ASC")
    suspend fun getTranslatedConferenceTalksSync(langCode: String): List<TranslatedConferenceTalkEntity>

    @Query("SELECT * FROM translated_conference_talks WHERE compositeId = :compositeId LIMIT 1")
    suspend fun getTranslatedConferenceTalk(compositeId: String): TranslatedConferenceTalkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslatedConferenceTalks(talks: List<TranslatedConferenceTalkEntity>)

    @Query("DELETE FROM translated_conference_talks WHERE langCode = :langCode")
    suspend fun deleteTranslatedConferenceTalksForLanguage(langCode: String)
}

@Dao
interface LanguageDictionaryDao {
    @Query("SELECT * FROM language_dict_entries WHERE dictKey = :dictKey AND headword = :headword LIMIT 5")
    suspend fun lookupExact(dictKey: String, headword: String): List<LanguageDictEntryEntity>

    @Query("SELECT COUNT(*) FROM language_dict_entries WHERE dictKey = :dictKey")
    suspend fun countEntries(dictKey: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntries(entries: List<LanguageDictEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LanguageDictEntryEntity)

    @Query("DELETE FROM language_dict_entries WHERE dictKey = :dictKey")
    suspend fun deleteDictionary(dictKey: String)
}
