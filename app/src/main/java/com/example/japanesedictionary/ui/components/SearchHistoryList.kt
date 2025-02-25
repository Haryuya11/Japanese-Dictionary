package com.example.japanesedictionary.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.japanesedictionary.MainActivity
import com.example.japanesedictionary.R
import com.example.japanesedictionary.viewmodel.DictionaryViewModel

@Composable
fun SearchHistoryList(
    navController: NavController,
    searchHistory: List<String>,
    mainActivity: MainActivity,
    onItemClick: (String) -> Unit,
    viewModel: DictionaryViewModel,
    modifier: Modifier = Modifier
) {
    val isSelectionMode by viewModel.isSelectionMode
    val selectedItems = viewModel.selectedItems

    LazyColumn(modifier = modifier) {
        itemsIndexed(searchHistory) { _, item ->
            SearchHistoryItem(
                item = item,
                isSelected = selectedItems.contains(item),
                isSelectionMode = isSelectionMode,
                onItemClick = {
                    if (isSelectionMode) {
                        viewModel.toggleSelection(item)
                    } else {
                        onItemClick(item)
                    }
                },
                onLongClick = { viewModel.toggleSelection(item) },
                mainActivity = mainActivity
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchHistoryItem(
    item: String,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    mainActivity: MainActivity
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = { onItemClick() },
                onLongClick = { onLongClick() }
            ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            if (isSelectionMode) {
                Icon(
                    painter = painterResource(id = if (isSelected) R.drawable.baseline_radio_button_checked_24 else R.drawable.baseline_radio_button_unchecked_24),
                    contentDescription = "Selection",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }

            Text(
                text = item,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { mainActivity.speakOut(item) }
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