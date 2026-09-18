"""
Bongo Type - Settings Window
Configures global shortcut key, language defaults, speech engine,
typing speed, startup, and device options.
"""

from PyQt6.QtCore import Qt, pyqtSignal
from PyQt6.QtGui import QFont, QIcon
from PyQt6.QtWidgets import (
    QCheckBox, QComboBox, QDialog, QFormLayout, QGroupBox, QHBoxLayout,
    QLabel, QPushButton, QSlider, QVBoxLayout
)

from settings.config_manager import ConfigManager


class SettingsWindow(QDialog):
    """Configuration dialog for Bongo Type."""

    settings_applied = pyqtSignal()

    def __init__(self, config_manager: ConfigManager, parent=None):
        super().__init__(parent)
        self.config = config_manager
        self.setWindowTitle("Bongo Type Settings")
        self.setFixedSize(420, 520)
        self.setStyleSheet("""
            QDialog {
                background-color: #0F172A;
                color: #F8FAFC;
                font-family: 'Segoe UI', sans-serif;
            }
            QLabel {
                color: #E2E8F0;
                font-size: 13px;
            }
            QGroupBox {
                border: 1px solid #334155;
                border-radius: 8px;
                margin-top: 14px;
                padding-top: 12px;
                font-weight: bold;
                color: #38BDF8;
            }
            QGroupBox::title {
                subcontrol-origin: margin;
                left: 10px;
                padding: 0 4px;
            }
            QComboBox {
                background-color: #1E293B;
                color: #FFFFFF;
                border: 1px solid #475569;
                border-radius: 6px;
                padding: 5px;
            }
            QCheckBox {
                color: #E2E8F0;
                spacing: 8px;
            }
            QPushButton {
                background-color: #0284C7;
                color: #FFFFFF;
                border: none;
                border-radius: 6px;
                padding: 8px 16px;
                font-weight: bold;
            }
            QPushButton:hover {
                background-color: #0369A1;
            }
        """)

        self._init_ui()

    def _init_ui(self):
        main_layout = QVBoxLayout(self)
        main_layout.setSpacing(16)

        # 1. Hotkey & Voice Recognition
        voice_box = QGroupBox("Voice Recognition & Hotkeys", self)
        voice_layout = QFormLayout(voice_box)

        self.shortcut_combo = QComboBox(self)
        self.shortcut_combo.addItems(["ctrl+space", "alt+space", "windows+h"])
        self.shortcut_combo.setCurrentText(self.config.get("shortcut", "ctrl+space"))
        voice_layout.addRow("Global Shortcut:", self.shortcut_combo)

        self.lang_default_combo = QComboBox(self)
        self.lang_default_combo.addItems(["Bangla", "Banglish", "Arabic"])
        self.lang_default_combo.setCurrentText(self.config.get("default_language", "Bangla"))
        voice_layout.addRow("Default Language:", self.lang_default_combo)

        self.engine_combo = QComboBox(self)
        self.engine_combo.addItem("Google Speech API (Fast, Free)", "google")
        self.engine_combo.addItem("OpenAI Whisper (Multilingual)", "whisper")
        self.engine_combo.addItem("Gemini Speech API (Smart Punctuation)", "gemini")
        current_eng = self.config.get("speech_engine", "google")
        for i in range(self.engine_combo.count()):
            if self.engine_combo.itemData(i) == current_eng:
                self.engine_combo.setCurrentIndex(i)
                break
        voice_layout.addRow("Speech Engine:", self.engine_combo)

        main_layout.addWidget(voice_box)

        # 2. Text Injection
        typing_box = QGroupBox("Text Injection", self)
        typing_layout = QFormLayout(typing_box)

        self.speed_slider = QSlider(Qt.Orientation.Horizontal, self)
        self.speed_slider.setRange(0, 50)
        self.speed_slider.setValue(self.config.get("typing_speed_ms", 15))
        self.speed_label = QLabel(f"{self.speed_slider.value()} ms")
        self.speed_slider.valueChanged.connect(lambda v: self.speed_label.setText(f"{v} ms"))

        slider_row = QHBoxLayout()
        slider_row.addWidget(self.speed_slider)
        slider_row.addWidget(self.speed_label)
        typing_layout.addRow("Typing Speed:", slider_row)

        self.auto_inject_cb = QCheckBox("Auto insert into active window without pasting", self)
        self.auto_inject_cb.setChecked(self.config.get("auto_inject", True))
        typing_layout.addRow(self.auto_inject_cb)

        main_layout.addWidget(typing_box)

        # 3. System Options
        system_box = QGroupBox("System & Startup", self)
        system_layout = QVBoxLayout(system_box)

        self.always_on_top_cb = QCheckBox("Always On Top (Stay above all windows)", self)
        self.always_on_top_cb.setChecked(self.config.get("always_on_top", True))
        system_layout.addWidget(self.always_on_top_cb)

        self.startup_cb = QCheckBox("Start with Windows automatically", self)
        self.startup_cb.setChecked(self.config.get("start_with_windows", False))
        system_layout.addWidget(self.startup_cb)

        main_layout.addWidget(system_box)

        # Buttons
        btn_layout = QHBoxLayout()
        btn_layout.addStretch()

        cancel_btn = QPushButton("Cancel", self)
        cancel_btn.setStyleSheet("background-color: #334155;")
        cancel_btn.clicked.connect(self.reject)
        btn_layout.addWidget(cancel_btn)

        save_btn = QPushButton("Save Settings", self)
        save_btn.clicked.connect(self._save_settings)
        btn_layout.addWidget(save_btn)

        main_layout.addLayout(btn_layout)

    def _save_settings(self):
        self.config.set("shortcut", self.shortcut_combo.currentText())
        self.config.set("default_language", self.lang_default_combo.currentText())
        self.config.set("speech_engine", self.engine_combo.currentData())
        self.config.set("typing_speed_ms", self.speed_slider.value())
        self.config.set("auto_inject", self.auto_inject_cb.isChecked())
        self.config.set("always_on_top", self.always_on_top_cb.isChecked())
        self.config.set("start_with_windows", self.startup_cb.isChecked())
        self.config.save()
        self.settings_applied.emit()
        self.accept()
