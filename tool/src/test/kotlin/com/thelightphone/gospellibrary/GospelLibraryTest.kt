package com.thelightphone.gospellibrary

import com.thelightphone.gospellibrary.data.ConferenceSyncService
import com.thelightphone.gospellibrary.data.GeneralConferenceData
import com.thelightphone.gospellibrary.data.StandardWorksData
import com.thelightphone.gospellibrary.model.ReaderFontSize
import kotlin.test.Test
import kotlin.test.assertEquals
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
        // Book of Mormon 1 Nephi 1
        val nephi1 = StandardWorksData.getChapter("bofm", "1-ne", 1)
        assertEquals("1 Nephi 1", nephi1.title)
        assertTrue(nephi1.verses.isNotEmpty())
        assertEquals(null, nephi1.prevId)
        assertEquals("scripture/bofm/1-ne/2", nephi1.nextId)
        assertEquals("1 Nephi 2", nephi1.nextLabel)

        // New Testament Matthew 5
        val matt5 = StandardWorksData.getChapter("nt", "matt", 5)
        assertEquals("Matthew 5", matt5.title)
        assertTrue(matt5.verses.any { it.text.contains("light of the world") })

        // Old Testament Psalm 23
        val ps23 = StandardWorksData.getChapter("ot", "ps", 23)
        assertEquals("Psalms 23", ps23.title)
        assertTrue(ps23.verses.any { it.text.contains("shepherd") })

        // Doctrine and Covenants Section 4
        val dc4 = StandardWorksData.getChapter("dc", "sections", 4)
        assertEquals("Section 4", dc4.title)
        assertTrue(dc4.verses.any { it.text.contains("marvelous work") })

        // Pearl of Great Price Articles of Faith
        val aOfF = StandardWorksData.getChapter("pgp", "a-of-f", 1)
        assertEquals("Articles of Faith 1", aOfF.title)
        assertTrue(aOfF.verses.any { it.text.contains("We believe in God") })
    }

    @Test
    fun generalConferenceOctoberTalks() {
        val periods = GeneralConferenceData.conferencePeriods
        assertEquals(1, periods.size)

        val october = periods.first()
        assertEquals("2026-10", october.id)
        assertEquals("October 2026", october.name)
        assertEquals(2026, october.year)
        assertEquals(5, october.sessions.size)

        val totalTalks = october.sessions.sumOf { it.talks.size }
        assertEquals(35, totalTalks)

        val nelsonTalk = GeneralConferenceData.findTalk("gc/2026-10/nelson-lord-jesus-christ-will-come")
        assertNotNull(nelsonTalk)
        assertEquals("The Lord Jesus Christ Will Come Again", nelsonTalk.title)
        assertEquals("President Russell M. Nelson", nelsonTalk.speaker)

        val talkContent = GeneralConferenceData.getTalkContent("gc/2026-10/nelson-lord-jesus-christ-will-come")
        assertEquals("The Lord Jesus Christ Will Come Again", talkContent.title)
        assertTrue(talkContent.verses.size >= 5)

        val hollandTalk = GeneralConferenceData.findTalk("gc/2026-10/holland-i-am-he")
        assertNotNull(hollandTalk)

        val freemanTalk = GeneralConferenceData.findTalk("gc/2026-10/freeman-live-up-privileges")
        assertNotNull(freemanTalk)
        assertEquals("President Emily Belle Freeman", freemanTalk.speaker)
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
                          "speaker": "President Russell M. Nelson",
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
