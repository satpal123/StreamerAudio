# Changelog

All notable user-facing changes to Streamer Audio are documented here.

## [2.0.0](https://github.com/satpal123/StreamerAudio/releases/tag/v2.0.0) - 2026-11-05

Streamer Audio 2.0 is a ground-up modernization focused on phone usability, reliable background audio, Android Auto, user-controlled routing, secure Twitch authentication, and production release readiness.

### Highlights

- Rebuilt the application as a single-activity Jetpack Compose and Material 3 experience.
- Added guest browsing for popular live channels and categories.
- Added Twitch sign-in for followed live channels, authenticated search, and profile details.
- Added Media3 background playback, a floating mini-player, a full player, retry controls, and sleep timers.
- Added Android Auto browsing and playback for Following, Categories, Live, and Search.
- Replaced bundled proxies with validated, user-managed HTTPS endpoints.
- Added signed APK and Play Store AAB production builds.

### User experience

- Added branded light and dark themes, edge-to-edge layout, adaptive navigation, responsive content, and swipe navigation transitions.
- Added category cards with viewer totals, live stream metadata, automatic pagination, pull-to-refresh, retry actions, and useful empty/error states.
- Added a persistent floating mini-player and a full-screen player with profile artwork, buffering feedback, retry, play/pause, and stream-duration metadata.
- Added a profile/settings experience with the signed-in Twitch account name and profile image.
- Added a launch splash screen and adaptive app icon.
- Added a full-screen loading overlay while resolving a stream to prevent accidental duplicate actions.

### Playback

- Migrated playback to Media3 with a background `MediaLibraryService` and shared phone/Android Auto catalogue logic.
- Added service-owned stream retry and 15, 30, 45, and 60-minute sleep timers with visible remaining time.
- Increased buffering tolerance for unreliable connections while retaining responsive playback controls.
- Added direct playback with optional alternate-route fallback.
- Fixed Twitch/Luminous master-playlist parsing for standalone `#EXT-X-STREAM-INF` variants, including `audio_only`.
- Ensured channel profile artwork is used instead of transient video thumbnails during playback.

### Proxies and routing

- Removed all hardcoded proxy endpoints, regional defaults, and bundled third-party proxy recommendations.
- Added create, edit, select, and delete controls for custom HTTPS proxy endpoints.
- Added strict validation for HTTPS, host, endpoint name, credentials, queries, fragments, and normalized duplicates.
- Added Direct and Custom proxy routing modes with optional fallback.
- Reset playback safely to Direct when the selected proxy is deleted.

### Twitch authentication and security

- Replaced the embedded WebView login with AndroidX Auth Tabs and a verified HTTPS App Link callback.
- Added cryptographically random OAuth state validation.
- Added Android Keystore-backed AES-GCM token storage under no-backup storage.
- Added one-time migration of existing login sessions from legacy storage.
- Added Twitch token validation and best-effort revocation during logout.
- Fixed release-build JSON field preservation and OAuth callback verification.
- Redacted authorization headers and disabled verbose production network logging.

### Android Auto

- Preserved stable media-library IDs for Following, Categories, Live, and Search.
- Added category and stream browsing, paging, authenticated search, live playback, profile artwork, and live-duration presentation.
- Removed seek presentation for non-seekable live streams.
- Kept playback, retry, routing, and metadata owned by the same Media3 service used by the phone UI.

### Build and release

- Updated the project to AGP 9.3.2, Gradle 9.x, JDK 17, API 36, and stable Android dependencies.
- Retained Android 8.0/API 26 as the minimum supported version.
- Enabled R8 minification, resource shrinking, release lint, deterministic screenshot builds, and release mapping artifacts.
- Added protected CI signing for APK and AAB outputs.
- Added automatic develop pre-releases and production releases from `v*` tags.
- Added Play Store listing material, a release checklist, privacy/support pages, Digital Asset Links, and a data-flow inventory.

### Verification

- Restored and expanded the unit-test suite to 133 passing tests.
- Production CI passed unit tests, release lint, debug assembly, signed release APK generation, and Play Store AAB generation.

### Upgrade notes

- Existing Twitch sessions are migrated once where possible. Sign in again if the previous token is no longer valid.
- Legacy bundled proxy selections are intentionally discarded. Direct playback is the default; add a compatible HTTPS endpoint manually only if required.
- The APK is for direct installation. Upload the AAB to Google Play internal testing rather than attempting to install it directly.

**Full commit comparison:** [v1.0.0...v2.0.0](https://github.com/satpal123/StreamerAudio/compare/v1.0.0...v2.0.0)
