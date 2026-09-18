package com.example.speech

object BanglishTransliteration {

    private val commonWordMap = mapOf(
        "আমি" to "Ami",
        "আমার" to "amar",
        "আমরা" to "amra",
        "তুমি" to "Tumi",
        "তোমার" to "tomar",
        "তোমরা" to "tomra",
        "সে" to "she",
        "তার" to "tar",
        "তারা" to "tara",
        "সাথে" to "sathe",
        "কথা" to "kotha",
        "বলছি" to "bolchi",
        "বলছিলে" to "bolchhile",
        "বলবে" to "bolbe",
        "এখন" to "ekhon",
        "কেমন" to "kemon",
        "আছো" to "acho",
        "আছেন" to "achen",
        "ধন্যবাদ" to "dhonnobad",
        "বাংলাদেশ" to "Bangladesh",
        "ভালো" to "bhalo",
        "ভাল" to "bhalo",
        "কি" to "ki",
        "কী" to "ki",
        "কোথায়" to "kothay",
        "কোথা" to "kotha",
        "কেন" to "keno",
        "হ্যাঁ" to "haan",
        "না" to "na",
        "খুব" to "khub",
        "সুন্দর" to "shundor",
        "দিন" to "din",
        "রাত" to "raat",
        "কাজ" to "kaaj",
        "করছি" to "korchi",
        "করবো" to "korbo",
        "যাবো" to "jabo",
        "আসছি" to "ashchi",
        "ভালোবাসি" to "bhalobashi",
        "মানুষ" to "manush",
        "বন্ধু" to "bondhu",
        "ভাই" to "bhai",
        "কে" to "ke",
        "কখন" to "kokhon",
        "একটু" to "ektu",
        "অনেক" to "onek",
        "সবাই" to "shobai",
        "আজকে" to "aajke",
        "কালকে" to "kaalke",
        "ঠিক" to "thik",
        "আছে" to "ache"
    )

    private val vowels = mapOf(
        'অ' to "o",
        'আ' to "a",
        'ই' to "i",
        'ঈ' to "ee",
        'উ' to "u",
        'ঊ' to "oo",
        'ঋ' to "ri",
        'এ' to "e",
        'ঐ' to "oi",
        'ও' to "o",
        'ঔ' to "ou"
    )

    private val vowelMarks = mapOf(
        'া' to "a",
        'ি' to "i",
        'ী' to "ee",
        'ু' to "u",
        'ূ' to "oo",
        'ৃ' to "ri",
        'ে' to "e",
        'ৈ' to "oi",
        'ো' to "o",
        'ৌ' to "ou"
    )

    private val consonants = mapOf(
        'ক' to "k", 'খ' to "kh", 'গ' to "g", 'ঘ' to "gh", 'ঙ' to "ng",
        'চ' to "ch", 'ছ' to "chh", 'জ' to "j", 'ঝ' to "jh", 'ঞ' to "n",
        'ট' to "t", 'ঠ' to "th", 'ড' to "d", 'ঢ' to "dh", 'ণ' to "n",
        'ত' to "t", 'থ' to "th", 'দ' to "d", 'ধ' to "dh", 'ন' to "n",
        'প' to "p", 'ফ' to "f", 'ব' to "b", 'ভ' to "bh", 'ম' to "m",
        'য' to "j", 'র' to "r", 'ল' to "l", 'শ' to "sh", 'ষ' to "sh",
        'স' to "s", 'হ' to "h", 'ড়' to "r", 'ঢ়' to "rh", 'য়' to "y",
        'ৎ' to "t", 'ং' to "ng", 'ঃ' to "h", 'ঁ' to "n"
    )

    fun toBanglish(bengaliText: String): String {
        val trimmed = bengaliText.trim()
        if (trimmed.isEmpty()) return ""

        // Check if input is already in Latin script
        val hasBengaliChars = trimmed.any { it in '\u0980'..'\u09FF' }
        if (!hasBengaliChars) {
            return trimmed
        }

        val words = trimmed.split(Regex("\\s+"))
        val convertedWords = words.mapIndexed { index, word ->
            // Strip punctuation for matching
            val cleanWord = word.replace(Regex("[।.,!?:;\"'()_]+"), "")
            val punctuationSuffix = word.takeLastWhile { it in "।.,!?:;\"'()" }.replace("।", ".")

            val matched = commonWordMap[cleanWord]
            val result = if (matched != null) {
                if (index == 0) matched.replaceFirstChar { it.uppercase() } else matched.lowercase()
            } else {
                phoneticConvertWord(cleanWord)
            }
            result + punctuationSuffix
        }

        val finalString = convertedWords.joinToString(" ")
        return finalString.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun phoneticConvertWord(word: String): String {
        if (word.isEmpty()) return ""
        val sb = StringBuilder()
        var i = 0
        val len = word.length

        while (i < len) {
            val ch = word[i]

            when {
                vowels.containsKey(ch) -> {
                    sb.append(vowels[ch])
                }
                vowelMarks.containsKey(ch) -> {
                    sb.append(vowelMarks[ch])
                }
                consonants.containsKey(ch) -> {
                    val cons = consonants[ch] ?: ""
                    sb.append(cons)

                    // Lookahead: check if next character is vowel mark, hasanta, or next consonant
                    if (i + 1 < len) {
                        val nextCh = word[i + 1]
                        if (nextCh == '্') {
                            // Hasanta (conjunct connector)
                            i += 1 // skip hasanta
                        } else if (!vowelMarks.containsKey(nextCh) && consonants.containsKey(nextCh)) {
                            // Inherent 'o' or 'a' between consonants if not ending
                            if (i + 2 < len && !vowelMarks.containsKey(word[i + 2])) {
                                sb.append("o")
                            }
                        }
                    }
                }
                else -> {
                    sb.append(ch)
                }
            }
            i++
        }
        return sb.toString()
    }
}
