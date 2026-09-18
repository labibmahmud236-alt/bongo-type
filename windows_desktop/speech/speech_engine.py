"""
Bongo Type - Speech Recognition Engine
Manages continuous background recognition loop with support for:
- Google Speech Recognition API
- OpenAI Whisper local/API
- Gemini Speech API
- Seamless language switching (Bangla, Banglish, Arabic)
"""

import io
import queue
import threading
import time
from typing import Callable, Optional

try:
    import speech_recognition as sr
except ImportError:
    sr = None

from speech.banglish_transliteration import to_banglish


class SpeechEngine:
    """Orchestrates speech recognition services with continuous dictation."""

    LANGUAGE_CODES = {
        "Bangla": "bn-BD",
        "Banglish": "bn-BD",
        "Arabic": "ar-SA"
    }

    def __init__(self, engine_type: str = "google", language: str = "Bangla"):
        self.engine_type = engine_type
        self.language = language
        self.is_running = False
        self._recognizer = sr.Recognizer() if sr is not None else None
        if self._recognizer:
            self._recognizer.energy_threshold = 300
            self._recognizer.dynamic_energy_threshold = True

        self._audio_queue = queue.Queue()
        self._worker_thread = None

        # Callbacks
        self.on_state_change: Optional[Callable[[str], None]] = None  # idle, listening, processing, muted
        self.on_partial_text: Optional[Callable[[str], None]] = None
        self.on_final_text: Optional[Callable[[str, str], None]] = None  # text, language

    def set_language(self, language: str):
        """Switch language instantly without restart."""
        self.language = language
        print(f"[SpeechEngine] Language set to: {language}")

    def set_engine_type(self, engine_type: str):
        self.engine_type = engine_type

    def start_continuous(self):
        """Begin continuous listening loop in a background thread."""
        if self.is_running:
            return

        self.is_running = True
        if self.on_state_change:
            self.on_state_change("listening")

        self._worker_thread = threading.Thread(target=self._continuous_loop, daemon=True)
        self._worker_thread.start()

    def stop(self):
        """Stop listening."""
        self.is_running = False
        if self.on_state_change:
            self.on_state_change("idle")

    def _continuous_loop(self):
        """Continuous background dictation loop."""
        if sr is None:
            print("[SpeechEngine] speech_recognition library not installed.")
            return

        try:
            with sr.Microphone() as source:
                self._recognizer.adjust_for_ambient_noise(source, duration=0.6)

                while self.is_running:
                    try:
                        if self.on_state_change:
                            self.on_state_change("listening")

                        # Listen for an utterance with non-blocking timeout
                        audio_data = self._recognizer.listen(source, timeout=3.0, phrase_time_limit=15.0)

                        if not self.is_running:
                            break

                        if self.on_state_change:
                            self.on_state_change("processing")

                        self._process_audio(audio_data)

                    except sr.WaitTimeoutError:
                        # Continue waiting for speech (continuous mode)
                        continue
                    except Exception as e:
                        print(f"[SpeechEngine] Loop iteration error: {e}")
                        time.sleep(0.2)

        except Exception as e:
            print(f"[SpeechEngine] Microphone initialization failed: {e}")
            self.is_running = False
            if self.on_state_change:
                self.on_state_change("idle")

    def _process_audio(self, audio_data):
        """Transcribes audio using the selected engine and language rules."""
        raw_text = ""
        lang_code = self.LANGUAGE_CODES.get(self.language, "bn-BD")

        try:
            if self.engine_type == "whisper":
                # OpenAI Whisper local or API fallback
                try:
                    raw_text = self._recognizer.recognize_whisper(audio_data, language=lang_code[:2])
                except Exception:
                    raw_text = self._recognizer.recognize_google(audio_data, language=lang_code)
            else:
                # Primary: Google Speech Recognition API
                raw_text = self._recognizer.recognize_google(audio_data, language=lang_code)

        except sr.UnknownValueError:
            return
        except sr.RequestError as e:
            print(f"[SpeechEngine] Speech service request failed: {e}")
            return

        if not raw_text:
            return

        # Format according to user rules
        final_text = self._format_output(raw_text)

        if self.on_final_text:
            self.on_final_text(final_text, self.language)

    def _format_output(self, raw_text: str) -> str:
        """Applies Bangla, Banglish, or Arabic formatting rules."""
        raw_text = raw_text.strip()
        if not raw_text:
            return ""

        if self.language == "Banglish":
            # Feature 7: Keep Latin script, e.g. "Ami tomar sathe kotha bolchi"
            return to_banglish(raw_text)

        elif self.language == "Bangla":
            # Feature 8: Proper Bengali Unicode with Bengali danda if not punctuated
            if not raw_text.endswith("।") and not raw_text.endswith("?") and not raw_text.endswith("!"):
                raw_text += "।"
            return raw_text

        elif self.language == "Arabic":
            # Feature 9: Proper Arabic Unicode
            if not raw_text.endswith(".") and not raw_text.endswith("؟") and not raw_text.endswith("!"):
                raw_text += "."
            return raw_text

        return raw_text
