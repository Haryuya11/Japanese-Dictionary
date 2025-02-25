package com.example.japanesedictionary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "examples")
data class Example(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Id của ví dụ
    val senseId : Int, // Id của sense
    val exText: String, // Ví dụ
    val exSentJpn: String, // Ví dụ tiếng Nhật
    val exSentEng: String // Ví dụ tiếng Anh
)
