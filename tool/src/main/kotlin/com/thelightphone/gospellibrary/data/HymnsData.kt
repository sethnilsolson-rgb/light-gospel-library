package com.thelightphone.gospellibrary.data

import com.thelightphone.gospellibrary.db.SyncedHymnEntity
import com.thelightphone.gospellibrary.model.ContentChapter
import com.thelightphone.gospellibrary.model.ContentType
import com.thelightphone.gospellibrary.model.ContentVerse
import com.thelightphone.gospellibrary.model.HymnBook
import com.thelightphone.gospellibrary.model.HymnItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.util.zip.GZIPInputStream

@Serializable
private data class BundledHymnJson(
    val id: String = "",
    val bookId: String = "",
    val number: Int = 0,
    val slug: String = "",
    val uri: String = "",
    val title: String = "",
    val topic: String = "",
    val stanzas: List<String> = emptyList(),
    val credits: String = "",
)

@Serializable
private data class BundledHymnBookJson(
    val bookId: String = "",
    val hymns: List<BundledHymnJson> = emptyList(),
)

object HymnsData {

    private const val STANZA_SEP = "\u001E"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Volatile
    private var bundledClassic: List<HymnItem> = emptyList()

    @Volatile
    private var bundledHomeAndChurch: List<HymnItem> = emptyList()

    fun encodeStanzas(stanzas: List<String>): String = stanzas.joinToString(STANZA_SEP)

    fun decodeStanzas(encoded: String): List<String> =
        encoded.split(STANZA_SEP).map { it.trim() }.filter { it.isNotBlank() }

    fun ensureBundledLoaded(assetStreamProvider: ((String) -> InputStream?)?) {
        if (assetStreamProvider == null) return
        if (bundledClassic.isNotEmpty() && bundledHomeAndChurch.isNotEmpty()) return
        synchronized(this) {
            if (bundledHomeAndChurch.isEmpty()) {
                bundledHomeAndChurch = loadBookAsset(assetStreamProvider, HymnBook.HOME_AND_CHURCH)
            }
            if (bundledClassic.isEmpty()) {
                bundledClassic = loadBookAsset(assetStreamProvider, HymnBook.CLASSIC_1985)
            }
        }
    }

    private fun loadBookAsset(
        assetStreamProvider: (String) -> InputStream?,
        book: HymnBook,
    ): List<HymnItem> {
        val rawStream = runCatching { assetStreamProvider(book.assetPath) }.getOrNull() ?: return emptyList()
        return runCatching {
            val text = GZIPInputStream(rawStream).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val parsed = json.decodeFromString<BundledHymnBookJson>(text)
            parsed.hymns
                .filter { it.id.isNotBlank() && it.title.isNotBlank() && it.number > 0 }
                .map { h ->
                    HymnItem(
                        id = h.id,
                        bookId = h.bookId.ifBlank { book.id },
                        number = h.number,
                        slug = h.slug,
                        uri = h.uri,
                        title = h.title,
                        topic = h.topic,
                        stanzas = h.stanzas.filter { it.isNotBlank() },
                        credits = h.credits,
                    )
                }
                .sortedBy { it.number }
        }.getOrDefault(emptyList())
    }

    fun entityToItem(entity: SyncedHymnEntity): HymnItem = HymnItem(
        id = entity.id,
        bookId = entity.bookId,
        number = entity.number,
        slug = entity.slug,
        uri = entity.uri,
        title = entity.title,
        topic = entity.topic,
        stanzas = decodeStanzas(entity.stanzasEncoded),
        credits = entity.credits,
    )

    fun getHymnsForBook(
        book: HymnBook,
        synced: List<SyncedHymnEntity> = emptyList(),
    ): List<HymnItem> {
        val base = when (book) {
            HymnBook.HOME_AND_CHURCH -> bundledHomeAndChurch
            HymnBook.CLASSIC_1985 -> bundledClassic
        }
        val byNumber = LinkedHashMap<Int, HymnItem>()
        base.forEach { byNumber[it.number] = it }
        synced
            .filter { it.bookId == book.id }
            .forEach { entity ->
                val item = entityToItem(entity)
                if (item.number > 0 && item.title.isNotBlank()) {
                    byNumber[item.number] = item
                }
            }
        return byNumber.values.sortedBy { it.number }
    }

    fun getAllMergedHymns(synced: List<SyncedHymnEntity> = emptyList()): List<HymnItem> {
        val homeAndChurch = getHymnsForBook(HymnBook.HOME_AND_CHURCH, synced)
        val classic = getHymnsForBook(HymnBook.CLASSIC_1985, synced)
        return homeAndChurch + classic
    }

    fun getHymnContent(
        contentId: String,
        synced: List<SyncedHymnEntity> = emptyList(),
    ): ContentChapter? {
        val book = when {
            contentId.startsWith("hymn/home-and-church/") -> HymnBook.HOME_AND_CHURCH
            contentId.startsWith("hymn/1985/") -> HymnBook.CLASSIC_1985
            else -> return null
        }
        val list = getHymnsForBook(book, synced)
        val idx = list.indexOfFirst { it.id == contentId }
        if (idx < 0) return null

        val target = list[idx]
        val prev = if (idx > 0) list[idx - 1] else null
        val next = if (idx < list.lastIndex) list[idx + 1] else null

        val verses = buildList {
            target.stanzas.forEach { stanza ->
                val cleaned = stanza.trim()
                if (cleaned.isNotBlank()) {
                    add(ContentVerse(number = null, text = cleaned))
                }
            }
            if (target.credits.isNotBlank()) {
                add(ContentVerse(number = null, text = target.credits.trim()))
            }
        }

        val subtitle = buildString {
            append(book.shortTitle)
            if (target.topic.isNotBlank()) {
                append(" • ")
                append(target.topic)
            }
        }

        return ContentChapter(
            id = target.id,
            type = ContentType.HYMN,
            title = "#${target.number} ${target.title}",
            subtitle = subtitle,
            verses = verses,
            prevId = prev?.id,
            nextId = next?.id,
            prevLabel = prev?.let { "#${it.number} ${it.title}" },
            nextLabel = next?.let { "#${it.number} ${it.title}" },
        )
    }
}
