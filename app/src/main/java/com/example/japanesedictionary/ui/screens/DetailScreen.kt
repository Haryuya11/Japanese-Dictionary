package com.example.japanesedictionary.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.japanesedictionary.MainActivity
import com.example.japanesedictionary.data.model.DictionaryEntry
import com.example.japanesedictionary.data.model.Example
import com.example.japanesedictionary.data.model.Kanji
import com.example.japanesedictionary.data.model.Reading
import com.example.japanesedictionary.data.model.Sense
import com.example.japanesedictionary.ui.components.DictionaryCard
import com.example.japanesedictionary.ui.components.KanjiListCard
import com.example.japanesedictionary.ui.components.RelatedWordsList
import com.example.japanesedictionary.viewmodel.DictionaryViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    navController: NavController,
    entryId: String,
    dictionaryViewModel: DictionaryViewModel = viewModel(),
    mainActivity: MainActivity
) {
    var entry by remember { mutableStateOf<DictionaryEntry?>(null) }
    var kanji by remember { mutableStateOf(listOf<Kanji>()) }
    var reading by remember { mutableStateOf(listOf<Reading>()) }
    var senses by remember { mutableStateOf(listOf<Sense>()) }
    var examples by remember { mutableStateOf(listOf<Example>()) }
    var relatedWords by remember { mutableStateOf(listOf<DictionaryEntry>()) }

    var fieldsForSenses by remember { mutableStateOf<Map<Int, List<String>>>(emptyMap()) }

    LaunchedEffect(entryId) {
        dictionaryViewModel.searchWordById(entryId) { fetchedEntry, kanjiList, readingList, sensesList, examplesList ->
            entry = fetchedEntry
            kanji = kanjiList
            reading = readingList
            senses = sensesList
            examples = examplesList

            launch {
                val deferredFields = sensesList.map { sense ->
                    async { sense.id to dictionaryViewModel.getFieldsForSense(sense.id) }
                }
                val fieldsMap = deferredFields.awaitAll().toMap()
                fieldsForSenses = fieldsMap
                println("FieldsForSenses map updated: $fieldsMap")
            }
            dictionaryViewModel.fetchRelatedWords(
                fetchedEntry,
                kanjiList,
                readingList
            ) { relatedEntries ->
                relatedWords = relatedEntries
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {

        entry?.let {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    DictionaryCard(
                        entry = it,
                        kanji = kanji,
                        reading = reading,
                        senses = senses,
                        examples = examples,
                        mainActivity = mainActivity,
                        navController = navController,
                        dictionaryViewModel = dictionaryViewModel
                    )
                }
                if (kanji.isNotEmpty()) {
                    dictionaryViewModel.extractKanjiCharacters(kanji.joinToString { it.kanji })
                    item {
                        KanjiListCard(
                            kanjiSet = dictionaryViewModel.kanjiSet.value,
                            mainActivity = mainActivity,
                            navController = navController
                        )
                    }
                }
                if (relatedWords.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Related Words",
                                    style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                RelatedWordsList(
                                    navController = navController,
                                    entries = relatedWords,
                                    mainActivity = mainActivity
                                )
                            }
                        }
                    }
                }

            }
        }
    }
}