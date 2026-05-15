# Paryavaran Kavalu (Environmental Guardian) 🌍

**Paryavaran Kavalu** is an Android application designed to empower citizens and authorities to work together for a cleaner environment. By identifying and reporting waste blackspots, users earn "Eco-Karma" points, turning environmental responsibility into a community-driven effort.

## 🚀 Key Features

- **Dual Portal System**:
    - **Citizen Portal**: Report waste blackspots with photo evidence and automatic geo-tagging.
    - **Authority Monitor**: Verify cleanup efforts and resolve reports with photographic proof.
- **Eco-Karma Leaderboard**: Gamified experience where users earn XP for reporting and cleaning up waste.
- **Interactive Map View**: Visualize reported blackspots using Google Maps integration with status-coded markers (Red for Pending, Green for Cleaned).
- **Hybrid List View**: Toggle between a map and a detailed list of environmental reports.
- **Geo-Tagging**: Automatic extraction of location data from photos or real-time GPS coordinates.

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Maps**: Google Maps SDK for Android & Maps Compose
- **Architecture**: MVVM (ViewModel, StateFlow)
- **Location**: Google Play Services Location
- **Media**: CameraX / Activity Result APIs for photo capture and processing

## ⚙️ Setup & Installation

### Prerequisites
- Android Studio Ladybug or newer.
- JDK 11+.
- A physical Android device or emulator with Google Play Services.

### Google Maps API Key
1. Obtain an API Key from the [Google Cloud Console](https://console.cloud.google.com/).
2. Enable the **Maps SDK for Android**.
3. Open `app/src/main/AndroidManifest.xml`.
4. Replace `YOUR_API_KEY` with your actual key:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_ACTUAL_KEY_HERE" />
   ```

## 🤝 Contributing

Contributions are welcome! Whether it's fixing bugs, improving the UI, or adding new features like Firebase integration for real-time syncing, feel free to fork the repo and submit a PR.

---
*Developed for a cleaner, greener tomorrow.*
