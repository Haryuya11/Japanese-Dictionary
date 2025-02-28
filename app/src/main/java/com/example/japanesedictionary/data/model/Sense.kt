package com.example.japanesedictionary.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.japanesedictionary.utils.Converters

@Entity(
    tableName = "senses",
    foreignKeys = [ForeignKey(
        entity = DictionaryEntry::class,
        parentColumns = ["id"],
        childColumns = ["entryId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["entryId"])]
)
@TypeConverters(Converters::class)
data class Sense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entryId: String,
    val pos: List<String>,
    val glosses: List<String>,
    val misc: List<String>?,
    val stagk: List<String>?,
    val stagr: List<String>?,
    val xref: List<String>?,
    val ant: List<String>?,
    val sInf: List<String>?
)

@Entity(tableName = "fields")
data class Field(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(
    primaryKeys = ["senseId", "fieldId"],
    foreignKeys = [
        ForeignKey(
            entity = Sense::class,
            parentColumns = ["id"],
            childColumns = ["senseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Field::class,
            parentColumns = ["id"],
            childColumns = ["fieldId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["senseId"]),
        Index(value = ["fieldId"])
    ]
)
data class SenseFieldCrossRef(
    val senseId: Int,
    val fieldId: Int
)