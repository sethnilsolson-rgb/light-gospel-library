package com.thelightphone.gospellibrary.data

import com.thelightphone.gospellibrary.db.SyncedHymnEntity
import com.thelightphone.gospellibrary.model.HymnBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI

data class ParsedHymnManifestEntry(
    val uri: String,
    val slug: String,
    val title: String,
    val topic: String,
)

data class HymnSyncResult(
    val downloadedHymns: List<SyncedHymnEntity>,
    val hadNetworkError: Boolean,
)

/**
 * Checks churchofjesuschrist.org for newly released hymns in
 * "Hymns—For Home and Church" and downloads only the ones not yet stored locally.
 */
object HymnSyncService {

    private const val CHURCH_API_BASE =
        "https://www.churchofjesuschrist.org/study/api/v3/language-pages/type/content?lang=eng&uri="

    private const val HOME_AND_CHURCH_MANIFEST_URI = "/music/hymns-for-home-and-church"

    suspend fun fetchNewHomeAndChurchHymns(
        existingSlugs: Set<String>,
        existingNumbers: Set<Int>,
    ): HymnSyncResult = withContext(Dispatchers.IO) {
        val manifestResult = runCatching {
            val manifestJson = fetchApiPage(HOME_AND_CHURCH_MANIFEST_URI)
            val manifestHtml = ConferenceSyncService.extractContentBody(manifestJson)
            parseHomeAndChurchManifest(manifestHtml)
        }

        val entries = manifestResult.getOrElse {
            return@withContext HymnSyncResult(emptyList(), hadNetworkError = true)
        }
        if (entries.isEmpty()) {
            return@withContext HymnSyncResult(emptyList(), hadNetworkError = false)
        }

        val missingEntries = entries.filter { entry ->
            entry.slug !in existingSlugs
        }
        if (missingEntries.isEmpty()) {
            return@withContext HymnSyncResult(emptyList(), hadNetworkError = false)
        }

        val semaphore = Semaphore(4)
        var anyFailure = false

        val downloaded = coroutineScope {
            missingEntries.map { entry ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        runCatching {
                            val pageJson = fetchApiPage(entry.uri)
                            val htmlBody = ConferenceSyncService.extractContentBody(pageJson)
                            val fallbackTitle = ConferenceSyncService.extractMetaTitle(pageJson)
                                .ifBlank { entry.title }
                            parseHymnHtml(
                                book = HymnBook.HOME_AND_CHURCH,
                                uri = entry.uri,
                                slug = entry.slug,
                                topic = entry.topic,
                                htmlBody = htmlBody,
                                fallbackTitle = fallbackTitle,
                            )
                        }.onFailure {
                            anyFailure = true
                        }.getOrNull()
                    }
                }
            }.awaitAll().filterNotNull().filter { it.number !in existingNumbers }
        }

        HymnSyncResult(
            downloadedHymns = downloaded.sortedBy { it.number },
            hadNetworkError = anyFailure && downloaded.isEmpty(),
        )
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
                setRequestProperty("User-Agent", "Mozilla/5.0 (LightPhone GospelLibrary/1.3)")
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

    internal fun parseHomeAndChurchManifest(manifestHtml: String): List<ParsedHymnManifestEntry> {
        val results = mutableListOf<ParsedHymnManifestEntry>()
        val seenSlugs = HashSet<String>()

        // Split by topic headings or scan tokens in document order
        val tokenRegex = Regex(
            """<h2[^>]*class="[^"]*label[^"]*"[^>]*>.*?<p[^>]*class="[^"]*title[^"]*"[^>]*>(.*?)</p>|<a[^>]*href="(/study/music/hymns-for-home-and-church/([^"?#]+))[^"]*"[^>]*>(.*?)</a>""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
        val tileTitleRegex = Regex(
            """<p[^>]*class="[^"]*title[^"]*"[^>]*>(.*?)</p>""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )

        var currentTopic = "General"
        for (match in tokenRegex.findAll(manifestHtml)) {
            val topicRaw = match.groupValues[1]
            val hrefRaw = match.groupValues[2]
            if (topicRaw.isNotBlank()) {
                val cleaned = ConferenceSyncService.cleanHtmlText(topicRaw)
                if (cleaned.isNotBlank()) currentTopic = cleaned
            } else if (hrefRaw.isNotBlank()) {
                val slug = match.groupValues[3].trimEnd('/')
                if (slug.isBlank() || '/' in slug) continue
                if (slug in setOf("about", "contents", "index", "introduction", "title-page", "using-the-hymnbook")) continue
                if (!seenSlugs.add(slug)) continue

                val innerHtml = match.groupValues[4]
                val titleHtml = tileTitleRegex.find(innerHtml)?.groupValues?.getOrNull(1) ?: innerHtml
                val title = ConferenceSyncService.cleanHtmlText(titleHtml)
                if (title.isBlank()) continue

                val uri = hrefRaw.removePrefix("/study")
                results.add(
                    ParsedHymnManifestEntry(
                        uri = uri,
                        slug = slug,
                        title = title,
                        topic = currentTopic,
                    )
                )
            }
        }
        return results
    }

    internal fun parseHymnHtml(
        book: HymnBook,
        uri: String,
        slug: String,
        topic: String,
        htmlBody: String,
        fallbackTitle: String,
    ): SyncedHymnEntity? {
        val numRegex = Regex(
            """<p[^>]*class="[^"]*song-number[^"]*"[^>]*>(.*?)</p>""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
        val numRaw = numRegex.find(htmlBody)?.groupValues?.getOrNull(1)
            ?.let { ConferenceSyncService.cleanHtmlText(it) }
            .orEmpty()
        val number = Regex("""\d+""").find(numRaw)?.value?.toIntOrNull() ?: return null

        val h1Regex = Regex("""<h1[^>]*>(.*?)</h1>""", setOf(RegexOption.DOT_MATCHES_ALL))
        val title = h1Regex.find(htmlBody)?.groupValues?.getOrNull(1)
            ?.let { ConferenceSyncService.cleanHtmlText(it) }
            ?.takeIf { it.isNotBlank() }
            ?: fallbackTitle.trim()
        if (title.isBlank()) return null

        val stanzas = mutableListOf<String>()
        val blockRegex = Regex(
            """<div[^>]*class="[^"]*(stanza|chorus)[^"]*"[^>]*>(.*?)</div>""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
        val lineOrLabelRegex = Regex(
            """<p[^>]*class="[^"]*(label|line)[^"]*"[^>]*>(.*?)</p>""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )

        var stanzaCounter = 0
        for (block in blockRegex.findAll(htmlBody)) {
            val kind = block.groupValues[1]
            val inner = block.groupValues[2]
            val lines = mutableListOf<String>()
            var explicitLabel = ""

            for (item in lineOrLabelRegex.findAll(inner)) {
                val itemKind = item.groupValues[1]
                val text = ConferenceSyncService.cleanHtmlText(item.groupValues[2])
                if (text.isBlank()) continue
                if (itemKind == "label") {
                    explicitLabel = text
                } else {
                    lines.add(text)
                }
            }
            if (lines.isEmpty()) continue

            val formatted = if (kind == "chorus") {
                val label = explicitLabel.ifBlank { "Chorus" }
                "[$label]\n" + lines.joinToString("\n")
            } else {
                stanzaCounter++
                val prefix = if (explicitLabel.isNotBlank()) {
                    if (explicitLabel.endsWith(".")) "$explicitLabel " else "$explicitLabel. "
                } else {
                    "$stanzaCounter. "
                }
                val firstLine = lines.first()
                val alreadyNumbered = Regex("""^\d+\.\s""").containsMatchIn(firstLine)
                if (alreadyNumbered) {
                    lines.joinToString("\n")
                } else {
                    (listOf(prefix + firstLine) + lines.drop(1)).joinToString("\n")
                }
            }
            stanzas.add(formatted)
        }

        if (stanzas.isEmpty()) return null

        val creditRegex = Regex(
            """<p[^>]*class="[^"]*(?:credit|copyright|scripture-ref)[^"]*"[^>]*>(.*?)</p>""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
        val credits = creditRegex.findAll(htmlBody)
            .map { ConferenceSyncService.cleanHtmlText(it.groupValues[1]) }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n")

        return SyncedHymnEntity(
            id = "hymn/${book.id}/$number",
            bookId = book.id,
            number = number,
            slug = slug,
            uri = uri,
            title = title,
            topic = topic,
            stanzasEncoded = HymnsData.encodeStanzas(stanzas),
            credits = credits,
        )
    }
}
