package com.thelightphone.gospellibrary.db

import androidx.room.Entity
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
