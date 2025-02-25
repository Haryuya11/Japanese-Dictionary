package com.example.japanesedictionary.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.japanesedictionary.utils.XmlToRoomImporter

@Composable
fun LoadingScreen(context: Context, onLoadingFinished: (Boolean, Int) -> Unit) {
    val phaseWeights = listOf(0.1f, 0.4f, 0.1f, 0.4f)
    val totalEntries = listOf(210694, 210694, 13108, 13108)
    var currentPhase by remember { mutableStateOf(0) }
    var currentProgress by remember { mutableStateOf(0f) }
    var currentCount by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) {
        val dictResult = XmlToRoomImporter.importData(context) { phase, count, totalPhase ->
            currentPhase = phase
            currentCount = count
            total = totalPhase
            currentProgress =
                calculateProgress(phase, count, totalPhase, phaseWeights, totalEntries)
        }

        val kanjiResult = XmlToRoomImporter.importKanjiData(context) { phase, count, totalPhase ->
            currentPhase = phase
            currentCount = count
            total = totalPhase
            currentProgress =
                calculateProgress(phase, count, totalPhase, phaseWeights, totalEntries)
        }

        onLoadingFinished(
            dictResult.isSuccess && kanjiResult.isSuccess,
            (dictResult.getOrNull() ?: 0) + (kanjiResult.getOrNull() ?: 0)
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Progress: ${(currentProgress * 100).toInt()}%")
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { currentProgress },
                modifier = Modifier.fillMaxWidth(0.8f),
            )
            Spacer(Modifier.height(8.dp))
            Text("Entries: $currentCount / $total")
            Spacer(Modifier.height(8.dp))
            Text(
                "Phase: ${
                    when (currentPhase) {
                        0 -> "Reading data from XML"
                        1 -> "Writing data to database"
                        2 -> "Reading kanji data from XML"
                        3 -> "Writing kanji data to database"
                        else -> ""
                    }
                }"
            )
        }
    }
}

private fun calculateProgress(
    phase: Int,
    current: Int,
    totalPhase: Int,
    weights: List<Float>,
    totals: List<Int>
): Float {
    if (totalPhase == 0) return 0f
    val phaseMax = totals[phase].toFloat()
    val normalized = current.toFloat() / phaseMax.coerceAtLeast(1f)
    var progress = 0f
    for (i in 0 until phase) progress += weights[i]
    progress += normalized * weights[phase]
    return progress.coerceIn(0f, 1f)
}