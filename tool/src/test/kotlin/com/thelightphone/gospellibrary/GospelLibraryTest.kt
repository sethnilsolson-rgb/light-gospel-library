package com.thelightphone.gospellibrary

import com.thelightphone.gospellibrary.data.ConferenceSyncService
import com.thelightphone.gospellibrary.data.GeneralConferenceData
import com.thelightphone.gospellibrary.data.LanguageContentService
import com.thelightphone.gospellibrary.data.LanguageDictionaryService
import com.thelightphone.gospellibrary.data.StandardWorksData
import com.thelightphone.gospellibrary.model.ContentVerse
import com.thelightphone.gospellibrary.model.GospelLanguage
import com.thelightphone.gospellibrary.model.ReaderFontSize
import com.thelightphone.gospellibrary.model.ReaderPageMode
import com.thelightphone.gospellibrary.ui.GospelPageSlice
import com.thelightphone.gospellibrary.ui.GospelReaderPage
import com.thelightphone.gospellibrary.ui.GospelReaderPaginator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GospelLibraryTest {

    @Test
    fun standardWorksContainsAllFiveVolumes() {
        assertEquals(5, StandardWorksData.volumes.size)
        val ids = StandardWorksData.volumes.map { it.id }
        assertTrue(ids.contains("bofm"))
        assertTrue(ids.contains("nt"))
        assertTrue(ids.contains("ot"))
        assertTrue(ids.contains("dc"))
        assertTrue(ids.contains("pgp"))

        assertEquals(15, StandardWorksData.getBooksForVolume("bofm").size)
        assertEquals(27, StandardWorksData.getBooksForVolume("nt").size)
        assertEquals(39, StandardWorksData.getBooksForVolume("ot").size)
        assertEquals(1, StandardWorksData.getBooksForVolume("dc").size)
        assertEquals(5, StandardWorksData.getBooksForVolume("pgp").size)
    }

    @Test
    fun scriptureChapterLoadingAndSwipeLinks() {
        val nephi1 = StandardWorksData.getChapter("bofm", "1-ne", 1)
        assertEquals("1 Nephi 1", nephi1.title)
        assertTrue(nephi1.verses.isNotEmpty())
        assertEquals(null, nephi1.prevId)
        assertEquals("scripture/bofm/1-ne/2", nephi1.nextId)
        assertEquals("1 Nephi 2", nephi1.nextLabel)

        val matt5 = StandardWorksData.getChapter("nt", "matt", 5)
        assertEquals("Matthew 5", matt5.title)
        assertTrue(matt5.verses.any { it.text.contains("light of the world") })

        val ps23 = StandardWorksData.getChapter("ot", "ps", 23)
        assertEquals("Psalms 23", ps23.title)
        assertTrue(ps23.verses.any { it.text.contains("shepherd") })

        val dc4 = StandardWorksData.getChapter("dc", "sections", 4)
        assertEquals("Section 4", dc4.title)
        assertTrue(dc4.verses.any { it.text.contains("marvelous work") })

        val aOfF = StandardWorksData.getChapter("pgp", "a-of-f", 1)
        assertEquals("Articles of Faith 1", aOfF.title)
        assertTrue(aOfF.verses.any { it.text.contains("We believe in God") })
    }

    @Test
    fun bundledOctober2026ConferenceIsCompleteAndAuthentic() {
        val assetFile = File("src/main/assets/conference/2026-10.json")
        assertTrue(assetFile.exists(), "Bundled October 2026 conference asset must exist at ${assetFile.absolutePath}")

        val loaded = GeneralConferenceData.ensureBundledLoaded { path ->
            File("src/main/assets/$path").takeIf { it.exists() }?.inputStream()
        }
        assertEquals(38, loaded.size, "October 2026 General Conference should contain all 38 talks")

        val periods = GeneralConferenceData.getConferencePeriods()
        assertEquals(1, periods.size)

        val october = periods.first()
        assertEquals("2026-10", october.id)
        assertEquals("October 2026", october.name)
        assertEquals(2026, october.year)
        assertEquals(4, october.sessions.size)

        // Verify every single talk has full authentic paragraphs and NO repetitive placeholder text
        val openingParagraphs = mutableSetOf<String>()
        loaded.forEach { entity ->
            val chapter = GeneralConferenceData.getTalkContent(entity.talkId)
            assertNotNull(chapter, "Chapter should load for ${entity.talkId}")
            assertTrue(chapter.verses.size >= 7, "Talk ${entity.title} should have full text, got ${chapter.verses.size} paragraphs")
            assertFalse(
                chapter.verses.any { it.text.contains("what a sacred privilege it is to assemble together across the nations") },
                "Talk ${entity.title} must not contain placeholder text"
            )
            openingParagraphs.add(chapter.verses.first().text)
        }
        assertTrue(openingParagraphs.size >= 35, "Talks should have distinct authentic openings")

        // Verify specific real October 2026 talks and prev/next navigation
        val gongTalk = GeneralConferenceData.getTalkContent("gc/2026-10/12gong")
        assertNotNull(gongTalk)
        assertEquals("Come to the Lord in His Holy House", gongTalk.title)
        assertTrue(gongTalk.subtitle.orEmpty().contains("Gerrit W. Gong"))
        assertEquals("gc/2026-10/11christofferson", gongTalk.prevId)
        assertEquals("gc/2026-10/13runia", gongTalk.nextId)
        assertTrue(gongTalk.verses.size >= 25)
    }

    @Test
    fun churchOrgHtmlManifestAndTalkParsing() {
        val sampleManifest = """
            <ul class="doc-map">
              <li data-content-type="general-conference-session">
                <h2 class="label"><p class="title">Saturday Morning Session</p></h2>
                <ul class="doc-map">
                  <li><a href="/study/general-conference/2027/04/saturday-morning-session?lang=eng">Session</a></li>
                  <li><a href="/study/general-conference/2027/04/11oaks?lang=eng">Opening Talk</a></li>
                </ul>
              </li>
            </ul>
        """.trimIndent()

        val sessions = ConferenceSyncService.parseChurchOrgManifest(sampleManifest, 2027, "04")
        assertEquals(1, sessions.size)
        assertEquals("Saturday Morning Session", sessions.first().sessionName)
        assertEquals(listOf("/general-conference/2027/04/11oaks"), sessions.first().talkUris)

        val sampleTalkHtml = """
            <header>
              <h1>Walking in Faith</h1>
              <div class="byline">
                <p class="author-name">By President Dallin&nbsp;H. Oaks</p>
                <p class="author-role">President of the Church</p>
              </div>
            </header>
            <div class="body-block">
              <p>Beloved brothers and sisters, we rejoice today.<sup class="marker">1</sup></p>
              <p>Hold fast to the iron rod&mdash;it leads to &ldquo;eternal life.&rdquo;</p>
            </div>
            <footer class="notes"><p>Footnote 1 should not appear.</p></footer>
        """.trimIndent()

        val parsed = ConferenceSyncService.parseChurchOrgTalkHtml(
            talkUri = "/general-conference/2027/04/11oaks",
            htmlBody = sampleTalkHtml,
            fallbackTitle = "Fallback",
            periodId = "2027-04",
            periodName = "April 2027",
            year = 2027,
            monthName = "April",
            sessionName = "Saturday Morning Session",
            sessionOrder = 0,
            talkOrder = 0,
        )

        assertNotNull(parsed)
        assertEquals("gc/2027-04/11oaks", parsed.talkId)
        assertEquals("Walking in Faith", parsed.title)
        assertEquals("President Dallin H. Oaks", parsed.speaker)
        assertEquals("President of the Church", parsed.speakerTitle)
        assertFalse(parsed.paragraphs.contains("Footnote 1"))
        assertFalse(parsed.paragraphs.contains("today.1"))
        assertTrue(parsed.paragraphs.contains("Hold fast to the iron rod—it leads to “eternal life.”"))

        // Verify candidate period generation from Oct 2026 onward
        val candidates = ConferenceSyncService.getCandidateConferencePeriods(2027, 10)
        assertEquals(listOf(2026 to 10, 2027 to 4, 2027 to 10), candidates)
    }

    @Test
    fun autoUpdateConferenceJsonParsing() {
        val sampleJson = """
            {
              "periods": [
                {
                  "id": "2027-04",
                  "name": "April 2027",
                  "year": 2027,
                  "month": "April",
                  "sessions": [
                    {
                      "sessionName": "Sunday Morning Session",
                      "talks": [
                        {
                          "id": "gc/2027-04/sample-talk",
                          "title": "A Future of Peace",
                          "speaker": "President Dallin H. Oaks",
                          "speakerTitle": "President of the Church",
                          "summary": "Finding tranquility in an ever-accelerating world.",
                          "paragraphs": [
                            "Beloved saints around the world, welcome to this blessed morning.",
                            "The Lord continues to guide His people with unceasing love."
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val parsedTalks = ConferenceSyncService.parseConferenceJson(sampleJson)
        assertEquals(1, parsedTalks.size)
        val talk = parsedTalks.first()
        assertEquals("gc/2027-04/sample-talk", talk.talkId)
        assertEquals("April 2027", talk.periodName)
        assertEquals(2027, talk.year)
        assertTrue(talk.paragraphs.contains("Beloved saints around the world"))

        val dynamicPeriods = GeneralConferenceData.getConferencePeriods(parsedTalks)
        assertEquals("April 2027", dynamicPeriods.first().name)
        assertEquals(1, dynamicPeriods.first().sessions.size)
        assertEquals("A Future of Peace", dynamicPeriods.first().sessions.first().talks.first().title)
    }

    @Test
    fun readerFontSizeLabels() {
        assertEquals("Normal", ReaderFontSize.REGULAR.label)
        assertEquals("Aa", ReaderFontSize.REGULAR.displayLabel)
        assertEquals("Aa-", ReaderFontSize.COMPACT.displayLabel)
        assertEquals("Aa+", ReaderFontSize.LARGE.displayLabel)
    }

    @Test
    fun gospelLanguagesMetadataAndDownloadableList() {
        assertEquals(13, GospelLanguage.entries.size)
        assertEquals(12, GospelLanguage.downloadableLanguages.size)
        assertFalse(GospelLanguage.downloadableLanguages.contains(GospelLanguage.ENG))
        assertEquals(GospelLanguage.ZHO, GospelLanguage.fromCode("zho"))
        assertEquals(GospelLanguage.ZHS, GospelLanguage.fromCode("zhs"))
        assertEquals(GospelLanguage.SPA, GospelLanguage.fromCode("spa"))
        assertEquals(GospelLanguage.JPN, GospelLanguage.fromCode("jpn"))
        assertEquals(GospelLanguage.CES, GospelLanguage.fromCode("ces"))
        assertEquals(GospelLanguage.ENG, GospelLanguage.fromCode("unknown"))
        // Traditional and Simplified Chinese share the CC-CEDICT dictionary key
        assertEquals(GospelLanguage.ZHO.dictKey, GospelLanguage.ZHS.dictKey)
    }

    @Test
    fun pinyinToneMarksAndCedictParsing() {
        assertEquals("fú yīn", LanguageDictionaryService.formatNumberedPinyin("fu2 yin1"))
        assertEquals("lǜ", LanguageDictionaryService.formatNumberedPinyin("lu:4"))
        assertEquals("nǐ hǎo", LanguageDictionaryService.formatNumberedPinyin("ni3 hao3"))

        val cedictLine = "福音 福音 [fu2 yin1] /gospel/good news/"
        val sameFormEntries = LanguageDictionaryService.parseCedictLine(cedictLine)
        assertEquals(1, sameFormEntries.size)
        assertEquals("福音", sameFormEntries.first().headword)
        assertEquals("fú yīn", sameFormEntries.first().reading)
        assertTrue(sameFormEntries.first().definition.contains("gospel"))

        val tradSimpLine = "耶穌 耶稣 [Ye1 su1] /Jesus/"
        val bothEntries = LanguageDictionaryService.parseCedictLine(tradSimpLine)
        assertEquals(2, bothEntries.size)
        assertTrue(bothEntries.any { it.headword == "耶穌" })
        assertTrue(bothEntries.any { it.headword == "耶稣" })
    }

    @Test
    fun webster1828AndEdict2ParsingAndKjvStemming() {
        val websterRow = "\"FAITH\",\"Belief; the assent of the mind to the truth of what is declared by another.\""
        val websterEntry = LanguageDictionaryService.parseWebster1828CsvRow(websterRow)
        assertNotNull(websterEntry)
        assertEquals("eng_1828", websterEntry.dictKey)
        assertEquals("faith", websterEntry.headword)
        assertTrue(websterEntry.definition.contains("assent of the mind"))

        val edictLine = "福音 [ふくいん] /(n) gospel; good news/(P)/EntL1498700X/"
        val edictEntries = LanguageDictionaryService.parseEdict2Line(edictLine)
        assertTrue(edictEntries.any { it.headword == "福音" && it.reading == "ふくいん" })
        assertTrue(edictEntries.any { it.headword == "ふくいん" && it.definition.contains("gospel") })

        val extracted = LanguageDictionaryService.extractWordAt("For God so loved the world.", 13)
        assertEquals("loved", extracted)

        val kjvCandidates = LanguageDictionaryService.buildWordCandidates("believeth", GospelLanguage.ENG)
        assertTrue(kjvCandidates.contains("believe"), "Expected KJV stem 'believe' in $kjvCandidates")

        val pluralCandidates = LanguageDictionaryService.buildWordCandidates("commandments", GospelLanguage.ENG)
        assertTrue(pluralCandidates.contains("commandment"), "Expected stem 'commandment' in $pluralCandidates")
    }

    @Test
    fun languageContentUriMappingAndHtmlParsing() {
        assertEquals("/scriptures/dc-testament/dc/4", LanguageContentService.buildChurchOrgScriptureUri("dc", "sections", 4))
        assertEquals("/scriptures/bofm/moro/10", LanguageContentService.buildChurchOrgScriptureUri("bofm", "moroni", 10))
        assertEquals("/scriptures/bofm/1-ne/2", LanguageContentService.buildChurchOrgScriptureUri("bofm", "1-ne", 2))

        val sampleChineseHtml = """
            <header>
              <h1>尼腓一書 第2章</h1>
              <p class="subtitle">尼腓一書</p>
            </header>
            <div class="body-block">
              <p class="verse"><span class="verse-number">1 </span>看啊，事情是這樣的，主對我父親說話<sup class="marker">甲</sup>。</p>
              <p class="verse"><span class="verse-number">2 </span>事情是這樣的，主命令我父親在夢中帶家人離開。</p>
            </div>
        """.trimIndent()

        val entity = LanguageContentService.parseScriptureChapterHtml(
            langCode = "zho",
            contentId = "scripture/bofm/1-ne/2",
            htmlBody = sampleChineseHtml,
            fallbackTitle = "尼腓一書 第2章",
        )
        assertNotNull(entity)
        assertEquals("zho|scripture/bofm/1-ne/2", entity.cacheKey)
        assertEquals("尼腓一書 第2章", entity.title)

        // When meta title is blank, fall back to <h1> from HTML
        val h1Fallback = LanguageContentService.parseScriptureChapterHtml(
            langCode = "zho",
            contentId = "scripture/bofm/1-ne/2",
            htmlBody = sampleChineseHtml,
            fallbackTitle = "",
        )
        assertNotNull(h1Fallback)
        assertEquals("尼腓一書 第2章", h1Fallback.title)

        val verses = LanguageContentService.decodeVerses(entity.versesEncoded)
        assertEquals(2, verses.size)
        assertEquals(1, verses[0].number)
        assertEquals("看啊，事情是這樣的，主對我父親說話。", verses[0].text)
        assertEquals(2, verses[1].number)

        val roundTrip = LanguageContentService.decodeVerses(
            LanguageContentService.encodeVerses(listOf(ContentVerse(1, "Test verse"), ContentVerse(null, "Paragraph")))
        )
        assertEquals(2, roundTrip.size)
        assertEquals(1, roundTrip[0].number)
        assertEquals(null, roundTrip[1].number)
    }

    @Test
    fun czechWiktionaryTsvParsingWithInflections() {
        val sampleLine =
            "Abrahám|Abrahámem|Abrahámovi\t<i>name</i><br><ol><li>Abraham, a prophet in the Old Testament</li><li>Patriarch</li></ol>"
        val entries = LanguageDictionaryService.parseWiktionaryTsvLine("ces", sampleLine)
        assertEquals(3, entries.size)

        val base = entries.first { it.headword == "abrahám" }
        assertEquals("", base.reading)
        assertTrue(base.definition.contains("Abraham, a prophet in the Old Testament"))
        assertTrue(base.definition.contains("Patriarch"))
        assertFalse(base.definition.contains("<li>"))

        val inflected = entries.first { it.headword == "abrahámem" }
        assertEquals("Abrahám", inflected.reading)
        assertEquals(base.definition, inflected.definition)
    }

    @Test
    fun readerPageModeAndPaginatorPermilleMapping() {
        assertEquals(ReaderPageMode.PAGED, ReaderPageMode.SCROLL.toggle())
        assertEquals(ReaderPageMode.SCROLL, ReaderPageMode.PAGED.toggle())
        assertEquals(ReaderPageMode.PAGED, ReaderPageMode.fromName("PAGED"))
        assertEquals(ReaderPageMode.SCROLL, ReaderPageMode.fromName("unknown"))

        val samplePages = listOf(
            GospelReaderPage(0, listOf(GospelPageSlice(0, 1, true, "First page")), permilleStart = 0),
            GospelReaderPage(1, listOf(GospelPageSlice(1, 2, true, "Second page")), permilleStart = 320),
            GospelReaderPage(2, listOf(GospelPageSlice(2, 3, true, "Third page")), permilleStart = 680),
        )
        assertEquals(0, GospelReaderPaginator.pageIndexForPermille(samplePages, 0))
        assertEquals(0, GospelReaderPaginator.pageIndexForPermille(samplePages, 250))
        assertEquals(1, GospelReaderPaginator.pageIndexForPermille(samplePages, 320))
        assertEquals(1, GospelReaderPaginator.pageIndexForPermille(samplePages, 600))
        assertEquals(2, GospelReaderPaginator.pageIndexForPermille(samplePages, 700))
        assertEquals(2, GospelReaderPaginator.pageIndexForPermille(samplePages, 1000))
        assertEquals(0, GospelReaderPaginator.pageIndexForPermille(emptyList(), 500))
    }

    @Test
    fun bundledHymnbooksLoadAndSearchCorrectly() {
        val assetsRoot = java.io.File("src/main/assets")
        val provider: (String) -> java.io.InputStream? = { path ->
            val f = java.io.File(assetsRoot, path)
            if (f.isFile) f.inputStream() else null
        }

        com.thelightphone.gospellibrary.data.HymnsData.ensureBundledLoaded(provider)
        val homeAndChurch = com.thelightphone.gospellibrary.data.HymnsData.getHymnsForBook(
            com.thelightphone.gospellibrary.model.HymnBook.HOME_AND_CHURCH
        )
        val classic = com.thelightphone.gospellibrary.data.HymnsData.getHymnsForBook(
            com.thelightphone.gospellibrary.model.HymnBook.CLASSIC_1985
        )

        assertTrue(homeAndChurch.size >= 90, "Expected at least 90 Home & Church hymns, got ${homeAndChurch.size}")
        assertTrue(classic.size >= 335, "Expected at least 335 classic 1985 hymns, got ${classic.size}")

        val spiritOfGod = com.thelightphone.gospellibrary.data.HymnsData.getHymnContent("hymn/1985/2")
        assertNotNull(spiritOfGod)
        assertTrue(spiritOfGod.title.contains("The Spirit of God"))
        assertTrue(spiritOfGod.verses.isNotEmpty())
        assertEquals("hymn/1985/1", spiritOfGod.prevId)
        assertEquals("hymn/1985/3", spiritOfGod.nextId)

        val comeThouFount = com.thelightphone.gospellibrary.data.HymnsData.getHymnContent("hymn/home-and-church/1001")
        assertNotNull(comeThouFount)
        assertTrue(comeThouFount.title.contains("Come, Thou Fount"))
        assertTrue(comeThouFount.verses.isNotEmpty())

        val allHymns = com.thelightphone.gospellibrary.data.HymnsData.getAllMergedHymns()
        val numSearch = com.thelightphone.gospellibrary.data.GospelSearchEngine.search(
            rawQuery = "1001",
            scope = com.thelightphone.gospellibrary.data.SearchScope.HYMNS,
            assetStreamProvider = null,
            conferenceTalks = emptyList(),
            hymns = allHymns,
        )
        assertTrue(numSearch.results.any { it.contentId == "hymn/home-and-church/1001" })

        val lyricSearch = com.thelightphone.gospellibrary.data.GospelSearchEngine.search(
            rawQuery = "Spirit of God like a fire",
            scope = com.thelightphone.gospellibrary.data.SearchScope.HYMNS,
            assetStreamProvider = null,
            conferenceTalks = emptyList(),
            hymns = allHymns,
        )
        assertTrue(lyricSearch.results.any { it.contentId == "hymn/1985/2" })
    }

    @Test
    fun hymnSyncManifestAndHtmlParsing() {
        val manifestHtml = """
            <h2 class="label"><p class="title">Easter</p></h2>
            <a href="/study/music/hymns-for-home-and-church/he-is-risen-new?lang=eng" class="list-tile">
              <p class="title">He Is Risen (New)</p>
            </a>
        """.trimIndent()
        val entries = com.thelightphone.gospellibrary.data.HymnSyncService.parseHomeAndChurchManifest(manifestHtml)
        assertEquals(1, entries.size)
        assertEquals("Easter", entries.first().topic)
        assertEquals("he-is-risen-new", entries.first().slug)
        assertEquals("/music/hymns-for-home-and-church/he-is-risen-new", entries.first().uri)

        val hymnHtml = """
            <header>
              <p class="song-number">1099</p>
              <h1>New Sample Hymn</h1>
            </header>
            <div class="poetry">
              <div class="stanza">
                <p class="line">1. First line of verse one,</p>
                <p class="line">Second line of verse one.</p>
              </div>
              <div class="chorus">
                <p class="label">Chorus</p>
                <p class="line">Sing praises forevermore.</p>
              </div>
            </div>
            <p class="credit">Text: Sample Author</p>
        """.trimIndent()
        val parsed = com.thelightphone.gospellibrary.data.HymnSyncService.parseHymnHtml(
            book = com.thelightphone.gospellibrary.model.HymnBook.HOME_AND_CHURCH,
            uri = "/music/hymns-for-home-and-church/new-sample-hymn",
            slug = "new-sample-hymn",
            topic = "Easter",
            htmlBody = hymnHtml,
            fallbackTitle = "Fallback",
        )
        assertNotNull(parsed)
        assertEquals("hymn/home-and-church/1099", parsed.id)
        assertEquals(1099, parsed.number)
        assertEquals("New Sample Hymn", parsed.title)
        val stanzas = com.thelightphone.gospellibrary.data.HymnsData.decodeStanzas(parsed.stanzasEncoded)
        assertEquals(2, stanzas.size)
        assertTrue(stanzas[0].contains("First line of verse one"))
        assertTrue(stanzas[1].startsWith("[Chorus]"))
        assertEquals("Text: Sample Author", parsed.credits)
    }
}
