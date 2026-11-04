# Streamer Audio

[![Android CI and Release](https://github.com/satpal123/StreamerAudio/actions/workflows/android-build.yml/badge.svg)](https://github.com/satpal123/StreamerAudio/actions/workflows/android-build.yml)
[![Latest release](https://img.shields.io/github/v/release/satpal123/StreamerAudio)](https://github.com/satpal123/StreamerAudio/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Streamer Audio is an independent Android app for listening to live Twitch streams as audio. It supports guest browsing, optional Twitch sign-in for followed channels, user-managed HTTPS playback proxies, background playback, sleep timers, and Android Auto.

Streamer Audio is not affiliated with, endorsed by, or sponsored by Twitch Interactive, Inc.

## Install

The latest stable version is **2.0.0** (version code 2).

- [Download the signed Android APK](https://github.com/satpal123/StreamerAudio/releases/download/v2.0.0/StreamerAudio-v2.0.0-2-b6ddba1-release.apk) for direct installation and testing.
- [Open the v2.0.0 release](https://github.com/satpal123/StreamerAudio/releases/tag/v2.0.0) to view release notes and download the Play Store AAB.
- Read the [changelog](CHANGELOG.md) for the complete release summary.

Android may ask you to allow installation from the browser or file manager used to open the APK. The AAB is intended for Google Play and cannot be installed directly on a phone.

## 2.0 highlights

- Single-activity Jetpack Compose and Material 3 UI with adaptive bottom navigation/navigation rail
- Direct playback by default; no bundled or recommended proxy services
- Optional HTTPS proxy endpoints created and managed by the user
- AndroidX Auth Tabs OAuth with a verified HTTPS App Link callback and random `state`
- Android Keystore-backed token storage in no-backup storage
- Media3 playback, persistent mini-player, retry, and 15/30/45/60 minute sleep timers
- Search for live channels while signed in, automatic paging, responsive category browsing, and account/profile details
- Android Auto media browsing with stable Following, Categories, Live, and Search roots
- Deterministic `screenshot` build type for store captures

## Requirements

- JDK 17
- Android SDK 36
- Android Studio compatible with AGP 9.3.2
- Twitch application whose OAuth redirect URL is exactly `https://streameraudio-twitch-backend.netlify.app/oauth/callback`

The permanent package ID is `com.player.streameraudio`; debug and screenshot builds use suffixes.

## Local configuration

Set the Twitch client ID without committing it:

```properties
# ~/.gradle/gradle.properties or project gradle.properties (untracked)
TWITCH_CLIENT_ID=your_client_id
```

Build and test:

```shell
./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleDebug
./gradlew :app:assembleScreenshot
```

On Windows PowerShell, use `./gradlew.bat` in place of `./gradlew`.

## User-managed proxy contract

Direct playback is the default. Users may add an endpoint in Settings and choose Proxy mode. Endpoints must use HTTPS, have a valid host and non-empty name, and must not contain credentials, a query, or fragment. Equivalent normalized URLs cannot be saved twice.

The app requests:

```text
GET {baseUrl}/live/{encodedChannel}?allow_source=true&allow_audio_only=true&fast_bread={true|false}
```

Deleting the selected endpoint resets routing to Direct. Optional fallback only tries the alternate route after the preferred route fails.

## Local release signing

Create an RSA upload key outside this repository, enrol the app in Play App Signing, and expose these environment variables when building locally:

```text
ANDROID_KEYSTORE_PATH
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

`bundleRelease` and `assembleRelease` fail with an actionable error when any value is missing. Never commit a keystore. Keep each release's `mapping.txt` with the uploaded AAB.

```shell
./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleRelease :app:bundleRelease
```

The checked-in `assetlinks.json` trusts the CI-signed pre-release APK and the local debug build. Before Play testing, add the Google Play **app-signing** certificate fingerprint to the release package entry as an additional fingerprint; Play-generated APKs are not signed with the upload key. Deployment and Play Console steps are tracked in [`play/RELEASE_CHECKLIST.md`](play/RELEASE_CHECKLIST.md).

## GitHub Actions

The [`Android CI and Release`](.github/workflows/android-build.yml) workflow uses JDK 17 and behaves as follows:

| Event | Verification | Signed outputs | GitHub release |
| --- | --- | --- | --- |
| Pull request to `main` or `develop` | Unit tests, release lint, debug APK | No | No |
| Push to `main` | Unit tests, release lint, debug APK | APK, AAB, R8 mapping | No |
| Push to `develop` | Unit tests, release lint, debug APK | APK, AAB, R8 mapping | Automatic pre-release |
| Push a `v*` tag | Unit tests, release lint, debug APK | APK, AAB, R8 mapping | Production release |
| Manual dispatch | Unit tests, release lint, debug APK | APK, AAB, R8 mapping | No |

Develop pre-releases use tags such as `develop-2.0.0.42`. Production releases are created from explicit version tags:

```shell
git tag -a v2.0.0 -m "Streamer Audio 2.0.0"
git push origin v2.0.0
```

Configure these repository secrets under **Settings → Secrets and variables → Actions**:

```text
TWITCH_CLIENT_ID
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

`ANDROID_KEYSTORE_BASE64` is the upload keystore encoded as a single Base64 string. Signed artifacts are retained for 90 days; debug APK artifacts are retained for 14 days. The APK and AAB are attached to GitHub releases, while `mapping.txt` remains in the protected workflow artifact for crash deobfuscation.

## Architecture

The Compose UI observes immutable `StateFlow` state. `PlaybackConnection` connects the phone UI to the Media3 `MediaLibraryService`; playback resolution and the Android Auto catalog remain service-owned. A shared OkHttp client is used by Retrofit, HLS parsing, token revocation, and proxy resolution. Release logging does not include authorization headers.

Authentication tokens are AES-GCM encrypted with a non-exportable Android Keystore key and stored under `noBackupFilesDir`. Version 2.0 performs one migration from the old preference store; `security-crypto` is retained only for this transition release.

## Privacy and support

- Privacy: <https://streameraudio-twitch-backend.netlify.app/privacy>
- Support: <https://streameraudio-twitch-backend.netlify.app/support>
- OAuth callback: <https://streameraudio-twitch-backend.netlify.app/oauth/callback>
- Releases: <https://github.com/satpal123/StreamerAudio/releases>

Deployable static source and the Digital Asset Links files are under [`play/netlify`](play/netlify). Deploy that directory whenever a signing fingerprint changes, then verify `/.well-known/assetlinks.json` returns the release package and certificate without a redirect.

## License

This project is licensed under the MIT License. Twitch names and trademarks belong to their respective owner.
