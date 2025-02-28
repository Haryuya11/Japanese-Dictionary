package com.example.japanesedictionary.data.model

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(tokenizer = "unicode61")
@Entity(tableName = "dictionary_fts")
data class DictionaryFTS(
    val entryId: String,
    val kanji: String,
    val reading: String,
    val reading_hiragana: String,
    val glosses: String
)