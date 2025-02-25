package com.example.japanesedictionary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kanji_entries")
data class KanjiEntry(
    @PrimaryKey val literal: String, // Kanji character
    val strokeCount: Int, // Number of strokes
    val freq: Int?, // Frequency in newspapers
    val jlpt: Int?, // JLPT level
    val meanings: List<String>,// Meanings
    val fileSvgName: String? // File name of the SVG image
)

@Entity(tableName = "kanji_readings")
data class KanjiReading(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val kanjiLiteral: String, // Kanji character
    val reading: String, // Reading
    val type: String // Type of reading (ja_on or ja_kun)
)