# Data flow and Play Data Safety working inventory

This is an implementation inventory, not a submitted declaration. Re-check it against the final binary and backend logging configuration immediately before completing Play Console.

| Destination | Data | Purpose | Storage / retention | User control |
|---|---|---|---|---|
| Twitch OAuth and Helix/GQL APIs | OAuth token, Twitch account ID/login, followed-channel and catalog requests, IP/network metadata | Authentication, account features, catalog and playback resolution | Token is encrypted locally; Twitch controls its server retention | Optional login; logout attempts revocation and deletes the local token |
| Streamer Audio Netlify backend | Category/channel/profile requests and normal request metadata such as IP/user agent | Guest browsing and public catalog lookup | Confirm Netlify/function log settings and retention before submission | Guest browsing causes these requests; stop using the app to stop future requests |
| User-supplied proxy | Channel login, audio/source flags, IP and normal HTTPS request metadata | Optional alternate playback routing | Controlled entirely by the proxy operator | Disabled by default; user adds/selects/deletes endpoints |
| Device Keystore + no-backup storage | Twitch access token, account ID and login | Maintain the signed-in session | Until logout, invalidation, corruption, or app-data deletion; excluded from backup | Logout or clear app data |
| Typed DataStore | Proxy names/URLs, routing and fallback preferences, favorite channels, recently played channels, sleep-timer deadline, migration notice | User preferences and on-device library | Until edited/deleted or app-data deletion; eligible for backup/transfer | Favorite toggle, clear recent history, full in-app proxy CRUD, or clear app data |
| Media3 notification / Android Auto | Current stream title, creator/category and artwork URL | Playback controls and media browsing | Ephemeral OS/media-session state | Stop playback |

## Permissions

- `INTERNET`: Twitch, backend, artwork and user-supplied proxy traffic
- `ACCESS_NETWORK_STATE`: offline/error handling
- `POST_NOTIFICATIONS`: media playback notification on supported Android versions
- `WAKE_LOCK`: uninterrupted audio playback
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: background audio playback only

No advertising, analytics, location, contacts, microphone, camera, purchase, or crash-reporting SDK is intentionally included in 2.0.

## Console answers to validate

- Privacy policy: required and public
- Data Safety: complete even if the final legal conclusion is “not collected” for particular fields
- Ads: No
- App access: explain optional Twitch login; provide review access if Google requires authenticated coverage
- Target audience: choose the actual intended age groups; do not mark children unless the app and content flow meet Families requirements
- Foreground service: media playback only
- Account deletion: the app does not create its own account; logout revokes/clears Twitch credentials. Verify whether Play requires an additional URL based on the final account-feature classification.
