package com.thelightphone.gospellibrary.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BookmarkEntity::class,
        HighlightEntity::class,
        LastReadEntity::class,
        ConferenceTalkEntity::class,
        DownloadedLanguageEntity::class,
        TranslatedChapterEntity::class,
        TranslatedConferenceTalkEntity::class,
        LanguageDictEntryEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class GospelDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun highlightDao(): HighlightDao
    abstract fun lastReadDao(): LastReadDao
    abstract fun conferenceTalkDao(): ConferenceTalkDao
    abstract fun languageDao(): LanguageDao
    abstract fun languageDictionaryDao(): LanguageDictionaryDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `downloaded_languages` (
                        `langCode` TEXT NOT NULL,
                        `dictionaryDownloaded` INTEGER NOT NULL,
                        `dictionaryEntryCount` INTEGER NOT NULL,
                        `conferenceDownloaded` INTEGER NOT NULL,
                        `scriptureChaptersCached` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `downloadedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`langCode`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `translated_chapters` (
                        `cacheKey` TEXT NOT NULL,
                        `langCode` TEXT NOT NULL,
                        `contentId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subtitle` TEXT NOT NULL,
                        `versesEncoded` TEXT NOT NULL,
                        `cachedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`cacheKey`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `translated_conference_talks` (
                        `compositeId` TEXT NOT NULL,
                        `langCode` TEXT NOT NULL,
                        `talkId` TEXT NOT NULL,
                        `periodId` TEXT NOT NULL,
                        `periodName` TEXT NOT NULL,
                        `year` INTEGER NOT NULL,
                        `month` TEXT NOT NULL,
                        `sessionName` TEXT NOT NULL,
                        `sessionOrder` INTEGER NOT NULL,
                        `talkOrder` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `speaker` TEXT NOT NULL,
                        `speakerTitle` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `paragraphs` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`compositeId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `language_dict_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dictKey` TEXT NOT NULL,
                        `headword` TEXT NOT NULL,
                        `reading` TEXT NOT NULL,
                        `definition` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_language_dict_entries_dictKey_headword`
                    ON `language_dict_entries` (`dictKey`, `headword`)
                    """.trimIndent()
                )
            }
        }
    }
}
