"""
Bongo Type - Banglish Transliteration Engine
Converts Bengali spoken text to authentic Romanized Latin script (Banglish),
preserving phonetics without transforming into Bengali script.
"""

import re

COMMON_WORDS = {
    "আমি": "Ami",
    "আমার": "amar",
    "আমরা": "amra",
    "তুমি": "Tumi",
    "তোমার": "tomar",
    "তোমরা": "tomra",
    "সে": "she",
    "তার": "tar",
    "তারা": "tara",
    "সাথে": "sathe",
    "কথা": "kotha",
    "বলছি": "bolchi",
    "বলছিলেন": "bolchilen",
    "বলবে": "bolbe",
    "এখন": "ekhon",
    "কেমন": "kemon",
    "আছো": "acho",
    "আছেন": "achen",
    "ধন্যবাদ": "dhonnobad",
    "বাংলাদেশ": "Bangladesh",
    "ভালো": "bhalo",
    "ভাল": "bhalo",
    "কি": "ki",
    "কী": "ki",
    "কোথায়": "kothay",
    "কোথা": "kotha",
    "কেন": "keno",
    "হ্যাঁ": "haan",
    "না": "na",
    "খুব": "khub",
    "সুন্দর": "shundor",
    "দিন": "din",
    "রাত": "raat",
    "কাজ": "kaaj",
    "করছি": "korchi",
    "করবো": "korbo",
    "ভালোবাসি": "bhalobashi",
    "বন্ধু": "bondhu",
    "ভাই": "bhai",
    "একটু": "ektu",
    "অনেক": "onek",
    "আজকে": "aajke",
    "কালকে": "kaalke",
    "ঠিক": "thik",
    "আছে": "ache"
}

VOWELS = {
    'অ': 'o', 'আ': 'a', 'ই': 'i', 'ঈ': 'ee', 'উ': 'u',
    'ঊ': 'oo', 'ঋ': 'ri', 'এ': 'e', 'ঐ': 'oi', 'ও': 'o', 'ঔ': 'ou'
}

VOWEL_MARKS = {
    'া': 'a', 'ি': 'i', 'ী': 'ee', 'ু': 'u', 'ূ': 'oo',
    'ৃ': 'ri', 'ে': 'e', 'ৈ': 'oi', 'ো': 'o', 'ৌ': 'ou'
}

CONSONANTS = {
    'ক': 'k', 'খ': 'kh', 'গ': 'g', 'ঘ': 'gh', 'ঙ': 'ng',
    'চ': 'ch', 'ছ': 'chh', 'জ': 'j', 'ঝ': 'jh', 'ঞ': 'n',
    'ট': 't', 'ঠ': 'th', 'ড': 'd', 'ঢ': 'dh', 'ণ': 'n',
    'ত': 't', 'থ': 'th', 'দ': 'd', 'ধ': 'dh', 'ন': 'n',
    'প': 'p', 'ফ': 'f', 'ব': 'b', 'ভ': 'bh', 'ম': 'm',
    'য': 'j', 'র': 'r', 'ল': 'l', 'শ': 'sh', 'ষ': 'sh',
    'স': 's', 'হ': 'h', 'ড়': 'r', 'ঢ়': 'rh', 'য়': 'y',
    'ৎ': 't', 'ং': 'ng', 'ঃ': 'h', 'ঁ': 'n'
}


def to_banglish(text: str) -> str:
    """Converts Bengali speech output to natural Banglish Latin script."""
    text = text.strip()
    if not text:
        return ""

    # Check if already primarily Latin script
    has_bengali = any('\u0980' <= c <= '\u09FF' for c in text)
    if not has_bengali:
        return text

    words = re.split(r'\s+', text)
    converted_words = []

    for idx, word in enumerate(words):
        clean_word = re.sub(r'[।.,!?:;\"\'()_]+', '', word)
        trailing_punct = ""
        for c in reversed(word):
            if c in "।.,!?:;\"'()":
                trailing_punct = ('.' if c == '।' else c) + trailing_punct
            else:
                break

        if clean_word in COMMON_WORDS:
            res = COMMON_WORDS[clean_word]
            if idx == 0:
                res = res.capitalize()
            else:
                res = res.lower()
        else:
            res = phonetic_transliterate(clean_word)

        converted_words.append(res + trailing_punct)

    result = " ".join(converted_words)
    if result:
        result = result[0].upper() + result[1:]
    return result


def phonetic_transliterate(word: str) -> str:
    """Fallback phonetic converter using rule-based mapping."""
    res = []
    i = 0
    length = len(word)

    while i < length:
        ch = word[i]
        if ch in VOWELS:
            res.append(VOWELS[ch])
        elif ch in VOWEL_MARKS:
            res.append(VOWEL_MARKS[ch])
        elif ch in CONSONANTS:
            res.append(CONSONANTS[ch])
            if i + 1 < length:
                next_ch = word[i + 1]
                if next_ch == '্':  # Hasanta / conjunct
                    i += 1
                elif next_ch not in VOWEL_MARKS and next_ch in CONSONANTS:
                    if i + 2 < length and word[i + 2] not in VOWEL_MARKS:
                        res.append('o')
        else:
            res.append(ch)
        i += 1

    return "".join(res)
