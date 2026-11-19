package com.thelightphone.gospellibrary.data

import com.thelightphone.gospellibrary.db.ConferenceTalkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URI
import java.util.Calendar

@Serializable
data class RemoteConferenceResponse(
    val periods: List<RemotePeriod> = emptyList(),
)

@Serializable
data class RemotePeriod(
    val id: String = "",
    val name: String = "",
    val year: Int = 2026,
    val month: String = "",
    val sessions: List<RemoteSession> = emptyList(),
)

@Serializable
data class RemoteSession(
    val sessionName: String = "",
    val talks: List<RemoteTalk> = emptyList(),
)

@Serializable
data class RemoteTalk(
    val id: String = "",
    val title: String = "",
    val speaker: String = "",
    val speakerTitle: String = "",
    val summary: String = "",
    val paragraphs: List<String> = emptyList(),
    val content: String = "",
)

data class ParsedSessionManifest(
    val sessionName: String,
    val talkUris: List<String>,
)

data class ConferenceSyncResult(
    val downloadedTalks: List<ConferenceTalkEntity>,
    val downloadedPeriods: List<String>,
    val hadNetworkError: Boolean,
)

object ConferenceSyncService {

    private const val CHURCH_API_BASE =
        "https://www.churchofjesuschrist.org/study/api/v3/language-pages/type/content?lang=eng&uri="

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Returns all conference periods (year, monthNum) from October 2026 up to the current date.
     * General Conference takes place in April (4) and October (10) each year.
     */
    fun getCandidateConferencePeriods(
        currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
        currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    ): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        var y = 2026
        var m = 10
        while (y < currentYear || (y == currentYear && m <= currentMonth)) {
            result.add(y to m)
            if (m == 4) {
                m = 10
            } else {
                y += 1
                m = 4
            }
        }
        return result
    }

    /**
     * Checks churchofjesuschrist.org for any new General Conference periods (April or October)
     * from October 2026 onward that are not already in [existingPeriodIds].
     */
    suspend fun fetchMissingConferences(
        existingPeriodIds: Set<String>,
    ): ConferenceSyncResult = withContext(Dispatchers.IO) {
        val candidates = getCandidateConferencePeriods()
            .filter { (year, month) ->
                val periodId = "$year-${"%02d".format(month)}"
                periodId !in existingPeriodIds
            }

        if (candidates.isEmpty()) {
            return@withContext ConferenceSyncResult(
                downloadedTalks = emptyList(),
                downloadedPeriods = emptyList(),
                hadNetworkError = false,
            )
        }

        val allDownloaded = mutableListOf<ConferenceTalkEntity>()
        val downloadedNames = mutableListOf<String>()
        var anyNetworkFailure = false

        for ((year, month) in candidates) {
            val result = fetchConferenceFromChurchOrg(year, month)
            result.onSuccess { talks ->
                if (talks.isNotEmpty()) {
                    allDownloaded.addAll(talks)
                    val monthName = if (month == 4) "April" else "October"
                    downloadedNames.add("$monthName $year")
                }
            }.onFailure {
                anyNetworkFailure = true
            }
        }

        ConferenceSyncResult(
            downloadedTalks = allDownloaded,
            downloadedPeriods = downloadedNames,
            hadNetworkError = anyNetworkFailure && allDownloaded.isEmpty(),
        )
    }

    /**
     * Fetches an entire General Conference (all sessions and full talk text) directly from
     * churchofjesuschrist.org's study content API.
     */
    suspend fun fetchConferenceFromChurchOrg(
        year: Int,
        monthNum: Int,
    ): Result<List<ConferenceTalkEntity>> = withContext(Dispatchers.IO) {
        runCatching {
            val monthStr = "%02d".format(monthNum)
            val monthName = if (monthNum == 4) "April" else "October"
            val periodId = "$year-$monthStr"
            val periodName = "$monthName $year"

            val manifestJson = fetchApiPage("/general-conference/$year/$monthStr")
            val manifestBody = extractContentBody(manifestJson)
            if (manifestBody.isBlank()) {
                return@runCatching emptyList()
            }

            val sessions = parseChurchOrgManifest(manifestBody, year, monthStr)
            if (sessions.isEmpty()) {
                return@runCatching emptyList()
            }

            val semaphore = Semaphore(4)
            val entities = mutableListOf<ConferenceTalkEntity>()

            sessions.forEachIndexed { sIdx, session ->
                val talkEntities = coroutineScope {
                    session.talkUris.mapIndexed { tIdx, talkUri ->
                        async(Dispatchers.IO) {
                            semaphore.withPermit {
                                runCatching {
                                    val talkJson = fetchApiPage(talkUri)
                                    val talkBody = extractContentBody(talkJson)
                                    val metaTitle = extractMetaTitle(talkJson)
                                    parseChurchOrgTalkHtml(
                                        talkUri = talkUri,
                                        htmlBody = talkBody,
                                        fallbackTitle = metaTitle,
                                        periodId = periodId,
                                        periodName = periodName,
                                        year = year,
                                        monthName = monthName,
                                        sessionName = session.sessionName,
                                        sessionOrder = sIdx,
                                        talkOrder = tIdx,
                                    )
                                }.getOrNull()
                            }
                        }
                    }.awaitAll().filterNotNull()
                }
                entities.addAll(talkEntities)
            }

            entities
        }
    }

    private fun fetchApiPage(uriPath: String): String {
        val fullUrl = "$CHURCH_API_BASE$uriPath"
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
                error("HTTP $code for $uriPath")
            }

            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection?.disconnect()
        }
    }

    internal fun extractContentBody(apiJson: String): String {
        val root = json.parseToJsonElement(apiJson).jsonObject
        val content = root["content"]?.jsonObject ?: return ""
        return content["body"]?.jsonPrimitive?.content ?: ""
    }

    internal fun extractMetaTitle(apiJson: String): String {
        val root = json.parseToJsonElement(apiJson).jsonObject
        val meta = root["meta"]?.jsonObject ?: return ""
        return meta["title"]?.jsonPrimitive?.content?.trim() ?: ""
    }

    internal fun parseChurchOrgManifest(
        manifestHtml: String,
        year: Int,
        monthStr: String,
    ): List<ParsedSessionManifest> {
        val sessionSplitRegex = Regex("""<li[^>]*data-content-type="general-conference-session"[^>]*>""")
        val chunks = sessionSplitRegex.split(manifestHtml).drop(1)
        val sessions = mutableListOf<ParsedSessionManifest>()

        val sessionTitleRegex = Regex(
            """<h2[^>]*class="[^"]*label[^"]*"[^>]*>.*?<p[^>]*class="[^"]*title[^"]*"[^>]*>(.*?)</p>""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        val hrefRegex = Regex("""href="(/study/general-conference/$year/$monthStr/[^"?]+)""")

        chunks.forEachIndexed { idx, chunk ->
            val sessionName = sessionTitleRegex.find(chunk)
                ?.groupValues?.getOrNull(1)
                ?.let { cleanHtmlText(it) }
                ?.takeIf { it.isNotBlank() }
                ?: "Session ${idx + 1}"

            val seen = LinkedHashSet<String>()
            hrefRegex.findAll(chunk).forEach { match ->
                val rawHref = match.groupValues[1]
                val uri = rawHref.removePrefix("/study")
                val slug = uri.substringAfterLast("/")
                if (!slug.endsWith("-session") && slug.isNotBlank()) {
                    seen.add(uri)
                }
            }

            if (seen.isNotEmpty()) {
                sessions.add(ParsedSessionManifest(sessionName = sessionName, talkUris = seen.toList()))
            }
        }
        return sessions
    }

    internal fun parseChurchOrgTalkHtml(
        talkUri: String,
        htmlBody: String,
        fallbackTitle: String,
        periodId: String,
        periodName: String,
        year: Int,
        monthName: String,
        sessionName: String,
        sessionOrder: Int,
        talkOrder: Int,
    ): ConferenceTalkEntity? {
        val slug = talkUri.trimEnd('/').substringAfterLast('/')
        val talkId = "gc/$periodId/$slug"

        val h1Regex = Regex("""<h1[^>]*>(.*?)</h1>""", setOf(RegexOption.DOT_MATCHES_ALL))
        val title = h1Regex.find(htmlBody)?.groupValues?.getOrNull(1)
            ?.let { cleanHtmlText(it) }
            ?.takeIf { it.isNotBlank() }
            ?: fallbackTitle

        val authorRegex = Regex("""<p[^>]*class="[^"]*author-name[^"]*"[^>]*>(.*?)</p>""", setOf(RegexOption.DOT_MATCHES_ALL))
        val rawSpeaker = authorRegex.find(htmlBody)?.groupValues?.getOrNull(1)?.let { cleanHtmlText(it) } ?: ""
        val speaker = rawSpeaker.replace(Regex("""^(?:By|Presented by)\s+""", RegexOption.IGNORE_CASE), "").trim()

        val roleRegex = Regex("""<p[^>]*class="[^"]*author-role[^"]*"[^>]*>(.*?)</p>""", setOf(RegexOption.DOT_MATCHES_ALL))
        val speakerTitle = roleRegex.find(htmlBody)?.groupValues?.getOrNull(1)?.let { cleanHtmlText(it) } ?: ""

        val kickerRegex = Regex("""<p[^>]*class="[^"]*kicker[^"]*"[^>]*>(.*?)</p>""", setOf(RegexOption.DOT_MATCHES_ALL))
        val kicker = kickerRegex.find(htmlBody)?.groupValues?.getOrNull(1)?.let { cleanHtmlText(it) } ?: ""

        val bodyBlockStart = htmlBody.indexOf("""<div class="body-block">""")
        val bodyHtml = if (bodyBlockStart != -1) {
            val sub = htmlBody.substring(bodyBlockStart)
            val footerIdx = sub.indexOf("<footer")
            if (footerIdx != -1) sub.substring(0, footerIdx) else sub
        } else {
            htmlBody
        }

        val blockRegex = Regex("""<(p|h2|h3|h4|li)(?:\s[^>]*)?>(.*?)</\1>""", setOf(RegexOption.DOT_MATCHES_ALL))
        val paragraphs = mutableListOf<String>()
        blockRegex.findAll(bodyHtml).forEach { match ->
            val tag = match.groupValues[1]
            val inner = match.groupValues[2]
            if (tag == "li" && inner.contains("<p")) return@forEach
            val cleaned = cleanHtmlText(inner)
            if (cleaned.isNotBlank()) {
                paragraphs.add(cleaned)
            }
        }

        if (title.isBlank() || paragraphs.isEmpty()) return null

        val summary = kicker.ifBlank { paragraphs.first().take(140) }

        return ConferenceTalkEntity(
            talkId = talkId,
            periodId = periodId,
            periodName = periodName,
            year = year,
            month = monthName,
            sessionName = sessionName,
            sessionOrder = sessionOrder,
            talkOrder = talkOrder,
            title = title,
            speaker = speaker,
            speakerTitle = speakerTitle,
            summary = summary,
            paragraphs = paragraphs.joinToString("\n\n"),
        )
    }

    internal fun cleanHtmlText(rawHtml: String): String {
        var s = rawHtml
        // Remove footnote marker superscripts
        s = s.replace(Regex("""<sup[^>]*class="[^"]*marker[^"]*"[^>]*>.*?</sup>""", setOf(RegexOption.DOT_MATCHES_ALL)), "")
        s = s.replace(Regex("""<sup[^>]*>\s*\d+\s*</sup>""", setOf(RegexOption.DOT_MATCHES_ALL)), "")
        // Replace <br> with newline
        s = s.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
        // Strip remaining HTML tags
        s = s.replace(Regex("""<[^>]+>"""), "")
        // Decode common HTML entities
        s = decodeHtmlEntities(s)
        // Normalize spaces
        s = s.replace('\u00A0', ' ').replace('\u202F', ' ')
        return s.split("\n")
            .map { it.replace(Regex("""[ \t]+"""), " ").trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .trim()
    }

    private fun decodeHtmlEntities(input: String): String {
        var s = input
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&ldquo;", "“")
            .replace("&rdquo;", "”")
            .replace("&lsquo;", "‘")
            .replace("&rsquo;", "’")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&hellip;", "…")

        // Numeric decimal entities &#123;
        s = Regex("""&#(\d+);""").replace(s) { m ->
            val code = m.groupValues[1].toIntOrNull()
            if (code != null && code > 0) code.toChar().toString() else m.value
        }
        // Numeric hex entities &#x1F;
        s = Regex("""&#[xX]([0-9a-fA-F]+);""").replace(s) { m ->
            val code = m.groupValues[1].toIntOrNull(16)
            if (code != null && code > 0) code.toChar().toString() else m.value
        }
        return s
    }

    fun parseConferenceJson(jsonString: String): List<ConferenceTalkEntity> {
        val response = json.decodeFromString<RemoteConferenceResponse>(jsonString)
        val entities = mutableListOf<ConferenceTalkEntity>()

        response.periods.forEach { period ->
            period.sessions.forEachIndexed { sIdx, session ->
                session.talks.forEachIndexed { tIdx, talk ->
                    val paragraphsText = if (talk.paragraphs.isNotEmpty()) {
                        talk.paragraphs.joinToString("\n\n")
                    } else {
                        talk.content
                    }

                    if (talk.id.isNotBlank() && talk.title.isNotBlank()) {
                        entities.add(
                            ConferenceTalkEntity(
                                talkId = talk.id,
                                periodId = period.id,
                                periodName = period.name,
                                year = period.year,
                                month = period.month,
                                sessionName = session.sessionName,
                                sessionOrder = sIdx,
                                talkOrder = tIdx,
                                title = talk.title,
                                speaker = talk.speaker,
                                speakerTitle = talk.speakerTitle,
                                summary = talk.summary,
                                paragraphs = paragraphsText,
                            )
                        )
                    }
                }
            }
        }
        return entities
    }
}
