package com.example.japanesedictionary.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "examples",
    foreignKeys = [ForeignKey(
        entity = Sense::class,
        parentColumns = ["id"],
        childColumns = ["senseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["senseId"])] // Thêm chỉ mục cho senseId
)
data class Example(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senseId: Int,
    val exText: String,
    val exSentJpn: String,
    val exSentEng: String
)