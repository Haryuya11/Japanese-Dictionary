package com.example.japanesedictionary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.japanesedictionary.utils.Converters

@Entity(tableName = "senses")
@TypeConverters(Converters::class)
data class Sense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Id của sense
    val entryId: String, // Id của từ
    val pos: List<String>, // Loại từ
    val glosses: List<String>, // Danh sách nghĩa
    val misc: List<String>?, // Thông tin thêm
    val stagk: List<String>?, // Kanji giới hạn
    val stagr: List<String>?, // Hiragana/Katakana giới hạn
    val xref: List<String>?, // Tham khảo chéo
    val ant: List<String>?, // Từ trái nghĩa
    val sInf: List<String>? // Thông tin bổ sung
)

@Entity(tableName = "fields")
data class Field(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(primaryKeys = ["senseId", "fieldId"])
data class SenseFieldCrossRef(
    val senseId: Int,
    val fieldId: Int
)