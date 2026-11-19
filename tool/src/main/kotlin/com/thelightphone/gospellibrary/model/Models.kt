package com.thelightphone.gospellibrary.model

enum class ContentType {
    SCRIPTURE,
    GENERAL_CONFERENCE,
}

enum class GospelLanguage(
    val code: String,
    val shortBadge: String,
    val englishName: String,
    val nativeName: String,
    val dictKey: String,
    val dictLabel: String,
    val iso2: String,
    val isCjkNoSpaces: Boolean = false,
) {
    ENG(
        code = "eng",
        shortBadge = "EN",
        englishName = "English",
        nativeName = "English",
        dictKey = "eng_1828",
        dictLabel = "Webster's 1828 Dictionary",
        iso2 = "en",
    ),
    ZHO(
        code = "zho",
        shortBadge = "繁",
        englishName = "Chinese (Traditional)",
        nativeName = "中文 (繁體)",
        dictKey = "zh",
        dictLabel = "CC-CEDICT Chinese–English",
        iso2 = "zh-TW",
        isCjkNoSpaces = true,
    ),
    ZHS(
        code = "zhs",
        shortBadge = "简",
        englishName = "Chinese (Simplified)",
        nativeName = "中文 (简体)",
        dictKey = "zh",
        dictLabel = "CC-CEDICT Chinese–English",
        iso2 = "zh-CN",
        isCjkNoSpaces = true,
    ),
    SPA(
        code = "spa",
        shortBadge = "ES",
        englishName = "Spanish",
        nativeName = "Español",
        dictKey = "spa",
        dictLabel = "Spanish–English Dictionary",
        iso2 = "es",
    ),
    POR(
        code = "por",
        shortBadge = "PT",
        englishName = "Portuguese",
        nativeName = "Português",
        dictKey = "por",
        dictLabel = "Portuguese–English Dictionary",
        iso2 = "pt",
    ),
    JPN(
        code = "jpn",
        shortBadge = "日",
        englishName = "Japanese",
        nativeName = "日本語",
        dictKey = "jpn",
        dictLabel = "EDICT2 Japanese–English",
        iso2 = "ja",
        isCjkNoSpaces = true,
    ),
    KOR(
        code = "kor",
        shortBadge = "한",
        englishName = "Korean",
        nativeName = "한국어",
        dictKey = "kor",
        dictLabel = "Korean–English Lookup",
        iso2 = "ko",
    ),
    FRA(
        code = "fra",
        shortBadge = "FR",
        englishName = "French",
        nativeName = "Français",
        dictKey = "fra",
        dictLabel = "French–English Dictionary",
        iso2 = "fr",
    ),
    DEU(
        code = "deu",
        shortBadge = "DE",
        englishName = "German",
        nativeName = "Deutsch",
        dictKey = "deu",
        dictLabel = "German–English Lookup",
        iso2 = "de",
    ),
    TGL(
        code = "tgl",
        shortBadge = "TL",
        englishName = "Tagalog",
        nativeName = "Tagalog",
        dictKey = "tgl",
        dictLabel = "Tagalog–English Lookup",
        iso2 = "tl",
    ),
    ITA(
        code = "ita",
        shortBadge = "IT",
        englishName = "Italian",
        nativeName = "Italiano",
        dictKey = "ita",
        dictLabel = "Italian–English Lookup",
        iso2 = "it",
    ),
    RUS(
        code = "rus",
        shortBadge = "RU",
        englishName = "Russian",
        nativeName = "Русский",
        dictKey = "rus",
        dictLabel = "Russian–English Lookup",
        iso2 = "ru",
    );

    companion object {
        fun fromCode(code: String?): GospelLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENG

        val downloadableLanguages: List<GospelLanguage>
            get() = entries.filter { it != ENG }
    }
}

data class WordLookupMatch(
    val headword: String,
    val reading: String = "",
    val definition: String,
)

data class WordLookupResult(
    val query: String,
    val headword: String,
    val reading: String = "",
    val definition: String,
    val sourceLabel: String,
    val characterBreakdown: WordLookupMatch? = null,
    val englishVersePreview: String? = null,
    val notFound: Boolean = false,
)

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
    val langCode: String = "eng",
)

enum class ReaderFontSize(val label: String, val displayLabel: String) {
    COMPACT("Small", "Aa-"),
    REGULAR("Normal", "Aa"),
    LARGE("Large", "Aa+"),
}
