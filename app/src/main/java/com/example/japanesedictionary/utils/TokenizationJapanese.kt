package com.example.japanesedictionary.utils

import com.atilika.kuromoji.TokenizerBase
import com.atilika.kuromoji.ipadic.Tokenizer
import com.atilika.kuromoji.ipadic.Token

object JapaneseTokenizer {
    val tokenizer: Tokenizer by lazy {
        Tokenizer.Builder().mode(TokenizerBase.Mode.SEARCH).build()
    }
}

fun tokenizeJapaneseText(text: String): String {
    val tokens: List<Token> = JapaneseTokenizer.tokenizer.tokenize(text)
    return tokens.joinToString(" ") { it.surface }
}