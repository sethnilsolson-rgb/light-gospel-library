package com.thelightphone.gospellibrary.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contentId: String,
    val title: String,
    val snippet: String,
    val verseNumber: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contentId: String,
    val verseNumber: Int,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "last_read")
data class LastReadEntity(
    @PrimaryKey
    val recordId: String = "singleton",
    val contentId: String,
    val title: String,
    val subtitle: String?,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "conference_talks")
data class ConferenceTalkEntity(
    @PrimaryKey
    val talkId: String,
    val periodId: String,
    val periodName: String,
    val year: Int,
    val month: String,
    val sessionName: String,
    val sessionOrder: Int,
    val talkOrder: Int,
    val title: String,
    val speaker: String,
    val speakerTitle: String,
    val summary: String,
    val paragraphs: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "downloaded_languages")
data class DownloadedLanguageEntity(
    @PrimaryKey
    val langCode: String,
    val dictionaryDownloaded: Boolean = false,
    val dictionaryEntryCount: Int = 0,
    val conferenceDownloaded: Boolean = false,
    val scriptureChaptersCached: Int = 0,
    val isActive: Boolean = false,
    val downloadedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "translated_chapters")
data class TranslatedChapterEntity(
    @PrimaryKey
    val cacheKey: String, // "$langCode|$contentId"
    val langCode: String,
    val contentId: String,
    val title: String,
    val subtitle: String,
    val versesEncoded: String, // lines separated by \u001E, each "num\u001Ftext"
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "translated_conference_talks")
data class TranslatedConferenceTalkEntity(
    @PrimaryKey
    val compositeId: String, // "$langCode|$talkId"
    val langCode: String,
    val talkId: String,
    val periodId: String,
    val periodName: String,
    val year: Int,
    val month: String,
    val sessionName: String,
    val sessionOrder: Int,
    val talkOrder: Int,
    val title: String,
    val speaker: String,
    val speakerTitle: String,
    val summary: String,
    val paragraphs: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "language_dict_entries",
    indices = [Index(value = ["dictKey", "headword"])],
)
data class LanguageDictEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dictKey: String,
    val headword: String,
    val reading: String = "",
    val definition: String,
)
