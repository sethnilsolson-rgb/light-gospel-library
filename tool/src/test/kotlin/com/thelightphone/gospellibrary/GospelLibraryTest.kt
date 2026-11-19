package com.thelightphone.gospellibrary

import com.thelightphone.gospellibrary.data.ConferenceSyncService
import com.thelightphone.gospellibrary.data.GeneralConferenceData
import com.thelightphone.gospellibrary.data.StandardWorksData
import com.thelightphone.gospellibrary.model.ReaderFontSize
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
}
