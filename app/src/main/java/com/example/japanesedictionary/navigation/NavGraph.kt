package com.example.japanesedictionary.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.japanesedictionary.ui.screens.DetailScreen
import com.example.japanesedictionary.ui.screens.DictionaryScreen
import com.example.japanesedictionary.ui.screens.LoadingScreen
import com.example.japanesedictionary.ui.screens.SearchResultsScreen
import com.example.japanesedictionary.MainActivity
import com.example.japanesedictionary.ui.screens.AddToGroupScreen
import com.example.japanesedictionary.ui.screens.FlashcardScreen
import com.example.japanesedictionary.ui.screens.GroupDetailScreen
import com.example.japanesedictionary.ui.screens.KanjiDetailScreen
import com.example.japanesedictionary.ui.screens.SaveGroupsScreen
import com.example.japanesedictionary.ui.screens.SplashScreen
import com.example.japanesedictionary.ui.screens.SearchScreen
import com.example.japanesedictionary.viewmodel.DictionaryViewModel
import com.example.japanesedictionary.viewmodel.KanjiViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = "splash",
    mainActivity: MainActivity,
    hasData: Boolean
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("splash") {
            SplashScreen(navController = navController, hasData = hasData)
        }
        composable("loading") {
            LoadingScreen(context = navController.context) { isSuccess, count ->
                if (isSuccess) {
                    navController.navigate("dictionary") {
                        popUpTo("loading") { inclusive = true }
                    }
                } else {
                    mainActivity.showToast("Failed to load data")
                }
            }
        }
        composable("dictionary") { backStackEntry ->
            val viewModel = viewModel<DictionaryViewModel>(backStackEntry)
            DictionaryScreen(
                navController = navController,
                dictionaryViewModel = viewModel
            )
        }
        composable("detail/{entryId}") { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId") ?: return@composable
            val parentEntry =
                remember(backStackEntry) { navController.getBackStackEntry("dictionary") }
            val dictionaryViewModel = viewModel<DictionaryViewModel>(parentEntry)
            DetailScreen(
                navController = navController,
                entryId = entryId,
                mainActivity = mainActivity,
                dictionaryViewModel = dictionaryViewModel
            )
        }
        composable("kanjiDetail/{kanjiLiteral}") { backStackEntry ->
            val kanjiLiteral =
                backStackEntry.arguments?.getString("kanjiLiteral") ?: return@composable
            val viewModel = viewModel<KanjiViewModel>(backStackEntry)
            KanjiDetailScreen(
                navController = navController,
                kanjiLiteral = kanjiLiteral,
                mainActivity = mainActivity,
                kanjiViewModel = viewModel
            )
        }
        composable("searchResults/{query}") { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: return@composable
            val parentEntry =
                remember(backStackEntry) { navController.getBackStackEntry("dictionary") }
            val viewModel = viewModel<DictionaryViewModel>(parentEntry)
            SearchResultsScreen(
                navController = navController,
                query = query,
                mainActivity = mainActivity,
                viewModel = viewModel
            )
        }
        composable("searchScreen") { backStackEntry ->
            val parentEntry =
                remember(backStackEntry) { navController.getBackStackEntry("dictionary") }
            val viewModel = viewModel<DictionaryViewModel>(parentEntry)
            var query by remember { mutableStateOf(TextFieldValue("")) }
            // Giả sử viewModel.searchHistory và viewModel.suggestions là State hoặc Flow mà bạn có thể observe
            val searchHistory by remember { viewModel.searchHistory }
            val suggestions by viewModel.suggestions.collectAsState()
            SearchScreen(
                query = query,
                onQueryChange = { newValue ->
                    query = newValue
                    viewModel.fetchSuggestions(newValue.text)
                },
                onSearch = {
                    viewModel.searchWord(query.text)
                    viewModel.addSearchHistory(query.text)
                    navController.navigate("searchResults/${query.text}")
                },
                viewModel = viewModel,
                searchHistory = searchHistory,
                suggestions = suggestions,
                onBack = { navController.popBackStack() },
                mainActivity = mainActivity,
                navController = navController
            )
        }
        composable("addToGroup/{entryId}") { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId") ?: return@composable
            val parentEntry =
                remember(backStackEntry) { navController.getBackStackEntry("dictionary") }
            val dictionaryViewModel = viewModel<DictionaryViewModel>(parentEntry)
            AddToGroupScreen(
                navController = navController,
                entryId = entryId,
                dictionaryViewModel = dictionaryViewModel,
                mainActivity = mainActivity
            )
        }
        composable("saveGroups") { backStackEntry ->
            val viewModel = viewModel<DictionaryViewModel>(backStackEntry)
            SaveGroupsScreen(
                navController = navController,
                dictionaryViewModel = viewModel,
                mainActivity = mainActivity
            )
        }
        composable("groupDetail/{groupId}") { backStackEntry ->
            val groupId =
                backStackEntry.arguments?.getString("groupId")?.toIntOrNull() ?: return@composable
            val viewModel = viewModel<DictionaryViewModel>(backStackEntry)
            GroupDetailScreen(
                navController = navController,
                groupId = groupId,
                dictionaryViewModel = viewModel,
                mainActivity = mainActivity
            )
        }

        // Trong file NavGraph.kt
        composable("flashcard/{groupId}?showWord={showWord}&showReading={showReading}&showMeaning={showMeaning}",
            arguments = listOf(
                navArgument("groupId") { type = NavType.IntType },
                navArgument("showWord") {
                    type = NavType.BoolType
                    defaultValue = true
                },
                navArgument("showReading") {
                    type = NavType.BoolType
                    defaultValue = true
                },
                navArgument("showMeaning") {
                    type = NavType.BoolType
                    defaultValue = true
                }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getInt("groupId") ?: return@composable
            val showWord = backStackEntry.arguments?.getBoolean("showWord") ?: true
            val showReading = backStackEntry.arguments?.getBoolean("showReading") ?: true
            val showMeaning = backStackEntry.arguments?.getBoolean("showMeaning") ?: true

            val viewModel = viewModel<DictionaryViewModel>(backStackEntry)

            FlashcardScreen(
                navController = navController,
                groupId = groupId,
                dictionaryViewModel = viewModel,
                mainActivity = mainActivity,
                showWord = showWord,
                showReading = showReading,
                showMeaning = showMeaning
            )
        }
    }
}
