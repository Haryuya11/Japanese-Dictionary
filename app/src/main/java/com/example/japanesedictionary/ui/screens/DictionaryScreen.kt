package com.example.japanesedictionary.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.japanesedictionary.MainActivity
import com.example.japanesedictionary.ui.components.DailyChallengeCard
import com.example.japanesedictionary.ui.components.DictionaryHeader
import com.example.japanesedictionary.viewmodel.DictionaryViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner as LifecycleOwner

@Composable
fun DictionaryScreen(
    navController: NavController,
    dictionaryViewModel: DictionaryViewModel = viewModel(),
    mainActivity: MainActivity
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }

    val lifecycleOwner = LifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                query = TextFieldValue("")
                dictionaryViewModel.query.value = ""
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column {
        DictionaryHeader(
            query = query,
            onQueryChange = { newValue ->
                query = newValue
            },
            onSearch = {
                dictionaryViewModel.searchWord(query.text)
                dictionaryViewModel.addSearchHistory(query.text)
                navController.navigate("searchResults/${query.text}")
            },
            onSaveIconClick = { navController.navigate("saveGroups") },
            viewModel = dictionaryViewModel,
            onFocusChanged = { focused ->
                if (focused) {
                    navController.navigate("searchScreen")
                }
            }
        )

        DailyChallengeCard(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        )
    }
}
