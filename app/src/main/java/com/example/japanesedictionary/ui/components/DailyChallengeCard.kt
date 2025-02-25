package com.example.japanesedictionary.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.japanesedictionary.R
import com.example.japanesedictionary.viewmodel.DictionaryEntryDetail
import com.example.japanesedictionary.viewmodel.QuizState
import com.example.japanesedictionary.viewmodel.QuizViewModel

@Composable
fun DailyChallengeCard(
    viewModel: QuizViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val quizState by viewModel.quizState

    Card(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Daily Challenge",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            when (quizState) {
                is QuizState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                is QuizState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_cancel_24),
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Error: ${(quizState as QuizState.Error).message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                is QuizState.TrueFalseQuiz -> {
                    TrueFalseQuizUI(
                        state = quizState as QuizState.TrueFalseQuiz,
                        viewModel = viewModel
                    )
                }

                is QuizState.MultipleChoiceQuiz -> {
                    MultipleChoiceQuizUI(
                        state = quizState as QuizState.MultipleChoiceQuiz,
                        viewModel = viewModel
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.loadNewQuiz() },
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Change Question")
            }
        }
    }
}

@Composable
fun QuestionHeader(entry: DictionaryEntryDetail) {
    val hasKanji = entry.kanji.isNotEmpty() && entry.kanji.first().kanji.isNotBlank()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (hasKanji) {
            Text(
                text = entry.kanji.first().kanji,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.readings.firstOrNull()?.reading ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = entry.readings.firstOrNull()?.reading ?: "",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SuccessResult(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Success",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            message,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ErrorResult(message: String, correctAnswer: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_cancel_24),
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            message,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Correct answer: $correctAnswer",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TrueFalseQuizUI(
    state: QuizState.TrueFalseQuiz,
    viewModel: QuizViewModel
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        QuestionHeader(state.entry)
        Spacer(modifier = Modifier.height(20.dp))
        if (!state.isAnswered) {
            Text(
                text = state.displayMeaning,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { viewModel.checkAnswer(true) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("True")
                }
                Button(
                    onClick = { viewModel.checkAnswer(false) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("False")
                }
            }
        } else {
            if (state.isCorrect) {
                SuccessResult(message = "Correct!")
            } else {
                ErrorResult(message = "Incorrect!", correctAnswer = state.correctMeaning)
            }
        }
    }
}

@Composable
private fun MultipleChoiceQuizUI(
    state: QuizState.MultipleChoiceQuiz,
    viewModel: QuizViewModel
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        QuestionHeader(state.entry)
        Spacer(modifier = Modifier.height(20.dp))
        if (!state.isAnswered) {
            state.choices.forEachIndexed { index, meaning ->
                Button(
                    onClick = { viewModel.checkAnswer(index) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(meaning)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            if (state.isCorrect == true) {
                SuccessResult(message = "Correct!")
            } else {
                ErrorResult(
                    message = "Incorrect!",
                    correctAnswer = state.choices[state.correctAnswer]
                )
            }
        }
    }
}
