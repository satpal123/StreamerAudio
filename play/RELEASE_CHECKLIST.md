# Streamer Audio 2.0 release checklist

## External configuration

- [ ] Register the exact Twitch redirect URL: `https://streameraudio-twitch-backend.netlify.app/oauth/callback`
- [ ] Create an RSA upload key outside the repository and back it up securely
- [ ] Enrol `com.player.streameraudio` in Play App Signing
- [ ] Add the **Play app-signing** SHA-256 certificate fingerprint to the `com.player.streameraudio` entry in `netlify/.well-known/assetlinks.json`, deploy it, and verify the URL returns JSON without redirects. Keep the CI upload-key fingerprint only while distributing directly signed pre-release APKs.
- [ ] Deploy `/privacy`, `/support`, and `/oauth/callback` over HTTPS
- [ ] Configure protected CI signing variables documented in the root README
- [ ] Confirm backend and hosting log retention for the Data Safety declaration

## Automated verification

```shell
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew lintRelease
./gradlew generateBaselineProfile
./gradlew assembleRelease bundleRelease
bundletool build-apks --bundle app-release.aab --output app-release.apks --mode universal
bundletool install-apks --apks app-release.apks
```

- [ ] Run phone/tablet tests on API 26, 31, and 36
- [ ] Test the minified Play-generated build, not only debug APKs
- [ ] Archive the AAB and `app/build/outputs/mapping/release/mapping.txt`
- [ ] Verify no debug signing certificate is used
- [ ] Verify auth callback with the build installed from Play internal testing
- [ ] Verify Direct, Proxy, fallback, selected-proxy deletion, retry, and timer expiry
- [ ] Verify offline, empty, server-error, and expired-token states

## Android Auto

- [ ] Test root, Following, Categories, Live, Search, paging, playback and background recovery in Desktop Head Unit
- [ ] Confirm stable media IDs remain compatible with saved car state
- [ ] Opt into Android Auto in Play Console form factors
- [ ] Complete the media-app review and supply Android Auto screenshots

Foreground-service declaration text:

> Streamer Audio uses a `mediaPlayback` foreground service only while playing user-selected live audio. The service maintains playback when the app is backgrounded and exposes required notification, lock-screen, Bluetooth, and Android Auto controls. Playback stops when the user stops it, logs out, or the sleep timer expires.

Demonstration video script: start a live stream in the app, background it, show the persistent media notification, pause/resume from the notification, control it in Android Auto/DHU, then stop playback and show the notification/service ending.

## Play rollout

- [ ] Upload to internal testing and resolve pre-launch report findings
- [ ] Complete content rating, target audience, ads, app access, privacy, foreground service, and Data Safety forms
- [ ] Run the required closed test for the developer-account type before applying for production access
- [ ] Complete Android Auto approval and incorporate tester feedback
- [ ] Use a staged production rollout
- [ ] Monitor Android Vitals for crashes, ANRs, startup, battery and foreground-service issues
