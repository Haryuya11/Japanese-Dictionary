package com.example.japanesedictionary.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun SuggestionsList(
    navController: NavController,
    suggestions: List<Pair<Triple<DictionaryEntry, String?, String?>, String>>,
    mainActivity: MainActivity,
    modifier: Modifier = Modifier,
    viewModel: DictionaryViewModel = viewModel()
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(suggestions) { index, suggestion ->
            val (entry, kanji, reading) = suggestion.first
            val meaning = suggestion.second

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        viewModel.addSearchHistory(kanji ?: reading ?: "")
                        navController.navigate("detail/${entry.id}")
                    },
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = kanji ?: reading ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.width(8.dp))

                            if (kanji != null) {
                                Text(
                                    text = reading ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Text(
                            text = meaning,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    IconButton(
                        onClick = { mainActivity.speakOut(kanji ?: reading ?: "") }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_volume_up_24),
                            contentDescription = "Speak",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}