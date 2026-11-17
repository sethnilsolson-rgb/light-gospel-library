package com.thelightphone.gospellibrary.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE contentId = :contentId AND verseNumber IS NULL)")
    fun isChapterBookmarked(contentId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE contentId = :contentId AND verseNumber = :verseNumber)")
    fun isVerseBookmarked(contentId: String, verseNumber: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE contentId = :contentId AND verseNumber IS NULL")
    suspend fun deleteChapterBookmark(contentId: String)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("SELECT COUNT(*) FROM bookmarks")
    fun getBookmarkCount(): Flow<Int>
}

@Dao
interface HighlightDao {
    @Query("SELECT verseNumber FROM highlights WHERE contentId = :contentId")
    fun getHighlightedVerseNumbers(contentId: String): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity): Long

    @Query("DELETE FROM highlights WHERE contentId = :contentId AND verseNumber = :verseNumber")
    suspend fun removeHighlight(contentId: String, verseNumber: Int)
}

@Dao
interface LastReadDao {
    @Query("SELECT * FROM last_read WHERE recordId = 'singleton' LIMIT 1")
    fun getLastRead(): Flow<LastReadEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLastRead(entity: LastReadEntity)
}

@Dao
interface ConferenceTalkDao {
    @Query("SELECT * FROM conference_talks ORDER BY year DESC, sessionOrder ASC, talkOrder ASC")
    fun getAllTalks(): Flow<List<ConferenceTalkEntity>>

    @Query("SELECT * FROM conference_talks WHERE talkId = :talkId LIMIT 1")
    suspend fun getTalk(talkId: String): ConferenceTalkEntity?

    @Query("SELECT * FROM conference_talks ORDER BY year DESC, sessionOrder ASC, talkOrder ASC")
    suspend fun getAllTalksSync(): List<ConferenceTalkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTalks(talks: List<ConferenceTalkEntity>)

    @Query("SELECT COUNT(*) FROM conference_talks")
    suspend fun getCount(): Int
}
