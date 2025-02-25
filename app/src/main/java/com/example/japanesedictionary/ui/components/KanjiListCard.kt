package com.example.japanesedictionary.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.japanesedictionary.MainActivity

@Composable
fun KanjiListCard(
    kanjiSet: Set<Char>,
    mainActivity: MainActivity,
    navController: NavController
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Kanji",
                style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.primary)
            )

            LazyRow(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(kanjiSet.toList()) { kanji ->
                    Card(
                        modifier = Modifier
                            .clickable {
                                navController.navigate("kanjiDetail/${kanji}")
                            }
                            .size(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = kanji.toString(),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 48.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
