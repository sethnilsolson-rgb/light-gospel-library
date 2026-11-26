package com.thelightphone.gospellibrary.data

import com.thelightphone.gospellibrary.db.ConferenceTalkEntity
import com.thelightphone.gospellibrary.model.ConferencePeriod
import com.thelightphone.gospellibrary.model.ConferenceSessionGroup
import com.thelightphone.gospellibrary.model.ConferenceTalk
import com.thelightphone.gospellibrary.model.ContentChapter
import com.thelightphone.gospellibrary.model.ContentType
import com.thelightphone.gospellibrary.model.ContentVerse
import java.io.InputStream

object GeneralConferenceData {

    const val BUNDLED_CONFERENCE_ASSET = "conference/2026-10.json"

    @Volatile
    private var bundledEntities: List<ConferenceTalkEntity> = emptyList()

    /**
     * Loads the bundled complete October 2026 General Conference talks from app assets once.
     */
    fun ensureBundledLoaded(assetStreamProvider: ((String) -> InputStream?)?): List<ConferenceTalkEntity> {
        if (bundledEntities.isNotEmpty()) return bundledEntities
        return synchronized(this) {
            if (bundledEntities.isEmpty()) {
                val stream = assetStreamProvider?.invoke(BUNDLED_CONFERENCE_ASSET) ?: return@synchronized bundledEntities
                val jsonText = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                bundledEntities = ConferenceSyncService.parseConferenceJson(jsonText)
            }
            bundledEntities
        }
    }

    /**
     * Returns all bundled October 2026 entities (empty if not yet loaded).
     */
    fun getBundledTalkEntities(): List<ConferenceTalkEntity> = bundledEntities

    /**
     * Merges bundled talks (October 2026) with any dynamically synced talks from Room DB.
     * Newer conferences appear first; within each period, sessions and talks preserve conference order.
     */
    fun getConferencePeriods(
        dbTalks: List<ConferenceTalkEntity> = emptyList(),
    ): List<ConferencePeriod> {
        val mergedById = LinkedHashMap<String, ConferenceTalkEntity>()
        // Start with bundled October 2026 talks
        bundledEntities.forEach { mergedById[it.talkId] = it }
        // Overlay / add any downloaded talks from DB (ignoring legacy 2024-10 or non-authentic slugs)
        dbTalks
            .filter { it.periodId >= "2026-10" && !it.talkId.contains("2024-10") }
            .forEach { mergedById[it.talkId] = it }

        val allEntities = mergedById.values.toList()
        if (allEntities.isEmpty()) return emptyList()

        return allEntities
            .groupBy { it.periodId }
            .entries
            .sortedByDescending { it.key } // e.g. "2027-04" > "2026-10"
            .map { (periodId, talksInPeriod) ->
                val first = talksInPeriod.first()
                val sessions = talksInPeriod
                    .groupBy { it.sessionOrder to it.sessionName }
                    .entries
                    .sortedBy { it.key.first }
                    .map { (_, sessionTalks) ->
                        val sortedTalks = sessionTalks.sortedBy { it.talkOrder }
                        ConferenceSessionGroup(
                            sessionName = sortedTalks.first().sessionName,
                            talks = sortedTalks.map { entity ->
                                ConferenceTalk(
                                    id = entity.talkId,
                                    title = entity.title,
                                    speaker = entity.speaker,
                                    speakerTitle = entity.speakerTitle,
                                    session = entity.sessionName,
                                    period = entity.periodName,
                                    summary = entity.summary,
                                )
                            }
                        )
                    }

                ConferencePeriod(
                    id = periodId,
                    name = first.periodName,
                    year = first.year,
                    month = first.month,
                    sessions = sessions,
                )
            }
    }

    /**
     * Builds a full [ContentChapter] for a conference talk with proper previous/next talk navigation
     * within the same conference period.
     */
    fun getTalkContent(
        talkId: String,
        dbTalks: List<ConferenceTalkEntity> = emptyList(),
    ): ContentChapter? {
        val mergedById = LinkedHashMap<String, ConferenceTalkEntity>()
        bundledEntities.forEach { mergedById[it.talkId] = it }
        dbTalks
            .filter { it.periodId >= "2026-10" && !it.talkId.contains("2024-10") }
            .forEach { mergedById[it.talkId] = it }

        val target = mergedById[talkId] ?: return null

        val periodTalks = mergedById.values
            .filter { it.periodId == target.periodId }
            .sortedWith(compareBy<ConferenceTalkEntity> { it.sessionOrder }.thenBy { it.talkOrder })

        val idx = periodTalks.indexOfFirst { it.talkId == target.talkId }
        val prevTalk = if (idx > 0) periodTalks[idx - 1] else null
        val nextTalk = if (idx in 0 until periodTalks.lastIndex) periodTalks[idx + 1] else null

        val paragraphs = target.paragraphs
            .split("\n\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { paragraph -> ContentVerse(number = null, text = paragraph) }

        val subtitle = buildString {
            if (target.speaker.isNotBlank()) append(target.speaker)
            if (target.periodName.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(target.periodName)
            }
        }

        return ContentChapter(
            id = target.talkId,
            type = ContentType.GENERAL_CONFERENCE,
            title = target.title,
            subtitle = subtitle,
            verses = paragraphs,
            prevId = prevTalk?.talkId,
            nextId = nextTalk?.talkId,
            prevLabel = prevTalk?.title,
            nextLabel = nextTalk?.title,
        )
    }
}
