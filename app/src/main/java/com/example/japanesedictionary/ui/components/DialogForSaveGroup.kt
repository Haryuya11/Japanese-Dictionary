package com.example.japanesedictionary.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.example.japanesedictionary.data.model.SortOption

@Composable
fun CreateGroupDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var groupName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group") },
        text = {
            TextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") }
            )
        },
        confirmButton = {
            Button(onClick = { onCreate(groupName) }) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortOptionsDialog(
    currentSortOption: SortOption,
    onDismiss: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort Groups") },
        text = {
            Column {
                Text("Sort by:")
                RadioButtonGroup(
                    options = SortOption.entries,
                    selectedOption = currentSortOption,
                    onOptionSelected = onSortOptionSelected
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun <T> RadioButtonGroup(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit
) {
    Column {
        options.forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = (option == selectedOption),
                    onClick = { onOptionSelected(option) }
                )
                Text(option.toString())
            }
        }
    }
}
