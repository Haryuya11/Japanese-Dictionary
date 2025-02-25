package com.example.japanesedictionary.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.japanesedictionary.data.model.KanjiEntry
import com.example.japanesedictionary.data.model.KanjiReading

@Dao
interface KanjiDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKanjiEntry(entry: KanjiEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKanjiEntries(entries: List<KanjiEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKanjiReading(reading: KanjiReading)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKanjiReadings(readings: List<KanjiReading>)

    @Query("SELECT COUNT(*) FROM kanji_entries")
    suspend fun getCount(): Int

    @Query("SELECT * FROM kanji_entries WHERE literal = :entryId")
    suspend fun getKanjiEntry(entryId: String): KanjiEntry

    @Query("SELECT * FROM kanji_readings WHERE kanjiLiteral = :entryId")
    suspend fun getKanjiReadings(entryId: String): List<KanjiReading>
}
