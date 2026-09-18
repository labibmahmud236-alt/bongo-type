"""
BONGO TYPE — WINDOWS VOICE TYPING ASSISTANT
Main Application Entrypoint
"""

import sys
import os
from pathlib import Path

# Ensure local modules are found
sys.path.insert(0, str(Path(__file__).parent))

from PyQt6.QtCore import QObject, pyqtSignal, QTimer
from PyQt6.QtWidgets import QApplication

from audio.recorder import AudioRecorder
from hotkeys.global_hotkey import GlobalHotkeyManager
from settings.config_manager import ConfigManager
from speech.speech_engine import SpeechEngine
from tray.tray_icon import BongoTrayIcon
from typing.injector import WindowsTextInjector
from ui.floating_widget import FloatingVoiceWidget
from ui.settings_window import SettingsWindow


class BongoTypeApp(QObject):
    """Main application controller coordinating all Bongo Type services."""

    def __init__(self, qapp: QApplication):
        super().__init__()
        self.qapp = qapp

        # 1. Configuration
        self.config = ConfigManager()

        # 2. Text Injector
        self.injector = WindowsTextInjector(typing_delay_ms=self.config.get("typing_speed_ms", 15))

        # 3. Speech & Audio Engines
        initial_lang = self.config.get("default_language", "Bangla")
        self.speech_engine = SpeechEngine(
            engine_type=self.config.get("speech_engine", "google"),
            language=initial_lang
        )
        self.recorder = AudioRecorder(device_index=self.config.get("audio_device_index"))

        # 4. UI Components
        self.floating_widget = FloatingVoiceWidget(
            current_language=initial_lang,
            always_on_top=self.config.get("always_on_top", True)
        )
        self.settings_window = SettingsWindow(self.config)
        self.tray = BongoTrayIcon()

        # 5. Global Hotkey
        self.hotkey_manager = GlobalHotkeyManager(
            hotkey=self.config.get("shortcut", "ctrl+space"),
            callback=self._on_hotkey_pressed
        )

        self._connect_signals()
        self._position_widget()

        # Start Services
        self.tray.show()
        self.floating_widget.show()
        self.hotkey_manager.start()

    def _connect_signals(self):
        # Widget clicks
        self.floating_widget.mic_clicked.connect(self._toggle_dictation)
        self.floating_widget.language_changed.connect(self._on_language_changed)

        # Tray actions
        self.tray.open_requested.connect(self._show_and_focus)
        self.tray.pause_toggled.connect(self._on_pause_toggled)
        self.tray.settings_requested.connect(self._open_settings)
        self.tray.exit_requested.connect(self._exit_app)

        # Settings
        self.settings_window.settings_applied.connect(self._on_settings_applied)

        # Speech callbacks
        self.speech_engine.on_state_change = self._on_speech_state_change
        self.speech_engine.on_final_text = self._on_final_text_recognized

        # Audio RMS level callback to update waveform
        self.recorder.on_rms_level = lambda rms: self.floating_widget.set_audio_rms(rms)

    def _position_widget(self):
        """Positions the floating widget horizontally centered at the top, like Windows Win+H."""
        screen = self.qapp.primaryScreen()
        if screen:
            screen_geom = screen.availableGeometry()
            x = (screen_geom.width() - self.floating_widget.width()) // 2
            y = screen_geom.top() + 60
            self.floating_widget.move(x, y)

    def _toggle_dictation(self):
        """Starts or stops continuous dictation."""
        if self.speech_engine.is_running:
            self.speech_engine.stop()
            self.recorder.stop()
            self.floating_widget.set_state("idle")
        else:
            self.recorder.start()
            self.speech_engine.start_continuous()
            self.floating_widget.set_state("listening")

    def _on_hotkey_pressed(self):
        """Global shortcut handler: focuses mic and begins continuous listening."""
        QTimer.singleShot(0, self._handle_hotkey_action)

    def _handle_hotkey_action(self):
        self.floating_widget.show()
        self.floating_widget.raise_()
        self._toggle_dictation()

    def _on_language_changed(self, language: str):
        self.speech_engine.set_language(language)
        self.config.set("default_language", language)

    def _on_speech_state_change(self, state: str):
        QTimer.singleShot(0, lambda: self.floating_widget.set_state(state))

    def _on_final_text_recognized(self, text: str, language: str):
        """Invoked when speech recognition completes a phrase."""
        if not text:
            return

        print(f"[Bongo Type] Recognized ({language}): {text}")

        # Universal Text Injection into active window
        if self.config.get("auto_inject", True):
            self.injector.inject_text(text + " ")

    def _on_pause_toggled(self, is_paused: bool):
        if is_paused:
            if self.speech_engine.is_running:
                self.speech_engine.stop()
                self.recorder.stop()
            self.floating_widget.set_state("muted")
        else:
            self.floating_widget.set_state("idle")

    def _open_settings(self):
        self.settings_window.show()
        self.settings_window.raise_()

    def _on_settings_applied(self):
        # Update hotkey
        new_hotkey = self.config.get("shortcut", "ctrl+space")
        self.hotkey_manager.update_hotkey(new_hotkey)

        # Update injector speed
        self.injector.typing_delay_ms = self.config.get("typing_speed_ms", 15)

        # Update speech engine
        self.speech_engine.set_engine_type(self.config.get("speech_engine", "google"))

    def _show_and_focus(self):
        self.floating_widget.show()
        self.floating_widget.raise_()
        self.floating_widget.activateWindow()

    def _exit_app(self):
        self.speech_engine.stop()
        self.recorder.stop()
        self.hotkey_manager.stop()
        self.qapp.quit()


def main():
    app = QApplication(sys.argv)
    app.setQuitOnLastWindowClosed(False)
    app.setApplicationName("Bongo Type")

    bongo = BongoTypeApp(app)
    sys.exit(app.exec())


if __name__ == "__main__":
    main()
