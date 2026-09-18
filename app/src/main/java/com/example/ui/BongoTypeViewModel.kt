package com.example.ui

import android.app.Application
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ActiveAppType
import com.example.model.BongoSettings
import com.example.model.DictationHistoryItem
import com.example.model.DictationLanguage
import com.example.model.MicState
import com.example.service.BongoVoiceBubbleService
import com.example.speech.SpeechRecognizerManager
import com.example.typing.TextInjector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BongoTypeViewModel(application: Application) : AndroidViewModel(application) {

    private val speechManager = SpeechRecognizerManager(application.applicationContext, viewModelScope)
    private val textInjector = TextInjector(application.applicationContext)

    val micState: StateFlow<MicState> = speechManager.micState
    val audioRms: StateFlow<Float> = speechManager.audioRms
    val livePartialText: StateFlow<String> = speechManager.livePartialText

    private val _selectedLanguage = MutableStateFlow(DictationLanguage.BANGLA)
    val selectedLanguage: StateFlow<DictationLanguage> = _selectedLanguage.asStateFlow()

    private val _settings = MutableStateFlow(BongoSettings())
    val settings: StateFlow<BongoSettings> = _settings.asStateFlow()

    private val _activeApp = MutableStateFlow(ActiveAppType.WHATSAPP)
    val activeApp: StateFlow<ActiveAppType> = _activeApp.asStateFlow()

    // Editor texts for each active simulated application
    private val _appTexts = MutableStateFlow(
        mutableMapOf(
            ActiveAppType.WHATSAPP to "Hey! Checking out the new Bengali voice assistant\n",
            ActiveAppType.IMO to "হ্যালো, কেমন আছো?\n",
            ActiveAppType.MESSENGER to "Bongo Type দিয়ে মেসেঞ্জারে ভয়েস টাইপিং হচ্ছে!\n",
            ActiveAppType.TELEGRAM to "Bongo Type voice bubble enabled for Telegram\n",
            ActiveAppType.CHROME to "https://google.com/search?q=",
            ActiveAppType.NOTEPAD to "স্বাগতম Bongo Type এ। এখানে সরাসরি ভয়েস টাইপিং হবে।\n",
            ActiveAppType.VS_CODE to "# Bongo Type Voice Typing Test\nprint('Universal dictation ready')\n"
        )
    )
    val appTexts: StateFlow<Map<ActiveAppType, String>> = _appTexts.asStateFlow()

    private val _history = MutableStateFlow<List<DictationHistoryItem>>(emptyList())
    val history: StateFlow<List<DictationHistoryItem>> = _history.asStateFlow()

    private val _isPureMiniMode = MutableStateFlow(false)
    val isPureMiniMode: StateFlow<Boolean> = _isPureMiniMode.asStateFlow()

    private val _isBubbleServiceActive = MutableStateFlow(false)
    val isBubbleServiceActive: StateFlow<Boolean> = _isBubbleServiceActive.asStateFlow()

    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    init {
        speechManager.onFinalResult = { text, lang ->
            handleDictatedText(text, lang)
        }
        checkBubblePermissions()
    }

    fun checkBubblePermissions() {
        val app = getApplication<Application>()
        _isBubbleServiceActive.value = BongoVoiceBubbleService.isAccessibilityEnabled(app)
        _hasOverlayPermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(app)
        } else {
            true
        }
    }

    fun toggleMic() {
        textInjector.triggerHapticFeedback()
        speechManager.toggleListening()
    }

    fun selectLanguage(lang: DictationLanguage) {
        _selectedLanguage.value = lang
        speechManager.setLanguage(lang)
    }

    fun toggleMiniMode() {
        _isPureMiniMode.value = !_isPureMiniMode.value
    }

    fun selectActiveApp(app: ActiveAppType) {
        _activeApp.value = app
    }

    fun updateActiveAppText(newText: String) {
        val currentMap = _appTexts.value.toMutableMap()
        currentMap[_activeApp.value] = newText
        _appTexts.value = currentMap
    }

    fun updateSettings(newSettings: BongoSettings) {
        _settings.value = newSettings
        if (_selectedLanguage.value != newSettings.defaultLanguage) {
            selectLanguage(newSettings.defaultLanguage)
        }
    }

    fun simulateGlobalShortcut() {
        textInjector.triggerHapticFeedback()
        speechManager.toggleListening()
    }

    fun injectSampleUtterance(prompt: String) {
        speechManager.injectSimulatedUtterance(prompt)
    }

    private fun handleDictatedText(text: String, lang: DictationLanguage) {
        textInjector.triggerHapticFeedback()

        // 1. Add to history
        val item = DictationHistoryItem(
            text = text,
            language = lang,
            targetApp = _activeApp.value.appName
        )
        _history.value = listOf(item) + _history.value.take(25)

        // 2. Auto Text Injection into active app text field
        if (_settings.value.autoTextInjection) {
            val currentContent = _appTexts.value[_activeApp.value] ?: ""
            val separator = if (currentContent.isNotEmpty() && !currentContent.endsWith(" ") && !currentContent.endsWith("\n")) " " else ""
            val newContent = currentContent + separator + text

            val updatedMap = _appTexts.value.toMutableMap()
            updatedMap[_activeApp.value] = newContent
            _appTexts.value = updatedMap

            // Also copy to Android clipboard for fallback / system paste
            textInjector.injectToClipboard(text, notifyUser = false)
        }
    }

    fun copyCurrentActiveText() {
        val content = _appTexts.value[_activeApp.value] ?: ""
        if (content.isNotBlank()) {
            textInjector.injectToClipboard(content, notifyUser = true)
        }
    }

    fun clearCurrentActiveText() {
        val updatedMap = _appTexts.value.toMutableMap()
        updatedMap[_activeApp.value] = ""
        _appTexts.value = updatedMap
    }

    fun pauseDictation() {
        speechManager.setMuted(true)
    }

    fun resumeDictation() {
        speechManager.setMuted(false)
    }

    fun resetApp() {
        speechManager.stopListening()
        _isPureMiniMode.value = false
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}
