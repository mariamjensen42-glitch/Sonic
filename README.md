<h1 align="center">Sonic</h1>

<p align="center">
  <strong>🎵 A Modern Android Music Player with AI-Powered Insights</strong>
</p>

<p align="center">
  <a href="https://opensource.org/licenses/MIT"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License"></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0.21-purple.svg" alt="Kotlin"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-2024.10.01-brightgreen.svg" alt="Compose"></a>
  <a href="https://android-arsenal.com/api?level=30"><img src="https://img.shields.io/badge/API-30%2B-orange.svg" alt="API Level"></a>
  <a href="https://github.com/cycling02/Sonic/actions"><img src="https://img.shields.io/github/actions/workflow/status/cycling02/Sonic/android.yml?branch=main" alt="Build Status"></a>
</p>

<p align="center">
  <a href="#-features">Features</a> •
  <a href="#-tech-stack">Tech Stack</a> •
  <a href="#-architecture">Architecture</a> •
  <a href="#-getting-started">Getting Started</a> •
  <a href="#-contributing">Contributing</a>
</p>


## ✨ Overview

**Sonic** is a beautifully crafted, feature-rich Android music player that combines modern Material 3 design with iOS-inspired UI elements. Built with the latest Android technologies including Jetpack Compose, Clean Architecture, and MVI pattern, Sonic delivers a premium music listening experience with AI-powered album and artist insights.

### 🎯 Key Highlights

- 🎨 **Stunning UI** - Material 3 design with iOS-style components
- 🎤 **Karaoke Lyrics** - Real-time synchronized lyrics display
- 🤖 **AI Integration** - DeepSeek-powered album and artist insights
- 🎵 **Powerful Playback** - Media3 ExoPlayer with background playback
- 📱 **Modern Architecture** - Clean Architecture + MVI pattern

---

## 🚀 Features

### 🎵 Music Playback
- **Background Playback** - Seamless playback with foreground service
- **Media Controls** - Notification bar controls and media session support
- **Queue Management** - Add, remove, reorder songs in play queue
- **Playback Modes** - Single repeat, list repeat, shuffle
- **Smart Statistics** - Track play count after 50% completion

### 🎤 Advanced Lyrics System
Support for multiple lyrics formats:
| Format | Description |
|--------|-------------|
| **LRC** | Standard LRC lyrics |
| **Enhanced LRC** | Enhanced LRC with word-level timing |
| **TTML** | Timed Text Markup Language |
| **Lyricify Syllable** | Syllable-by-syllable lyrics |
| **Word by Word LRC** | Word-level synchronized lyrics |

**Karaoke Experience:**
- Real-time syllable highlighting
- Smooth scrolling animations
- Tap to seek to lyric position

### 🤖 AI-Powered Insights
- **Album Information** - AI-generated album descriptions and context
- **Artist Background** - Comprehensive artist introductions
- **Smart Caching** - Avoid redundant API calls
- **Markdown Rendering** - Rich formatted content display

### 📚 Music Library Management
- **Auto Scanning** - MediaStore-based local music discovery
- **Folder Exclusion** - Customize scan locations
- **Playlists** - Create and manage custom playlists
- **Favorites** - Quick access to loved tracks
- **Statistics** - Recently played, most played, library stats

### 🎨 iOS-Inspired UI Components
- `IOSInsetGrouped` - iOS-style grouped list layout
- `IOSLargeTitleTopAppBar` - Large title navigation header
- `IOSListItem` - iOS-style list item with swipe actions
- `IOSButton` - iOS-style button components

---

## 🛠 Tech Stack

### Core Technologies
| Technology | Version | Purpose |
|------------|---------|---------|
| ![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin) | 2.0.21 | Primary language |
| ![Jetpack Compose](https://img.shields.io/badge/Compose-2024.10.01-4285F4?logo=jetpackcompose) | BOM 2024.10.01 | Declarative UI |
| ![Material 3](https://img.shields.io/badge/Material%203-Latest-4285F4) | - | Design system |
| ![Hilt](https://img.shields.io/badge/Hilt-2.52-4285F4) | 2.52 | Dependency injection |
| ![Room](https://img.shields.io/badge/Room-2.6.1-4285F4) | 2.6.1 | Local database |
| ![Media3](https://img.shields.io/badge/Media3-1.9.0-4285F4) | 1.9.0 | Media playback |
| ![Coil](https://img.shields.io/badge/Coil-3.1.0-4285F4) | 3.1.0 | Image loading |

### Architecture Components
- **Clean Architecture** - Separation of concerns with domain/data/presentation layers
- **MVI Pattern** - Unidirectional data flow with Intent/State/Effect
- **Kotlin Coroutines** - Asynchronous programming
- **Flow** - Reactive data streams
- **Navigation Compose** - Type-safe navigation

---

## 🏗 Architecture

Sonic follows **Clean Architecture** principles with **MVI (Model-View-Intent)** pattern:

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Compose UI │  │ ViewModels  │  │  Contract/State/    │  │
│  │             │◄─┤             │◄─┤  Intent             │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                      DOMAIN LAYER                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Use Cases  │  │  Models     │  │  Repository         │  │
│  │             │──┤             │──┤  Interfaces         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                       DATA LAYER                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Repository  │  │    Room     │  │   MediaStore/       │  │
│  │  Impl       │──┤  Database   │──┤   Remote API        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow
```
User Action → Intent → ViewModel → UseCase → Repository → Data Source
                    ↓
               State Update → UI Render
```

---

## 📁 Project Structure

```
Sonic/
├── app/                           # Main application module
│   ├── MainActivity.kt            # Single Activity entry point
│   ├── SonicApplication.kt        # Hilt entry point
│   └── MainViewModel.kt           # Theme management
│
├── presentation/                  # UI Layer (Compose + ViewModels)
│   ├── components/                # Reusable UI components
│   │   ├── IOSLayout.kt          # iOS-style layouts
│   │   ├── IOSListItem.kt        # iOS-style list items
│   │   └── IOSButton.kt          # iOS-style buttons
│   ├── player/                    # Player screen
│   ├── lyrics/                    # Lyrics display
│   ├── home/                      # Home screen
│   ├── albums/                    # Album list/detail
│   ├── artists/                   # Artist list/detail
│   ├── songs/                     # Song list
│   ├── playlists/                 # Playlist management
│   ├── search/                    # Search functionality
│   ├── settings/                  # App settings
│   └── navigation/                # Navigation graph
│
├── domain/                        # Business Logic Layer
│   ├── model/                     # Domain models
│   ├── repository/                # Repository interfaces
│   ├── usecase/                   # Business use cases
│   └── lyrics/                    # Lyrics parsing system
│       ├── parser/                # LRC/TTML/Syllable parsers
│       ├── model/                 # Lyrics data models
│       └── exporter/              # Lyrics export utilities
│
├── data/                          # Data Layer
│   ├── local/                     # Local data sources
│   │   ├── database/              # Room database
│   │   ├── dao/                   # Data access objects
│   │   ├── entity/                # Database entities
│   │   ├── mediastore/            # MediaStore queries
│   │   └── scanner/               # Music file scanner
│   ├── api/                       # Remote API (DeepSeek)
│   ├── player/                    # Playback manager
│   ├── repository/                # Repository implementations
│   ├── mapper/                    # Data mappers
│   └── di/                        # Hilt modules
│
└── gradle/
    └── libs.versions.toml         # Version catalog
```

---

## 🏃 Getting Started

### Prerequisites

- ![Android Studio](https://img.shields.io/badge/Android%20Studio-Ladybug%20%7C%202024.2.1-green?logo=androidstudio)
- ![JDK](https://img.shields.io/badge/JDK-17-orange?logo=openjdk)
- ![Android SDK](https://img.shields.io/badge/Android%20SDK-36-blue?logo=android)
- ![Min SDK](https://img.shields.io/badge/Min%20SDK-30-blue?logo=android) (Android 11+)

### Clone & Build

```bash
# Clone the repository
git clone https://github.com/cycling02/Sonic.git
cd sonic

# Build the project
./gradlew build

# Run on device/emulator
./gradlew installDebug
```

### Configuration

1. **DeepSeek API Key** (Optional - for AI features)
   
   Create `local.properties` in project root:
   ```properties
   DEEPSEEK_API_KEY=your_api_key_here
   ```

2. **Build Variants**
   - `debug` - Development build with debugging enabled
   - `release` - Production build with optimization

---

## 📥 Download

<p align="center">
  <a href="https://github.com/cycling02/Sonic/releases/latest">
    <img src="https://img.shields.io/github/v/release/cycling02/Sonic?label=Download%20Latest&style=for-the-badge&color=blue" alt="Download Latest Release">
  </a>
</p>

| Version | Min Android | Target Android |
|---------|-------------|----------------|
| Latest  | Android 11 (API 30) | Android 14 (API 36) |

---

## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guidelines](CONTRIBUTING.md) before submitting a Pull Request.

### Development Setup

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Commit Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):

| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation |
| `style` | Code style (formatting) |
| `refactor` | Code refactoring |
| `test` | Adding tests |
| `chore` | Maintenance tasks |

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 cycling02

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

## 🙏 Acknowledgments

- [Media3 ExoPlayer](https://developer.android.com/media/media3) - Powerful media playback
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI toolkit
- [Coil](https://coil-kt.github.io/coil/) - Image loading for Android
- [DeepSeek](https://deepseek.com/) - AI-powered insights

---

<p align="center">
  <strong>Made with ❤️ by cycling02</strong>
</p>

<p align="center">
  <a href="https://github.com/cycling02/Sonic/stargazers">
    <img src="https://img.shields.io/github/stars/cycling02/Sonic?style=social" alt="Star this repo">
  </a>
  <a href="https://github.com/cycling02/Sonic/network/members">
    <img src="https://img.shields.io/github/forks/cycling02/Sonic?style=social" alt="Fork this repo">
  </a>
</p>
