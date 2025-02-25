package com.example.japanesedictionary.utils

fun String.convertKatakanaToHiragana(): String {
    return this.map { char ->
        if (char in '\u30A1'..'\u30F6') {
            (char.code - 0x60).toChar()
        } else {
            char
        }
    }.joinToString("")
}

fun String.removeSymbols(): String {
    return this.replace(Regex("[-.]"), "")
}

fun String.isKatakana(): Boolean = any { it in '\u30A0'..'\u30FF' }

fun String.isJapanese(): Boolean {
    return this.any {
        it in '\u3040'..'\u30FF' || it in '\u4E00'..'\u9FFF'
    }
}