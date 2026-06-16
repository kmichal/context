# Context
(WIP)

A modern Android application for recording, managing, and transcribing audio using AI-powered speech recognition. Context uses the xai API to provide accurate and efficient audio transcription with a clean, intuitive Material 3 interface built with Jetpack Compose.

## Features

- 🎙️ **Audio Recording** - High-quality audio recording directly from your device
- 🔊 **Playback** - Listen to your recordings with intuitive playback controls
- 🤖 **AI Transcription** - Automatic speech-to-text transcription using Xai API
- ⚙️ **Settings Management** - Configure your Xai API key and app preferences
- 🎨 **Modern UI** - Built with Jetpack Compose and Material 3 design
- 📱 **Responsive Design** - Adapts seamlessly to different screen sizes
- **Summary** - Summarizes the meeting and provides action items (In Development)

## Requirements

- **Android** 7.0+ (API 24)
- **Target SDK** Android 15 (API 36)
- **Kotlin** 2.2.10+
- **Gradle** 9.2.1+

## Setup

### Prerequisites

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files

### Configuration

1. Obtain an Xai API key from [xai](https://www.xai.com)
2. Launch the app and go to **Settings**
3. Enter your Xai API key in the configuration screen
4. Your key will be securely stored for future use

## Project Structure

```
app/
├── src/
│   └── main/
│       ├── java/com/asylus/context/
│       │   ├── MainActivity.kt              # Main entry point
│       │   ├── data/
│       │   │   ├── model/                   # Data models
│       │   │   ├── player/                  # Audio playback
│       │   │   ├── recorder/                # Audio recording
│       │   │   ├── repository/              # Repository pattern
│       │   │   └── transcription/           # Transcription services
│       │   └── ui/
│       │       ├── MainScreen.kt            # Navigation logic
│       │       ├── MainViewModel.kt         # Main view model
│       │       ├── screens/                 # UI screens
│       │       ├── components/              # Reusable UI components
│       │       ├── navigation/              # Navigation setup
│       │       └── theme/                   # Material 3 theming
│       └── AndroidManifest.xml
└── build.gradle.kts
```

## Key Technologies

- **Jetpack Compose** - Modern declarative UI framework
- **Material 3** - Latest Material Design system
- **LiveData & StateFlow** - Reactive data management
- **MVVM Architecture** - Clean separation of concerns
- **OkHttp** - HTTP client for API requests
- **Android Security** - Encrypted preferences for API key storage

## Permissions

The app requests the following permissions:

- `RECORD_AUDIO` - To record audio from the microphone
- `INTERNET` - To communicate with Xai API for transcription

## Building

### Debug Build

```bash
./gradlew assembleDebug
```

### Release Build

```bash
./gradlew assembleRelease
```

## Architecture

Context follows the MVVM (Model-View-ViewModel) pattern:

- **Model** - Data models for audio recordings and transcription metadata
- **View** - Compose UI screens (RecordingHomeScreen, PlayerScreen, SettingsScreen)
- **ViewModel** - MainViewModel manages UI state and business logic
- **Repository** - Handles data access and API communication

## Usage

### Recording Audio

1. Open the app on the home screen
2. Tap the record button to start recording
3. Speak clearly into the microphone
4. Tap stop to save the recording

### Transcribing Audio

1. Select a recording from the list
2. Tap the transcribe button
3. Wait for the transcription to complete
4. View the transcript in the player screen

### Managing Settings

1. Navigate to the Settings screen
2. Enter or update your Xai API key
3. Changes are automatically saved

## Dependencies

Key dependencies (see [gradle/libs.versions.toml](gradle/libs.versions.toml)):

- `androidx-compose-bom` 2025.12.00 - Compose framework
- `androidx-lifecycle-runtime-ktx` 2.6.1 - Lifecycle management
- `androidx-activity-compose` 1.8.0 - Activity integration
- `okhttp` 4.12.0 - HTTP client
- `androidx-security-crypto` 1.1.0 - Secure storage


## Author

Asylus

## Contributing

