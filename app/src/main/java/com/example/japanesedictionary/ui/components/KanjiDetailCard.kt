package com.example.japanesedictionary.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.japanesedictionary.data.model.KanjiEntry
import com.example.japanesedictionary.data.model.KanjiReading

@Composable
fun KanjiDetailCard(kanjiEntry: KanjiEntry, kanjiReadings: List<KanjiReading>) {
    // Lấy ra các reading dựa theo type
    val kunyomiReadings = kanjiReadings.filter { it.type == "ja_kun" }.joinToString(", ") { it.reading }
    val onyomiReadings = kanjiReadings.filter { it.type == "ja_on" }.joinToString(", ") { it.reading }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Hiển thị chữ Kanji (không quá lớn vì có canvas bên dưới)
            Text(
                text = kanjiEntry.literal,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Hàng hiển thị Kunyomi
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Kunyomi: ",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.3f)
                )
                Text(
                    text = kunyomiReadings,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.7f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Hàng hiển thị Onyomi
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Onyomi: ",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.3f)
                )
                Text(
                    text = onyomiReadings,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.7f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Hàng hiển thị Stroke Count
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Strokes: ",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.3f)
                )
                Text(
                    text = kanjiEntry.strokeCount.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.7f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Hàng hiển thị JLPT (nếu có)
            if (kanjiEntry.jlpt != null) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "JLPT: ",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(0.3f)
                    )
                    Text(
                        text = kanjiEntry.jlpt.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Hàng hiển thị Frequency (nếu có)
            if (kanjiEntry.freq != null) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Frequency: ",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(0.3f)
                    )
                    Text(
                        text = kanjiEntry.freq.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(0.7f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Hàng hiển thị Meanings
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Meanings: ",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.3f)
                )
                Text(
                    text = kanjiEntry.meanings.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.7f)
                )
            }
        }
    }
}
