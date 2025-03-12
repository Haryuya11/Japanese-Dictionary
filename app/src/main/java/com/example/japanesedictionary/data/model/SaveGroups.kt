package com.example.japanesedictionary.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "save_groups")
data class SaveGroups(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(primaryKeys = ["groupId", "entryId"])
data class DictionaryGroupCrossRef(
    val groupId: Int,
    val entryId: String
)