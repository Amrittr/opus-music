<div align="center">
<img src="assets/opus-logo.png" width="140" height="140" alt="Opus logo"/>
<h1>Opus</h1>
<p>An Android music player that uses your Spotify taste to pick songs and plays them from YouTube Music.</p>
</div>

## Features

**Music**
- Stream any song, album, artist or playlist on YouTube Music
- Background playback with lock-screen and notification controls
- Download songs for offline listening
- Live lyrics, sleep timer, audio normalization and skip silence
- Library management: liked songs, playlists, queue reordering
- Home-screen widget

**Spotify integration** (optional, free Spotify account)
- Home screen and search powered by your Spotify top tracks, artists and playlists
- Your Spotify playlists and liked songs inside Opus
- Each Spotify track is matched to its YouTube Music version for playback

**Opus account**
- Login is required: email and password through Firebase Authentication
- Create account, forgot password and email verification
- User page with library stats, edit name, change password, sign out and delete account

**Look and feel**
- Light, Dark, Pure Black and system themes
- 20 accent colors and a **Color style** setting: Subtle, Vibrant, Expressive, Rainbow, Fruit salad
- Material 3 design

## How streaming works

Opus does not host any music.

1. **Choosing songs:** after you log in to Spotify, Opus reads your Spotify data (top tracks, playlists, search results). No audio comes from Spotify, so a free account is enough.
2. **Matching:** each Spotify track is searched on YouTube Music and matched by title, artist and duration. Matches are cached on the phone.
3. **Playing:** Opus asks YouTube Music's own app API (InnerTube) for the song's audio stream and plays it with ExoPlayer (Media3). YouTube's bot check (PoToken) is handled in a hidden WebView.

Because this uses unofficial YouTube and Spotify APIs, playback can stop working when those services change, and using it may break their terms of service. Use spare accounts.

## Building from source

Requirements:
- JDK 21
- Android SDK with platform 37 and build tools 37.0.0 (Android Studio installs these)

Steps:
1. Copy `local.properties.sample` to `local.properties`.
2. Fill in your Firebase project's settings (Firebase console → Project settings → Your apps), and enable **Email/Password** under Authentication → Sign-in method.
3. Build:
   ```bash
   ./gradlew :app:assembleFossRelease
   ```
   The unsigned APK is in `app/build/outputs/apk/foss/release/`. Sign it with your own key using `apksigner`, or build a debug APK with `./gradlew :app:assembleFossDebug`.

To test login without touching your real Firebase project, run the Firebase Auth emulator (`firebase emulators:start --only auth`) and build with `-PopusAuthEmulatorHost=10.0.2.2`.

## Where the Opus code lives

| Area | Path |
|---|---|
| Firebase login logic | `app/src/main/kotlin/com/metrolist/music/auth/OpusAuth.kt` |
| Login, user page and account dialogs | `app/src/main/kotlin/com/metrolist/music/ui/screens/auth/` |
| Developer page | `app/src/main/kotlin/com/metrolist/music/ui/screens/settings/DeveloperScreen.kt` |
| Themes and color styles | `app/src/main/kotlin/com/metrolist/music/ui/theme/Theme.kt`, `ui/screens/settings/ThemeScreen.kt` |
| YouTube Music API client | `innertube/` |

## Developer

**Amrit TR**: [github.com/Amrittr](https://github.com/Amrittr) · amrittr08@gmail.com

## Credits

Opus is built on [Meld](https://github.com/FrancescoGrazioso/Meld) by Francesco Grazioso, which is a fork of [Metrolist](https://github.com/MetrolistGroup/Metrolist) (originally by Mo Agamy) and InnerTune (Zion Huang, Malopieds). Thanks to everyone who contributed to those projects.

Changes in Opus: new name and logo, required Firebase login with a user page, a Developer page, color styles, the Meld auto-updater turned off, and Meld's Qobuz download feature removed.

## License

GPL-3.0, the same as the projects it is based on. See [LICENSE](LICENSE). If you share a build of Opus, you must also share its source code under the same license.

## Disclaimer

This project is not affiliated with, funded, authorized or endorsed by YouTube, Google LLC, Spotify AB or any of their affiliates. All trademarks belong to their respective owners.
