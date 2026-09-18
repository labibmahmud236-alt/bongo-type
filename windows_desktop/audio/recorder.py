"""
Bongo Type - Audio Stream Recorder
Captures real-time continuous microphone audio using sounddevice & numpy,
calculates audio RMS volume levels for 60 FPS waveform animation, and
queues audio buffers for speech recognition.
"""

import queue
import threading
import time
import numpy as np
from typing import Callable, Optional

try:
    import sounddevice as sd
except ImportError:
    sd = None


class AudioRecorder:
    """Manages continuous microphone streaming and audio analysis."""

    def __init__(
        self,
        sample_rate: int = 16000,
        chunk_duration_ms: int = 100,
        device_index: Optional[int] = None
    ):
        self.sample_rate = sample_rate
        self.chunk_size = int(sample_rate * (chunk_duration_ms / 1000.0))
        self.device_index = device_index

        self.audio_queue = queue.Queue()
        self.is_recording = False
        self._stream = None
        self._thread = None

        # Callbacks
        self.on_rms_level: Optional[Callable[[float], None]] = None
        self.on_audio_data: Optional[Callable[[bytes], None]] = None

    def start(self) -> bool:
        """Start non-blocking continuous recording."""
        if self.is_recording:
            return True

        self.is_recording = True
        try:
            if sd is not None:
                self._stream = sd.InputStream(
                    samplerate=self.sample_rate,
                    blocksize=self.chunk_size,
                    device=self.device_index,
                    channels=1,
                    dtype="int16",
                    callback=self._audio_callback
                )
                self._stream.start()
                return True
            else:
                print("[AudioRecorder] sounddevice not available, running in fallback mode.")
                self._start_fallback_thread()
                return True
        except Exception as e:
            print(f"[AudioRecorder] Failed to start audio stream: {e}")
            self.is_recording = False
            return False

    def _audio_callback(self, indata, frames, time_info, status):
        """sounddevice input stream callback running on audio thread."""
        if not self.is_recording:
            return

        # 1. Compute RMS volume level (0.0 to 10.0 scale)
        data = np.frombuffer(indata, dtype=np.int16)
        rms = np.sqrt(np.mean(data.astype(np.float32) ** 2))
        normalized_rms = min(10.0, max(0.0, (rms / 32768.0) * 45.0))

        if self.on_rms_level:
            self.on_rms_level(normalized_rms)

        # 2. Forward raw audio bytes
        raw_bytes = indata.tobytes()
        self.audio_queue.put(raw_bytes)
        if self.on_audio_data:
            self.on_audio_data(raw_bytes)

    def _start_fallback_thread(self):
        """Generates realistic waveform activity if running without physical mic."""
        def run():
            phase = 0.0
            while self.is_recording:
                phase += 0.2
                simulated_rms = float((np.sin(phase) * 3.5 + 4.5))
                if self.on_rms_level:
                    self.on_rms_level(simulated_rms)
                time.sleep(0.06)

        self._thread = threading.Thread(target=run, daemon=True)
        self._thread.start()

    def stop(self):
        """Stop microphone recording."""
        self.is_recording = False
        if self._stream is not None:
            try:
                self._stream.stop()
                self._stream.close()
            except Exception as e:
                print(f"[AudioRecorder] Error closing stream: {e}")
            self._stream = None

        if self.on_rms_level:
            self.on_rms_level(0.0)

    def get_devices(self):
        """List available audio input devices."""
        if sd is None:
            return []
        try:
            return [d for d in sd.query_devices() if d.get('max_input_channels', 0) > 0]
        except Exception:
            return []
