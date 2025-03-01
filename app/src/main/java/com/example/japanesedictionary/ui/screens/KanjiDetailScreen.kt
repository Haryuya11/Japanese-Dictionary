package com.example.japanesedictionary.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.japanesedictionary.data.model.KanjiEntry
import com.example.japanesedictionary.data.model.KanjiReading
import com.example.japanesedictionary.ui.components.KanjiDetailCard
import com.example.japanesedictionary.ui.components.KanjiStrokeCard
import com.example.japanesedictionary.ui.components.RelatedReadingSection
import com.example.japanesedictionary.utils.convertKatakanaToHiragana
import com.example.japanesedictionary.utils.removeSymbols
import com.example.japanesedictionary.viewmodel.DictionaryViewModel
import com.example.japanesedictionary.viewmodel.KanjiViewModel

@Composable
fun KanjiDetailScreen(
    navController: NavController,
    kanjiLiteral: String,
    kanjiViewModel: KanjiViewModel = viewModel(),
    dictionaryViewModel: DictionaryViewModel = viewModel(),
    mainActivity: MainActivity
) {
    var kanjiEntry by remember { mutableStateOf<KanjiEntry?>(null) }
    var kanjiReadings by remember { mutableStateOf(listOf<KanjiReading>()) }
    var svgExists by remember { mutableStateOf(false) } // Biến kiểm tra file SVG

    LaunchedEffect(kanjiLiteral) {
        kanjiViewModel.getKanjiEntry(kanjiLiteral) { fetchedKanjiEntry, fetchedKanjiReadings ->
            kanjiEntry = fetchedKanjiEntry
            kanjiReadings = fetchedKanjiReadings
            fetchedKanjiEntry?.fileSvgName?.let { fileSvgName ->
                try {
                    // Kiểm tra file SVG trong thư mục assets
                    mainActivity.assets.open("kanji_svg/$fileSvgName").close()
                    svgExists = true
                } catch (e: Exception) {
                    // Nếu không mở được file, svgExists giữ giá trị false
                    svgExists = false
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        item {
            kanjiEntry?.let {
                KanjiDetailCard(kanjiEntry = it, kanjiReadings = kanjiReadings)
                // Chỉ hiển thị KanjiStrokeCard nếu file SVG tồn tại
                if (svgExists) {
                    it.fileSvgName?.let { fileSvgName ->
                        KanjiStrokeCard(fileSvgName = fileSvgName)
                    }
                }
            }
        }

        kanjiEntry?.let {
            // Phần từ liên quan
            val onyomiReadings = kanjiReadings.filter { it.type == "ja_on" }
                .map { it.reading.convertKatakanaToHiragana() }
            val kunyomiReadings = kanjiReadings.filter { it.type == "ja_kun" }
                .map { it.reading.removeSymbols() }

            if (onyomiReadings.isNotEmpty()) {
                item {
                    RelatedReadingSection(
                        title = "Onyomi",
                        readings = onyomiReadings,
                        kanji = kanjiLiteral,
                        navController = navController,
                        viewModel = dictionaryViewModel,
                        mainActivity = mainActivity
                    )
                }
            }

            if (kunyomiReadings.isNotEmpty()) {
                item {
                    RelatedReadingSection(
                        title = "Kunyomi",
                        readings = kunyomiReadings,
                        kanji = kanjiLiteral,
                        navController = navController,
                        viewModel = dictionaryViewModel,
                        mainActivity = mainActivity
                    )
                }
            }
        }
    }
}