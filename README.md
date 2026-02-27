# 🎬 FreyTube - Ad-Free YouTube Client

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android" />
  <img src="https://img.shields.io/badge/API-26%2B-blue?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Kotlin-1.9-purple?style=for-the-badge&logo=kotlin" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material3-orange?style=for-the-badge" />
  <img src="https://img.shields.io/badge/License-GPL--3.0-red?style=for-the-badge" />
</p>

**FreyTube** is an open-source, privacy-focused, ad-free YouTube client for Android. It provides a clean, modern experience without ads, tracking, or data collection.

## 📥 Download

Go to the [**Releases**](../../releases) page and download the latest APK.

> **Install:** Enable "Install from Unknown Sources" in your Android settings, then open the APK.

---

## ✨ Features

### 🚫 Ad-Free Experience
- **Zero advertisements** - Uses Piped API to stream content without any ads
- **SponsorBlock integration** - Automatically skip sponsored segments

### 🎵 Background Playback
- Continue listening with the screen off
- Full media notification controls (play, pause, skip)
- Lock screen controls
- Bluetooth/headset support

### 📥 Video Downloads
- Download videos in any available quality
- Download audio-only (M4A)
- Support up to **4K and 8K** resolution downloads
- Download progress tracking
- Offline playback

### 🎬 High Resolution Playback
- Support for **360p, 480p, 720p, 1080p, 1440p, 4K (2160p), and 8K (4320p)**
- Adaptive streaming (HLS)
- Quality selection during playback
- DASH stream support for highest resolutions

### 🔍 Full Search
- Real-time search suggestions
- Search history
- Search videos, channels, and playlists
- Infinite scroll with pagination

### 📺 Channel Pages
- Channel banner and avatar
- Subscriber count
- Channel description
- Channel videos with pagination

### 💬 Comments
- View comments with like counts
- Pinned and hearted comments
- Creator replies indicator

### ⚡ Playback Controls
- Playback speed control (0.25x - 3.0x)
- Seek forward/backward
- Picture-in-Picture (PiP) mode
- Chapters support

### 🎨 Modern UI
- **Material Design 3** with Jetpack Compose
- Dark and light themes
- Smooth animations
- Gradient app header
- Responsive layout
- Bottom navigation

### 🛡️ Privacy
- No Google services required
- No tracking or analytics
- No data collection
- Powered by Piped API (privacy proxy)

---

## 📱 Screenshots

| Home | Player | Search | Downloads | Settings |
|------|--------|--------|-----------|----------|
| Trending videos with region selection | Full-featured video player | Search with suggestions | Download manager | App settings |

---

## 🏗️ Tech Stack

| Technology | Purpose |
|-----------|---------|
| **Kotlin** | Programming language |
| **Jetpack Compose** | Modern declarative UI |
| **Material Design 3** | Design system |
| **Media3 (ExoPlayer)** | Video/audio playback |
| **Retrofit + OkHttp** | HTTP networking |
| **Room** | Local database |
| **DataStore** | Settings storage |
| **Coil** | Image loading |
| **Piped API** | YouTube data proxy |
| **Coroutines + Flow** | Async programming |
| **Navigation Compose** | App navigation |

---

## 🛠️ Building from Source

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/AiCodeCraft/FreyTube.git
   cd FreyTube
   ```

2. **Open in Android Studio**

3. **Build debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Build signed release APK:**
   ```bash
   # Generate keystore first
   keytool -genkeypair -v -storetype PKCS12 -keyalg RSA -keysize 2048 \
     -validity 10000 -storepass freytube2026 -keypass freytube2026 \
     -alias freytube -keystore keystore/freytube-release.jks \
     -dname "CN=FreyTube, OU=FreyTube, O=FreyTube, L=Internet, ST=Open, C=US"

   # Build release
   ./gradlew assembleRelease
   ```

5. **APK location:** `app/build/outputs/apk/release/app-release.apk`

---

## 🔄 CI/CD

This project uses **GitHub Actions** for automated builds:

- ✅ Builds on every push to `main`
- ✅ Generates signed APK
- ✅ Creates GitHub Release with downloadable APK on tags (`v*`)
- ✅ Manual workflow dispatch support

### Creating a Release
```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## 📂 Project Structure

```
FreyTube/
├── app/
│   └── src/main/
│       ├── java/com/freytube/app/
│       │   ├── FreyTubeApp.kt           # Application class
│       │   ├── MainActivity.kt          # Main activity
│       │   ├── data/
│       │   │   ├── api/                  # Piped API interface
│       │   │   ├── model/                # Data models
│       │   │   ├── repository/           # Data repository
│       │   │   └── local/                # Room DB & DataStore
│       │   ├── service/
│       │   │   ├── BackgroundPlayService.kt  # Background audio
│       │   │   └── DownloadService.kt        # Video downloads
│       │   ├── viewmodel/                # ViewModels
│       │   └── ui/
│       │       ├── theme/                # Material 3 theme
│       │       ├── navigation/           # Navigation graph
│       │       ├── screens/              # Screen composables
│       │       └── components/           # Reusable UI components
│       └── res/                          # Android resources
├── .github/workflows/                    # CI/CD pipeline
├── keystore/                             # Signing keystore
├── build.gradle.kts                      # Project build config
└── README.md
```

---

## ⚖️ Legal Disclaimer

FreyTube is an open-source project for **educational purposes**. It does not host, store, or distribute any copyrighted content. All video content is streamed via the Piped API, which acts as a privacy-focused proxy. Users are responsible for complying with their local laws regarding content consumption.

---

## 📄 License

```
GNU General Public License v3.0

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License.
```

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

<p align="center">
  Made with ❤️ for the open-source community<br/>
  <b>FreyTube</b> - Ad-Free • Open Source • Privacy First
</p>
