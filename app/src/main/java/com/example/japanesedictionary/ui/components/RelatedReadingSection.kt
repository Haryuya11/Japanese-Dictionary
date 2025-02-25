package com.example.japanesedictionary.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.japanesedictionary.data.model.DictionaryEntry
import com.example.japanesedictionary.utils.isKatakana
import com.example.japanesedictionary.viewmodel.DictionaryViewModel
import com.example.japanesedictionary.MainActivity

@Composable
fun RelatedReadingSection(
    title: String,
    readings: List<String>,
    kanji: String,
    navController: NavController,
    viewModel: DictionaryViewModel,
    mainActivity: MainActivity
) {
    // Lấy các entry liên quan cho mỗi reading
    var relatedEntries = remember { mutableStateOf<Map<String, List<DictionaryEntry>>>(emptyMap()) }

    LaunchedEffect(kanji, readings) {
        relatedEntries.value = readings.associateWith { reading ->
            viewModel.getRelatedEntries(kanji, reading)
        }
    }

    // Tách ra 2 nhóm: onyomi (các reading chứa ký tự katakana) và kunyomi (không chứa)
    val onyomiReadings = readings.filter { it.isKatakana() }
    val kunyomiReadings = readings.filter { it.isNotEmpty() && !it.isKatakana() }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 8.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (onyomiReadings.isNotEmpty()) {
                Text(
                    text = "Onyomi",
                    style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                onyomiReadings.forEach { reading ->
                    val entries = relatedEntries.value[reading] ?: emptyList()
                    if (entries.isNotEmpty()) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = reading,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            entries.forEach { entry ->
                                RelatedEntryItem(
                                    entry = entry,
                                    navController = navController,
                                    viewModel = viewModel,
                                    mainActivity = mainActivity
                                )
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }

            if (kunyomiReadings.isNotEmpty()) {
                Text(
                    text = "Kunyomi",
                    style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                kunyomiReadings.forEach { reading ->
                    val entries = relatedEntries.value[reading] ?: emptyList()
                    if (entries.isNotEmpty()) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = reading,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            entries.forEach { entry ->
                                RelatedEntryItem(
                                    entry = entry,
                                    navController = navController,
                                    viewModel = viewModel,
                                    mainActivity = mainActivity
                                )
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}
