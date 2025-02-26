package com.example.japanesedictionary.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.*
import com.example.japanesedictionary.data.DictionaryDatabase
import com.example.japanesedictionary.data.model.*
import com.example.japanesedictionary.utils.isKanji
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

class DictionaryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = DictionaryDatabase.getDatabase(application).dictionaryDao()
    private val sharedPreferences = application.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    var query = mutableStateOf("")
    var searchResults = mutableStateOf(listOf<DictionaryEntry>())
    var suggestions =
        mutableStateOf(listOf<Pair<Triple<DictionaryEntry, String?, String?>, String>>())
    var searchHistory = mutableStateOf(listOf<String>())
    var searchMode = mutableStateOf("ja")
    var kanjiSet = mutableStateOf(setOf<Char>())
    var isSelectionMode = mutableStateOf(false)
    var selectedItems = mutableStateListOf<String>()

    init {
        loadSearchHistory()
    }

    // **Search Functions**
    fun searchWord(query: String) {
        this.query.value = query
        viewModelScope.launch {
            if (query.isEmpty()) {
                searchResults.value = emptyList()
                return@launch
            }
            if (searchMode.value == "ja") {
                searchJapaneseMode(query)
            } else if (searchMode.value == "en") {
                searchEnglishWordMode(query)
            }
        }
    }

    private suspend fun searchJapaneseMode(query: String) {
        withContext(Dispatchers.IO) {
            val exactMatches = dao.searchExactMatches(query)
            val relatedMatches = dao.searchRelatedMatches("%$query%", query)
            searchResults.value = exactMatches + relatedMatches
        }
    }

    private suspend fun searchEnglishWordMode(query: String) {
        withContext(Dispatchers.IO) {
            val exactMatches = dao.searchExactEnglishMeaning(query)
            val relatedMatches = dao.searchRelatedEnglishMeaning("%$query%", query)
            searchResults.value = exactMatches + relatedMatches
        }
    }

    fun searchWordById(
        id: String,
        onResult: (DictionaryEntry, List<Kanji>, List<Reading>, List<Sense>, List<Example>) -> Unit
    ) {
        viewModelScope.launch {
            val entry = withContext(Dispatchers.IO) { dao.getEntry(id) }
            val kanji = withContext(Dispatchers.IO) { dao.getKanji(id) }
            val reading = withContext(Dispatchers.IO) { dao.getReading(id) }
            val senses = withContext(Dispatchers.IO) { dao.getSenses(id) }
            val examples = senses.flatMap { sense ->
                withContext(Dispatchers.IO) { dao.getExamples(sense.id) }
            }
            onResult(entry, kanji, reading, senses, examples)
        }
    }

    fun fetchSuggestions(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                suggestions.value = emptyList()
                return@launch
            }
            if (searchMode.value == "ja") {
                fetchJapaneseSuggestionsMode(query)
            } else if (searchMode.value == "en") {
                fetchEnglishSuggestionsMode(query)
            }
        }
    }

    private suspend fun fetchJapaneseSuggestionsMode(query: String) {
        withContext(Dispatchers.IO) {
            val exactMatches = dao.searchExactMatches(query)
            val relatedMatches = dao.searchRelatedMatches("%$query%", query)
            suggestions.value = (exactMatches + relatedMatches).map { entry ->
                val kanji = dao.getKanji(entry.id).firstOrNull()?.kanji
                val reading = dao.getReading(entry.id).firstOrNull()?.reading
                val meaning = dao.getSenses(entry.id).flatMap { it.glosses }.joinToString(", ")
                Triple(entry, kanji, reading) to meaning
            }
        }
    }

    private suspend fun fetchEnglishSuggestionsMode(query: String) {
        withContext(Dispatchers.IO) {
            val exactMatches = dao.searchExactEnglishMeaning(query)
            val relatedMatches = dao.searchRelatedEnglishMeaning("%$query%", query)
            val entries = exactMatches + relatedMatches
            suggestions.value = entries.map { entry ->
                val kanji = dao.getKanji(entry.id).firstOrNull()?.kanji
                val reading = dao.getReading(entry.id).firstOrNull()?.reading
                val meaning = dao.getSenses(entry.id).flatMap { it.glosses }.joinToString(", ")
                Triple(entry, kanji, reading) to meaning
            }
        }
    }

    fun fetchRelatedWords(
        entry: DictionaryEntry?,
        kanji: List<Kanji>,
        reading: List<Reading>,
        onResult: (List<DictionaryEntry>) -> Unit
    ) {
        if (entry == null) {
            onResult(emptyList())
            return
        }

        viewModelScope.launch {
            val query =
                kanji.firstOrNull()?.kanji ?: reading.firstOrNull()?.reading ?: return@launch
            val relatedMatches = withContext(Dispatchers.IO) {
                dao.searchRelatedMatches("%$query%", query)
            }
            onResult(relatedMatches)
        }
    }

    // **Data Retrieval Functions**
    suspend fun getKanji(entryId: String): List<Kanji> {
        return withContext(Dispatchers.IO) { dao.getKanji(entryId) }
    }

    suspend fun getReading(entryId: String): List<Reading> {
        return withContext(Dispatchers.IO) { dao.getReading(entryId) }
    }

    suspend fun getSenses(entryId: String): List<Sense> {
        return withContext(Dispatchers.IO) { dao.getSenses(entryId) }
    }

    suspend fun getExamples(senseId: Int): List<Example> {
        return withContext(Dispatchers.IO) { dao.getExamples(senseId) }
    }

    // **Search History Functions với Async Storage**
    fun addSearchHistory(query: String) {
        if (query.isNotEmpty() && !searchHistory.value.contains(query)) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    val history = getSearchHistoryFromStorage().toMutableList()
                    history.add(0, query) // Thêm vào đầu danh sách
                    saveSearchHistoryToStorage(history)
                    loadSearchHistory()
                }
            }
        }
    }

    private fun loadSearchHistory() {
        viewModelScope.launch {
            val history = withContext(Dispatchers.IO) { getSearchHistoryFromStorage() }
            searchHistory.value = history
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                saveSearchHistoryToStorage(emptyList())
                loadSearchHistory()
            }
        }
    }

    private fun getSearchHistoryFromStorage(): List<String> {
        val historyJson = sharedPreferences.getString("history", "[]") ?: "[]"
        val historyArray = JSONArray(historyJson)
        val historyList = mutableListOf<String>()
        for (i in 0 until historyArray.length()) {
            historyList.add(historyArray.getString(i))
        }
        return historyList.take(50) // Giới hạn 50 mục
    }

    private fun saveSearchHistoryToStorage(history: List<String>) {
        val historyArray = JSONArray()
        history.take(50).forEach { historyArray.put(it) } // Giới hạn 50 mục
        sharedPreferences.edit { putString("history", historyArray.toString()) }
    }

    // **Search Mode Functions**
    fun saveSearchMode(mode: String) {
        searchMode.value = mode
    }

    fun getSearchMode(): String {
        return searchMode.value
    }

    // **Kanji Extraction Function**
    fun extractKanjiCharacters(text: String) {
        kanjiSet.value = text.filter { it.isKanji() }.toSet()
    }

    // **Synchronous Search Function**
    suspend fun searchExactMatchesSync(query: String): List<DictionaryEntry> {
        return withContext(Dispatchers.IO) { dao.searchExactMatches(query) }
    }

    // **Field Functions**
    suspend fun getFieldsForSense(senseId: Int): List<String> {
        return withContext(Dispatchers.IO) { dao.getFieldsForSense(senseId) }
    }

    fun getAllFields(onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            val fields = withContext(Dispatchers.IO) { dao.getAllFields().map { it.name } }
            onResult(fields)
        }
    }

    // **Group Functions**
    fun createGroup(name: String) {
        viewModelScope.launch { dao.insertGroup(SaveGroups(name = name)) }
    }

    fun addEntryToGroup(entryId: String, groupId: Int) {
        viewModelScope.launch {
            dao.insertDictionaryGroupCrossRef(
                DictionaryGroupCrossRef(
                    groupId,
                    entryId
                )
            )
        }
    }

    fun getGroupName(groupId: Int): LiveData<String> {
        return dao.getGroupName(groupId)
    }

    private val _currentSortOption = MutableLiveData(SortOption.NAME_ASC)
    val currentSortOption: LiveData<SortOption> get() = _currentSortOption
    val allGroups: LiveData<List<SaveGroups>> = _currentSortOption.switchMap { sortOption ->
        when (sortOption) {
            SortOption.NAME_ASC -> dao.getAllGroupsSortedByNameAsc()
            SortOption.NAME_DESC -> dao.getAllGroupsSortedByNameDesc()
            SortOption.DATE_ASC -> dao.getAllGroupsSortedByDateAsc()
            SortOption.DATE_DESC -> dao.getAllGroupsSortedByDateDesc()
            else -> dao.getAllGroupsSortedByNameAsc()
        }
    }

    fun sortGroups(sortOption: SortOption) {
        _currentSortOption.value = sortOption
    }

    fun getGroupSize(groupId: Int): LiveData<Int> {
        return dao.getGroupSize(groupId)
    }

    fun deleteEntriesFromGroup(entryIds: List<String>, groupId: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.deleteEntriesFromGroup(entryIds, groupId)
            }
        }
    }

    suspend fun isEntryInGroup(entryId: String, groupId: Int): Boolean {
        return dao.isEntryInGroup(entryId, groupId)
    }

    fun fetchGroupEntries(groupId: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.getEntriesForGroup(groupId).value?.let {
                }
            }
        }
    }

    fun getEntriesForGroup(groupId: Int): LiveData<List<DictionaryEntry>> {
        return dao.getEntriesForGroup(groupId)
    }

    suspend fun getRelatedEntries(kanji: String, reading: String): List<DictionaryEntry> {
        return withContext(Dispatchers.IO) {
            val entryIds = dao.getEntryIdsContainingKanji("%$kanji%", reading)
            if (entryIds.isEmpty()) emptyList() else dao.getEntriesByIds(entryIds)
        }
    }

    // **Selection Mode Functions**
    fun toggleSelection(query: String) {
        if (selectedItems.contains(query)) {
            selectedItems.remove(query)
        } else {
            selectedItems.add(query)
            isSelectionMode.value = true
        }
    }

    fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll(searchHistory.value)
        isSelectionMode.value = true
    }

    fun deselectAll() {
        selectedItems.clear()
    }

    fun clearSelection() {
        selectedItems.clear()
        isSelectionMode.value = false
    }

    fun deleteSelectedItems() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val history = getSearchHistoryFromStorage().toMutableList()
                history.removeAll(selectedItems)
                saveSearchHistoryToStorage(history)
            }
            selectedItems.clear()
            isSelectionMode.value = false
            loadSearchHistory()
        }
    }
}
