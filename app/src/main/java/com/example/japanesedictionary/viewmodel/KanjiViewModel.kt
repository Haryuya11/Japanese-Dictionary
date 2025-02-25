package com.example.japanesedictionary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.japanesedictionary.data.DictionaryDatabase
import com.example.japanesedictionary.data.model.KanjiEntry
import com.example.japanesedictionary.data.model.KanjiReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KanjiViewModel(application: Application) : AndroidViewModel(application) {
    val dao = DictionaryDatabase.getDatabase(application).kanjiDao()

    fun getKanjiEntry(
        entryId: String,
        onResult: (KanjiEntry?, List<KanjiReading>) -> Unit
    ) {
        viewModelScope.launch {
            val kanjiEntry = withContext(Dispatchers.IO) {
                dao.getKanjiEntry(entryId)
            }
            val kanjiReadings = withContext(Dispatchers.IO) {
                kanjiEntry.let { dao.getKanjiReadings(entryId) }
            }
            onResult(kanjiEntry, kanjiReadings)
        }
    }
}