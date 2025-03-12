package com.example.japanesedictionary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "examples")
data class Example(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senseId: Int,
    val exText: String,
    val exSentJpn: String,
    val exSentEng: String
)