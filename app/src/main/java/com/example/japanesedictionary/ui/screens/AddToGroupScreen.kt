package com.example.japanesedictionary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.japanesedictionary.MainActivity
import com.example.japanesedictionary.data.model.SaveGroups
import com.example.japanesedictionary.data.model.SortOption
import com.example.japanesedictionary.ui.components.CreateGroupDialog
import com.example.japanesedictionary.ui.components.SortOptionsDialog
import com.example.japanesedictionary.viewmodel.DictionaryViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToGroupScreen(
    navController: NavController,
    entryId: String,
    dictionaryViewModel: DictionaryViewModel = viewModel(),
    mainActivity: MainActivity
) {
    val groups: List<SaveGroups> by dictionaryViewModel.allGroups.observeAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showSortOptions by remember { mutableStateOf(false) }
    var entryKanjiOrReading by remember { mutableStateOf("") }
    val currentSortOption by dictionaryViewModel.currentSortOption.observeAsState(SortOption.NAME_ASC)

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(entryId) {
        val kanji = dictionaryViewModel.getKanji(entryId).firstOrNull()?.kanji
        val reading = dictionaryViewModel.getReading(entryId).firstOrNull()?.reading
        entryKanjiOrReading = kanji ?: reading ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopAppBar(
            title = { Text("Add $entryKanjiOrReading") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            },
            actions = {
                IconButton(onClick = { showSortOptions = true }) {
                    Icon(imageVector = Icons.Default.List, contentDescription = "Sort")
                }
                IconButton(onClick = { showCreateGroupDialog = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Create Group")
                }
            }
        )

        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Groups") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        LazyColumn {
            items(groups.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }) { group ->
                val groupSize by dictionaryViewModel.getGroupSize(group.id).observeAsState(0)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                        .clickable {
                            coroutineScope.launch {
                                val isInGroup = dictionaryViewModel.isEntryInGroup(entryId, group.id)
                                if (isInGroup) {
                                    mainActivity.showToast("Word already in group")
                                } else {
                                    dictionaryViewModel.addEntryToGroup(entryId, group.id)
                                    navController.popBackStack()
                                }
                            }
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(group.name) },
                        supportingContent = { Text("Words: $groupSize") },
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        if (showCreateGroupDialog) {
            CreateGroupDialog(
                onDismiss = { showCreateGroupDialog = false },
                onCreate = { groupName ->
                    dictionaryViewModel.createGroup(groupName)
                    showCreateGroupDialog = false
                }
            )
        }

        if (showSortOptions) {
            SortOptionsDialog(
                currentSortOption = currentSortOption,
                onDismiss = { showSortOptions = false },
                onSortOptionSelected = { sortOption ->
                    dictionaryViewModel.sortGroups(sortOption)
                    showSortOptions = false
                }
            )
        }
    }
}
