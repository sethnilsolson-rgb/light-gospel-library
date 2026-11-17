package com.thelightphone.gospellibrary.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookmarkEntity::class,
        HighlightEntity::class,
        LastReadEntity::class,
        ConferenceTalkEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class GospelDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun highlightDao(): HighlightDao
    abstract fun lastReadDao(): LastReadDao
    abstract fun conferenceTalkDao(): ConferenceTalkDao
}
