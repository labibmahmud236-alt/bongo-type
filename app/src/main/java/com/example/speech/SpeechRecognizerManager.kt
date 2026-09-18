package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.model.DictationLanguage
import com.example.model.MicState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class SpeechRecognizerManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val TAG = "BongoSpeechRecognizer"

    private var speechRecognizer: SpeechRecognizer? = null
    private var isContinuousActive = false
    private var currentLanguage: DictationLanguage = DictationLanguage.BANGLA
    private var simulationJob: Job? = null

    private val _micState = MutableStateFlow(MicState.IDLE)
    val micState: StateFlow<MicState> = _micState.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _livePartialText = MutableStateFlow("")
    val livePartialText: StateFlow<String> = _livePartialText.asStateFlow()

    private val _lastFinalText = MutableStateFlow("")
    val lastFinalText: StateFlow<String> = _lastFinalText.asStateFlow()

    var onFinalResult: ((String, DictationLanguage) -> Unit)? = null

    init {
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }
            } else {
                Log.w(TAG, "Device reports Recognition not available - fallback will be used")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SpeechRecognizer", e)
        }
    }

    fun setLanguage(language: DictationLanguage) {
        currentLanguage = language
        if (isContinuousActive) {
            // Restart with new language
            stopListening()
            startListening(language)
        }
    }

    fun startListening(language: DictationLanguage = currentLanguage) {
        currentLanguage = language
        isContinuousActive = true
        _micState.value = MicState.LISTENING
        _livePartialText.value = ""

        try {
            if (speechRecognizer == null) {
                initSpeechRecognizer()
            }

            val recognizer = speechRecognizer
            if (recognizer != null && SpeechRecognizer.isRecognitionAvailable(context)) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                    val localeStr = when (language) {
                        DictationLanguage.BANGLA -> "bn-BD"
                        DictationLanguage.BANGLISH -> "bn-BD"
                        DictationLanguage.ARABIC -> "ar-SA"
                    }
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeStr)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeStr)
                }
                recognizer.startListening(intent)
            } else {
                startSimulatedWaveformAndRecognition()
            }
        } catch (e: Exception) {
            Log.e(TAG, "startListening error, falling back to simulated mode", e)
            startSimulatedWaveformAndRecognition()
        }
    }

    fun stopListening() {
        isContinuousActive = false
        simulationJob?.cancel()
        simulationJob = null
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognizer", e)
        }
        _micState.value = MicState.IDLE
        _audioRms.value = 0f
    }

    fun toggleListening() {
        if (_micState.value == MicState.LISTENING) {
            stopListening()
        } else {
            startListening(currentLanguage)
        }
    }

    fun setMuted(muted: Boolean) {
        if (muted) {
            _micState.value = MicState.MUTED
            speechRecognizer?.stopListening()
            simulationJob?.cancel()
        } else {
            if (isContinuousActive) {
                startListening(currentLanguage)
            } else {
                _micState.value = MicState.IDLE
            }
        }
    }

    private fun handleUtterance(rawText: String) {
        if (rawText.isBlank()) return
        _micState.value = MicState.PROCESSING

        val formattedText = when (currentLanguage) {
            DictationLanguage.BANGLISH -> {
                BanglishTransliteration.toBanglish(rawText)
            }
            DictationLanguage.BANGLA -> {
                // Ensure proper Bengali punctuation
                var bn = rawText.trim()
                if (!bn.endsWith("।") && !bn.endsWith("?") && !bn.endsWith("!")) {
                    bn = "$bn।"
                }
                bn
            }
            DictationLanguage.ARABIC -> {
                var ar = rawText.trim()
                if (!ar.endsWith(".") && !ar.endsWith("؟") && !ar.endsWith("!")) {
                    ar = "$ar."
                }
                ar
            }
        }

        _lastFinalText.value = formattedText
        _livePartialText.value = ""
        onFinalResult?.invoke(formattedText, currentLanguage)

        // Continuous dictation: keep listening indefinitely
        coroutineScope.launch {
            delay(250)
            if (isContinuousActive) {
                _micState.value = MicState.LISTENING
                restartNativeRecognizerIfNeeded()
            } else {
                _micState.value = MicState.IDLE
            }
        }
    }

    private fun restartNativeRecognizerIfNeeded() {
        if (!isContinuousActive) return
        try {
            val recognizer = speechRecognizer
            if (recognizer != null && SpeechRecognizer.isRecognitionAvailable(context)) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    val localeStr = when (currentLanguage) {
                        DictationLanguage.BANGLA, DictationLanguage.BANGLISH -> "bn-BD"
                        DictationLanguage.ARABIC -> "ar-SA"
                    }
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeStr)
                }
                recognizer.startListening(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restarting recognizer", e)
        }
    }

    // Interactive speech simulation for emulator / browser environments
    fun injectSimulatedUtterance(sampleText: String) {
        coroutineScope.launch {
            _micState.value = MicState.LISTENING
            _livePartialText.value = sampleText.take(sampleText.length / 2) + "..."
            _audioRms.value = 8.5f
            delay(350)
            _livePartialText.value = sampleText
            delay(200)
            handleUtterance(sampleText)
        }
    }

    private fun startSimulatedWaveformAndRecognition() {
        simulationJob?.cancel()
        simulationJob = coroutineScope.launch(Dispatchers.Default) {
            // Animate waveform audio RMS values smoothly while listening
            var phase = 0f
            while (isContinuousActive) {
                phase += 0.2f
                val noise = (Math.sin(phase.toDouble()) * 4.0 + 4.5).toFloat()
                _audioRms.value = noise.coerceIn(0.5f, 10f)
                delay(60)
            }
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _micState.value = MicState.LISTENING
        }

        override fun onBeginningOfSpeech() {
            _micState.value = MicState.LISTENING
        }

        override fun onRmsChanged(rmsdB: Float) {
            _audioRms.value = rmsdB.coerceAtLeast(0f)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _micState.value = MicState.PROCESSING
        }

        override fun onError(error: Int) {
            Log.w(TAG, "SpeechRecognizer error code: $error")
            if (isContinuousActive) {
                coroutineScope.launch {
                    delay(300)
                    restartNativeRecognizerIfNeeded()
                }
            } else {
                _micState.value = MicState.IDLE
                _audioRms.value = 0f
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull() ?: ""
            handleUtterance(recognizedText)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull() ?: ""
            if (partial.isNotBlank()) {
                val formatted = when (currentLanguage) {
                    DictationLanguage.BANGLISH -> BanglishTransliteration.toBanglish(partial)
                    else -> partial
                }
                _livePartialText.value = formatted
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun destroy() {
        simulationJob?.cancel()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying recognizer", e)
        }
    }
}
