package com.example.japanesedictionary.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.japanesedictionary.MainActivity
import com.example.japanesedictionary.R
import com.example.japanesedictionary.data.model.DictionaryEntry
import com.example.japanesedictionary.data.model.Kanji
import com.example.japanesedictionary.data.model.Reading
import com.example.japanesedictionary.data.model.Sense
import com.example.japanesedictionary.viewmodel.DictionaryViewModel

@Composable
fun RelatedEntryItem(
    entry: DictionaryEntry,
    navController: NavController,
    viewModel: DictionaryViewModel,
    mainActivity: MainActivity
) {
    var kanjiList = remember { mutableStateOf(emptyList<Kanji>()) }
    var readingList = remember { mutableStateOf(emptyList<Reading>()) }
    var senses = remember { mutableStateOf(emptyList<Sense>()) }

    LaunchedEffect(entry.id) {
        kanjiList.value = viewModel.getKanji(entry.id)
        readingList.value = viewModel.getReading(entry.id)
        senses.value = viewModel.getSenses(entry.id)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { navController.navigate("detail/${entry.id}") },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hiển thị Kanji đầu tiên (nếu có)
            val displayKanji = kanjiList.value.firstOrNull()?.kanji ?: ""
            Text(
                text = displayKanji,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(0.3f)
            )
            // Hiển thị Reading đầu tiên
            val displayReading = readingList.value.firstOrNull()?.reading.orEmpty()
            Text(
                text = "「$displayReading」",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.3f)
            )
            // Hiển thị nghĩa (glosses) đầu tiên
            val displayMeanings = senses.value.firstOrNull()?.glosses?.joinToString(", ").orEmpty()
            Text(
                text = displayMeanings,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.3f),
                maxLines = 1
            )
            // Icon loa để phát âm
            IconButton(
                onClick = {
                    mainActivity.speakOut(
                        if (displayKanji.isNotEmpty()) displayKanji else displayReading
                    )
                },
                modifier = Modifier.weight(0.1f)
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
