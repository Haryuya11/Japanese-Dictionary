package com.example.japanesedictionary.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.japanesedictionary.MainActivity
import com.example.japanesedictionary.R
import com.example.japanesedictionary.data.model.DictionaryEntry
import com.example.japanesedictionary.data.model.Sense
import com.example.japanesedictionary.viewmodel.DictionaryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    navController: NavController,
    groupId: Int,
    dictionaryViewModel: DictionaryViewModel = viewModel(),
    mainActivity: MainActivity,
    showWord: Boolean,
    showReading: Boolean,
    showMeaning: Boolean
) {
    val entries by dictionaryViewModel.getEntriesForGroup(groupId).observeAsState(emptyList())
    val groupName by dictionaryViewModel.getGroupName(groupId).observeAsState("")

    var isAutoAdvance by remember { mutableStateOf(false) }
    var shuffledEntries by remember { mutableStateOf(emptyList<DictionaryEntry>()) }
    val pagerState = rememberPagerState(pageCount = { shuffledEntries.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(entries) {
        shuffledEntries = entries.shuffled()
    }

    LaunchedEffect(Unit) {
        dictionaryViewModel.fetchGroupEntries(groupId)
    }

    LaunchedEffect(isAutoAdvance) {
        if (isAutoAdvance) {
            while (true) {
                delay(3000)
                with(pagerState) {
                    if (currentPage < pageCount - 1) {
                        animateScrollToPage(currentPage + 1)
                    } else {
                        animateScrollToPage(0)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        shuffledEntries = entries.shuffled()
                        coroutineScope.launch {
                            pagerState.scrollToPage(0)
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_shuffle_24),
                            contentDescription = "Shuffle"
                        )
                    }
                    IconButton(onClick = { isAutoAdvance = !isAutoAdvance }) {
                        Icon(
                            painter = painterResource(
                                id = if (isAutoAdvance) R.drawable.baseline_pause_24
                                else R.drawable.baseline_play_arrow_24
                            ),
                            contentDescription = "Auto Advance"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (shuffledEntries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No cards available", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) { page ->
                    val entry = shuffledEntries[page]
                    FlipCard(
                        entry = entry,
                        dictionaryViewModel = dictionaryViewModel,
                        mainActivity = mainActivity,
                        showWord = showWord,
                        showReading = showReading,
                        showMeaning = showMeaning
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "${pagerState.currentPage + 1}/${shuffledEntries.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun FlipCard(
    entry: DictionaryEntry,
    dictionaryViewModel: DictionaryViewModel,
    mainActivity: MainActivity,
    showWord: Boolean,
    showReading: Boolean,
    showMeaning: Boolean
) {
    var isFront by remember { mutableStateOf(true) }
    // Sử dụng tween để tạo animation flip mượt mà hơn
    val rotation by animateFloatAsState(
        targetValue = if (isFront) 0f else 180f,
        animationSpec = tween(durationMillis = 600)
    )

    val kanji by produceState(initialValue = "", key1 = entry.id) {
        value = dictionaryViewModel.getKanji(entry.id).firstOrNull()?.kanji ?: ""
    }
    val reading by produceState(initialValue = "", key1 = entry.id) {
        value = dictionaryViewModel.getReading(entry.id).firstOrNull()?.reading ?: ""
    }
    val senses by produceState(initialValue = emptyList<Sense>(), key1 = entry.id) {
        value = dictionaryViewModel.getSenses(entry.id)
    }
    val meanings = senses.flatMap { it.glosses }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    isFront = !isFront
                })
            }
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12 * density
            },
        contentAlignment = Alignment.Center
    ) {
        if (rotation <= 90f) {
            FrontContent(
                kanji = kanji,
                reading = reading,
                showWord = showWord,
                showReading = showReading,
                mainActivity = mainActivity
            )
        } else {
            // Đảo ngược mặt sau để hiển thị đúng hướng
            Box(modifier = Modifier.graphicsLayer { scaleX = -1f }) {
                BackContent(
                    entry = entry,
                    dictionaryViewModel = dictionaryViewModel,
                    mainActivity = mainActivity,
                    showWord = showWord,
                    showReading = showReading,
                    showMeaning = showMeaning
                )
            }
        }
    }
}

@Composable
fun FrontContent(
    kanji: String,
    reading: String,
    showWord: Boolean,
    showReading: Boolean,
    mainActivity: MainActivity
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(MaterialTheme.shapes.medium),
//        colors = CardDefaults.cardColors(containerColor = Color.LightGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when {
                        showWord && kanji.isNotEmpty() -> kanji
                        showReading && reading.isNotEmpty() -> reading
                        else -> "N/A"
                    },
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                IconButton(
                    onClick = {
                        mainActivity.speakOut(if (kanji.isNotEmpty()) kanji else reading)
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_volume_up_24),
                        contentDescription = "Speak",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
fun BackContent(
    entry: DictionaryEntry,
    dictionaryViewModel: DictionaryViewModel,
    mainActivity: MainActivity,
    showWord: Boolean,
    showReading: Boolean,
    showMeaning: Boolean
) {
    val kanjiList by produceState(initialValue = emptyList<String>(), key1 = entry.id) {
        value = dictionaryViewModel.getKanji(entry.id).map { it.kanji }
    }
    val readingList by produceState(initialValue = emptyList<String>(), key1 = entry.id) {
        value = dictionaryViewModel.getReading(entry.id).map { it.reading }
    }
    val senses by produceState(initialValue = emptyList<Sense>(), key1 = entry.id) {
        value = dictionaryViewModel.getSenses(entry.id)
    }
    val meanings = senses.flatMap { it.glosses }

    val titleText: String = if (kanjiList.isNotEmpty() && showWord) {
        kanjiList.first()
    } else if (readingList.isNotEmpty() && showReading) {
        readingList.first()
    } else {
        "N/A"
    }

    val supplementaryReadingText: String = if (kanjiList.isNotEmpty() && showWord) {
        if (readingList.isNotEmpty() && showReading) readingList.joinToString(", ") else ""
    } else {
        if (readingList.size > 1 && showReading) {
            readingList.drop(1).joinToString(", ")
        } else if (readingList.size == 1 && showReading) {
            readingList.first()
        } else {
            ""
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(MaterialTheme.shapes.medium),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showWord || showReading) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                if (supplementaryReadingText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = supplementaryReadingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (showMeaning && meanings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = meanings.joinToString("; "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

