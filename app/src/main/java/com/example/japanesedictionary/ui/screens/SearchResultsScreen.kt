package com.example.japanesedictionary.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.launch

@Composable
fun SearchResultsScreen(
    navController: NavController,
    query: String,
    viewModel: DictionaryViewModel = viewModel(),
    mainActivity: MainActivity
) {
    val searchResults by remember { viewModel.searchResults }
    var relatedWords by remember { mutableStateOf(listOf<DictionaryEntry>()) }
    var fieldsForSenses by remember { mutableStateOf<Map<Int, List<String>>>(emptyMap()) }
    LaunchedEffect(query) {
        viewModel.searchWord(query)
        viewModel.fetchRelatedWords(null, emptyList(), emptyList()) { relatedEntries ->
            relatedWords = relatedEntries
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No results found",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(searchResults) { entry ->
                    var kanji by remember { mutableStateOf(listOf<Kanji>()) }
                    var reading by remember { mutableStateOf(listOf<Reading>()) }
                    var senses by remember { mutableStateOf(listOf<Sense>()) }
                    var examples by remember { mutableStateOf(listOf<Example>()) }

                    LaunchedEffect(entry.id) {
                        viewModel.searchWordById(entry.id) { _, kanjiList, readingList, sensesList, examplesList ->
                            kanji = kanjiList
                            reading = readingList
                            senses = sensesList
                            examples = examplesList

                            launch {
                                val fieldsMap = sensesList.associate { sense ->
                                    sense.id to viewModel.getFieldsForSense(sense.id)
                                }
                                fieldsForSenses = fieldsMap
                            }
                        }
                    }

                    DictionaryCard(
                        entry = entry,
                        kanji = kanji,
                        reading = reading,
                        senses = senses,
                        examples = examples,
                        mainActivity = mainActivity,
                        navController = navController,
                        dictionaryViewModel = viewModel
                    )

                    if (kanji.isNotEmpty()) {
                        viewModel.extractKanjiCharacters(kanji.joinToString { it.kanji })
                        KanjiListCard(
                            kanjiSet = viewModel.kanjiSet.value,
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