# 🎙️ AudioLink

<div align="center">

**Transform your Android phone into a high-quality wireless microphone for your Windows PC**

[![Python Version](https://img.shields.io/badge/python-3.8%2B-blue.svg)](https://www.python.org/downloads/)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://developer.android.com/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Windows%2011-blue)](https://www.microsoft.com/windows)

[Features](#-features) • [Quick Start](#-quick-start) • [Installation](#-installation) • [Usage](#-usage) • [Documentation](#-documentation) • [Troubleshooting](#-troubleshooting)

</div>

---

## 📖 Overview

AudioLink is a real-time audio streaming solution that turns your Android smartphone into a professional wireless microphone for your Windows PC. Perfect for podcasting, gaming, video conferencing, and content creation.

### How It Works

```
📱 Android Phone  →  🌐 WiFi/USB  →  💻 Windows PC  →  🎯 Discord/Zoom/OBS
   (Microphone)      (WebSocket)     (Virtual Mic)      (Applications)
```

The app captures audio from your phone's microphone, compresses it using the Opus codec, and streams it to your PC via WebSocket. Using a Virtual Audio Cable (VB-Cable), the audio becomes available as a system-wide microphone input.

---

## ✨ Features

### Phase 2 Complete (Current)
- ✅ **Opus Compression** - 64 kbps bitrate (~90% bandwidth reduction)
- ✅ **Low Latency** - 90-115ms end-to-end on WiFi 5GHz
- ✅ **Adaptive JitterBuffer** - Smooth playback with network jitter compensation
- ✅ **Dual Mode** - Opus (compressed) or PCM (raw) audio
- ✅ **Background Streaming** - Continue streaming while app is minimized
- ✅ **Audio Controls** - Volume slider and Mute button
- ✅ **Easy Server Launch** - Auto-IP detection and startup script

### Upcoming (Phase 3)
- 🔜 **USB Mode** - Ultra-low latency (30-50ms) via ADB tunnel
- 🔜 **TLS Encryption** - Secure WebSocket connections
- 🔜 **Auto-reconnection** - Automatic recovery from network issues
- 🔜 **Bidirectional Control** - Remote configuration from PC

---

## 🚀 Quick Start

### Prerequisites

| Component | Requirement |
|-----------|-------------|
| **Windows PC** | Windows 10/11, Python 3.8+ |
| **Android Phone** | Android 8.0+ (API 26+) |
| **Network** | Same WiFi network (5GHz recommended) |
| **Optional** | [VB-Cable](https://vb-audio.com/Cable/) for system mic |

### 5-Minute Setup

1. **Start Server (Windows)**
   - Go to `AudioLink/Server`
   - Double-click **`start_server.bat`**
   - Note the displayed IP address (e.g., `192.168.1.X`)

2. **Install Client (Android)**
   - Install the APK from `AudioLink/Android/app/build/outputs/apk/debug/app-debug.apk`

3. **Connect**
   - Enter your PC's IP in the app
   - Tap "Connect"
   - Use Volume/Mute controls as needed 🎤
   - Look for the "AudioLink Active" notification when minimizing the app

---

## 📦 Installation

### Server (Windows)

#### Option 1: Quick Install
```bash
cd AudioLink/Server
pip install -r requirements.txt
```

#### Option 2: With Conda (Recommended for Opus)
```bash
conda install -c conda-forge opus
pip install -r requirements.txt
```

#### Dependencies
- `websockets` - WebSocket server
- `pyaudio` - Audio output
- `opuslib` - Opus codec (requires Opus library)

> **Note:** If Opus installation fails, `start_server.bat` automatically falls back to PCM mode.

### Client (Android)

#### Option 1: Build from Source
1. Open `AudioLink/Android` in Android Studio
2. Sync Gradle dependencies
3. Connect your phone via USB
4. Click **Run** ▶️

#### Option 2: Install APK
1. Transfer the built APK to your phone
2. Enable "Install from Unknown Sources"
3. Install and grant microphone permission

---

## 🎯 Usage

### Basic Usage

1. **Start the server:**
   Double-click `start_server.bat`
   
   You should see:
   ```
   ==================================================
    AUDIO LINK SERVER STARTED
   ==================================================
    Connect your phone to WiFi and enter this IP:
   
         192.168.1.113
   
    Port: 8765
   ==================================================
   ```

2. **Connect from Android:**
   - Open AudioLink app
   - Enter the IP displayed above
   - Tap **Connect**
   - **Background Mode:** Press Home button; streaming continues via notification.
   - **Mute:** Use the Mute button to silence audio without disconnecting.

3. **Verify audio:**
   - You should hear yourself through PC speakers (unless using VB-Cable)

### Use as System Microphone

To use AudioLink in Discord, Zoom, OBS, etc.:

1. **Install VB-Cable:**
   - Download from [vb-audio.com/Cable](https://vb-audio.com/Cable/)
   - Run installer as Administrator
   - Restart PC

2. **Configure Windows Audio:**
   - Settings → Sound
   - **Output Device:** "CABLE Input (VB-Audio Virtual Cable)"

3. **Configure Your App:**
   - Discord: Settings → Voice & Video → Input Device → "CABLE Output"
   - Zoom: Settings → Audio → Microphone → "CABLE Output"
   - OBS: Settings → Audio → Mic/Auxiliary → "CABLE Output"

4. **Done!** Your phone is now your system microphone 🎉

### Advanced Usage

#### USB Mode (Ultra-Low Latency)
```bash
# Connect phone via USB, enable USB debugging
adb reverse tcp:8765 tcp:8765

# Start server
double-click start_server.bat

# In Android app, connect to: localhost
```
**Latency:** ~30-50ms (vs 90-115ms on WiFi)

---

## 📊 Performance

### Latency Breakdown

| Component | WiFi 5GHz | WiFi 2.4GHz | USB |
|-----------|-----------|-------------|-----|
| Audio Capture | 20ms | 20ms | 20ms |
| Encoding | 5ms | 5ms | 5ms |
| Network | 15ms | 30ms | 3ms |
| Decoding | 5ms | 5ms | 5ms |
| JitterBuffer | 40ms | 60ms | 20ms |
| Output | 5ms | 5ms | 5ms |
| **Total** | **90-115ms** | **125-150ms** | **30-50ms** |

### Audio Quality

- **Sample Rate:** 48kHz
- **Bit Depth:** 16-bit
- **Channels:** Mono
- **Codec:** Opus (64 kbps) or PCM (16-bit raw)
- **Compression:** ~91% bandwidth reduction (Opus mode)
- **Quality:** Comparable to mid-range USB microphones

---

## 📚 Documentation

- **[Setup Guide](docs/SETUP_GUIDE.md)** - Detailed installation and configuration
- **[WiFi 5GHz Optimization](docs/WIFI_5GHZ_GUIDE.md)** - ⭐ Get the best performance (90-115ms latency)
- **[Architecture](docs/ARCHITECTURE.md)** - System design and data flow
- **[Implementation Plan](docs/implementation_plan.md)** - Development roadmap
- **[Troubleshooting](docs/SETUP_GUIDE.md#troubleshooting)** - Common issues and solutions

---

## 🔧 Troubleshooting

### Connection Failed

**Problem:** App shows "Connection Failed"

**Solutions:**
- ✅ Verify both devices on same WiFi network
- ✅ Check Windows Firewall allows Python on port 8765
- ✅ Ensure server is running
- ✅ Try disabling firewall temporarily to test

### Opus Installation Issues

**Problem:** `Failed to initialize Opus decoder`

**Solutions:**
- ✅ `start_server.bat` handles this automatically by switching to PCM mode.
- ✅ To fix Opus manually: `conda install -c conda-forge opus`

### High Latency (>200ms)

**Problem:** Audio has noticeable delay

**Solutions:**
- ✅ Use WiFi 5GHz instead of 2.4GHz
- ✅ Move closer to router
- ✅ Close bandwidth-heavy applications
- ✅ Try USB mode for minimum latency

### Audio Choppy/Stuttering

**Problem:** Audio has cuts or pauses

**Solutions:**
- ✅ Increase JitterBuffer in `server.py`:
  ```python
  self.jitter_buffer = JitterBuffer(target_buffer_ms=60, frame_duration_ms=20)
  ```
- ✅ Improve WiFi signal strength
- ✅ Close CPU-intensive applications

### No Audio in Discord/Zoom

**Problem:** Applications don't receive audio

**Solutions:**
- ✅ Verify VB-Cable is installed
- ✅ Set Windows output to "CABLE Input"
- ✅ Set app input to "CABLE Output"
- ✅ Restart application after changing settings

---

## 🛠️ Development

### Project Structure

```
AudioLink/
├── Server/                 # Python server (Windows)
│   ├── server.py          # Main WebSocket server
│   ├── start_server.bat   # Startup script
│   ├── JitterBuffer.py    # Adaptive jitter buffer
│   └── requirements.txt   # Python dependencies
├── Android/               # Android client (Kotlin)
│   └── app/src/main/java/com/audiolink/
│       ├── MainActivity.kt      # UI & Service binding
│       ├── AudioService.kt      # Foreground Service
│       └── AudioStreamer.kt     # Audio capture & streaming
└── README.md
```

### Tech Stack

**Server (Python):**
- `asyncio` + `websockets` - Async WebSocket server
- `pyaudio` - Audio output
- `opuslib` - Opus codec

**Client (Kotlin/Android):**
- `AudioRecord` - Audio capture
- `OkHttp` - WebSocket client
- `Concentus` - Opus encoder
- `Foreground Service` - Background execution

### Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

---

## 🗺️ Roadmap

### ✅ Phase 1 - MVP (Completed)
- Basic WebSocket streaming
- PCM audio transmission
- Simple Android UI

### ✅ Phase 2 - Quality (Current)
- Opus compression
- Adaptive JitterBuffer
- Latency statistics
- Dual codec support

### ✅ Phase 3 - Enhanced (Current)
- Background service (Android) ✔️
- Volume and Mute controls ✔️
- Easy server startup ✔️
- Modular architecture

### 🔜 Phase 4 - Professional
- Desktop GUI (Windows)
- macOS/Linux support
- Bluetooth mode
- Session recording
- Built-in equalizer
- Stereo support

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- **Opus Codec** - [opus-codec.org](https://opus-codec.org/)
- **VB-Audio** - [vb-audio.com](https://vb-audio.com/)
- **PyAudio** - [people.csail.mit.edu/hubert/pyaudio](https://people.csail.mit.edu/hubert/pyaudio/)
- **Concentus** - Opus implementation for .NET/Kotlin

---

## 📞 Support

- **Issues:** [GitHub Issues](../../issues)
- **Documentation:** [docs/](docs/)
- **Discussions:** [GitHub Discussions](../../discussions)

---

<div align="center">

**Made with ❤️ for content creators, podcasters, and gamers**

⭐ Star this repo if you find it useful!

</div>
