package com.example.model

enum class DictationLanguage(val displayName: String, val nativeName: String, val localeCode: String, val flag: String) {
    BANGLA("Bangla", "বাংলা", "bn-BD", "🇧🇩"),
    BANGLISH("Banglish", "Banglish", "bn-BD", "🔤"),
    ARABIC("Arabic", "العربية", "ar-SA", "🇸🇦")
}

enum class MicState {
    IDLE,
    LISTENING,
    PROCESSING,
    MUTED
}

enum class SpeechEngineType(val label: String, val description: String) {
    GOOGLE_SPEECH("Google Speech API", "Fast cloud recognition for Bengali and Arabic"),
    OPENAI_WHISPER("OpenAI Whisper", "High-accuracy multilingual recognition"),
    GEMINI_SPEECH("Gemini Speech API", "Intelligent context-aware dictation & punctuation")
}

enum class GlobalShortcut(val label: String, val keys: String) {
    CTRL_SPACE("CTRL + SPACE", "Ctrl+Space"),
    ALT_SPACE("ALT + SPACE", "Alt+Space"),
    WIN_H("WIN + H", "Windows+H")
}

enum class ActiveAppType(val appName: String, val windowTitle: String, val iconName: String) {
    NOTEPAD("Notepad", "Untitled - Notepad", "edit_note"),
    VS_CODE("VS Code", "main.py - Bongo Type - Visual Studio Code", "code"),
    WHATSAPP("WhatsApp Desktop", "WhatsApp - Family Group", "chat"),
    WORD("Microsoft Word", "Document 1 - Word", "description"),
    CHROME("Google Chrome", "New Tab - Google Chrome", "language")
}

data class DictationHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val language: DictationLanguage,
    val timestamp: Long = System.currentTimeMillis(),
    val targetApp: String
)

data class BongoSettings(
    val defaultLanguage: DictationLanguage = DictationLanguage.BANGLA,
    val shortcut: GlobalShortcut = GlobalShortcut.CTRL_SPACE,
    val speechEngine: SpeechEngineType = SpeechEngineType.GOOGLE_SPEECH,
    val autoTextInjection: Boolean = true,
    val continuousDictation: Boolean = true,
    val startWithWindows: Boolean = true,
    val alwaysOnTop: Boolean = true,
    val typingSpeedMs: Long = 20L,
    val soundEffects: Boolean = true,
    val darkTheme: Boolean = true
)
