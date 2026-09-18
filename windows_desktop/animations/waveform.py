"""
Bongo Type - 60 FPS Audio Waveform & Glow Visualizer
Custom PyQt6 painted component with smooth pulsing glow and dynamic audio bars.
"""

import math
from PyQt6.QtCore import Qt, QTimer
from PyQt6.QtGui import QBrush, QColor, QLinearGradient, QPainter, QPainterPath, QPen
from PyQt6.QtWidgets import QWidget


class WaveformWidget(QWidget):
    """60 FPS dynamic waveform visualizer with glowing gradient."""

    def __init__(self, parent=None, bar_count: int = 7):
        super().__init__(parent)
        self.bar_count = bar_count
        self.is_active = False
        self.audio_rms = 0.0
        self.phase = 0.0

        self.setFixedSize(80, 32)
        self.setAttribute(Qt.WidgetAttribute.WA_TransparentForMouseEvents, True)

        # 60 FPS Animation Timer (16ms per frame)
        self.timer = QTimer(self)
        self.timer.timeout.connect(self._on_tick)
        self.timer.start(16)

    def set_active(self, active: bool):
        self.is_active = active
        self.update()

    def set_audio_rms(self, rms: float):
        self.audio_rms = max(0.0, min(10.0, rms))

    def _on_tick(self):
        if self.is_active:
            self.phase += 0.08
            if self.phase > 2 * math.pi:
                self.phase -= 2 * math.pi
            self.update()

    def paintEvent(self, event):
        painter = QPainter(self)
        painter.setRenderHint(QPainter.RenderHint.Antialiasing)

        width = self.width()
        height = self.height()

        spacing = 4
        total_spacing = spacing * (self.bar_count - 1)
        bar_width = max(3.0, (width - total_spacing) / self.bar_count)

        norm_rms = self.audio_rms / 10.0

        for i in range(self.bar_count):
            fraction = i / max(1, self.bar_count - 1)

            if self.is_active:
                sine = (math.sin(self.phase + fraction * 3.5) + 1.0) / 2.0
                bar_h = height * 0.25 + (height * 0.70) * (norm_rms * 0.6 + sine * 0.4)
            else:
                bar_h = height * 0.15

            bar_h = max(4.0, min(height - 4.0, bar_h))

            x = i * (bar_width + spacing)
            y = (height - bar_h) / 2.0

            # Gradient from Cyan to Electric Blue
            gradient = QLinearGradient(x, y, x, y + bar_h)
            if self.is_active:
                gradient.setColorAt(0.0, QColor(0, 229, 255))
                gradient.setColorAt(1.0, QColor(2, 132, 199))
            else:
                gradient.setColorAt(0.0, QColor(100, 116, 139, 120))
                gradient.setColorAt(1.0, QColor(71, 85, 105, 80))

            painter.setBrush(QBrush(gradient))
            painter.setPen(Qt.PenStyle.NoPen)
            painter.drawRoundedRect(int(x), int(y), int(bar_width), int(bar_h), 2, 2)
