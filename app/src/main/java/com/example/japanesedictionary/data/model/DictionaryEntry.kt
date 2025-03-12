package com.example.japanesedictionary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "kanji")
data class Kanji(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entryId: String,
    val kanji: String
)

@Entity(tableName = "reading")
data class Reading(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entryId: String,
    val reading: String
)

@Entity(tableName = "dictionary_entries")
data class DictionaryEntry(
    @PrimaryKey val id: String // Id của từ
)
