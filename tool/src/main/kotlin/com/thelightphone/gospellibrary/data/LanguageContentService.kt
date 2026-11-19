package com.thelightphone.gospellibrary.data

import com.thelightphone.gospellibrary.db.TranslatedChapterEntity
import com.thelightphone.gospellibrary.db.TranslatedConferenceTalkEntity
import com.thelightphone.gospellibrary.model.ContentVerse
import com.thelightphone.gospellibrary.model.GospelLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI

object LanguageContentService {

    private const val RECORD_SEP = "\u001E"
    private const val FIELD_SEP = "\u001F"

    fun buildChurchOrgScriptureUri(volumeId: String, bookId: String, chapterNum: Int): String {
        if (volumeId == "dc") {
            return "/scriptures/dc-testament/dc/$chapterNum"
        }
        val mappedBook = when (bookId) {
            "moroni" -> "moro"
            else -> bookId
        }
        return "/scriptures/$volumeId/$mappedBook/$chapterNum"
    }

    fun encodeVerses(verses: List<ContentVerse>): String {
        return verses.joinToString(RECORD_SEP) { v ->
            val numStr = v.number?.toString() ?: ""
            "$numStr$FIELD_SEP${v.text}"
        }
    }

    fun decodeVerses(encoded: String): List<ContentVerse> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split(RECORD_SEP).mapNotNull { record ->
            val idx = record.indexOf(FIELD_SEP)
            if (idx == -1) return@mapNotNull null
            val num = record.substring(0, idx).toIntOrNull()
            val text = record.substring(idx + 1)
            if (text.isBlank()) null else ContentVerse(num, text)
        }
    }

    suspend fun fetchScriptureChapter(
        langCode: String,
        volumeId: String,
        bookId: String,
        chapterNum: Int,
    ): Result<TranslatedChapterEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val uriPath = buildChurchOrgScriptureUri(volumeId, bookId, chapterNum)
            val apiJson = fetchApiPage(langCode, uriPath)
            val body = ConferenceSyncService.extractContentBody(apiJson)
            val metaTitle = ConferenceSyncService.extractMetaTitle(apiJson)
            val contentId = "scripture/$volumeId/$bookId/$chapterNum"
            parseScriptureChapterHtml(
                langCode = langCode,
                contentId = contentId,
                htmlBody = body,
                fallbackTitle = metaTitle,
            ) ?: error("No verses parsed for $uriPath ($langCode)")
        }
    }

    suspend fun fetchConferenceTalk(
        langCode: String,
        talkId: String,
    ): Result<TranslatedChapterEntity> = withContext(Dispatchers.IO) {
        runCatching {
            // talkId format: "gc/2026-10/12gong"
            val parts = talkId.split("/")
            require(parts.size == 3 && parts[0] == "gc") { "Invalid talkId: $talkId" }
            val periodParts = parts[1].split("-")
            require(periodParts.size == 2) { "Invalid period in talkId: $talkId" }
            val year = periodParts[0].toInt()
            val monthStr = periodParts[1]
            val slug = parts[2]
            val talkUri = "/general-conference/$year/$monthStr/$slug"

            val apiJson = fetchApiPage(langCode, talkUri)
            val body = ConferenceSyncService.extractContentBody(apiJson)
            val metaTitle = ConferenceSyncService.extractMetaTitle(apiJson)

            val parsed = ConferenceSyncService.parseChurchOrgTalkHtml(
                talkUri = talkUri,
                htmlBody = body,
                fallbackTitle = metaTitle,
                periodId = parts[1],
                periodName = parts[1],
                year = year,
                monthName = if (monthStr == "04") "April" else "October",
                sessionName = "",
                sessionOrder = 0,
                talkOrder = 0,
            ) ?: error("Could not parse talk $talkId in $langCode")

            val verses = parsed.paragraphs
                .split("\n\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { ContentVerse(number = null, text = it) }

            val lang = GospelLanguage.fromCode(langCode)
            val subtitle = buildString {
                if (parsed.speaker.isNotBlank()) append(parsed.speaker)
                if (isNotEmpty()) append(" • ")
                append(lang.nativeName)
            }

            TranslatedChapterEntity(
                cacheKey = "$langCode|$talkId",
                langCode = langCode,
                contentId = talkId,
                title = parsed.title,
                subtitle = subtitle,
                versesEncoded = encodeVerses(verses),
            )
        }
    }

    suspend fun fetchFullConferenceInLanguage(
        langCode: String,
        year: Int = 2026,
        monthNum: Int = 10,
        onProgress: ((String) -> Unit)? = null,
    ): Result<List<TranslatedConferenceTalkEntity>> = withContext(Dispatchers.IO) {
        runCatching {
            val monthStr = "%02d".format(monthNum)
            val periodId = "$year-$monthStr"
            val manifestJson = fetchApiPage(langCode, "/general-conference/$year/$monthStr")
            val manifestBody = ConferenceSyncService.extractContentBody(manifestJson)
            val localizedPeriodName = ConferenceSyncService.extractMetaTitle(manifestJson)
                .ifBlank { "${if (monthNum == 4) "April" else "October"} $year" }

            val sessions = ConferenceSyncService.parseChurchOrgManifest(manifestBody, year, monthStr)
            val total = sessions.sumOf { it.talkUris.size }
            var completed = 0
            val semaphore = Semaphore(4)
            val results = mutableListOf<TranslatedConferenceTalkEntity>()

            sessions.forEachIndexed { sIdx, session ->
                val talks = coroutineScope {
                    session.talkUris.mapIndexed { tIdx, talkUri ->
                        async(Dispatchers.IO) {
                            semaphore.withPermit {
                                runCatching {
                                    val talkJson = fetchApiPage(langCode, talkUri)
                                    val talkBody = ConferenceSyncService.extractContentBody(talkJson)
                                    val metaTitle = ConferenceSyncService.extractMetaTitle(talkJson)
                                    val base = ConferenceSyncService.parseChurchOrgTalkHtml(
                                        talkUri = talkUri,
                                        htmlBody = talkBody,
                                        fallbackTitle = metaTitle,
                                        periodId = periodId,
                                        periodName = localizedPeriodName,
                                        year = year,
                                        monthName = if (monthNum == 4) "April" else "October",
                                        sessionName = session.sessionName,
                                        sessionOrder = sIdx,
                                        talkOrder = tIdx,
                                    )
                                    completed++
                                    onProgress?.invoke("Conference talks: $completed / $total")
                                    base?.let { b ->
                                        TranslatedConferenceTalkEntity(
                                            compositeId = "$langCode|${b.talkId}",
                                            langCode = langCode,
                                            talkId = b.talkId,
                                            periodId = b.periodId,
                                            periodName = b.periodName,
                                            year = b.year,
                                            month = b.month,
                                            sessionName = b.sessionName,
                                            sessionOrder = b.sessionOrder,
                                            talkOrder = b.talkOrder,
                                            title = b.title,
                                            speaker = b.speaker,
                                            speakerTitle = b.speakerTitle,
                                            summary = b.summary,
                                            paragraphs = b.paragraphs,
                                        )
                                    }
                                }.getOrNull()
                            }
                        }
                    }.awaitAll().filterNotNull()
                }
                results.addAll(talks)
            }
            results
        }
    }

    internal fun parseScriptureChapterHtml(
        langCode: String,
        contentId: String,
        htmlBody: String,
        fallbackTitle: String,
    ): TranslatedChapterEntity? {
        val bodyBlockStart = htmlBody.indexOf("""<div class="body-block">""")
        val bodyHtml = if (bodyBlockStart != -1) {
            val sub = htmlBody.substring(bodyBlockStart)
            val footerIdx = sub.indexOf("<footer")
            if (footerIdx != -1) sub.substring(0, footerIdx) else sub
        } else {
            htmlBody
        }

        val verseRegex = Regex(
            """<p[^>]*class="[^"]*verse[^"]*"[^>]*>(.*?)</p>""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        val verseNumSpanRegex = Regex(
            """<span[^>]*class="[^"]*verse-number[^"]*"[^>]*>(.*?)</span>""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )

        val verses = mutableListOf<ContentVerse>()
        var sequentialNum = 1
        verseRegex.findAll(bodyHtml).forEach { match ->
            val inner = match.groupValues[1]
            val numMatch = verseNumSpanRegex.find(inner)
            val parsedNum = numMatch?.groupValues?.getOrNull(1)
                ?.let { ConferenceSyncService.cleanHtmlText(it) }
                ?.filter { it.isDigit() }
                ?.toIntOrNull()
                ?: sequentialNum

            val withoutNum = if (numMatch != null) {
                inner.removeRange(numMatch.range)
            } else {
                inner
            }
            val cleanedText = ConferenceSyncService.cleanHtmlText(withoutNum)
            if (cleanedText.isNotBlank()) {
                verses.add(ContentVerse(number = parsedNum, text = cleanedText))
                sequentialNum = parsedNum + 1
            }
        }

        if (verses.isEmpty()) return null

        val lang = GospelLanguage.fromCode(langCode)
        val h1Title = Regex("""<h1[^>]*>(.*?)</h1>""", setOf(RegexOption.DOT_MATCHES_ALL))
            .find(htmlBody)?.groupValues?.getOrNull(1)
            ?.let { ConferenceSyncService.cleanHtmlText(it) }
            .orEmpty()
        val cleanTitle = fallbackTitle.replace('\u00A0', ' ').trim()
            .ifBlank { h1Title }
            .ifBlank { contentId }

        return TranslatedChapterEntity(
            cacheKey = "$langCode|$contentId",
            langCode = langCode,
            contentId = contentId,
            title = cleanTitle,
            subtitle = lang.nativeName,
            versesEncoded = encodeVerses(verses),
        )
    }

    private fun fetchApiPage(langCode: String, uriPath: String): String {
        val fullUrl = "https://www.churchofjesuschrist.org/study/api/v3/language-pages/type/content?lang=$langCode&uri=$uriPath"
        var connection: HttpURLConnection? = null
        try {
            val url = URI(fullUrl).toURL()
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 15000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (LightPhone GospelLibrary/1.0)")
            }
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                error("HTTP $code for $uriPath ($langCode)")
            }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection?.disconnect()
        }
    }
}
