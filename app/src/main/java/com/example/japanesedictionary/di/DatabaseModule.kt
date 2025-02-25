package com.example.japanesedictionary.di

import android.app.Application
import androidx.room.Room
import com.example.japanesedictionary.data.DictionaryDatabase
import com.example.japanesedictionary.data.dao.DictionaryDao
import com.example.japanesedictionary.data.dao.KanjiDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(app: Application): DictionaryDatabase {
        return Room.databaseBuilder(
            app,
            DictionaryDatabase::class.java,
            "dictionary_database"
        )
            .build()
    }

    @Provides
    fun provideDictionaryDao(database: DictionaryDatabase): DictionaryDao {
        return database.dictionaryDao()
    }

    @Provides
    fun provideKanjiDao(database: DictionaryDatabase): KanjiDao {
        return database.kanjiDao()
    }
}