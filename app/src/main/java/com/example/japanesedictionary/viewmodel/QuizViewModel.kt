package com.example.japanesedictionary.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.japanesedictionary.data.dao.DictionaryDao
import com.example.japanesedictionary.data.model.DictionaryEntry
import com.example.japanesedictionary.data.model.Example
import com.example.japanesedictionary.data.model.Kanji
import com.example.japanesedictionary.data.model.Reading
import com.example.japanesedictionary.data.model.Sense
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val dao: DictionaryDao
) : ViewModel() {
    private val _quizState = mutableStateOf<QuizState>(QuizState.Loading)
    val quizState: State<QuizState> = _quizState

    private var currentEntries = emptyList<DictionaryEntryDetail>()
    private var correctEntry: DictionaryEntryDetail? = null
    private var currentQuizType: QuizType = QuizType.TRUE_FALSE

    init {
        loadNewQuiz()
    }

    fun loadNewQuiz() {
        viewModelScope.launch {
            _quizState.value = QuizState.Loading
            try {
                val entryIds = dao.getRandomEntry()
                currentEntries = entryIds.map { entryId ->
                    var entry: DictionaryEntry?
                    var kanji: List<Kanji>
                    var readings: List<Reading>
                    var senses: List<Sense>
                    var examples = emptyList<Example>()

                    withContext(Dispatchers.IO) {
                        entry = dao.getEntry(entryId)
                        kanji = dao.getKanji(entryId)
                        readings = dao.getReading(entryId)
                        senses = dao.getSenses(entryId)
                        examples = senses.flatMap { sense ->
                            dao.getExamples(sense.id)
                        }
                    }
                    DictionaryEntryDetail(
                        entry = entry!!,
                        kanji = kanji,
                        readings = readings,
                        senses = senses,
                        examples = examples
                    )
                }
                generateNewQuiz()
            } catch (e: Exception) {
                _quizState.value = QuizState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun generateNewQuiz() {
        correctEntry = currentEntries.random()
        currentQuizType = QuizType.entries.toTypedArray().random()

        when (currentQuizType) {
            QuizType.TRUE_FALSE -> {
                val correctMeaning = correctEntry?.getRandomGloss() ?: ""
                val wrongMeaning = currentEntries.filter { it != correctEntry }
                    .random()
                    .getRandomGloss()
                val isMeaningCorrect = Random.nextBoolean()
                _quizState.value = QuizState.TrueFalseQuiz(
                    entry = correctEntry!!,
                    displayMeaning = if (isMeaningCorrect) correctMeaning else wrongMeaning,
                    isCorrect = isMeaningCorrect,
                    isAnswered = false,
                    correctMeaning = correctMeaning
                )
            }
            QuizType.MULTIPLE_CHOICE -> {
                val correctMeaning = correctEntry?.getRandomGloss() ?: ""
                val meanings = currentEntries.map { entry ->
                    if (entry == correctEntry) correctMeaning else entry.getRandomGloss()
                }.shuffled()
                _quizState.value = QuizState.MultipleChoiceQuiz(
                    entry = correctEntry!!,
                    choices = meanings,
                    correctAnswer = meanings.indexOf(correctMeaning),
                    isAnswered = false,
                    selectedAnswer = null,
                    isCorrect = null
                )
            }
        }
    }

    fun checkAnswer(selected: Boolean) {
        when (val currentState = _quizState.value) {
            is QuizState.TrueFalseQuiz -> {
                val newState = currentState.copy(
                    isAnswered = true,
                    isCorrect = selected == currentState.isCorrect
                )
                _quizState.value = newState

                viewModelScope.launch {
                    delay(2000)
                    loadNewQuiz()
                }
            }
            else -> Unit
        }
    }

    fun checkAnswer(selectedIndex: Int) {
        when (val currentState = _quizState.value) {
            is QuizState.MultipleChoiceQuiz -> {
                val isCorrectAnswer = selectedIndex == currentState.correctAnswer
                val newState = currentState.copy(
                    isAnswered = true,
                    selectedAnswer = selectedIndex,
                    isCorrect = isCorrectAnswer
                )
                _quizState.value = newState

                viewModelScope.launch {
                    delay(5000)
                    loadNewQuiz()
                }
            }
            else -> Unit
        }
    }
}

data class DictionaryEntryDetail(
    val entry: DictionaryEntry,
    val kanji: List<Kanji>,
    val readings: List<Reading>,
    val senses: List<Sense>,
    val examples: List<Example>
) {
    fun getRandomGloss(): String {
        return senses.flatMap { it.glosses }.random()
    }
}

sealed class QuizState {
    data object Loading : QuizState()
    data class Error(val message: String) : QuizState()
    data class TrueFalseQuiz(
        val entry: DictionaryEntryDetail,
        val displayMeaning: String,
        val isCorrect: Boolean,
        val isAnswered: Boolean,
        val correctMeaning: String
    ) : QuizState()

    data class MultipleChoiceQuiz(
        val entry: DictionaryEntryDetail,
        val choices: List<String>,
        val correctAnswer: Int,
        val isAnswered: Boolean,
        val selectedAnswer: Int? = null,
        val isCorrect: Boolean? = null
    ) : QuizState()
}

enum class QuizType {
    TRUE_FALSE,
    MULTIPLE_CHOICE
}
