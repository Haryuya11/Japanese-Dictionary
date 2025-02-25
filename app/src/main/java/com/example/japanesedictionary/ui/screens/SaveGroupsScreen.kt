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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.japanesedictionary.MainActivity
import com.example.japanesedictionary.data.model.SortOption
import com.example.japanesedictionary.ui.components.CreateGroupDialog
import com.example.japanesedictionary.ui.components.SortOptionsDialog
import com.example.japanesedictionary.viewmodel.DictionaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveGroupsScreen(
    navController: NavController,
    dictionaryViewModel: DictionaryViewModel = viewModel(),
    mainActivity: MainActivity
) {
    val groups by dictionaryViewModel.allGroups.observeAsState(initial = emptyList())
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showSortOptions by remember { mutableStateOf(false) }
    val currentSortOption by dictionaryViewModel.currentSortOption.observeAsState(SortOption.NAME_ASC)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Save Groups") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(groups) { group ->
                    val groupSize by dictionaryViewModel.getGroupSize(group.id).observeAsState(0)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                navController.navigate("groupDetail/${group.id}")
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
