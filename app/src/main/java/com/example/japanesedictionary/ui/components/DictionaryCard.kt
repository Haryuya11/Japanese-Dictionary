package com.example.japanesedictionary.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.japanesedictionary.MainActivity
import com.example.japanesedictionary.R
import com.example.japanesedictionary.data.model.DictionaryEntry
import com.example.japanesedictionary.data.model.Example
import com.example.japanesedictionary.data.model.Kanji
import com.example.japanesedictionary.data.model.Reading
import com.example.japanesedictionary.data.model.Sense
import com.example.japanesedictionary.viewmodel.DictionaryViewModel
import kotlinx.coroutines.launch


@Composable
fun DictionaryCard(
    entry: DictionaryEntry,
    kanji: List<Kanji>,
    reading: List<Reading>,
    senses: List<Sense>,
    examples: List<Example>,
    mainActivity: MainActivity,
    navController: NavController,
    dictionaryViewModel: DictionaryViewModel,
) {
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (kanji.isNotEmpty()) {
                    Text(
                        text = kanji.first().kanji,
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                } else if (reading.isNotEmpty()) {
                    Text(
                        text = reading.first().reading,
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                IconButton(onClick = {
                    navController.navigate("addToGroup/${entry.id}")
                }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add to Group")
                }
            }

            if (kanji.isEmpty()) {
                if (reading.size > 1) {
                    Text(
                        text = "「 ${
                            reading.drop(1).joinToString(separator = " · ") { it.reading }
                        } 」",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else if (reading.size == 1) {
                    Text(
                        text = "「${reading.first().reading}」",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                Text(
                    text = "「 ${reading.joinToString(separator = " · ") { it.reading }} 」",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (reading.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    reading.forEach { read ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = read.reading,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { mainActivity.speakOut(read.reading) }) {
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

            if (kanji.size > 1) {
                Text(
                    text = "Other Kanji: ${
                        kanji.drop(1).joinToString(separator = " · ") { it.kanji }
                    }",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(12.dp))

            senses.forEach { sense ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = sense.pos.joinToString(", "),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sense.glosses.joinToString(", "),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    var fields by remember(sense.id) { mutableStateOf(emptyList<String>()) }

                    LaunchedEffect(sense.id) {
                        fields = dictionaryViewModel.getFieldsForSense(sense.id)
                    }

                    val additionalDetails = listOf(
                        "Field" to fields.takeIf { it.isNotEmpty() }?.joinToString(", "),
                        "Info" to sense.misc?.joinToString(", "),
                        "Restricted Kanji" to sense.stagk?.joinToString(", "),
                        "Restricted Reading" to sense.stagr?.joinToString(", "),
                        "See also" to sense.xref?.joinToString(", "),
                        "Antonyms" to sense.ant?.joinToString(", "),
                        "Extra Info" to sense.sInf?.joinToString(", ")
                    ).filter { (_, detail) ->
                        !detail.isNullOrEmpty()
                    }

                    if (additionalDetails.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Additional Information:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        additionalDetails.forEach { (label, detail) ->
                            Spacer(modifier = Modifier.height(2.dp))
                            if ((label == "See also" || label == "Antonyms") && detail is String) {
                                val words: List<String> = detail.split(", ")
                                if (words.isNotEmpty()) {
                                    Row {
                                        Text(
                                            text = "• $label: ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                        for (index in words.indices) {
                                            val word = words[index]
                                            Text(
                                                text = word,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.primary
                                                ),
                                                modifier = Modifier.clickable {
                                                    coroutineScope.launch {
                                                        val results =
                                                            dictionaryViewModel.searchExactMatchesSync(
                                                                word
                                                            )
                                                        if (results.isNotEmpty()) {
                                                            navController.navigate("detail/${results.first().id}")
                                                        }
                                                    }
                                                }
                                            )
                                            if (index < words.size - 1) {
                                                Text(
                                                    text = " · ",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.8f
                                                    )
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "• $label: $detail",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            } else {
                                Text(
                                    text = "• $label: $detail",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    val senseExamples = examples.filter { it.senseId == sense.id }
                    if (senseExamples.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        senseExamples.forEach { example ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = example.exSentJpn,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { mainActivity.speakOut(example.exSentJpn) }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.baseline_volume_up_24),
                                            contentDescription = "Speak",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = example.exSentEng,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}