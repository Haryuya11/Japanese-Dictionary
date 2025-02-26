package com.example.japanesedictionary.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import com.example.japanesedictionary.data.model.*

@Dao
interface DictionaryDao {

    // **Dictionary Entry Queries**
    @Query("SELECT * FROM dictionary_entries WHERE id = :id")
    suspend fun getEntry(id: String): DictionaryEntry

    @Query("SELECT COUNT(*) FROM dictionary_entries")
    suspend fun getCount(): Int

    @Query("SELECT * FROM dictionary_entries WHERE id IN (:entryIds)")
    suspend fun getEntriesByIds(entryIds: List<String>): List<DictionaryEntry>

    @Query("SELECT * FROM dictionary_entries ORDER BY RANDOM() LIMIT 4")
    suspend fun getRandomEntry(): List<String>

    // **Kanji Queries**
    @Query("SELECT * FROM kanji WHERE entryId = :entryId")
    suspend fun getKanji(entryId: String): List<Kanji>

    // **Reading Queries**
    @Query("SELECT * FROM reading WHERE entryId = :entryId")
    suspend fun getReading(entryId: String): List<Reading>

    // **Sense Queries**
    @Query("SELECT * FROM senses WHERE entryId = :entryId")
    suspend fun getSenses(entryId: String): List<Sense>

    @Query("SELECT * FROM examples WHERE senseId = :senseId")
    suspend fun getExamples(senseId: Int): List<Example>

    // **Search Queries**
    @Query(
        """
        SELECT * FROM dictionary_entries
        WHERE id IN (
            SELECT entryId FROM kanji WHERE kanji = :query
            UNION
            SELECT entryId FROM reading WHERE reading = :query
        )
    """
    )
    suspend fun searchExactMatches(query: String): List<DictionaryEntry>

    @Query(
        """
        SELECT * FROM dictionary_entries
        WHERE id IN (
            SELECT entryId FROM kanji WHERE kanji LIKE :query
            UNION
            SELECT entryId FROM reading WHERE reading LIKE :query
        )
        AND id NOT IN (
            SELECT entryId FROM kanji WHERE kanji = :exactQuery
            UNION
            SELECT entryId FROM reading WHERE reading = :exactQuery
        )
        LIMIT 50
    """
    )
    suspend fun searchRelatedMatches(query: String, exactQuery: String): List<DictionaryEntry>

    @Query(
        """
    SELECT * FROM dictionary_entries
    WHERE id IN (
        SELECT entryId FROM senses
        WHERE glosses = :query
    )
    LIMIT 50
    """
    )
    suspend fun searchExactEnglishMeaning(query: String): List<DictionaryEntry>

    @Query(
        """
    SELECT * FROM dictionary_entries
    WHERE id IN (
        SELECT entryId FROM senses
        WHERE (glosses LIKE :query || '%' OR glosses LIKE '% ' || :query || '%')
          AND glosses != :exactQuery
    )
    LIMIT 50
    """
    )
    suspend fun searchRelatedEnglishMeaning(
        query: String,
        exactQuery: String
    ): List<DictionaryEntry>

    @Query(
        """
    SELECT k.entryId 
    FROM kanji k 
    JOIN reading r ON k.entryId = r.entryId 
    WHERE k.kanji LIKE :targetKanji || '%' 
    AND r.reading LIKE :reading || '%'
    LIMIT 10
    """
    )
    suspend fun getEntryIdsContainingKanji(targetKanji: String, reading: String): List<String>

    // **Field Queries**
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertField(field: Field): Long

    @Query("SELECT id FROM fields WHERE name = :name")
    suspend fun getFieldId(name: String): Int

    @Query("SELECT * FROM fields")
    suspend fun getAllFields(): List<Field>

    @Query(
        """
    SELECT fields.name 
    FROM fields
    INNER JOIN SenseFieldCrossRef 
        ON fields.id = SenseFieldCrossRef.fieldId
    WHERE SenseFieldCrossRef.senseId = :senseId
    """
    )
    suspend fun getFieldsForSense(senseId: Int): List<String>

    // **Group Queries**
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(groups: SaveGroups): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDictionaryGroupCrossRef(crossRef: DictionaryGroupCrossRef)

    @RewriteQueriesToDropUnusedColumns
    @Transaction
    @Query(
        """
    SELECT * FROM dictionary_entries 
    INNER JOIN dictionarygroupcrossref 
    ON dictionary_entries.id = dictionarygroupcrossref.entryId 
    WHERE dictionarygroupcrossref.groupId = :groupId
"""
    )
    fun getEntriesForGroup(groupId: Int): LiveData<List<DictionaryEntry>>

    @Transaction
    @Query("SELECT * FROM save_groups ORDER BY name ASC")
    fun getAllGroupsSortedByNameAsc(): LiveData<List<SaveGroups>>

    @Transaction
    @Query("SELECT * FROM save_groups ORDER BY name DESC")
    fun getAllGroupsSortedByNameDesc(): LiveData<List<SaveGroups>>

    @Transaction
    @Query("SELECT * FROM save_groups ORDER BY createdAt ASC")
    fun getAllGroupsSortedByDateAsc(): LiveData<List<SaveGroups>>

    @Transaction
    @Query("SELECT * FROM save_groups ORDER BY createdAt DESC")
    fun getAllGroupsSortedByDateDesc(): LiveData<List<SaveGroups>>

    @Query("SELECT COUNT(*) FROM dictionarygroupcrossref WHERE groupId = :groupId")
    fun getGroupSize(groupId: Int): LiveData<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM dictionarygroupcrossref WHERE groupId = :groupId AND entryId = :entryId)")
    suspend fun isEntryInGroup(entryId: String, groupId: Int): Boolean

    @Query("SELECT name FROM save_groups WHERE id = :groupId")
    fun getGroupName(groupId: Int): LiveData<String>

    @Query("DELETE FROM dictionarygroupcrossref WHERE groupId = :groupId AND entryId IN (:entryIds)")
    suspend fun deleteEntriesFromGroup(entryIds: List<String>, groupId: Int)

    // **Insert Queries**
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDictionaryEntries(entries: List<DictionaryEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKanjiList(kanjis: List<Kanji>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingList(readings: List<Reading>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSenseList(senses: List<Sense>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExampleList(examples: List<Example>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSenseFieldCrossRefs(crossRefs: List<SenseFieldCrossRef>)
}