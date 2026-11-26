package com.thelightphone.gospellibrary

import com.thelightphone.gospellibrary.data.GospelSearchEngine
import com.thelightphone.gospellibrary.data.SearchMatchKind
import com.thelightphone.gospellibrary.data.SearchScope
import com.thelightphone.gospellibrary.db.ConferenceTalkEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun enforcesWordBoundariesAndSupportsTwoCharQueries() {
        val boundaryAssets: (String) -> InputStream? = { path ->
            when (path) {
                "scriptures/new-testament.json" -> """
                    {
                      "books": [
                        {
                          "book": "Matthew",
                          "chapters": [
                            {
                              "chapter": 5,
                              "verses": [
                                {"verse": 1, "text": "And he opened his mouth to sing a blessing toward every person."},
                                {"verse": 2, "text": "Repent of sin and walk in peace."},
                                {"verse": 3, "text": "Do not muzzle the ox that treadeth out the corn, nor put it in a box."}
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

        val sinResp = GospelSearchEngine.search(
            rawQuery = "sin",
            scope = SearchScope.NT,
            assetStreamProvider = boundaryAssets,
            conferenceTalks = emptyList(),
        )
        assertTrue(sinResp.results.any { it.title == "Matthew 5:2" })
        assertFalse(
            sinResp.results.any { it.title == "Matthew 5:1" },
            "Searching 'sin' must not match 'blessing' or 'sing'",
        )

        val warResp = GospelSearchEngine.search(
            rawQuery = "war",
            scope = SearchScope.NT,
            assetStreamProvider = boundaryAssets,
            conferenceTalks = emptyList(),
        )
        assertFalse(
            warResp.results.any { it.title == "Matthew 5:1" },
            "Searching 'war' must not match 'toward'",
        )

        val oxResp = GospelSearchEngine.search(
            rawQuery = "ox",
            scope = SearchScope.NT,
            assetStreamProvider = boundaryAssets,
            conferenceTalks = emptyList(),
        )
        assertTrue(
            oxResp.results.any { it.title == "Matthew 5:3" },
            "2-character query 'ox' should match Matthew 5:3",
        )
    }

    @Test
    fun streamsGzippedOldTestamentAndClosesCorruptStream() {
        val otJson = """
            {
              "books": [
                {
                  "book": "Genesis",
                  "chapters": [
                    {
                      "chapter": 11,
                      "verses": [
                        {"verse": 28, "text": "And Haran died before his father Terah in the land of his nativity, in Ur of the Chaldees."}
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val gzippedBytes = ByteArrayOutputStream().use { bos ->
            GZIPOutputStream(bos).use { gz -> gz.write(otJson.toByteArray(Charsets.UTF_8)) }
            bos.toByteArray()
        }

        val otResp = GospelSearchEngine.search(
            rawQuery = "Ur",
            scope = SearchScope.OT,
            assetStreamProvider = { path ->
                if (path == "scriptures/old-testament.bin") ByteArrayInputStream(gzippedBytes) else null
            },
            conferenceTalks = emptyList(),
        )
        assertTrue(otResp.results.any { it.title == "Genesis 11:28" })

        val streamClosed = AtomicBoolean(false)
        val corruptStream = object : ByteArrayInputStream("not-gzip".toByteArray()) {
            override fun close() {
                streamClosed.set(true)
                super.close()
            }
        }
        val corruptResp = GospelSearchEngine.search(
            rawQuery = "Genesis",
            scope = SearchScope.OT,
            assetStreamProvider = { path ->
                if (path == "scriptures/old-testament.bin") corruptStream else null
            },
            conferenceTalks = emptyList(),
        )
        assertTrue(streamClosed.get(), "rawStream must be closed even when GZIPInputStream throws")
        assertTrue(corruptResp.results.isEmpty())
    }
}
