package com.example.japanesedictionary.data.model

import androidx.room.Entity
import androidx.room.Fts4

// Định nghĩa entity cho bảng FTS
@Fts4
@Entity(tableName = "dictionary_fts")
data class DictionaryFTS(
    val entryId: String,
    val kanji: String,
    val reading: String,
    val reading_hiragana: String,
    val glosses: String
)