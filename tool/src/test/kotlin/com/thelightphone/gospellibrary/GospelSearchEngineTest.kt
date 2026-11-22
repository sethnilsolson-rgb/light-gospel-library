package com.thelightphone.gospellibrary

import com.thelightphone.gospellibrary.data.GospelSearchEngine
import com.thelightphone.gospellibrary.data.SearchMatchKind
import com.thelightphone.gospellibrary.data.SearchScope
import com.thelightphone.gospellibrary.db.ConferenceTalkEntity
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GospelSearchEngineTest {

    private fun fakeAssets(): (String) -> InputStream? = { path ->
        when (path) {
            "scriptures/new-testament.json" -> """
                {
                  "books": [
                    {
                      "book": "John",
                      "chapters": [
                        {
                          "chapter": 14,
                          "verses": [
                            {"verse": 27, "text": "Peace I leave with you, my peace I give unto you: not as the world giveth, give I unto you. Let not your heart be troubled, neither let it be afraid."}
                          ]
                        }
                      ]
                    },
                    {
                      "book": "Matthew",
                      "chapters": [
                        {
                          "chapter": 11,
                          "verses": [
                            {"verse": 28, "text": "Come unto me, all ye that labour and are heavy laden, and I will give you rest."}
                          ]
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent().byteInputStream()
            "scriptures/book-of-mormon.json" -> """
                {
                  "books": [
                    {
                      "book": "Alma",
                      "chapters": [
                        {
                          "chapter": 32,
                          "verses": [
                            {"verse": 21, "text": "And now as I said concerning faith—faith is not to have a perfect knowledge of things; therefore if ye have faith ye hope for things which are not seen, which are true."}
                          ]
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent().byteInputStream()
            else -> ByteArrayInputStream("""{"books":[],"sections":[]}""".toByteArray())
        }
    }

    private fun sampleConferenceTalks(): List<ConferenceTalkEntity> = listOf(
        ConferenceTalkEntity(
            talkId = "gc/2026-10/peace-in-christ",
            periodId = "2026-10",
            periodName = "October 2026",
            year = 2026,
            month = "October",
            sessionName = "Sunday Morning Session",
            sessionOrder = 3,
            talkOrder = 1,
            title = "Finding Peace Amid Heavy Burdens",
            speaker = "Elder Sample",
            speakerTitle = "Of the Quorum of the Twelve Apostles",
            summary = "How the Savior calms troubled hearts and lightens our burdens.",
            paragraphs = "When life feels heavy, the Savior invites us to bring our burdens unto Him.\n\nHe promises consolation and peace that passes all understanding, letting our troubled hearts rest.",
        )
    )

    @Test
    fun parsesCommonScriptureReferencesAndAbbreviations() {
        val r1 = GospelSearchEngine.parseReference("Alma 32:21")
        assertNotNull(r1)
        assertEquals("bofm", r1.volumeId)
        assertEquals("alma", r1.book.id)
        assertEquals(32, r1.chapter)
        assertEquals(21, r1.verse)

        val r2 = GospelSearchEngine.parseReference("1 ne 3:7")
        assertNotNull(r2)
        assertEquals("1-ne", r2.book.id)
        assertEquals(3, r2.chapter)
        assertEquals(7, r2.verse)

        val r3 = GospelSearchEngine.parseReference("D&C 121")
        assertNotNull(r3)
        assertEquals("dc", r3.volumeId)
        assertEquals(121, r3.chapter)
        assertEquals(null, r3.verse)
    }

    @Test
    fun searchesExactPhrasesAndReferenceJumps() {
        val refResp = GospelSearchEngine.search(
            rawQuery = "Alma 32:21",
            scope = SearchScope.ALL,
            assetStreamProvider = fakeAssets(),
            conferenceTalks = sampleConferenceTalks(),
        )
        assertTrue(refResp.results.isNotEmpty())
        assertEquals(SearchMatchKind.REFERENCE, refResp.results.first().matchKind)
        assertEquals("scripture/bofm/alma/32", refResp.results.first().contentId)

        val phraseResp = GospelSearchEngine.search(
            rawQuery = "heavy laden",
            scope = SearchScope.ALL,
            assetStreamProvider = fakeAssets(),
            conferenceTalks = sampleConferenceTalks(),
        )
        assertTrue(phraseResp.results.any { it.title == "Matthew 11:28" && it.matchKind == SearchMatchKind.EXACT })
    }

    @Test
    fun findsRelatedScripturesAndConferenceTalksForModernIdeaPhrases() {
        val resp = GospelSearchEngine.search(
            rawQuery = "feeling anxious and overwhelmed",
            scope = SearchScope.ALL,
            assetStreamProvider = fakeAssets(),
            conferenceTalks = sampleConferenceTalks(),
        )
        assertTrue(resp.matchedTopics.contains("Peace & Anxiety"), "Expected Peace & Anxiety topic, got: ${resp.matchedTopics}")
        assertTrue(
            resp.results.any { it.title == "John 14:27" && it.matchKind == SearchMatchKind.CONCEPT },
            "Expected conceptual match for John 14:27, got: ${resp.results.map { it.title to it.matchKind }}",
        )
        assertTrue(
            resp.results.any { it.title == "Matthew 11:28" && it.matchKind == SearchMatchKind.CONCEPT },
            "Expected conceptual match for Matthew 11:28",
        )
        assertTrue(
            resp.results.any { it.contentId == "gc/2026-10/peace-in-christ" },
            "Expected matching General Conference talk, got: ${resp.results.map { it.title }}",
        )
    }
}
