package com.example.japanesedictionary.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.japanesedictionary.data.dao.DictionaryDao
import com.example.japanesedictionary.data.dao.KanjiDao
import com.example.japanesedictionary.data.model.DictionaryEntry
import com.example.japanesedictionary.data.model.DictionaryGroupCrossRef
import com.example.japanesedictionary.data.model.Example
import com.example.japanesedictionary.data.model.Field
import com.example.japanesedictionary.data.model.Kanji
import com.example.japanesedictionary.data.model.KanjiEntry
import com.example.japanesedictionary.data.model.KanjiReading
import com.example.japanesedictionary.data.model.Reading
import com.example.japanesedictionary.data.model.SaveGroups
import com.example.japanesedictionary.data.model.Sense
import com.example.japanesedictionary.data.model.SenseFieldCrossRef
import com.example.japanesedictionary.utils.Converters

@Database(
    entities = [
        DictionaryEntry::class,
        Kanji::class,
        Reading::class,
        Sense::class,
        Example::class,
        Field::class,
        SenseFieldCrossRef::class,
        KanjiEntry::class,
        KanjiReading::class,
        SaveGroups::class,
        DictionaryGroupCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DictionaryDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun kanjiDao(): KanjiDao

    companion object {
        @Volatile
        private var INSTANCE: DictionaryDatabase? = null
        fun getDatabase(context: Context): DictionaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DictionaryDatabase::class.java,
                    "dictionary_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}