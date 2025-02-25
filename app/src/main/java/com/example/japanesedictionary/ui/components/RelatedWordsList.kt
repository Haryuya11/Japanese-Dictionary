package com.example.japanesedictionary.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.japanesedictionary.MainActivity
import com.example.japanesedictionary.R
import com.example.japanesedictionary.data.model.DictionaryEntry
import com.example.japanesedictionary.viewmodel.DictionaryViewModel

@Composable
fun RelatedWordsList(
    navController: NavController,
    entries: List<DictionaryEntry>,
    viewModel: DictionaryViewModel = viewModel(),
    mainActivity: MainActivity
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(300.dp)
    ) {
        items(entries) { entry ->
            var kanji by remember { mutableStateOf("") }
            var reading by remember { mutableStateOf("") }
            var meaning by remember { mutableStateOf("") }

            LaunchedEffect(entry.id) {
                viewModel.getKanji(entry.id).firstOrNull()?.let { kanji = it.kanji }
                viewModel.getReading(entry.id).firstOrNull()?.let { reading = it.reading }
                viewModel.getSenses(entry.id)
                    .flatMap { it.glosses }
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(", ")
                    ?.let { meaning = it }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable {
                        viewModel.addSearchHistory(kanji.ifEmpty { reading })
                        navController.navigate("detail/${entry.id}")
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (kanji.isNotEmpty()) {
                            Text(
                                text = kanji,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = reading,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = reading,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (meaning.isNotEmpty()) {
                            Text(
                                text = meaning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { mainActivity.speakOut(kanji.ifEmpty { reading }) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_volume_up_24),
                            contentDescription = "Speak",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}