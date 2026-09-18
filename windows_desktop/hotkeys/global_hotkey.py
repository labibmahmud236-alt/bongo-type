"""
Bongo Type - Global System-wide Hotkey Manager
Registers global shortcut keys (Ctrl+Space, Alt+Space) via keyboard library or pywin32,
triggering overlay activation and microphone focus from any active application.
"""

from typing import Callable, Optional

try:
    import keyboard
except ImportError:
    keyboard = None


class GlobalHotkeyManager:
    """Manages system-wide hotkey hooks."""

    def __init__(self, hotkey: str = "ctrl+space", callback: Optional[Callable[[], None]] = None):
        self.current_hotkey = hotkey.lower()
        self.callback = callback
        self.is_registered = False

    def start(self) -> bool:
        """Registers the global hotkey."""
        if keyboard is None:
            print("[GlobalHotkey] 'keyboard' module not installed.")
            return False

        try:
            self.stop()
            keyboard.add_hotkey(self.current_hotkey, self._on_triggered)
            self.is_registered = True
            print(f"[GlobalHotkey] Registered global shortcut: {self.current_hotkey}")
            return True
        except Exception as e:
            print(f"[GlobalHotkey] Failed to register {self.current_hotkey}: {e}")
            return False

    def stop(self):
        """Unregisters current hotkey."""
        if self.is_registered and keyboard is not None:
            try:
                keyboard.remove_hotkey(self._on_triggered)
            except Exception:
                pass
            self.is_registered = False

    def update_hotkey(self, new_hotkey: str):
        """Update to a new hotkey."""
        self.stop()
        self.current_hotkey = new_hotkey.lower()
        self.start()

    def _on_triggered(self):
        if self.callback:
            self.callback()
