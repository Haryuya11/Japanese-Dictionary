package com.example.japanesedictionary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.japanesedictionary.MainActivity
import com.example.japanesedictionary.R
import com.example.japanesedictionary.data.model.DictionaryEntry
import com.example.japanesedictionary.ui.components.CustomSearchBar
import com.example.japanesedictionary.ui.components.SearchModeSwitch
import com.example.japanesedictionary.ui.components.SuggestionsList
import com.example.japanesedictionary.ui.components.SearchHistoryList
import com.example.japanesedictionary.viewmodel.DictionaryViewModel

@Composable
fun SearchScreen(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    viewModel: DictionaryViewModel,
    searchHistory: List<String>,
    suggestions: List<Pair<Triple<DictionaryEntry, String?, String?>, String>>,
    onBack: () -> Unit,
    mainActivity: MainActivity,
    navController: NavController
) {
    val isSelectionMode by viewModel.isSelectionMode
    val selectedCount = viewModel.selectedItems.size

    Column {
        // Header: nếu đang ở chế độ selection (xóa history) thì hiển thị header chọn xóa,
        // ngược lại hiển thị search header thông thường.
        if (isSelectionMode) {
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
                    // Nút back để thoát khỏi chế độ chọn
                    IconButton(onClick = { viewModel.clearSelection() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel selection"
                        )
                    }
                    Text(
                        text = "$selectedCount selected",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row {
                        // Nút radio để chọn/deselect tất cả
                        IconButton(
                            onClick = {
                                if (selectedCount < searchHistory.size) {
                                    viewModel.selectAll()
                                } else {
                                    viewModel.deselectAll()
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (selectedCount < searchHistory.size)
                                        R.drawable.baseline_radio_button_unchecked_24
                                    else
                                        R.drawable.baseline_radio_button_checked_24
                                ),
                                contentDescription = "Select All"
                            )
                        }
                        // Nút xóa các item đã chọn
                        IconButton(
                            onClick = { viewModel.deleteSelectedItems() },
                            enabled = selectedCount > 0
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
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    CustomSearchBar(
                        query = query,
                        onQueryChange = onQueryChange,
                        onSearch = onSearch,
                        onFocusChanged = {},
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    SearchModeSwitch(viewModel = viewModel)
                }
            }
        }

        // Nội dung chính
        if (query.text.isEmpty()) {
            if (searchHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No search history", color = MaterialTheme.colorScheme.onSurface)
                }
            } else {
                // Danh sách history; lưu ý bên trong list không cần hiển thị header chọn nữa
                SearchHistoryList(
                    navController = navController,
                    searchHistory = searchHistory,
                    mainActivity = mainActivity,
                    modifier = Modifier.padding(8.dp),
                    onItemClick = {
                        if (isSelectionMode) {
                            viewModel.toggleSelection(it)
                        } else {
                            viewModel.searchWord(it)
                            navController.navigate("searchResults/$it")
                        }
                    },
                    viewModel = viewModel
                )
            }
        } else {
            if (suggestions.isNotEmpty()) {
                SuggestionsList(
                    navController = navController,
                    suggestions = suggestions,
                    mainActivity = mainActivity,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
