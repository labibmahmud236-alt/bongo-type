# BONGO TYPE — Windows Voice Typing Assistant

A lightweight, modern Windows voice typing assistant inspired by Windows Voice Typing (`Win + H`), with first-class support for **Bengali**, **Banglish**, and **Arabic**.

---

## 🌟 Features

- **Universal Voice Typing**: Automatically injects text directly into any focused window (Chrome, VS Code, Notepad, Word, WhatsApp, etc.).
- **Global Shortcut**: Press `CTRL + SPACE` (or `ALT + SPACE` / `WIN + H`) to summon the assistant and begin dictating from anywhere.
- **Mini Floating UI**: Ultra-compact, frameless, draggable, frosted glass pill design.
- **Continuous Dictation**: Listens indefinitely until explicitly paused or toggled.
- **Instant Language Switching**:
  - **বাংলা (Bangla)**: Proper Bengali Unicode with accurate sentence punctuation (`।`).
  - **Banglish**: Romanized Latin script for spoken Bengali (e.g. *"Ami tomar sathe kotha bolchi"*).
  - **العربية (Arabic)**: Native Arabic Unicode with RTL support.
- **Audio Waveform & 60 FPS Animations**: Audio-reactive waveform with glowing gradient effects.
- **System Tray Integration**: Background tray menu with Open, Pause, Settings, and Exit.
- **Settings Panel**: Configure hotkeys, engines, typing speeds, and autostart.

---

## 📦 Installation & Running on Windows

### 1. Requirements
- Python 3.11 or higher
- Windows 10 or Windows 11

### 2. Install Dependencies
```bash
cd windows_desktop
pip install -r requirements.txt
```

### 3. Run Bongo Type
```bash
python main.py
```

---

## 🛠️ Packaging into a Standalone `.exe` with PyInstaller

To create a single, portable `.exe` file that runs without installing Python:

```bash
pip install pyinstaller

pyinstaller --noconsole --onefile \
  --name "BongoType" \
  --add-data "settings;settings" \
  --add-data "speech;speech" \
  --add-data "audio;audio" \
  --add-data "typing;typing" \
  --add-data "ui;ui" \
  --add-data "animations;animations" \
  --add-data "tray;tray" \
  --add-data "hotkeys;hotkeys" \
  main.py
```

The compiled `BongoType.exe` will be located in the `dist/` directory.

---

## 📁 Architecture Overview

```
windows_desktop/
├── main.py                     # Application entrypoint & coordinator
├── requirements.txt            # Python dependencies
├── ui/
│   ├── floating_widget.py      # Windows 11 frameless floating pill
│   └── settings_window.py      # Settings configuration dialog
├── speech/
│   ├── speech_engine.py        # Speech recognition & language formatting
│   └── banglish_transliteration.py # Bengali-to-Banglish engine
├── audio/
│   └── recorder.py             # sounddevice & numpy real-time streaming
├── typing/
│   └── injector.py             # SendInput & clipboard text injection
├── animations/
│   └── waveform.py             # 60 FPS painted audio visualizer
├── tray/
│   └── tray_icon.py            # Windows notification tray icon & menu
├── hotkeys/
│   └── global_hotkey.py        # System-wide keyboard shortcut hooks
└── settings/
    └── config_manager.py       # JSON configuration manager
```
