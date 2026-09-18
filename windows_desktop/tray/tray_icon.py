"""
Bongo Type - Windows System Tray Icon
Maintains tray presence and menu options:
- Open
- Pause
- Settings
- Exit
"""

from PyQt6.QtCore import pyqtSignal
from PyQt6.QtGui import QAction, QIcon, QPixmap, QColor, QPainter
from PyQt6.QtWidgets import QMenu, QSystemTrayIcon


class BongoTrayIcon(QSystemTrayIcon):
    """System tray manager for Bongo Type."""

    open_requested = pyqtSignal()
    pause_toggled = pyqtSignal(bool)
    settings_requested = pyqtSignal()
    exit_requested = pyqtSignal()

    def __init__(self, parent=None):
        super().__init__(parent)
        self.is_paused = False
        self._init_icon()
        self._init_menu()

    def _init_icon(self):
        """Creates a vibrant tray icon badge."""
        pixmap = QPixmap(32, 32)
        pixmap.fill(QColor(0, 0, 0, 0))

        painter = QPainter(pixmap)
        painter.setRenderHint(QPainter.RenderHint.Antialiasing)
        painter.setBrush(QColor(2, 132, 199))
        painter.setPen(QColor(0, 229, 255))
        painter.drawEllipse(2, 2, 28, 28)

        painter.setPen(QColor(255, 255, 255))
        painter.drawText(pixmap.rect(), 0x0084, "🎙")  # AlignCenter
        painter.end()

        self.setIcon(QIcon(pixmap))
        self.setToolTip("Bongo Type — Voice Typing Assistant")

    def _init_menu(self):
        menu = QMenu()
        menu.setStyleSheet("""
            QMenu {
                background-color: #1E293B;
                color: #FFFFFF;
                border: 1px solid #334155;
                border-radius: 8px;
                padding: 4px;
            }
            QMenu::item {
                padding: 6px 20px;
                border-radius: 4px;
            }
            QMenu::item:selected {
                background-color: #0284C7;
            }
        """)

        open_action = QAction("Open Bongo Type", self)
        open_action.triggered.connect(lambda: self.open_requested.emit())
        menu.addAction(open_action)

        self.pause_action = QAction("Pause Dictation", self)
        self.pause_action.triggered.connect(self._toggle_pause)
        menu.addAction(self.pause_action)

        settings_action = QAction("Settings...", self)
        settings_action.triggered.connect(lambda: self.settings_requested.emit())
        menu.addAction(settings_action)

        menu.addSeparator()

        exit_action = QAction("Exit", self)
        exit_action.triggered.connect(lambda: self.exit_requested.emit())
        menu.addAction(exit_action)

        self.setContextMenu(menu)
        self.activated.connect(self._on_activated)

    def _toggle_pause(self):
        self.is_paused = not self.is_paused
        self.pause_action.setText("Resume Dictation" if self.is_paused else "Pause Dictation")
        self.pause_toggled.emit(self.is_paused)

    def _on_activated(self, reason):
        if reason == QSystemTrayIcon.ActivationReason.Trigger:
            self.open_requested.emit()
