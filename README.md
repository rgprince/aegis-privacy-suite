# 🛡️ AEGIS Privacy Suite

**Advanced Android Privacy & Firewall Solution**

A powerful, open-source Android application that provides comprehensive privacy protection through DNS-based content blocking and per-app network controls. Built with modern Android architecture (Kotlin, Jetpack Compose, Hilt).

[![Android CI](https://github.com/rgprince/aegis-privacy-suite/actions/workflows/build.yml/badge.svg)](https://github.com/rgprince/aegis-privacy-suite/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## ✨ Key Features

### 🌐 DNS-Based Blocking
- **Pre-configured Blocklists**: Steven Black, AdAway, OISD Basic
- **Custom Blocklist URLs**: Add your own hosts files from any URL
- **Auto-refresh**: Keep blocklists up-to-date
- **Trie-based matching**: Lightning-fast domain lookups
- **100,000+ blocked domains** out of the box

### 🔥 Per-App Firewall
- **Block individual apps** from accessing the internet
- **UID-based blocking**: Precise control at the system level
- **System app filtering**: Show/hide system applications
- **Search functionality**: Quickly find apps
- **Real-time statistics**: See blocked vs. total apps

### 🚀 Dual Operating Modes
1. **VPN Mode** (No root required)
   - Local VPN service intercepts DNS queries
   - Works on any Android device (API 21+)
   - Minimal battery impact
   
2. **Root Mode** (Requires root access)
   - Direct `/etc/hosts` file modification
   - System-wide blocking at kernel level
   - Optional Magisk module support

### 📊 Real-Time Monitoring
- **Connection Logs**: See every DNS request
- **Block/Allow tracking**: Real-time statistics
- **Dashboard**: At-a-glance overview
- **Per-source stats**: Track blocklist effectiveness

### 🎨 Modern Material 3 UI
- **Dark/Light themes**: Follows system preferences
- **Smooth animations**: Polished user experience
- **Intuitive navigation**: Bottom nav + nested screens
- **Responsive design**: Optimized for all screen sizes

---

## 📱 Screenshots

*(Add screenshots here)*

---

## 🏗️ Architecture

### Tech Stack
- **Language**: Kotlin 1.9.20
- **UI**: Jetpack Compose + Material 3
- **DI**: Hilt (Dagger)
- **Database**: Room
- **Async**: Kotlin Coroutines + Flow
- **Networking**: OkHttp + Retrofit
- **Root**: LibSu

### Project Structure
```
app/src/main/kotlin/com/aegis/privacy/
├── AegisApplication.kt          # App entry point
├── core/
│   ├── database/                # Room database, DAOs, entities
│   ├── engine/                  # Blocklist repository, adapters
│   └── parser/                  # Hosts file parser
├── di/                          # Hilt modules
├── network/                     # VPN, DNS interceptor, UID resolver
├── service/                     # AegisVpnService
├── ui/
│   ├── screens/                 # Compose screens
│   ├── viewmodel/               # ViewModels
│   ├── navigation/              # Navigation logic
│   └── theme/                   # Material 3 theme
└── util/                        # Utilities, constants
```

---

## 🚀 Getting Started

### Prerequisites
- Android device running **Android 5.0 (API 21)** or higher
- **(Optional)** Root access for Root Mode

### Installation

#### Option 1: GitHub Releases (Recommended)
1. Go to [Releases](https://github.com/rgprince/aegis-privacy-suite/releases)
2. Download the latest `app-debug.apk`
3. Install on your device (enable "Install from unknown sources")

#### Option 2: Build from Source
```bash
# Clone the repository
git clone https://github.com/rgprince/aegis-privacy-suite.git
cd aegis-privacy-suite

# Build debug APK
./gradlew assembleDebug

# APK location: app/build/outputs/apk/debug/app-debug.apk
```

### First Run Setup
1. **Launch AEGIS** on your device
2. **Select Mode**: Choose VPN (no root) or Root mode
3. **Grant VPN Permission** (if using VPN mode)
4. **Enable Protection**: Toggle the protection switch
5. **Select Blocklists**: Go to Blocklists tab → Enable sources → Apply
6. **Done!** Ads and trackers are now blocked

---

## 📖 Usage Guide

### Adding Custom Blocklists
1. Go to **Blocklists** tab
2. Tap the **+** button (bottom right)
3. Enter a name and URL (must be a hosts file format)
4. Tap **Add**
5. Enable the new source and tap **Apply Changes**

### Blocking Individual Apps
1. Go to **Firewall** tab
2. Browse or search for an app
3. Toggle the switch to **block** that app's internet access
4. The app will immediately lose network connectivity

### Viewing Connection Logs
1. Go to **Logs** tab
2. See real-time DNS queries
3. Green = Allowed, Red = Blocked
4. Tap an entry for details (domain, app, timestamp)

---

## 🔧 Configuration

### Settings Options
- **Auto-start on boot**: Enable protection automatically
- **Update notifications**: Get notified of blocklist updates
- **Logging level**: Control verbosity
- **Backup/Restore**: Export/import settings

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Development Setup
1. Clone the repo
2. Open in **Android Studio Hedgehog** or later
3. Sync Gradle
4. Run on emulator or device

### Code Style
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use `ktlint` for formatting
- Write tests for new features

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- **Blocklist Providers**: Steven Black, AdAway, OISD
- **Inspiration**: RethinkDNS, Blokada, AdGuard
- **Libraries**: Jetpack Compose, Hilt, Room, OkHttp, LibSu

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/rgprince/aegis-privacy-suite/issues)
- **Discussions**: [GitHub Discussions](https://github.com/rgprince/aegis-privacy-suite/discussions)

---

## ⚠️ Disclaimer

This application is provided "as is" without warranty. Use at your own risk. The developers are not responsible for any damage caused by the use of this software.

---

**Built with ❤️ for privacy**
