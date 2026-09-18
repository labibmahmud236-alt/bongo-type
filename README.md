# Bongo Type (বঙ্গ টাইপ) 🎙️

**Bongo Type** is an intelligent, high-speed Universal Voice Typing assistant built with first-class support for **Bangla (বাংলা)**, **Banglish (Latin phonetic Bengali)**, and **Arabic (العربية)**.

Inspired by Windows Voice Typing (`Win + H`), Bongo Type provides:
- **Universal Text Injection**: Automatically types transcribed text directly into any focused application window (VS Code, Notepad, Microsoft Word, Google Chrome, WhatsApp Desktop, etc.).
- **Windows 11 Fluent Floating Pill**: Frameless, minimal, draggable, always-on-top glass pill interface featuring a glowing microphone button, real-time 60 FPS audio waveform visualizer, and instant language switching.
- **Continuous Dictation**: Listens indefinitely without pausing or stopping after single sentences until explicitly toggled.
- **Dedicated Language Engines**:
  - **বাংলা (Bangla)**: Direct Bengali Unicode transcription with automatic Bengali danda punctuation (`।`).
  - **Banglish**: Romanized Latin script for spoken Bengali (e.g., *"Ami ekhon kotha bolchi"*), preserving Latin characters without converting back to Bengali script.
  - **العربية (Arabic)**: Full Arabic Unicode transcription with RTL punctuation.
- **Cross-Platform**: Includes both an Android Jetpack Compose interactive workbench & simulation suite and a complete Windows 10/11 Desktop application in `windows_desktop/` using PyQt6, Win32 SendInput, and continuous audio streaming.

---

## 📁 Repository Structure

```
├── app/                        # Android Jetpack Compose Application & Workbench
│   └── src/main/java/com/example/
│       ├── audio/              # Audio visualizer & RMS state
│       ├── model/              # Data models (Languages, MicState, Settings)
│       ├── speech/             # Continuous speech recognition manager
│       ├── typing/             # Simulated & clipboard universal text injection
│       ├── ui/                 # Bongo Type UI, floating pill, workbench, dialogs
│       └── MainActivity.kt
├── windows_desktop/            # Windows 10/11 PyQt6 Desktop Application
│   ├── main.py                 # Windows application entrypoint
│   ├── requirements.txt        # Python dependencies
│   ├── ui/                     # Frameless floating pill & settings dialog
│   ├── speech/                 # Continuous speech engine & Banglish transliterator
│   ├── audio/                  # sounddevice & numpy real-time audio capture
│   ├── typing/                 # Win32 SendInput & atomic clipboard injector
│   ├── animations/             # 60 FPS painted glowing audio visualizer
│   ├── tray/                   # Windows notification system tray icon & menu
│   ├── hotkeys/                # Global shortcut listener (CTRL + SPACE)
│   └── settings/               # App configuration & preferences
└── README.md
```

---

## 🚀 Running the Windows Desktop Application

### Prerequisites
- Python 3.11+
- Windows 10 or Windows 11

### Installation
```bash
cd windows_desktop
pip install -r requirements.txt
python main.py
```

### Packaging into a Standalone `.exe`
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
The standalone executable will be in the `windows_desktop/dist/` folder.

---

## 📱 Android App
Built with modern Android standards:
- Kotlin & Jetpack Compose (Material Design 3)
- Edge-to-Edge display with dynamic dark Mica theme
- Android SpeechRecognizer with simulated continuous dictation fallback
- Universal App workbench testing suite
