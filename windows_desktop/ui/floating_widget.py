"""
Bongo Type - Windows 11 Floating Assistant Widget
Ultra-small, frameless, draggable, always-on-top glass pill interface.
Contains only the Microphone button, Waveform visualizer, and Language selector.
"""

from PyQt6.QtCore import Qt, QPoint, pyqtSignal
from PyQt6.QtGui import QColor, QFont, QIcon, QPainter, QLinearGradient, QBrush, QPen
from PyQt6.QtWidgets import (
    QWidget, QPushButton, QComboBox, QHBoxLayout, QLabel, QGraphicsDropShadowEffect
)

from animations.waveform import WaveformWidget


class FloatingVoiceWidget(QWidget):
    """Windows 11 inspired floating voice typing pill widget."""

    mic_clicked = pyqtSignal()
    language_changed = pyqtSignal(str)
    settings_requested = pyqtSignal()

    def __init__(self, current_language: str = "Bangla", always_on_top: bool = True):
        super().__init__()
        self.current_state = "idle"  # idle, listening, processing, muted
        self.drag_position = QPoint()

        # Frameless, Transparent, Floating Window setup
        flags = Qt.WindowType.FramelessWindowHint | Qt.WindowType.SubWindow
        if always_on_top:
            flags |= Qt.WindowType.WindowStaysOnTopHint
        self.setWindowFlags(flags)
        self.setAttribute(Qt.WidgetAttribute.WA_TranslucentBackground)

        self._init_ui(current_language)

    def _init_ui(self, current_language: str):
        layout = QHBoxLayout(self)
        layout.setContentsMargins(10, 6, 10, 6)
        layout.setSpacing(8)

        # 1. Microphone Button
        self.mic_btn = QPushButton(self)
        self.mic_btn.setFixedSize(44, 44)
        self.mic_btn.setCursor(Qt.CursorShape.PointingHandCursor)
        self.mic_btn.clicked.connect(lambda: self.mic_clicked.emit())
        self._update_mic_style()

        # 2. Audio Waveform
        self.waveform = WaveformWidget(self, bar_count=7)

        # 3. Language Selector Dropdown
        self.lang_combo = QComboBox(self)
        self.lang_combo.addItem("বাংলা", "Bangla")
        self.lang_combo.addItem("Banglish", "Banglish")
        self.lang_combo.addItem("العربية", "Arabic")

        # Set initial selection
        for i in range(self.lang_combo.count()):
            if self.lang_combo.itemData(i) == current_language:
                self.lang_combo.setCurrentIndex(i)
                break

        self.lang_combo.currentIndexChanged.connect(self._on_language_change)
        self._style_combobox()

        layout.addWidget(self.mic_btn)
        layout.addWidget(self.waveform)
        layout.addWidget(self.lang_combo)

        self.setFixedSize(260, 56)

        # Window Drop Shadow
        shadow = QGraphicsDropShadowEffect(self)
        shadow.setBlurRadius(24)
        shadow.setColor(QColor(0, 0, 0, 160))
        shadow.setOffset(0, 6)
        self.setGraphicsEffect(shadow)

    def _style_combobox(self):
        self.lang_combo.setStyleSheet("""
            QComboBox {
                background-color: #1E293B;
                color: #FFFFFF;
                border: 1px solid #334155;
                border-radius: 12px;
                padding: 4px 10px;
                font-family: 'Segoe UI', sans-serif;
                font-size: 12px;
                font-weight: 600;
                min-height: 28px;
            }
            QComboBox:hover {
                background-color: #2D3A4F;
                border-color: #00E5FF;
            }
            QComboBox::drop-down {
                border: none;
                width: 18px;
            }
            QComboBox QAbstractItemView {
                background-color: #1E293B;
                color: #FFFFFF;
                selection-background-color: #0284C7;
                selection-color: #FFFFFF;
                border: 1px solid #475569;
                border-radius: 8px;
                padding: 4px;
            }
        """)

    def _update_mic_style(self):
        if self.current_state == "listening":
            bg = "qlineargradient(x1:0, y1:0, x2:1, y2:1, stop:0 #00E5FF, stop:1 #0284C7)"
            border = "2px solid #38BDF8"
            icon_text = "🎙"
        elif self.current_state == "processing":
            bg = "qlineargradient(x1:0, y1:0, x2:1, y2:1, stop:0 #F59E0B, stop:1 #D97706)"
            border = "2px solid #FCD34D"
            icon_text = "⏳"
        elif self.current_state == "muted":
            bg = "qlineargradient(x1:0, y1:0, x2:1, y2:1, stop:0 #EF4444, stop:1 #B91C1C)"
            border = "2px solid #FCA5A5"
            icon_text = "🔇"
        else:  # idle
            bg = "qlineargradient(x1:0, y1:0, x2:1, y2:1, stop:0 #334155, stop:1 #1E293B)"
            border = "1px solid #475569"
            icon_text = "🎙"

        self.mic_btn.setText(icon_text)
        self.mic_btn.setFont(QFont("Segoe UI Emoji", 16))
        self.mic_btn.setStyleSheet(f"""
            QPushButton {{
                background: {bg};
                color: #FFFFFF;
                border: {border};
                border-radius: 22px;
            }}
            QPushButton:hover {{
                border-color: #00E5FF;
            }}
        """)

    def set_state(self, state: str):
        self.current_state = state
        self._update_mic_style()
        self.waveform.set_active(state == "listening")

    def set_audio_rms(self, rms: float):
        self.waveform.set_audio_rms(rms)

    def _on_language_change(self, index: int):
        lang = self.lang_combo.itemData(index)
        self.language_changed.emit(lang)

    # Acrylic Glass Background Drawing
    def paintEvent(self, event):
        painter = QPainter(self)
        painter.setRenderHint(QPainter.RenderHint.Antialiasing)

        # Windows 11 Dark Mica / Glass Pill
        rect = self.rect()
        gradient = QLinearGradient(0, 0, 0, rect.height())
        gradient.setColorAt(0.0, QColor(22, 28, 36, 235))
        gradient.setColorAt(1.0, QColor(15, 23, 42, 245))

        painter.setBrush(QBrush(gradient))
        painter.setPen(QPen(QColor(71, 85, 105, 160), 1))
        painter.drawRoundedRect(rect.adjusted(1, 1, -1, -1), 26, 26)

    # Window Dragging support
    def mousePressEvent(self, event):
        if event.button() == Qt.MouseButton.LeftButton:
            self.drag_position = event.globalPosition().toPoint() - self.frameGeometry().topLeft()
            event.accept()

    def mouseMoveEvent(self, event):
        if event.buttons() == Qt.MouseButton.LeftButton and not self.drag_position.isNull():
            self.move(event.globalPosition().toPoint() - self.drag_position)
            event.accept()
