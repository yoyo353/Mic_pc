# AudioLink

**Turn your Android phone into a high-quality (low latency) microphone for your Windows PC.**

## Overview
AudioLink connects your Android device to your PC via Wi-Fi (WebSocket). It captures audio from the phone's mic, compresses it with Opus codec, and streams it to the PC where a Python server plays it back. By using a Virtual Audio Cable (like VB-Cable), this audio becomes a system-wide microphone input compatible with Discord, Zoom, Games, etc.

**Phase 2 Features:**
- ✅ Opus compression (64 kbps, ~90% bandwidth reduction)
- ✅ Advanced JitterBuffer for smooth playback
- ✅ Latency statistics and monitoring
- ✅ ~90-115ms end-to-end latency

## Prerequisites

### Android
- Android Studio to build and install the app (or ability to build provided gradle project).
- Android 8.0+ (Min SDK 26).

### Windows
- Python 3.8+
- [VB-Cable Driver](https://vb-audio.com/Cable/) (Optional, but required for "Mic" functionality).

## Installation

### 1. Server (Windows)
```bash
cd AudioLink/Server
pip install -r requirements.txt
```

**Note**: Installing `opuslib` requires the Opus library. On Windows:
- Download pre-built Opus DLL from [opus-codec.org](https://opus-codec.org/downloads/) or use `conda install -c conda-forge opus`.

### 2. Client (Android)
- Open the `AudioLink/Android` folder in Android Studio.
- Sync Gradle.
- Connect your phone and Run the app.

## Usage

1.  **Start the Server:**
    ```bash
    python server.py
    ```
    *Runs with Opus by default. Use `--pcm` flag for raw PCM mode.*

2.  **Start the Android App:**
    - Enter your PC's IP Address (e.g., `192.168.1.XX`).
    - Tap **Connect**.

3.  **Verify:**
    - Speak into phone.
    - You should hear yourself on PC (if Default Output is speakers).
    - To use as Mic, set Default Output to "CABLE Input" and use "CABLE Output" as mic in apps.

## Documentation
- [Phase 2 Feature Guide](../brain/.../phase2_guide.md) - Opus & JitterBuffer details
- [VAC Setup Guide](../brain/.../vac_guide.md) - Virtual Audio Cable configuration

## Troubleshooting
- **Latency/Jitter:** Ensure you are on 5GHz Wi-Fi close to the router.
- **Connection Failed:** Check Windows Firewall (Allow python.exe/port 8765).
- **Opus Installation Issues:** See Phase 2 guide for detailed instructions.
