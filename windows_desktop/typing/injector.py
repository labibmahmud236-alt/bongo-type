"""
Bongo Type - Windows Universal Text Injection Engine
Directly inserts text into currently focused application window using:
1. Win32 SendInput Unicode events (fastest, preserves clipboard)
2. Atomic Clipboard Paste (Ctrl+V) with clipboard state restoration fallback
3. PyAutoGUI / Keyboard fallback
"""

import sys
import time
from typing import Optional

try:
    import ctypes
    from ctypes import wintypes
except ImportError:
    ctypes = None

try:
    import pyperclip
except ImportError:
    pyperclip = None

try:
    import pyautogui
except ImportError:
    pyautogui = None


class WindowsTextInjector:
    """Universal text injector for Windows desktop applications."""

    # Win32 SendInput Structures
    INPUT_KEYBOARD = 1
    KEYEVENTF_UNICODE = 0x0004
    KEYEVENTF_KEYUP = 0x0002

    def __init__(self, typing_delay_ms: int = 15):
        self.typing_delay_ms = typing_delay_ms
        self.is_windows = sys.platform == "win32"

    def inject_text(self, text: str, method: str = "auto") -> bool:
        """
        Inject text into currently focused window.
        method options: 'auto', 'unicode_sendinput', 'clipboard_paste', 'pyautogui'
        """
        if not text:
            return False

        if method == "auto":
            if self.is_windows and ctypes is not None:
                success = self._inject_unicode_sendinput(text)
                if success:
                    return True
            return self._inject_clipboard_paste(text)

        elif method == "unicode_sendinput":
            return self._inject_unicode_sendinput(text)
        elif method == "clipboard_paste":
            return self._inject_clipboard_paste(text)
        elif method == "pyautogui":
            return self._inject_pyautogui(text)

        return False

    def _inject_unicode_sendinput(self, text: str) -> bool:
        """Inserts text via Windows SendInput API with KEYEVENTF_UNICODE."""
        if not self.is_windows or ctypes is None:
            return False

        try:
            class KEYBDINPUT(ctypes.Structure):
                _fields_ = [
                    ("wVk", wintypes.WORD),
                    ("wScan", wintypes.WORD),
                    ("dwFlags", wintypes.DWORD),
                    ("time", wintypes.DWORD),
                    ("dwExtraInfo", ctypes.c_ulong)
                ]

            class INPUT(ctypes.Structure):
                class _INPUT(ctypes.Union):
                    _fields_ = [("ki", KEYBDINPUT)]
                _anonymous_ = ("_input",)
                _fields_ = [
                    ("type", wintypes.DWORD),
                    ("_input", _INPUT)
                ]

            user32 = ctypes.windll.user32
            delay_sec = self.typing_delay_ms / 1000.0 if self.typing_delay_ms > 0 else 0.005

            for char in text:
                code_point = ord(char)

                # Key Down
                inp_down = INPUT()
                inp_down.type = self.INPUT_KEYBOARD
                inp_down.ki = KEYBDINPUT(0, code_point, self.KEYEVENTF_UNICODE, 0, 0)

                # Key Up
                inp_up = INPUT()
                inp_up.type = self.INPUT_KEYBOARD
                inp_up.ki = KEYBDINPUT(0, code_point, self.KEYEVENTF_UNICODE | self.KEYEVENTF_KEYUP, 0, 0)

                events = (INPUT * 2)(inp_down, inp_up)
                user32.SendInput(2, events, ctypes.sizeof(INPUT))

                if delay_sec > 0:
                    time.sleep(delay_sec)

            return True
        except Exception as e:
            print(f"[WindowsTextInjector] SendInput failed: {e}. Falling back to clipboard paste.")
            return self._inject_clipboard_paste(text)

    def _inject_clipboard_paste(self, text: str) -> bool:
        """Temporarily sets clipboard, simulates Ctrl+V, then restores previous clipboard."""
        if pyperclip is None:
            return False

        try:
            previous_clip = None
            try:
                previous_clip = pyperclip.paste()
            except Exception:
                pass

            pyperclip.copy(text)
            time.sleep(0.04)

            # Send Ctrl+V
            if pyautogui is not None:
                pyautogui.hotkey('ctrl', 'v')
            elif self.is_windows and ctypes is not None:
                # Direct VK_CONTROL + 'V' via keybd_event
                VK_CONTROL = 0x11
                VK_V = 0x56
                KEYEVENTF_KEYUP = 0x0002
                ctypes.windll.user32.keybd_event(VK_CONTROL, 0, 0, 0)
                ctypes.windll.user32.keybd_event(VK_V, 0, 0, 0)
                time.sleep(0.02)
                ctypes.windll.user32.keybd_event(VK_V, 0, KEYEVENTF_KEYUP, 0)
                ctypes.windll.user32.keybd_event(VK_CONTROL, 0, KEYEVENTF_KEYUP, 0)

            # Restore clipboard after small interval
            if previous_clip is not None:
                time.sleep(0.1)
                try:
                    pyperclip.copy(previous_clip)
                except Exception:
                    pass

            return True
        except Exception as e:
            print(f"[WindowsTextInjector] Clipboard paste failed: {e}")
            return False

    def _inject_pyautogui(self, text: str) -> bool:
        """PyAutoGUI typewriter injection."""
        if pyautogui is None:
            return False
        try:
            pyautogui.write(text, interval=(self.typing_delay_ms / 1000.0))
            return True
        except Exception as e:
            print(f"[WindowsTextInjector] pyautogui write failed: {e}")
            return False
