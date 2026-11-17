package com.thelightphone.gospellibrary.model

enum class ContentType {
    SCRIPTURE,
    GENERAL_CONFERENCE,
}

data class ScriptureVolume(
    val id: String,
    val title: String,
    val shortTitle: String,
    val bookCount: Int,
    val description: String,
)

data class ScriptureBook(
    val id: String,
    val volumeId: String,
    val title: String,
    val shortTitle: String,
    val chapterCount: Int,
    val summary: String,
)

data class ConferenceTalk(
    val id: String,
    val title: String,
    val speaker: String,
    val speakerTitle: String,
    val session: String,
    val period: String,
    val summary: String,
)

data class ConferenceSessionGroup(
    val sessionName: String,
    val talks: List<ConferenceTalk>,
)

data class ConferencePeriod(
    val id: String,
    val name: String,
    val year: Int,
    val month: String,
    val sessions: List<ConferenceSessionGroup>,
)

data class ContentVerse(
    val number: Int?,
    val text: String,
)

data class ContentChapter(
    val id: String,
    val type: ContentType,
    val title: String,
    val subtitle: String?,
    val verses: List<ContentVerse>,
    val prevId: String? = null,
    val nextId: String? = null,
    val nextLabel: String? = null,
    val prevLabel: String? = null,
)

enum class ReaderFontSize(val label: String, val displayLabel: String) {
    COMPACT("Small", "Aa-"),
    REGULAR("Normal", "Aa"),
    LARGE("Large", "Aa+"),
}
