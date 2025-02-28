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
import com.example.japanesedictionary.utils.convertKatakanaToHiragana
import com.example.japanesedictionary.utils.isKatakana
import com.example.japanesedictionary.utils.romajiToHiragana
import com.example.japanesedictionary.utils.tokenizeJapaneseText
import org.json.JSONArray

/**
 * ViewModel class to manage dictionary-related logic and UI state for a Japanese-English dictionary app.
 * Extends AndroidViewModel to access application context.
 */
class DictionaryViewModel(application: Application) : AndroidViewModel(application) {
    // Database access object (DAO) for interacting with the dictionary database
    private val dao = DictionaryDatabase.getDatabase(application).dictionaryDao()
    // SharedPreferences to persist search history
    private val sharedPreferences = application.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    // UI state variables using Compose mutable states
    var query = mutableStateOf("") // Current search query entered by the user
    var searchResults = mutableStateOf(listOf<DictionaryEntry>()) // List of search results
    var suggestions = mutableStateOf(listOf<Pair<Triple<DictionaryEntry, String?, String?>, String>>()) // List of suggestions
    var searchHistory = mutableStateOf(listOf<String>()) // User's search history
    var searchMode = mutableStateOf("ja") // Search mode: "ja" for Japanese, "en" for English
    var kanjiSet = mutableStateOf(setOf<Char>()) // Set of unique Kanji characters extracted from text
    var isSelectionMode = mutableStateOf(false) // Flag for selection mode in search history
    var selectedItems = mutableStateListOf<String>() // List of selected items in search history

    // Initialization block to load search history when ViewModel is created
    init {
        loadSearchHistory()
    }

    // --- Search Functions ---

    /**
     * Initiates a search based on the provided query and current search mode.
     * Updates searchResults with the results, prioritizing exact matches.
     */
    fun searchWord(query: String) {
        this.query.value = query // Update the current query
        viewModelScope.launch {
            if (query.isEmpty()) {
                searchResults.value = emptyList() // Clear results if query is empty
                return@launch
            }
            // Perform search based on the current mode
            if (searchMode.value == "ja") {
                searchJapaneseMode(query)
            } else if (searchMode.value == "en") {
                searchEnglishMode(query)
            }
        }
    }

    /**
     * Handles search in Japanese mode.
     * Normalizes the query (e.g., romaji to hiragana) and fetches results with exact matches prioritized.
     */
    private suspend fun searchJapaneseMode(query: String) {
        withContext(Dispatchers.IO) { // Run on IO thread to avoid blocking UI
            // Normalize query: convert romaji or katakana to hiragana if applicable
            val processedQuery = when {
                query.all { it in 'a'..'z' || it in 'A'..'Z' } -> query.romajiToHiragana()
                query.any { it.isKatakana() } -> query.convertKatakanaToHiragana()
                else -> query
            }
            // Tokenize query for Full-Text Search (FTS)
            val tokenizedQuery = tokenizeJapaneseText(processedQuery)
            // Fetch exact and related matches from the database
            val exactResults = dao.searchExactJapaneseFTS(tokenizedQuery)
            val relatedResults = dao.searchRelatedJapaneseFTS(tokenizedQuery)

            // Filter exact matches where kanji or reading matches the processed query exactly
            val exactMatches = exactResults.filter { entry ->
                val kanjiList = dao.getKanji(entry.id).map { it.kanji }
                val readingList = dao.getReading(entry.id).map { it.reading }
                kanjiList.contains(processedQuery) || readingList.contains(processedQuery)
            }

            // Related matches are distinct results excluding exact matches
            val relatedMatches = (exactResults + relatedResults).distinct() - exactMatches.toSet()

            // Update search results: exact matches first, followed by related matches
            searchResults.value = exactMatches + relatedMatches
        }
    }

    /**
     * Handles search in English mode.
     * Fetches results from glosses, prioritizing exact matches.
     */
    private suspend fun searchEnglishMode(query: String) {
        withContext(Dispatchers.IO) {
            // Fetch exact and related matches from the database
            val exactResults = dao.searchExactEnglishFTS(query)
            val relatedResults = dao.searchRelatedEnglish(query)

            // Filter exact matches where glosses contain the exact query
            val exactMatches = exactResults.filter { entry ->
                val senses = dao.getSenses(entry.id)
                senses.any { sense -> sense.glosses.contains(query) }
            }

            // Related matches are distinct results excluding exact matches
            val relatedMatches = (exactResults + relatedResults).distinct() - exactMatches.toSet()

            // Update search results: exact matches first, followed by related matches
            searchResults.value = exactMatches + relatedMatches
        }
    }

    /**
     * Fetches a specific dictionary entry by ID, including related data (kanji, readings, senses, examples).
     * Invokes the provided callback with the results.
     */
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

    // --- Suggestion Functions ---

    /**
     * Fetches suggestions based on the provided query and current search mode.
     * Updates suggestions state with the results, prioritizing exact matches.
     */
    fun fetchSuggestions(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                suggestions.value = emptyList() // Clear suggestions if query is empty
                return@launch
            }
            // Fetch suggestions based on the current mode
            if (searchMode.value == "ja") {
                fetchJapaneseSuggestionsMode(query)
            } else if (searchMode.value == "en") {
                fetchEnglishSuggestionsMode(query)
            }
        }
    }

    /**
     * Fetches suggestions in Japanese mode.
     * Normalizes the query and prioritizes exact matches in the suggestion list.
     */
    private suspend fun fetchJapaneseSuggestionsMode(query: String) {
        withContext(Dispatchers.IO) {
            // Normalize query: convert romaji or katakana to hiragana if applicable
            val processedQuery = when {
                query.all { it in 'a'..'z' || it in 'A'..'Z' } -> query.romajiToHiragana()
                query.any { it.isKatakana() } -> query.convertKatakanaToHiragana()
                else -> query
            }
            // Tokenize query for Full-Text Search (FTS)
            val tokenizedQuery = tokenizeJapaneseText(processedQuery)
            // Fetch exact and related matches from the database
            val exactResults = dao.searchExactJapaneseFTS(tokenizedQuery)
            val relatedResults = dao.searchRelatedJapaneseFTS(tokenizedQuery)

            // Filter exact matches where kanji or reading matches the processed query exactly
            val exactMatches = exactResults.filter { entry ->
                val kanjiList = dao.getKanji(entry.id).map { it.kanji }
                val readingList = dao.getReading(entry.id).map { it.reading }
                kanjiList.contains(processedQuery) || readingList.contains(processedQuery)
            }

            // Related matches are distinct results excluding exact matches
            val relatedMatches = (exactResults + relatedResults).distinct() - exactMatches.toSet()

            // Combine exact and related matches into a single list
            val entries = exactMatches + relatedMatches

            // Format suggestions as a list of pairs: (entry data, meaning)
            suggestions.value = entries.map { entry ->
                val kanji = dao.getKanji(entry.id).firstOrNull()?.kanji
                val reading = dao.getReading(entry.id).firstOrNull()?.reading
                val meaning = dao.getSenses(entry.id).flatMap { it.glosses }.joinToString(", ")
                Triple(entry, kanji, reading) to meaning
            }
        }
    }

    /**
     * Fetches suggestions in English mode.
     * Prioritizes exact matches based on glosses in the suggestion list.
     */
    private suspend fun fetchEnglishSuggestionsMode(query: String) {
        withContext(Dispatchers.IO) {
            // Fetch exact and related matches from the database
            val exactResults = dao.searchExactEnglishFTS(query)
            val relatedResults = dao.searchRelatedEnglish(query)

            // Filter exact matches where glosses contain the exact query
            val exactMatches = exactResults.filter { entry ->
                val senses = dao.getSenses(entry.id)
                senses.any { sense -> sense.glosses.contains(query) }
            }

            // Related matches are distinct results excluding exact matches
            val relatedMatches = (exactResults + relatedResults).distinct() - exactMatches.toSet()

            // Combine exact and related matches into a single list
            val entries = exactMatches + relatedMatches

            // Format suggestions as a list of pairs: (entry data, meaning)
            suggestions.value = entries.map { entry ->
                val kanji = dao.getKanji(entry.id).firstOrNull()?.kanji
                val reading = dao.getReading(entry.id).firstOrNull()?.reading
                val meaning = dao.getSenses(entry.id).flatMap { it.glosses }.joinToString(", ")
                Triple(entry, kanji, reading) to meaning
            }
        }
    }

    /**
     * Fetches related words based on an entry's kanji or reading.
     * Invokes the provided callback with the results.
     */
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
            val query = kanji.firstOrNull()?.kanji ?: reading.firstOrNull()?.reading ?: return@launch
            val relatedMatches = withContext(Dispatchers.IO) {
                dao.searchRelatedMatches("%$query%", query)
            }
            onResult(relatedMatches)
        }
    }

    // --- Data Retrieval Functions ---

    /** Retrieves kanji data for a specific entry ID. */
    suspend fun getKanji(entryId: String): List<Kanji> = withContext(Dispatchers.IO) { dao.getKanji(entryId) }

    /** Retrieves reading data for a specific entry ID. */
    suspend fun getReading(entryId: String): List<Reading> = withContext(Dispatchers.IO) { dao.getReading(entryId) }

    /** Retrieves senses data for a specific entry ID. */
    suspend fun getSenses(entryId: String): List<Sense> = withContext(Dispatchers.IO) { dao.getSenses(entryId) }

    /** Retrieves examples for a specific sense ID. */
    suspend fun getExamples(senseId: Int): List<Example> = withContext(Dispatchers.IO) { dao.getExamples(senseId) }

    // --- Search History Functions ---

    /**
     * Adds a query to the search history if it’s not already present.
     * Updates the history in SharedPreferences and reloads it.
     */
    fun addSearchHistory(query: String) {
        if (query.isNotEmpty() && !searchHistory.value.contains(query)) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    val history = getSearchHistoryFromStorage().toMutableList()
                    history.add(0, query) // Add new query at the start
                    saveSearchHistoryToStorage(history)
                    loadSearchHistory()
                }
            }
        }
    }

    /** Loads search history from SharedPreferences and updates the state. */
    private fun loadSearchHistory() {
        viewModelScope.launch {
            val history = withContext(Dispatchers.IO) { getSearchHistoryFromStorage() }
            searchHistory.value = history
        }
    }

    /** Clears all entries from the search history. */
    fun clearSearchHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                saveSearchHistoryToStorage(emptyList())
                loadSearchHistory()
            }
        }
    }

    /** Retrieves search history from SharedPreferences as a list, limited to 50 entries. */
    private fun getSearchHistoryFromStorage(): List<String> {
        val historyJson = sharedPreferences.getString("history", "[]") ?: "[]"
        val historyArray = JSONArray(historyJson)
        val historyList = mutableListOf<String>()
        for (i in 0 until historyArray.length()) {
            historyList.add(historyArray.getString(i))
        }
        return historyList.take(50)
    }

    /** Saves the search history list to SharedPreferences, limited to 50 entries. */
    private fun saveSearchHistoryToStorage(history: List<String>) {
        val historyArray = JSONArray()
        history.take(50).forEach { historyArray.put(it) }
        sharedPreferences.edit { putString("history", historyArray.toString()) }
    }

    // --- Search Mode Functions ---

    /** Updates the current search mode ("ja" or "en"). */
    fun saveSearchMode(mode: String) {
        searchMode.value = mode
    }

    /** Returns the current search mode. */
    fun getSearchMode(): String = searchMode.value

    // --- Kanji Extraction Function ---

    /** Extracts unique Kanji characters from text and updates the kanjiSet state. */
    fun extractKanjiCharacters(text: String) {
        kanjiSet.value = text.filter { it.isKanji() }.toSet()
    }

    // --- Synchronous Search Function ---

    /** Synchronously searches for exact matches (useful for testing or specific use cases). */
    suspend fun searchExactMatchesSync(query: String): List<DictionaryEntry> =
        withContext(Dispatchers.IO) { dao.searchExactMatches(query) }

    // --- Field Functions ---

    /** Retrieves fields associated with a specific sense ID. */
    suspend fun getFieldsForSense(senseId: Int): List<String> =
        withContext(Dispatchers.IO) { dao.getFieldsForSense(senseId) }

    /** Fetches all fields from the database and passes them to the provided callback. */
    fun getAllFields(onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            val fields = withContext(Dispatchers.IO) { dao.getAllFields().map { it.name } }
            onResult(fields)
        }
    }

    // --- Group Functions ---

    /** Creates a new group with the specified name. */
    fun createGroup(name: String) {
        viewModelScope.launch { dao.insertGroup(SaveGroups(name = name)) }
    }

    /** Adds a dictionary entry to a group using a cross-reference. */
    fun addEntryToGroup(entryId: String, groupId: Int) {
        viewModelScope.launch {
            dao.insertDictionaryGroupCrossRef(DictionaryGroupCrossRef(groupId, entryId))
        }
    }

    /** Returns LiveData for the name of a group by its ID. */
    fun getGroupName(groupId: Int): LiveData<String> = dao.getGroupName(groupId)

    // LiveData for the current sort option and all groups
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

    /** Sets the sort option for groups. */
    fun sortGroups(sortOption: SortOption) {
        _currentSortOption.value = sortOption
    }

    /** Returns LiveData for the size of a group. */
    fun getGroupSize(groupId: Int): LiveData<Int> = dao.getGroupSize(groupId)

    /** Deletes specified entries from a group. */
    fun deleteEntriesFromGroup(entryIds: List<String>, groupId: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.deleteEntriesFromGroup(entryIds, groupId)
            }
        }
    }

    /** Checks if an entry exists in a group. */
    suspend fun isEntryInGroup(entryId: String, groupId: Int): Boolean =
        dao.isEntryInGroup(entryId, groupId)

    /** Fetches entries for a group (placeholder for future implementation). */
    fun fetchGroupEntries(groupId: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.getEntriesForGroup(groupId).value?.let { /* Handle entries if needed */ }
            }
        }
    }

    /** Returns LiveData for entries in a group. */
    fun getEntriesForGroup(groupId: Int): LiveData<List<DictionaryEntry>> =
        dao.getEntriesForGroup(groupId)

    /** Fetches related entries based on kanji and reading. */
    suspend fun getRelatedEntries(kanji: String, reading: String): List<DictionaryEntry> =
        withContext(Dispatchers.IO) {
            val entryIds = dao.getEntryIdsContainingKanji("%$kanji%", reading)
            if (entryIds.isEmpty()) emptyList() else dao.getEntriesByIds(entryIds)
        }

    // --- Selection Mode Functions ---

    /** Toggles selection of a query in the search history. */
    fun toggleSelection(query: String) {
        if (selectedItems.contains(query)) {
            selectedItems.remove(query)
        } else {
            selectedItems.add(query)
            isSelectionMode.value = true
        }
    }

    /** Selects all items in the search history. */
    fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll(searchHistory.value)
        isSelectionMode.value = true
    }

    /** Deselects all items in the search history. */
    fun deselectAll() {
        selectedItems.clear()
    }

    /** Clears selection and exits selection mode. */
    fun clearSelection() {
        selectedItems.clear()
        isSelectionMode.value = false
    }

    /** Deletes selected items from the search history and updates the state. */
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