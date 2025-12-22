# Streamer Audio

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin-blue.svg" alt="Language" />
  <img src="https://img.shields.io/badge/MinSDK-24-orange.svg" alt="MinSDK" />
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License" />
</p>

**Streamer Audio** is an Android app that lets you listen to live Twitch streams as **audio-only** — ideal for multitasking, low-data usage, and **Android Auto** in-car listening.

It supports both **logged-in users** (followed channels) and **guest browsing**.

---

## 📸 Screenshots

<p align="center">
  <img src="screenshots/01_login_screen.png" width="200" alt="Login" />
  <img src="screenshots/02_main_screen_logged_in.png" width="200" alt="Main - Logged In" />
  <img src="screenshots/03_categories_screen.png" width="200" alt="Categories" />
  <img src="screenshots/04_live_screen.png" width="200" alt="Live" />
  <img src="screenshots/05_player_screen.png" width="200" alt="Player" />
  <img src="screenshots/06_settings_screen.png" width="200" alt="Settings" />
</p>

<p align="center">
  <img src="screenshots/07_main_screen_aa.png" width="400" alt="Android Auto - Main" />
  <img src="screenshots/08_main_screen_not_logged_in.png" width="400" alt="Main - Guest" />
  <img src="screenshots/09_live_screen_notlogged_in.png" width="400" alt="Live - Guest" />
  <img src="screenshots/10_player_screen.png" width="400" alt="Player - Large" />
</p>

---

## ✨ Features

### Core
- 🎵 **Audio-only streaming** (no video)
- 👤 **Login or guest mode**
- 📱 **Material Design 3** with light/dark theme
- 🚗 **Android Auto** support for in-car listening
- 🔄 **Infinite scroll** across lists
- 🔍 **Search** live channels (authenticated users)
- 🎮 **Browse categories** (games)

### Playback
- **HLS Direct** (Twitch)
- **Proxy streaming** (EU / AS regions)
- **Automatic fallback** (switches if HLS fails)
- **Optimised audio** for lower bandwidth

---

## 🏗️ Architecture

- Clean Architecture + MVVM
- Kotlin Coroutines + Flow
- Hilt / Dagger for dependency injection

High level structure:

```
data/  → domain/ → presentation/ → service/
```

---

## 🛠️ Tech Stack

- Kotlin
- Media3 / ExoPlayer
- Retrofit + OkHttp
- Glide
- EncryptedSharedPreferences
- Android Auto Media APIs

---

## 🚀 Getting Started

### 1) Clone the repository

```bash
git clone https://github.com/satpal123/StreamerAudio.git
cd StreamerAudio
```

### 2) Twitch API setup

1. Create a Twitch app at https://dev.twitch.tv
2. Set Redirect URI to:

   ```
   http://localhost
   ```

3. Copy your **Client ID**

### 3) Configure the Client ID

In `app/src/main/java/com/player/streameraudio/data/util/Constants.kt`:

```kotlin
const val TWITCH_CLIENT_ID = "your_client_id_here"
```

### 4) Build & install

```bash
./gradlew assembleDebug
./gradlew installDebug
```

---

## 🚗 Android Auto (Testing / Sideloaded APK)

> If you installed this app via **APK / debug build**, Android Auto may hide it unless you enable **Android Auto Developer Mode** and **Unknown sources**.  
> Play Store builds do **not** require this.

### ✅ TL;DR

1. Open **Android Auto** on your phone
2. Scroll down → tap **Version** **10 times**
3. Open **Developer settings** (⋮ menu)
4. Enable **Unknown sources**
5. Reconnect your phone to the car

### Step-by-step (with screenshot references)

#### 1) Open Android Auto settings
- Open **Android Auto** on your phone
- Scroll to the bottom of the settings page

#### 2) Enable Developer Mode
- Tap **Version** 10 times
- You should see: *“Developer mode enabled”*

**Screenshot annotation:**
- Box around **Version**
- Tap indicator (10x)

#### 3) Open Developer settings
- Tap the **⋮** menu (top-right)
- Select **Developer settings**

#### 4) Enable Unknown sources
- In Developer settings, enable:
    - ✅ **Unknown sources**

#### 5) Connect to your car
- Connect your phone (USB or Wireless Android Auto)
- Open Android Auto
- You should see **Streamer Audio** under media apps 🎵

---

## 🧪 Android Auto Troubleshooting

- **App not visible?**
    - Force stop Android Auto
    - Reboot phone
    - Reconnect USB / restart head unit

- **Plays on phone but not on car?**
    - Check media permissions
    - Check Android Auto media settings

---

## 🔒 Security

- OAuth tokens encrypted using `EncryptedSharedPreferences`
- No plaintext credential storage
- Secure HTTPS connections only
- WebView data cleared after logout
- Access tokens validated on app launch

---

## 🎯 Roadmap

- [ ] Favorites/bookmarks for guest users
- [ ] Playback history
- [ ] Sleep timer
- [ ] Chromecast support
- [ ] Equalizer integration

---

## 🐛 Known Issues

- HLS mode may occasionally fail due to Twitch API changes (use Proxy mode as fallback)
- Android Auto search not implemented yet
- Some thumbnails may take time to load on slow connections

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## ☕ Support the Project

If you enjoy **Streamer Audio** and want to support its development:

<p align="center">
  <a href="https://ko-fi.com/satpal123" target="_blank">
    <img src="https://ko-fi.com/img/githubbutton_sm.svg" alt="Support me on Ko-fi" />
  </a>
</p>

Your support helps with maintenance, hosting, and new features ❤️

---

## 📄 License

MIT License — see [LICENSE](LICENSE)

---

## ⚠️ Disclaimer

This app is not affiliated with, endorsed by, or sponsored by Twitch Interactive, Inc.  
All Twitch-related trademarks and logos are property of Twitch Interactive, Inc.

The app is intended for personal use and should respect Twitch's Terms of Service.

---

<p align="center">Made with ❤️ for the Twitch community</p>
<p align="center">⭐ Star the repo if you find it useful!</p>
