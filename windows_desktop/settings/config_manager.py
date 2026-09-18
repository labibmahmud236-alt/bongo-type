"""
Bongo Type - Configuration Manager
Handles loading, saving, and managing application settings on Windows.
"""

import json
import os
from pathlib import Path
from typing import Any, Dict


class ConfigManager:
    """Manages application preferences and persistence."""

    DEFAULT_SETTINGS = {
        "shortcut": "ctrl+space",
        "default_language": "Bangla",  # Bangla, Banglish, Arabic
        "speech_engine": "google",     # google, whisper, gemini
        "typing_speed_ms": 15,
        "auto_inject": True,
        "always_on_top": True,
        "start_with_windows": False,
        "dark_theme": True,
        "sample_rate": 16000,
        "audio_device_index": None
    }

    def __init__(self, config_dir: Path = None):
        if config_dir is None:
            # Stored in %APPDATA%/BongoType on Windows or home directory
            appdata = os.getenv("APPDATA")
            if appdata:
                base_dir = Path(appdata) / "BongoType"
            else:
                base_dir = Path.home() / ".bongo_type"
        else:
            base_dir = config_dir

        base_dir.mkdir(parents=True, exist_ok=True)
        self.config_file = base_dir / "config.json"
        self._settings = self.DEFAULT_SETTINGS.copy()
        self.load()

    def load(self) -> None:
        """Load configuration from disk."""
        if self.config_file.exists():
            try:
                with open(self.config_file, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    self._settings.update(data)
            except Exception as e:
                print(f"[ConfigManager] Error reading config: {e}. Using defaults.")

    def save(self) -> None:
        """Save configuration to disk."""
        try:
            with open(self.config_file, "w", encoding="utf-8") as f:
                json.dump(self._settings, f, indent=4, ensure_ascii=False)
        except Exception as e:
            print(f"[ConfigManager] Error saving config: {e}")

    def get(self, key: str, default: Any = None) -> Any:
        return self._settings.get(key, default if default is not None else self.DEFAULT_SETTINGS.get(key))

    def set(self, key: str, value: Any) -> None:
        self._settings[key] = value
        self.save()

    def all_settings(self) -> Dict[str, Any]:
        return self._settings.copy()
