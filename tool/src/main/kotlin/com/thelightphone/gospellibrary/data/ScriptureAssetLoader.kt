package com.thelightphone.gospellibrary.data

import android.util.JsonReader
import com.thelightphone.gospellibrary.model.ContentVerse
import java.io.InputStream
import java.io.InputStreamReader

object ScriptureAssetLoader {

    fun loadVerses(
        assetStreamProvider: ((String) -> InputStream?)?,
        volumeId: String,
        bookShortTitle: String,
        chapterNum: Int,
    ): List<ContentVerse> {
        if (assetStreamProvider == null) return emptyList()

        val assetPath = when (volumeId) {
            "bofm" -> "scriptures/book-of-mormon.json"
            "nt" -> "scriptures/new-testament.json"
            "ot" -> "scriptures/old-testament.json"
            "dc" -> "scriptures/doctrine-and-covenants.json"
            "pgp" -> "scriptures/pearl-of-great-price.json"
            else -> return emptyList()
        }

        val stream = try {
            assetStreamProvider(assetPath)
        } catch (_: Exception) {
            null
        } ?: return emptyList()

        return try {
            stream.use { inputStream ->
                JsonReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    if (volumeId == "dc") {
                        readDcSection(reader, chapterNum)
                    } else {
                        readBookChapter(reader, bookShortTitle, chapterNum)
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun readBookChapter(
        reader: JsonReader,
        bookTarget: String,
        chapterTarget: Int,
    ): List<ContentVerse> {
        reader.beginObject()
        var foundVerses: List<ContentVerse>? = null
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (name == "books") {
                reader.beginArray()
                while (reader.hasNext()) {
                    if (foundVerses != null) {
                        reader.skipValue()
                    } else {
                        val verses = readBookObject(reader, bookTarget, chapterTarget)
                        if (verses != null) {
                            foundVerses = verses
                        }
                    }
                }
                reader.endArray()
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()
        return foundVerses ?: emptyList()
    }

    private fun readBookObject(
        reader: JsonReader,
        bookTarget: String,
        chapterTarget: Int,
    ): List<ContentVerse>? {
        reader.beginObject()
        var isTargetBook = false
        var matchedVerses: List<ContentVerse>? = null

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "book" -> {
                    val bookName = reader.nextString()
                    if (isMatchingBook(bookName, bookTarget)) {
                        isTargetBook = true
                    }
                }
                "chapters" -> {
                    if (isTargetBook) {
                        matchedVerses = readChaptersArray(reader, chapterTarget)
                    } else {
                        reader.skipValue()
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return matchedVerses
    }

    private fun isMatchingBook(bookInJson: String, target: String): Boolean {
        if (bookInJson.equals(target, ignoreCase = true)) return true
        val normJson = bookInJson.replace("—", "-").replace(" ", "").lowercase()
        val normTarget = target.replace("—", "-").replace(" ", "").lowercase()
        if (normJson == normTarget) return true
        if (normTarget.contains("solomon") && normJson.contains("solomon")) return true
        if (normTarget.startsWith("js") && normJson.contains("josephsmith")) {
            val isMat = normTarget.contains("matthew")
            return if (isMat) normJson.contains("matthew") else normJson.contains("history")
        }
        return false
    }

    private fun readChaptersArray(
        reader: JsonReader,
        chapterTarget: Int,
    ): List<ContentVerse>? {
        reader.beginArray()
        var result: List<ContentVerse>? = null
        while (reader.hasNext()) {
            if (result != null) {
                reader.skipValue()
            } else {
                reader.beginObject()
                var isTargetChapter = false
                var verses: List<ContentVerse>? = null
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "chapter" -> {
                            if (reader.nextInt() == chapterTarget) {
                                isTargetChapter = true
                            }
                        }
                        "verses" -> {
                            if (isTargetChapter) {
                                verses = readVersesArray(reader)
                            } else {
                                reader.skipValue()
                            }
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                if (verses != null) {
                    result = verses
                }
            }
        }
        reader.endArray()
        return result
    }

    private fun readVersesArray(reader: JsonReader): List<ContentVerse> {
        val list = mutableListOf<ContentVerse>()
        reader.beginArray()
        while (reader.hasNext()) {
            reader.beginObject()
            var verseNum: Int? = null
            var text = ""
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "verse" -> verseNum = reader.nextInt()
                    "text" -> text = reader.nextString()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            if (text.isNotBlank()) {
                list.add(ContentVerse(verseNum, text))
            }
        }
        reader.endArray()
        return list
    }

    private fun readDcSection(reader: JsonReader, sectionTarget: Int): List<ContentVerse> {
        reader.beginObject()
        var foundVerses: List<ContentVerse>? = null
        while (reader.hasNext()) {
            if (reader.nextName() == "sections") {
                reader.beginArray()
                while (reader.hasNext()) {
                    if (foundVerses != null) {
                        reader.skipValue()
                    } else {
                        reader.beginObject()
                        var isTarget = false
                        var verses: List<ContentVerse>? = null
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "section" -> {
                                    if (reader.nextInt() == sectionTarget) {
                                        isTarget = true
                                    }
                                }
                                "verses" -> {
                                    if (isTarget) {
                                        verses = readVersesArray(reader)
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()
                        if (verses != null) {
                            foundVerses = verses
                        }
                    }
                }
                reader.endArray()
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()
        return foundVerses ?: emptyList()
    }
}
