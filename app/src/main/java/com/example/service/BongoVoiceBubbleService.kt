package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.R
import com.example.model.DictationLanguage
import com.example.speech.BanglishTransliteration
import java.util.Locale

/**
 * Universal Keyboard Voice Typing Floating Bubble Service
 *
 * Detects keyboard opening/focus in ANY app (WhatsApp, IMO, Messenger, Telegram, Chrome, etc.)
 * Displays a Messenger-like floating bubble. Clicking the bubble starts voice typing,
 * and directly inserts the recognized Bangla/Banglish/Arabic text into the active chatbox!
 */
class BongoVoiceBubbleService : AccessibilityService() {

    private val TAG = "BongoBubbleService"

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var isBubbleShowing = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isContinuousSessionActive = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentLanguage: DictationLanguage = DictationLanguage.BANGLA

    // Target input node where the user is typing
    private var currentInputNode: AccessibilityNodeInfo? = null

    // Bubble touch/drag state
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var layoutParams: WindowManager.LayoutParams? = null

    // UI elements inside bubble
    private var bubbleIcon: ImageView? = null
    private var bubbleStatusText: TextView? = null
    private var bubbleLangBadge: TextView? = null
    private var bubbleCloseBtn: ImageView? = null
    private var bubbleMicBg: FrameLayout? = null

    companion object {
        var isServiceRunning = false
            private set
        var instance: BongoVoiceBubbleService? = null
            private set

        fun isAccessibilityEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val expectedServiceName = "${context.packageName}/${BongoVoiceBubbleService::class.java.canonicalName}"
            return enabledServices.contains(expectedServiceName) ||
                   enabledServices.contains(BongoVoiceBubbleService::class.java.simpleName)
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }

        fun openOverlaySettings(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        instance = this
        Log.d(TAG, "BongoVoiceBubbleService connected!")

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 80
        }
        serviceInfo = info

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        initSpeechRecognizer()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val source = event.source
                if (source != null && isInputField(source)) {
                    currentInputNode = source
                    Log.d(TAG, "Input field focused in app: ${event.packageName}")
                    showBubbleOverlay()
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // When keyboard appears or window changes, check active focused element
                findFocusedInputNode()?.let { node ->
                    currentInputNode = node
                    showBubbleOverlay()
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    private fun isInputField(node: AccessibilityNodeInfo): Boolean {
        val isEditable = node.isEditable
        val className = node.className?.toString() ?: ""
        val isEditText = className.contains("EditText", ignoreCase = true) ||
                className.contains("TextView", ignoreCase = true) && isEditable ||
                className.contains("InputConnection", ignoreCase = true)
        return isEditable || isEditText
    }

    private fun findFocusedInputNode(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
    }

    private fun showBubbleOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted")
            return
        }

        if (isBubbleShowing && bubbleView != null) {
            bubbleView?.visibility = View.VISIBLE
            return
        }

        createBubbleView()
    }

    private fun createBubbleView() {
        try {
            val inflater = LayoutInflater.from(this)
            bubbleView = inflater.inflate(R.layout.layout_floating_voice_bubble, null)

            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 30
                y = 350
            }

            // Bind UI elements
            bubbleIcon = bubbleView?.findViewById(R.id.bubble_mic_icon)
            bubbleStatusText = bubbleView?.findViewById(R.id.bubble_status_text)
            bubbleLangBadge = bubbleView?.findViewById(R.id.bubble_lang_badge)
            bubbleCloseBtn = bubbleView?.findViewById(R.id.bubble_close_btn)
            bubbleMicBg = bubbleView?.findViewById(R.id.bubble_mic_bg)

            updateLanguageBadge()

            // Language switch on badge click
            bubbleLangBadge?.setOnClickListener {
                cycleLanguage()
            }

            // Close bubble button
            bubbleCloseBtn?.setOnClickListener {
                hideBubbleOverlay()
            }

            // Common touch listener to handle smooth drag and click on the floating bubble
            val bubbleDragTouchListener = View.OnTouchListener { _, motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams?.x ?: 0
                        initialY = layoutParams?.y ?: 0
                        initialTouchX = motionEvent.rawX
                        initialTouchY = motionEvent.rawY
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (motionEvent.rawX - initialTouchX).toInt()
                        val dy = (motionEvent.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                            isDragging = true
                            layoutParams?.x = initialX + dx
                            layoutParams?.y = initialY + dy
                            windowManager?.updateViewLayout(bubbleView, layoutParams)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            toggleVoiceTyping()
                        }
                        isDragging = false
                        true
                    }
                    else -> false
                }
            }

            bubbleMicBg?.setOnTouchListener(bubbleDragTouchListener)
            bubbleView?.setOnTouchListener(bubbleDragTouchListener)

            windowManager?.addView(bubbleView, layoutParams)
            isBubbleShowing = true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding floating bubble to window manager: ${e.message}")
        }
    }

    private fun hideBubbleOverlay() {
        stopVoiceTyping()
        if (isBubbleShowing && bubbleView != null) {
            try {
                windowManager?.removeView(bubbleView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing bubble: ${e.message}")
            }
            bubbleView = null
            isBubbleShowing = false
        }
    }

    private fun cycleLanguage() {
        currentLanguage = when (currentLanguage) {
            DictationLanguage.BANGLA -> DictationLanguage.BANGLISH
            DictationLanguage.BANGLISH -> DictationLanguage.ARABIC
            DictationLanguage.ARABIC -> DictationLanguage.BANGLA
        }
        updateLanguageBadge()
        Toast.makeText(this, "Language: ${currentLanguage.displayName}", Toast.LENGTH_SHORT).show()
    }

    private fun updateLanguageBadge() {
        bubbleLangBadge?.text = when (currentLanguage) {
            DictationLanguage.BANGLA -> "বাংলা"
            DictationLanguage.BANGLISH -> "EN"
            DictationLanguage.ARABIC -> "عربي"
        }
    }

    private fun initSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                            bubbleStatusText?.text = "বলুন..."
                            bubbleStatusText?.visibility = View.VISIBLE
                            bubbleMicBg?.setBackgroundResource(R.drawable.bg_bubble_active)
                        }

                        override fun onBeginningOfSpeech() {
                            bubbleStatusText?.text = "শুনছি..."
                        }

                        override fun onRmsChanged(rmsdB: Float) {}

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            bubbleStatusText?.text = "টাইপ হচ্ছে..."
                        }

                        override fun onError(error: Int) {
                            Log.w(TAG, "Speech recognition error code: $error")
                            isListening = false

                            // If user hasn't explicitly stopped continuous dictation and it was a timeout/no-match or pause,
                            // immediately keep listening so they don't have to press the bubble again!
                            if (isContinuousSessionActive) {
                                bubbleStatusText?.text = "শুনছি..."
                                mainHandler.postDelayed({
                                    if (isContinuousSessionActive && !isListening) {
                                        restartListeningSession()
                                    }
                                }, 300)
                            } else {
                                bubbleStatusText?.text = "আবার বলুন"
                                bubbleMicBg?.setBackgroundResource(R.drawable.bg_bubble_idle)
                                bubbleStatusText?.postDelayed({
                                    if (!isListening && !isContinuousSessionActive) bubbleStatusText?.visibility = View.GONE
                                }, 1500)
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            isListening = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val rawText = matches?.firstOrNull() ?: ""
                            if (rawText.isNotBlank()) {
                                val formattedText = formatSpokenText(rawText, currentLanguage)
                                bubbleStatusText?.text = formattedText
                                injectTextIntoTargetApp(formattedText)
                            }

                            // If continuous dictation is enabled, seamlessly restart listening so user can keep talking!
                            if (isContinuousSessionActive) {
                                mainHandler.postDelayed({
                                    if (isContinuousSessionActive && !isListening) {
                                        bubbleStatusText?.text = "বলুন..."
                                        restartListeningSession()
                                    }
                                }, 350)
                            } else {
                                bubbleMicBg?.setBackgroundResource(R.drawable.bg_bubble_idle)
                                bubbleStatusText?.postDelayed({
                                    if (!isListening && !isContinuousSessionActive) bubbleStatusText?.visibility = View.GONE
                                }, 2000)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull() ?: ""
                            if (partial.isNotBlank()) {
                                bubbleStatusText?.text = partial
                                bubbleStatusText?.visibility = View.VISIBLE
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SpeechRecognizer: ${e.message}")
        }
    }

    private fun restartListeningSession() {
        if (!isContinuousSessionActive) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage.localeCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguage.localeCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            bubbleMicBg?.setBackgroundResource(R.drawable.bg_bubble_active)
        } catch (e: Exception) {
            Log.e(TAG, "Error restarting voice recognition: ${e.message}")
        }
    }

    private fun toggleVoiceTyping() {
        if (isContinuousSessionActive || isListening) {
            stopVoiceTyping()
        } else {
            startVoiceTyping()
        }
    }

    private fun startVoiceTyping() {
        isContinuousSessionActive = true
        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage.localeCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguage.localeCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            bubbleStatusText?.text = "শুরু হচ্ছে..."
            bubbleStatusText?.visibility = View.VISIBLE
            bubbleMicBg?.setBackgroundResource(R.drawable.bg_bubble_active)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice recognition: ${e.message}")
            isContinuousSessionActive = false
            Toast.makeText(this, "Microphone unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopVoiceTyping() {
        isContinuousSessionActive = false
        mainHandler.removeCallbacksAndMessages(null)
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognizer: ${e.message}")
            }
            isListening = false
        }
        bubbleMicBg?.setBackgroundResource(R.drawable.bg_bubble_idle)
        bubbleStatusText?.visibility = View.GONE
    }

    private fun formatSpokenText(raw: String, language: DictationLanguage): String {
        return when (language) {
            DictationLanguage.BANGLA -> {
                val clean = raw.trim()
                if (!clean.endsWith("।") && !clean.endsWith("?") && !clean.endsWith("!")) {
                    "$clean।"
                } else clean
            }
            DictationLanguage.BANGLISH -> BanglishTransliteration.toBanglish(raw)
            DictationLanguage.ARABIC -> {
                val clean = raw.trim()
                if (!clean.endsWith(".") && !clean.endsWith("؟") && !clean.endsWith("!")) {
                    "$clean."
                } else clean
            }
        }
    }

    /**
     * Injects transcribed text directly into the chat input box using Accessibility APIs
     * Works across WhatsApp, IMO, Telegram, Messenger, Chrome, etc.!
     */
    private fun injectTextIntoTargetApp(text: String) {
        // Re-check target input node
        val target = currentInputNode ?: findFocusedInputNode()

        if (target != null && isInputField(target)) {
            try {
                // Method 1: Append text or set directly via ACTION_SET_TEXT
                val existingText = target.text?.toString() ?: ""
                val separator = if (existingText.isNotEmpty() && !existingText.endsWith(" ") && !existingText.endsWith("\n")) " " else ""
                val fullText = existingText + separator + text

                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, fullText)
                }
                val success = target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

                if (!success) {
                    // Fallback to Clipboard Paste
                    pasteViaClipboard(target, text)
                } else {
                    Toast.makeText(this, "✓ টাইপ সম্পন্ন হয়েছে", Toast.LENGTH_SHORT).show()
                }
                return
            } catch (e: Exception) {
                Log.e(TAG, "Error injecting via ACTION_SET_TEXT: ${e.message}")
                pasteViaClipboard(target, text)
                return
            }
        }

        // Method 2: Global fallback - copy to clipboard and notify
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Bongo Type", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "ক্লিপবোর্ডে কপি করা হয়েছে! পেস্ট করুন", Toast.LENGTH_SHORT).show()
    }

    private fun pasteViaClipboard(node: AccessibilityNodeInfo, text: String) {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Bongo Type", text)
            clipboard.setPrimaryClip(clip)

            // Try triggering paste action directly on the active input node
            node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            Toast.makeText(this, "✓ পেস্ট করা হয়েছে", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Paste fallback failed: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        instance = null
        hideBubbleOverlay()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
