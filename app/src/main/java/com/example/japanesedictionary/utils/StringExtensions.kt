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

private val romajiToHiraganaMap = mapOf(
    "a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "お",
    "ka" to "か", "ki" to "き", "ku" to "く", "ke" to "け", "ko" to "こ",
    "sa" to "さ", "shi" to "し", "su" to "す", "se" to "せ", "so" to "そ",
    "ta" to "た", "chi" to "ち", "tsu" to "つ", "te" to "て", "to" to "と",
    "na" to "な", "ni" to "に", "nu" to "ぬ", "ne" to "ね", "no" to "の",
    "ha" to "は", "hi" to "ひ", "fu" to "ふ", "he" to "へ", "ho" to "ほ",
    "ma" to "ま", "mi" to "み", "mu" to "む", "me" to "め", "mo" to "も",
    "ya" to "や", "yu" to "ゆ", "yo" to "よ",
    "ra" to "ら", "ri" to "り", "ru" to "る", "re" to "れ", "ro" to "ろ",
    "wa" to "わ", "wi" to "ゐ", "we" to "ゑ", "wo" to "を",
    "n" to "ん",
    "ga" to "が", "gi" to "ぎ", "gu" to "ぐ", "ge" to "げ", "go" to "ご",
    "za" to "ざ", "ji" to "じ", "zu" to "ず", "ze" to "ぜ", "zo" to "ぞ",
    "da" to "だ", "di" to "ぢ", "du" to "づ", "de" to "で", "do" to "ど",
    "ba" to "ば", "bi" to "び", "bu" to "ぶ", "be" to "べ", "bo" to "ぼ",
    "pa" to "ぱ", "pi" to "ぴ", "pu" to "ぷ", "pe" to "ぺ", "po" to "ぽ",
    // Small characters for combinations
    "kya" to "きゃ", "kyu" to "きゅ", "kyo" to "きょ",
    "sha" to "しゃ", "shu" to "しゅ", "sho" to "しょ",
    "cha" to "ちゃ", "chu" to "ちゅ", "cho" to "ちょ",
    "nya" to "にゃ", "nyu" to "にゅ", "nyo" to "にょ",
    "hya" to "ひゃ", "hyu" to "ひゅ", "hyo" to "ひょ",
    "mya" to "みゃ", "myu" to "みゅ", "myo" to "みょ",
    "rya" to "りゃ", "ryu" to "りゅ", "ryo" to "りょ",
    "gya" to "ぎゃ", "gyu" to "ぎゅ", "gyo" to "ぎょ",
    "ja" to "じゃ", "ju" to "じゅ", "jo" to "じょ",
    "bya" to "びゃ", "byu" to "びゅ", "byo" to "びょ",
    "pya" to "ぴゃ", "pyu" to "ぴゅ", "pyo" to "ぴょ"
)

fun String.romajiToHiragana(): String {
    val input = this.lowercase()
    val result = StringBuilder()
    var i = 0
    while (i < input.length) {
        var found = false
        for (len in 3 downTo 1) {
            if (i + len <= input.length) {
                val substring = input.substring(i, i + len)
                if (substring in romajiToHiraganaMap) {
                    val hiragana = romajiToHiraganaMap[substring]!!
                    if (i > 0 && input[i - 1] == substring[0] && len > 1) {
                        result.append("っ") // Small tsu
                    }
                    result.append(hiragana)
                    i += len
                    found = true
                    break
                }
            }
        }
        if (!found) {
            result.append(input[i])
            i++
        }
    }
    return result.toString()
}
