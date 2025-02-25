package com.example.japanesedictionary.utils


fun Char.isKanji(): Boolean {
    return this !in '\u3040'..'\u309F' && // Hiragana
            this !in '\u30A0'..'\u30FF' && // Katakana
            this !in '\u0000'..'\u007F' && // Latin and control characters
            this !in '\uFF10'..'\uFF19' && // Full-width digits
            this !in '\uFF21'..'\uFF3A' && // Full-width Latin uppercase
            this !in '\uFF41'..'\uFF5A' && // Full-width Latin lowercase
            this !in '\uFF66'..'\uFF9D' && // Half-width Katakana
            this !in '\uFFA0'..'\uFFDC'    // Half-width Hangul
}

fun Char.isHiragana(): Boolean {
    return this in '\u3040'..'\u309F'
}

fun Char.isKatakana(): Boolean {
    return this in '\u30A0'..'\u30FF'
}