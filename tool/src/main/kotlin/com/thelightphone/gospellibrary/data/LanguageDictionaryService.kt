package com.thelightphone.gospellibrary.data

import com.thelightphone.gospellibrary.db.LanguageDictEntryEntity
import com.thelightphone.gospellibrary.db.LanguageDictionaryDao
import com.thelightphone.gospellibrary.model.GospelLanguage
import com.thelightphone.gospellibrary.model.WordLookupMatch
import com.thelightphone.gospellibrary.model.WordLookupResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream

object LanguageDictionaryService {

    private const val WEBSTER_1828_CSV_URL =
        "https://raw.githubusercontent.com/akaitsurugi/webster1828/main/webster1828.csv"
    private const val CC_CEDICT_GZ_URL =
        "https://www.mdbg.net/chinese/export/cedict/cedict_1_0_ts_utf-8_mdbg.txt.gz"
    private const val EDICT2_GZ_URL =
        "https://www.edrdg.org/pub/Nihongo/edict2.gz"

    private fun freedictUrl(pair: String) =
        "https://raw.githubusercontent.com/freedict/fd-dictionaries/master/$pair/$pair.tei"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Downloads and indexes the best offline dictionary for [language] directly into Room SQLite.
     * Returns the total number of offline entries stored (0 for on-demand cached languages).
     */
    suspend fun downloadOfflineDictionary(
        language: GospelLanguage,
        dao: LanguageDictionaryDao,
        onProgress: (String) -> Unit,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = dao.countEntries(language.dictKey)
            if (existing > 1000) {
                onProgress("${language.dictLabel} ready ($existing entries)")
                return@runCatching existing
            }

            when (language.dictKey) {
                "eng_1828" -> downloadWebster1828(dao, onProgress)
                "zh" -> downloadCcCedict(dao, onProgress)
                "jpn" -> downloadEdict2(dao, onProgress)
                "spa" -> downloadFreeDictTei("spa", "spa-eng", dao, onProgress)
                "por" -> downloadFreeDictTei("por", "por-eng", dao, onProgress)
                "fra" -> downloadFreeDictTei("fra", "fra-eng", dao, onProgress)
                else -> {
                    // Korean, German, Tagalog, Italian, Russian use instant online lookup
                    // that automatically caches every looked-up word offline in Room.
                    onProgress("${language.dictLabel} enabled (auto-caches offline)")
                    0
                }
            }
        }
    }

    private suspend fun downloadWebster1828(
        dao: LanguageDictionaryDao,
        onProgress: (String) -> Unit,
    ): Int {
        onProgress("Downloading Webster's 1828 Dictionary...")
        dao.deleteDictionary("eng_1828")
        var connection: HttpURLConnection? = null
        var inserted = 0
        val batch = ArrayList<LanguageDictEntryEntity>(1500)

        try {
            connection = openConnection(WEBSTER_1828_CSV_URL, readTimeoutMs = 30000)
            BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8), 65536).use { reader ->
                val record = StringBuilder()
                var insideQuotes = false
                var line = reader.readLine()
                while (line != null) {
                    if (record.isNotEmpty()) record.append('\n')
                    record.append(line)

                    // Count unescaped quotes to see if CSV row is complete
                    var i = 0
                    while (i < line.length) {
                        if (line[i] == '"') {
                            if (i + 1 < line.length && line[i + 1] == '"') {
                                i++ // escaped quote
                            } else {
                                insideQuotes = !insideQuotes
                            }
                        }
                        i++
                    }

                    if (!insideQuotes) {
                        parseWebster1828CsvRow(record.toString())?.let { entry ->
                            batch.add(entry)
                            if (batch.size >= 1500) {
                                dao.insertEntries(batch)
                                inserted += batch.size
                                batch.clear()
                                if (inserted % 6000 == 0) {
                                    onProgress("Indexing Webster 1828: $inserted words...")
                                }
                            }
                        }
                        record.clear()
                    }
                    line = reader.readLine()
                }
                if (record.isNotEmpty()) {
                    parseWebster1828CsvRow(record.toString())?.let { batch.add(it) }
                }
            }
            if (batch.isNotEmpty()) {
                dao.insertEntries(batch)
                inserted += batch.size
                batch.clear()
            }
        } finally {
            connection?.disconnect()
        }
        onProgress("Webster's 1828 ready ($inserted words)")
        return inserted
    }

    internal fun parseWebster1828CsvRow(row: String): LanguageDictEntryEntity? {
        val trimmed = row.trim()
        if (trimmed.isEmpty()) return null
        val fields = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < trimmed.length) {
            val ch = trimmed[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < trimmed.length && trimmed[i + 1] == '"') {
                        cur.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    fields.add(cur.toString())
                    cur.clear()
                }
                else -> cur.append(ch)
            }
            i++
        }
        fields.add(cur.toString())

        if (fields.size < 2) return null
        val headword = fields[0].trim().lowercase()
        if (headword.isBlank() || headword == "word") return null

        val rawDef = fields[1]
            .replace(Regex("""</p>\s*<p[^>]*>""", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<[^>]+>"""), "")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("""[ \t]+"""), " ")
            .trim()

        if (rawDef.isBlank()) return null
        val compactDef = if (rawDef.length > 900) rawDef.take(900).trimEnd() + "…" else rawDef

        return LanguageDictEntryEntity(
            dictKey = "eng_1828",
            headword = headword,
            reading = "",
            definition = compactDef,
        )
    }

    private suspend fun downloadCcCedict(
        dao: LanguageDictionaryDao,
        onProgress: (String) -> Unit,
    ): Int {
        onProgress("Downloading CC-CEDICT Chinese dictionary...")
        dao.deleteDictionary("zh")
        var connection: HttpURLConnection? = null
        var inserted = 0
        val batch = ArrayList<LanguageDictEntryEntity>(2000)

        try {
            connection = openConnection(CC_CEDICT_GZ_URL, readTimeoutMs = 30000)
            GZIPInputStream(connection.inputStream).use { gz ->
                BufferedReader(InputStreamReader(gz, Charsets.UTF_8), 65536).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        val parsedList = parseCedictLine(line)
                        if (parsedList.isNotEmpty()) {
                            batch.addAll(parsedList)
                            if (batch.size >= 2000) {
                                dao.insertEntries(batch)
                                inserted += batch.size
                                batch.clear()
                                if (inserted % 20000 == 0) {
                                    onProgress("Indexing Chinese dictionary: $inserted entries...")
                                }
                            }
                        }
                        line = reader.readLine()
                    }
                }
            }
            if (batch.isNotEmpty()) {
                dao.insertEntries(batch)
                inserted += batch.size
                batch.clear()
            }
        } finally {
            connection?.disconnect()
        }
        onProgress("CC-CEDICT ready ($inserted entries)")
        return inserted
    }

    /**
     * Parses a CC-CEDICT line:
     *   福音 福音 [fu2 yin1] /gospel/good news/
     * Returns entries for Traditional (and Simplified if distinct) with proper Pinyin tone marks.
     */
    internal fun parseCedictLine(line: String): List<LanguageDictEntryEntity> {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return emptyList()

        val firstSpace = trimmed.indexOf(' ')
        if (firstSpace <= 0) return emptyList()
        val secondSpace = trimmed.indexOf(' ', firstSpace + 1)
        if (secondSpace <= firstSpace) return emptyList()

        val traditional = trimmed.substring(0, firstSpace)
        val simplified = trimmed.substring(firstSpace + 1, secondSpace)

        val bracketStart = trimmed.indexOf('[', secondSpace)
        val bracketEnd = trimmed.indexOf(']', bracketStart + 1)
        if (bracketStart == -1 || bracketEnd == -1) return emptyList()

        val rawPinyin = trimmed.substring(bracketStart + 1, bracketEnd)
        val pinyin = formatNumberedPinyin(rawPinyin)

        val slashStart = trimmed.indexOf('/', bracketEnd)
        if (slashStart == -1) return emptyList()
        val defs = trimmed.substring(slashStart + 1)
            .trimEnd('/')
            .split('/')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(5)
            .joinToString("; ")

        if (defs.isBlank()) return emptyList()

        val out = mutableListOf(
            LanguageDictEntryEntity(
                dictKey = "zh",
                headword = traditional,
                reading = pinyin,
                definition = defs,
            )
        )
        if (simplified != traditional) {
            out.add(
                LanguageDictEntryEntity(
                    dictKey = "zh",
                    headword = simplified,
                    reading = pinyin,
                    definition = defs,
                )
            )
        }
        return out
    }

    internal fun formatNumberedPinyin(numbered: String): String {
        return numbered.split(" ").joinToString(" ") { syllable ->
            convertPinyinSyllable(syllable)
        }
    }

    private fun convertPinyinSyllable(syllable: String): String {
        if (syllable.length < 2) return syllable
        val last = syllable.last()
        val tone = last - '0'
        if (tone !in 1..5) return syllable
        val base = syllable.dropLast(1).replace("u:", "ü").replace("U:", "Ü")
        if (tone == 5) return base

        val toneMap = mapOf(
            'a' to "āáǎà", 'e' to "ēéěè", 'i' to "īíǐì", 'o' to "ōóǒò", 'u' to "ūúǔù", 'ü' to "ǖǘǚǜ",
            'A' to "ĀÁǍÀ", 'E' to "ĒÉĚÈ", 'I' to "ĪÍǏÌ", 'O' to "ŌÓǑÒ", 'U' to "ŪÚǓÙ", 'Ü' to "ǕǗǙǛ",
        )

        val lower = base.lowercase()
        val idx = when {
            lower.contains('a') -> lower.indexOf('a')
            lower.contains('e') -> lower.indexOf('e')
            lower.contains("ou") -> lower.indexOf('o')
            else -> lower.indexOfLast { it in "aeiouü" }
        }
        if (idx == -1) return base
        val targetChar = base[idx]
        val marked = toneMap[targetChar]?.getOrNull(tone - 1) ?: return base
        return base.substring(0, idx) + marked + base.substring(idx + 1)
    }

    private suspend fun downloadEdict2(
        dao: LanguageDictionaryDao,
        onProgress: (String) -> Unit,
    ): Int {
        onProgress("Downloading EDICT2 Japanese dictionary...")
        dao.deleteDictionary("jpn")
        var connection: HttpURLConnection? = null
        var inserted = 0
        val batch = ArrayList<LanguageDictEntryEntity>(2000)

        try {
            connection = openConnection(EDICT2_GZ_URL, readTimeoutMs = 30000)
            GZIPInputStream(connection.inputStream).use { gz ->
                BufferedReader(InputStreamReader(gz, Charset.forName("EUC-JP")), 65536).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        val parsed = parseEdict2Line(line)
                        if (parsed.isNotEmpty()) {
                            batch.addAll(parsed)
                            if (batch.size >= 2000) {
                                dao.insertEntries(batch)
                                inserted += batch.size
                                batch.clear()
                                if (inserted % 20000 == 0) {
                                    onProgress("Indexing Japanese dictionary: $inserted entries...")
                                }
                            }
                        }
                        line = reader.readLine()
                    }
                }
            }
            if (batch.isNotEmpty()) {
                dao.insertEntries(batch)
                inserted += batch.size
                batch.clear()
            }
        } finally {
            connection?.disconnect()
        }
        onProgress("EDICT2 ready ($inserted entries)")
        return inserted
    }

    internal fun parseEdict2Line(line: String): List<LanguageDictEntryEntity> {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("　???")) return emptyList()
        val slashIdx = trimmed.indexOf('/')
        if (slashIdx <= 0) return emptyList()

        val headPart = trimmed.substring(0, slashIdx).trim()
        val glossPart = trimmed.substring(slashIdx + 1)
            .split('/')
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("EntL") && it != "(P)" }
            .take(4)
            .joinToString("; ")
        if (glossPart.isBlank()) return emptyList()

        val bracketStart = headPart.indexOf('[')
        val bracketEnd = headPart.indexOf(']', bracketStart + 1)

        val kanjiForms: List<String>
        val reading: String
        if (bracketStart != -1 && bracketEnd != -1) {
            kanjiForms = headPart.substring(0, bracketStart).trim()
                .split(';')
                .map { it.replace(Regex("""\([^)]*\)"""), "").trim() }
                .filter { it.isNotBlank() }
                .take(2)
            reading = headPart.substring(bracketStart + 1, bracketEnd)
                .split(';')
                .firstOrNull()
                ?.replace(Regex("""\([^)]*\)"""), "")
                ?.trim()
                ?: ""
        } else {
            kanjiForms = headPart.split(';')
                .map { it.replace(Regex("""\([^)]*\)"""), "").trim() }
                .filter { it.isNotBlank() }
                .take(2)
            reading = ""
        }

        val out = mutableListOf<LanguageDictEntryEntity>()
        kanjiForms.forEach { form ->
            out.add(
                LanguageDictEntryEntity(
                    dictKey = "jpn",
                    headword = form,
                    reading = reading,
                    definition = glossPart,
                )
            )
        }
        if (reading.isNotBlank() && reading !in kanjiForms) {
            out.add(
                LanguageDictEntryEntity(
                    dictKey = "jpn",
                    headword = reading,
                    reading = "",
                    definition = glossPart,
                )
            )
        }
        return out
    }

    private suspend fun downloadFreeDictTei(
        dictKey: String,
        pair: String,
        dao: LanguageDictionaryDao,
        onProgress: (String) -> Unit,
    ): Int {
        onProgress("Downloading $pair dictionary...")
        dao.deleteDictionary(dictKey)
        var connection: HttpURLConnection? = null
        var inserted = 0
        val batch = ArrayList<LanguageDictEntryEntity>(1000)

        try {
            connection = openConnection(freedictUrl(pair), readTimeoutMs = 25000)
            val fullXml = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val entryRegex = Regex("""<entry[^>]*>(.*?)</entry>""", setOf(RegexOption.DOT_MATCHES_ALL))
            val orthRegex = Regex("""<orth[^>]*>(.*?)</orth>""", setOf(RegexOption.DOT_MATCHES_ALL))
            val quoteRegex = Regex("""<quote[^>]*>(.*?)</quote>""", setOf(RegexOption.DOT_MATCHES_ALL))
            val defRegex = Regex("""<def[^>]*>(.*?)</def>""", setOf(RegexOption.DOT_MATCHES_ALL))

            entryRegex.findAll(fullXml).forEach { match ->
                val block = match.groupValues[1]
                val orth = orthRegex.find(block)?.groupValues?.getOrNull(1)
                    ?.let { ConferenceSyncService.cleanHtmlText(it) }
                    ?.lowercase()
                    ?: return@forEach

                val quotes = quoteRegex.findAll(block)
                    .map { ConferenceSyncService.cleanHtmlText(it.groupValues[1]) }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(5)
                    .toList()
                val defText = if (quotes.isNotEmpty()) {
                    quotes.joinToString("; ")
                } else {
                    defRegex.findAll(block)
                        .map { ConferenceSyncService.cleanHtmlText(it.groupValues[1]) }
                        .filter { it.isNotBlank() }
                        .take(3)
                        .joinToString("; ")
                }

                if (orth.isNotBlank() && defText.isNotBlank()) {
                    batch.add(
                        LanguageDictEntryEntity(
                            dictKey = dictKey,
                            headword = orth,
                            reading = "",
                            definition = defText,
                        )
                    )
                    if (batch.size >= 1000) {
                        dao.insertEntries(batch)
                        inserted += batch.size
                        batch.clear()
                    }
                }
            }
            if (batch.isNotEmpty()) {
                dao.insertEntries(batch)
                inserted += batch.size
                batch.clear()
            }
        } finally {
            connection?.disconnect()
        }
        onProgress("Dictionary ready ($inserted words)")
        return inserted
    }

    /**
     * Looks up the word or character at [charIndex] inside [text] for [language].
     * Uses offline SQLite first (with longest-match CJK compound matching & English 1828 stemming),
     * then falls back to online bilingual translation (automatically cached offline in SQLite).
     */
    suspend fun lookupAtOffset(
        text: String,
        charIndex: Int,
        language: GospelLanguage,
        dao: LanguageDictionaryDao,
        englishVersePreview: String? = null,
    ): WordLookupResult? = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext null
        val safeIdx = charIndex.coerceIn(0, text.lastIndex)

        if (language.isCjkNoSpaces) {
            return@withContext lookupCjk(text, safeIdx, language, dao, englishVersePreview)
        } else {
            return@withContext lookupSpaceDelimited(text, safeIdx, language, dao, englishVersePreview)
        }
    }

    private suspend fun lookupCjk(
        text: String,
        charIndex: Int,
        language: GospelLanguage,
        dao: LanguageDictionaryDao,
        englishVersePreview: String?,
    ): WordLookupResult? {
        // Snap to nearest CJK/Kana character if user tapped adjacent punctuation
        var idx = charIndex
        if (!isCjkOrKana(text[idx])) {
            val right = (idx + 1).takeIf { it < text.length && isCjkOrKana(text[it]) }
            val left = (idx - 1).takeIf { it >= 0 && isCjkOrKana(text[it]) }
            idx = right ?: left ?: return null
        }

        // Build candidate compound spans covering idx: prioritize longest match starting at idx,
        // then overlapping compounds containing idx (e.g. tapping 音 inside 福音).
        val candidates = LinkedHashSet<String>()
        for (len in 4 downTo 1) {
            if (idx + len <= text.length) {
                val sub = text.substring(idx, idx + len)
                if (sub.all { isCjkOrKana(it) }) candidates.add(sub)
            }
        }
        for (startShift in 1..2) {
            val start = idx - startShift
            if (start >= 0) {
                for (len in 4 downTo (startShift + 1)) {
                    if (start + len <= text.length) {
                        val sub = text.substring(start, start + len)
                        if (sub.all { isCjkOrKana(it) }) candidates.add(sub)
                    }
                }
            }
        }

        val singleChar = text[idx].toString()
        var bestMatch: LanguageDictEntryEntity? = null
        for (cand in candidates) {
            val rows = dao.lookupExact(language.dictKey, cand)
            if (rows.isNotEmpty()) {
                bestMatch = mergeEntries(rows)
                break
            }
        }

        val singleBreakdown = if (bestMatch != null && bestMatch.headword.length > 1) {
            val singleRows = dao.lookupExact(language.dictKey, singleChar)
            if (singleRows.isNotEmpty()) {
                val merged = mergeEntries(singleRows)
                WordLookupMatch(merged.headword, merged.reading, merged.definition)
            } else null
        } else null

        if (bestMatch != null) {
            return WordLookupResult(
                query = bestMatch.headword,
                headword = bestMatch.headword,
                reading = bestMatch.reading,
                definition = bestMatch.definition,
                sourceLabel = language.dictLabel,
                characterBreakdown = singleBreakdown,
                englishVersePreview = englishVersePreview,
            )
        }

        // Online fallback (cached permanently in SQLite)
        val onlineQuery = candidates.firstOrNull { it.length == 2 } ?: singleChar
        val translated = fetchMyMemoryTranslation(onlineQuery, language.iso2)
        if (!translated.isNullOrBlank()) {
            runCatching {
                dao.insertEntry(
                    LanguageDictEntryEntity(
                        dictKey = language.dictKey,
                        headword = onlineQuery,
                        reading = "",
                        definition = translated,
                    )
                )
            }
            return WordLookupResult(
                query = onlineQuery,
                headword = onlineQuery,
                reading = "",
                definition = translated,
                sourceLabel = "${language.nativeName} → English",
                englishVersePreview = englishVersePreview,
            )
        }

        return WordLookupResult(
            query = singleChar,
            headword = singleChar,
            definition = "No entry found for \"$singleChar\".",
            sourceLabel = language.dictLabel,
            englishVersePreview = englishVersePreview,
            notFound = true,
        )
    }

    private suspend fun lookupSpaceDelimited(
        text: String,
        charIndex: Int,
        language: GospelLanguage,
        dao: LanguageDictionaryDao,
        englishVersePreview: String?,
    ): WordLookupResult? {
        val rawWord = extractWordAt(text, charIndex) ?: return null
        val normalized = rawWord.lowercase()

        val candidates = buildWordCandidates(normalized, language)
        for (cand in candidates) {
            val rows = dao.lookupExact(language.dictKey, cand)
            if (rows.isNotEmpty()) {
                val merged = mergeEntries(rows)
                return WordLookupResult(
                    query = rawWord,
                    headword = if (language == GospelLanguage.ENG) merged.headword.uppercase() else merged.headword,
                    reading = merged.reading,
                    definition = merged.definition,
                    sourceLabel = language.dictLabel,
                    englishVersePreview = englishVersePreview,
                )
            }
        }

        // For non-English languages, fall back to fast online bilingual translation + permanent offline cache
        if (language != GospelLanguage.ENG) {
            val translated = fetchMyMemoryTranslation(normalized, language.iso2)
            if (!translated.isNullOrBlank()) {
                runCatching {
                    dao.insertEntry(
                        LanguageDictEntryEntity(
                            dictKey = language.dictKey,
                            headword = normalized,
                            reading = "",
                            definition = translated,
                        )
                    )
                }
                return WordLookupResult(
                    query = rawWord,
                    headword = rawWord,
                    definition = translated,
                    sourceLabel = "${language.nativeName} → English",
                    englishVersePreview = englishVersePreview,
                )
            }
        }

        return WordLookupResult(
            query = rawWord,
            headword = rawWord,
            definition = "No entry found for \"$rawWord\".",
            sourceLabel = language.dictLabel,
            englishVersePreview = englishVersePreview,
            notFound = true,
        )
    }

    internal fun extractWordAt(text: String, charIndex: Int): String? {
        if (text.isEmpty()) return null
        var idx = charIndex.coerceIn(0, text.lastIndex)
        if (!isWordChar(text[idx])) {
            val right = (idx + 1).takeIf { it < text.length && isWordChar(text[it]) }
            val left = (idx - 1).takeIf { it >= 0 && isWordChar(text[it]) }
            idx = right ?: left ?: return null
        }
        var start = idx
        while (start > 0 && isWordChar(text[start - 1])) start--
        var end = idx
        while (end < text.lastIndex && isWordChar(text[end + 1])) end++
        val word = text.substring(start, end + 1).trim('\'', '-', '’')
        return word.takeIf { it.isNotBlank() }
    }

    internal fun buildWordCandidates(normalized: String, language: GospelLanguage): List<String> {
        val list = LinkedHashSet<String>()
        list.add(normalized)

        if (language == GospelLanguage.ENG) {
            // Handle KJV / Book of Mormon archaic verb endings & common English inflections
            if (normalized.endsWith("ieth") && normalized.length > 5) {
                list.add(normalized.dropLast(4) + "y")
            }
            if (normalized.endsWith("eth") && normalized.length > 4) {
                list.add(normalized.dropLast(3))
                list.add(normalized.dropLast(3) + "e")
            }
            if (normalized.endsWith("est") && normalized.length > 4) {
                list.add(normalized.dropLast(3))
                list.add(normalized.dropLast(3) + "e")
            }
            if (normalized.endsWith("ies") && normalized.length > 4) {
                list.add(normalized.dropLast(3) + "y")
            }
            if (normalized.endsWith("ied") && normalized.length > 4) {
                list.add(normalized.dropLast(3) + "y")
            }
            if (normalized.endsWith("ing") && normalized.length > 5) {
                val stem = normalized.dropLast(3)
                list.add(stem)
                list.add(stem + "e")
                if (stem.length >= 2 && stem.last() == stem[stem.lastIndex - 1]) {
                    list.add(stem.dropLast(1))
                }
            }
            if (normalized.endsWith("ed") && normalized.length > 4) {
                val stem = normalized.dropLast(2)
                list.add(stem)
                list.add(stem + "e")
                if (stem.length >= 2 && stem.last() == stem[stem.lastIndex - 1]) {
                    list.add(stem.dropLast(1))
                }
            }
            if (normalized.endsWith("es") && normalized.length > 3) {
                list.add(normalized.dropLast(2))
            }
            if (normalized.endsWith("s") && normalized.length > 3 && !normalized.endsWith("ss")) {
                list.add(normalized.dropLast(1))
            }
        } else {
            // Simple plural stripping for Romance/Germanic languages
            if (normalized.endsWith("es") && normalized.length > 4) list.add(normalized.dropLast(2))
            if (normalized.endsWith("s") && normalized.length > 3) list.add(normalized.dropLast(1))
        }

        return list.toList()
    }

    private fun mergeEntries(rows: List<LanguageDictEntryEntity>): LanguageDictEntryEntity {
        if (rows.size == 1) return rows.first()
        val first = rows.first()
        val readings = rows.map { it.reading }.filter { it.isNotBlank() }.distinct().joinToString(" / ")
        val defs = rows.map { it.definition }.distinct().take(3).joinToString("\n\n")
        return first.copy(reading = readings, definition = defs)
    }

    private fun fetchMyMemoryTranslation(query: String, sourceIso2: String): String? {
        var connection: HttpURLConnection? = null
        return runCatching {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val langPair = URLEncoder.encode("$sourceIso2|en", "UTF-8")
            val url = "https://api.mymemory.translated.net/get?q=$encoded&langpair=$langPair"
            val conn = openConnection(url, readTimeoutMs = 5000)
            connection = conn
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = json.parseToJsonElement(body).jsonObject
            val translated = root["responseData"]?.jsonObject?.get("translatedText")?.jsonPrimitive?.content?.trim()
            translated?.takeIf { it.isNotBlank() && !it.equals(query, ignoreCase = true) }
        }.getOrNull().also {
            connection?.disconnect()
        }
    }

    private fun openConnection(urlString: String, readTimeoutMs: Int): HttpURLConnection {
        val url = URI(urlString).toURL()
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = readTimeoutMs
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 (LightPhone GospelLibrary/1.0)")
        }
        if (conn.responseCode != HttpURLConnection.HTTP_OK) {
            val code = conn.responseCode
            conn.disconnect()
            error("HTTP $code downloading $urlString")
        }
        return conn
    }

    private fun isWordChar(ch: Char): Boolean =
        ch.isLetter() || ch == '\'' || ch == '’' || ch == '-'

    private fun isCjkOrKana(ch: Char): Boolean {
        val code = ch.code
        return (code in 0x4E00..0x9FFF) || // CJK Unified Ideographs
            (code in 0x3400..0x4DBF) ||    // CJK Extension A
            (code in 0x3040..0x309F) ||    // Hiragana
            (code in 0x30A0..0x30FF)       // Katakana
    }
}
