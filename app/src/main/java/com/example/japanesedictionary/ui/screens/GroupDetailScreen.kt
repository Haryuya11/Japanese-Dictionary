package com.example.japanesedictionary.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.japanesedictionary.MainActivity
import com.example.japanesedictionary.R
import com.example.japanesedictionary.data.model.DictionaryEntry
import com.example.japanesedictionary.data.model.Kanji
import com.example.japanesedictionary.data.model.Reading
import com.example.japanesedictionary.data.model.Sense
import com.example.japanesedictionary.ui.components.CustomSearchBar
import com.example.japanesedictionary.viewmodel.DictionaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    navController: NavController,
    groupId: Int,
    dictionaryViewModel: DictionaryViewModel = viewModel(),
    mainActivity: MainActivity
) {
    // Lấy danh sách entries từ ViewModel
    val entries by dictionaryViewModel.getEntriesForGroup(groupId)
        .observeAsState(initial = emptyList())
    // Dùng TextFieldValue cho searchQuery để tương thích với CustomSearchBar
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showWord by remember { mutableStateOf(true) }
    var showReading by remember { mutableStateOf(true) }
    var showMeaning by remember { mutableStateOf(true) }

    val groupName by dictionaryViewModel.getGroupName(groupId).observeAsState("")
    var filteredEntries by remember { mutableStateOf(emptyList<DictionaryEntry>()) }

    // Local state cho selection mode trong GroupDetailScreen
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedItems = remember { mutableStateListOf<String>() }

    // Lọc danh sách dựa trên searchQuery.text
    LaunchedEffect(entries, searchQuery) {
        filteredEntries = entries.filter { entry ->
            entry.id.contains(searchQuery.text, ignoreCase = true) ||
                    dictionaryViewModel.getKanji(entry.id)
                        .any { it.kanji.contains(searchQuery.text, ignoreCase = true) } ||
                    dictionaryViewModel.getReading(entry.id)
                        .any { it.reading.contains(searchQuery.text, ignoreCase = true) } ||
                    dictionaryViewModel.getSenses(entry.id).any { sense ->
                        sense.glosses.any { it.contains(searchQuery.text, ignoreCase = true) }
                    }
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // Header khi đang ở chế độ chọn (selection mode)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shadowElevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Nút back để thoát khỏi selection mode (chỉ clear selectedItems)
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedItems.clear()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Cancel selection"
                            )
                        }
                        Text(
                            text = "${selectedItems.size} selected",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row {
                            // Nút select/deselect all: nếu chưa đủ thì thêm hết, nếu đủ rồi thì bỏ chọn hết
                            IconButton(
                                onClick = {
                                    if (selectedItems.size < filteredEntries.size) {
                                        selectedItems.addAll(
                                            filteredEntries.map { it.id }
                                                .filterNot { selectedItems.contains(it) }
                                        )
                                    } else {
                                        selectedItems.clear()
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (selectedItems.size < filteredEntries.size)
                                            R.drawable.baseline_radio_button_unchecked_24
                                        else
                                            R.drawable.baseline_radio_button_checked_24
                                    ),
                                    contentDescription = "Select All"
                                )
                            }
                            // Nút xóa các entry đã chọn
                            IconButton(
                                onClick = {
                                    dictionaryViewModel.deleteEntriesFromGroup(
                                        selectedItems.toList(),
                                        groupId
                                    )
                                    selectedItems.clear()
                                    isSelectionMode = false
                                },
                                enabled = selectedItems.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete selected"
                                )
                            }
                        }
                    }
                }
            } else {
                TopAppBar(
                    title = {
                        if (isSearchExpanded) {
                            // Hiển thị CustomSearchBar khi đang mở tìm kiếm
                            CustomSearchBar(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                onSearch = { /* Xử lý search nếu cần */ },
                                onFocusChanged = { /* Xử lý focus nếu cần */ },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // Hiển thị tên group
                            Text(text = groupName)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isSelectionMode) {
                                isSelectionMode = false
                                selectedItems.clear()
                            } else {
                                navController.popBackStack()
                            }
                        }) {
                            Icon(
                                imageVector = if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isSelectionMode) "Cancel" else "Back"
                            )
                        }
                    },
                    actions = {
                        if (!isSelectionMode) {
                            if (isSearchExpanded) {
                                IconButton(onClick = {
                                    isSearchExpanded = false
                                    searchQuery = TextFieldValue("")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Search"
                                    )
                                }
                            } else {
                                IconButton(onClick = { isSearchExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search"
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = {
                        navController.navigate(
                            "flashcard/$groupId" +
                                    "?showWord=${showWord}" +
                                    "&showReading=${showReading}" +
                                    "&showMeaning=${showMeaning}"
                        )
                    },
                    text = { Text("Flashcard") },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.baseline_style_24),
                            contentDescription = "Flashcard"
                        )
                    }
                )
            }
        }


    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showWord,
                        onCheckedChange = { showWord = it }
                    )
                    Text("Word")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showReading,
                        onCheckedChange = { showReading = it }
                    )
                    Text("Reading")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showMeaning,
                        onCheckedChange = { showMeaning = it }
                    )
                    Text("Meaning")
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredEntries) { entry ->
                    EntryCard(
                        entry = entry,
                        dictionaryViewModel = dictionaryViewModel,
                        isSelectionMode = isSelectionMode,
                        selectedItems = selectedItems,
                        mainActivity = mainActivity,
                        navController = navController,
                        showWord = showWord,
                        showReading = showReading,
                        showMeaning = showMeaning,
                        onLongPressAction = {
                            // Khi long press, bật selection mode và thêm entry nếu chưa có
                            if (!isSelectionMode) {
                                isSelectionMode = true
                            }
                            if (!selectedItems.contains(entry.id)) {
                                selectedItems.add(entry.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryCard(
    entry: DictionaryEntry,
    dictionaryViewModel: DictionaryViewModel,
    isSelectionMode: Boolean,
    selectedItems: MutableList<String>,
    mainActivity: MainActivity,
    navController: NavController,
    showWord: Boolean,
    showReading: Boolean,
    showMeaning: Boolean,
    onLongPressAction: () -> Unit
) {
    val kanjiList by produceState(initialValue = emptyList<Kanji>(), key1 = entry.id) {
        value = dictionaryViewModel.getKanji(entry.id)
    }
    val readingList by produceState(initialValue = emptyList<Reading>(), key1 = entry.id) {
        value = dictionaryViewModel.getReading(entry.id)
    }
    val sensesList by produceState(initialValue = emptyList<Sense>(), key1 = entry.id) {
        value = dictionaryViewModel.getSenses(entry.id)
    }

    // Khi showWord == false, ta ẩn tiêu đề (không dùng reading làm tiêu đề)
    val titleText: String? = if (showWord) {
        kanjiList.firstOrNull()?.kanji ?: readingList.firstOrNull()?.reading
    } else {
        null
    }

    val supplementaryReadings: String? = if (showReading && readingList.isNotEmpty()) {
        if (showWord && kanjiList.isNotEmpty()) {
            // Nếu tiêu đề là kanji, hiển thị tất cả các reading
            readingList.joinToString("、") { it.reading }
        } else if (showWord) {
            // Nếu tiêu đề là reading (do không có kanji)
            if (readingList.size > 1) {
                readingList.drop(1).joinToString("、") { it.reading }
            } else {
                readingList.first().reading
            }
        } else {
            // Nếu không hiển thị Word, hiển thị toàn bộ reading
            readingList.joinToString("、") { it.reading }
        }
    } else null

    val meanings = sensesList.flatMap { it.glosses }
    val isSelected = selectedItems.contains(entry.id)

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        if (isSelected) selectedItems.remove(entry.id)
                        else selectedItems.add(entry.id)
                    } else {
                        navController.navigate("detail/${entry.id}")
                    }
                },
                onLongClick = { onLongPressAction() }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (showWord || showReading) {
                // Dùng Row với chiều cao tối thiểu để luôn giữ không gian cho tiêu đề
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp), // chiều cao tối thiểu cho header
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        // Nếu có tiêu đề, hiển thị; nếu không, vẫn giữ chỗ
                        if (titleText != null) {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleLarge
                            )
                        } else {
                            // Hiển thị một Text rỗng để giữ layout (có thể dùng Spacer thay thế)
                            Text(text = "", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    // Icon loa luôn được hiển thị ở bên phải
                    IconButton(onClick = {
                        mainActivity.speakOut(titleText ?: supplementaryReadings.orEmpty())
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_volume_up_24),
                            contentDescription = "Speak"
                        )
                    }
                }
                if (showReading && !supplementaryReadings.isNullOrEmpty()) {
                    Text(
                        text = supplementaryReadings,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            if (showMeaning && meanings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = meanings.take(3).joinToString("; "),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(4.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isSelected) R.drawable.baseline_radio_button_checked_24
                            else R.drawable.baseline_radio_button_unchecked_24
                        ),
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
