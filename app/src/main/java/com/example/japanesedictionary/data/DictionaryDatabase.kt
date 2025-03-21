package com.example.japanesedictionary.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.japanesedictionary.data.dao.DictionaryDao
import com.example.japanesedictionary.data.dao.KanjiDao
import com.example.japanesedictionary.data.model.*
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
        DictionaryGroupCrossRef::class,
        DictionaryFTS::class
    ],
    version = 4,
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
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE dictionary_fts USING fts4(
                        entryId TEXT,
                        kanji TEXT,
                        reading TEXT,
                        glosses TEXT,
                        tokenize=simple
                    )
                """
                )
                db.execSQL(
                    """
                    INSERT INTO dictionary_fts (entryId, kanji, reading, glosses)
                    SELECT de.id, k.kanji, r.reading, s.glosses
                    FROM dictionary_entries de
                    LEFT JOIN kanji k ON de.id = k.entryId
                    LEFT JOIN reading r ON de.id = r.entryId
                    LEFT JOIN senses s ON de.id = s.entryId
                """
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop the old FTS table; Room will recreate it based on DictionaryFTS entity
                db.execSQL("DROP TABLE IF EXISTS dictionary_fts")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Xóa và tạo lại các bảng không có foreign key
                db.execSQL("DROP TABLE IF EXISTS kanji")
                db.execSQL(
                    """
                    CREATE TABLE kanji (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        entryId TEXT,
                        kanji TEXT
                    )
                """
                )

                db.execSQL("DROP TABLE IF EXISTS reading")
                db.execSQL(
                    """
                    CREATE TABLE reading (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        entryId TEXT,
                        reading TEXT
                    )
                """
                )

                db.execSQL("DROP TABLE IF EXISTS senses")
                db.execSQL(
                    """
                    CREATE TABLE senses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        entryId TEXT,
                        pos TEXT,
                        glosses TEXT,
                        misc TEXT,
                        stagk TEXT,
                        stagr TEXT,
                        xref TEXT,
                        ant TEXT,
                        sInf TEXT
                    )
                """
                )
            }
        }
    }
}