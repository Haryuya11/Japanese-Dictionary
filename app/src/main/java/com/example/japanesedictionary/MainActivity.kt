package com.example.japanesedictionary

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.japanesedictionary.data.DictionaryDatabase
import com.example.japanesedictionary.navigation.NavGraph
import com.example.japanesedictionary.ui.theme.JapaneseDictionaryTheme
import com.example.japanesedictionary.utils.convertKatakanaToHiragana
import com.example.japanesedictionary.utils.isJapanese
import com.example.japanesedictionary.utils.isKatakana
import com.example.japanesedictionary.utils.removeSymbols
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private var hasData = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        lifecycleScope.launchWhenCreated {
            hasData = checkIfDataExists()
        }
        setContent {
            JapaneseDictionaryTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController, mainActivity = this, hasData = hasData)
            }
        }
    }

    private suspend fun checkIfDataExists(): Boolean {
        return withContext(Dispatchers.IO) {
            val db = DictionaryDatabase.getDatabase(applicationContext)
            val dao = db.dictionaryDao()
            val kanjiDao = db.kanjiDao()
            dao.getCount() > 0 && kanjiDao.getCount() > 0
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.JAPANESE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                showToast("This language is not supported")
            } else {
                showToast("Text-to-Speech initialized successfully")

            }
        } else {
            showToast("Text-to-Speech initialization failed")
        }
    }

    fun speakOut(text: String) {
        val (newLocale, spokenText) =  if(text.isJapanese() || text.isKatakana()){
            Locale.JAPANESE to if (text.isKatakana()) text.convertKatakanaToHiragana() else text
        } else {
            Locale.ENGLISH to text.removeSymbols()
        }

        val result = tts.setLanguage(newLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            showToast(("Language $spokenText is not supported"))
            return
        }


        if (tts.isSpeaking) {
            showToast("TTS is already speaking")
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            showToast("Speaking: $text")
        }
    }

    override fun onDestroy() {
        if (tts.isSpeaking) {
            tts.stop()
        }
        tts.shutdown()
        super.onDestroy()
    }

    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}