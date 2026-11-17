package com.thelightphone.gospellibrary.data

import com.thelightphone.gospellibrary.db.ConferenceTalkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI

@Serializable
data class RemoteConferenceResponse(
    val periods: List<RemotePeriod> = emptyList(),
)

@Serializable
data class RemotePeriod(
    val id: String = "",
    val name: String = "",
    val year: Int = 2026,
    val month: String = "",
    val sessions: List<RemoteSession> = emptyList(),
)

@Serializable
data class RemoteSession(
    val sessionName: String = "",
    val talks: List<RemoteTalk> = emptyList(),
)

@Serializable
data class RemoteTalk(
    val id: String = "",
    val title: String = "",
    val speaker: String = "",
    val speakerTitle: String = "",
    val summary: String = "",
    val paragraphs: List<String> = emptyList(),
    val content: String = "",
)

object ConferenceSyncService {

    const val DEFAULT_UPDATE_URL =
        "https://raw.githubusercontent.com/setholson/gospel-library-data/main/general-conference.json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun fetchRemoteTalks(
        urlString: String = DEFAULT_UPDATE_URL,
    ): Result<List<ConferenceTalkEntity>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URI(urlString).toURL()
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 10000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "GospelLibrary-LightPhone/1.0")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                error("HTTP $responseCode from $urlString")
            }

            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseConferenceJson(body)
        }
    }

    fun parseConferenceJson(jsonString: String): List<ConferenceTalkEntity> {
        val response = json.decodeFromString<RemoteConferenceResponse>(jsonString)
        val entities = mutableListOf<ConferenceTalkEntity>()

        response.periods.forEach { period ->
            period.sessions.forEachIndexed { sIdx, session ->
                session.talks.forEachIndexed { tIdx, talk ->
                    val paragraphsText = if (talk.paragraphs.isNotEmpty()) {
                        talk.paragraphs.joinToString("\n\n")
                    } else {
                        talk.content
                    }

                    if (talk.id.isNotBlank() && talk.title.isNotBlank()) {
                        entities.add(
                            ConferenceTalkEntity(
                                talkId = talk.id,
                                periodId = period.id,
                                periodName = period.name,
                                year = period.year,
                                month = period.month,
                                sessionName = session.sessionName,
                                sessionOrder = sIdx,
                                talkOrder = tIdx,
                                title = talk.title,
                                speaker = talk.speaker,
                                speakerTitle = talk.speakerTitle,
                                summary = talk.summary,
                                paragraphs = paragraphsText,
                            )
                        )
                    }
                }
            }
        }
        return entities
    }
}
