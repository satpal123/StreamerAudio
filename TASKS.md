# Proposed follow-up tasks

## Fix a typo
- **Issue**: The README introduction splits "bandwidth" into two words ("band width"), which reads like a typo rather than intentional spacing.
- **Proposed Fix**: Update the README wording to use the correct single-word spelling "bandwidth" for clarity.

## Fix a bug
- **Issue**: The instrumentation test asserts the application ID `com.player.twitch`, but the manifest and code use `com.player.streameraudio`, so the test will fail on any run.
- **Proposed Fix**: Update `ExampleInstrumentedTest` to assert `com.player.streameraudio` (and rename variables/comments as needed) so the package check aligns with the actual app ID.

## Fix a code comment/discrepancy
- **Issue**: Comments in `MainActivity` and `TwitchStreamersDetailsActivity` contain garbled "FIX" markers (`Ã¢Å“â€¦` / `âœ…`) from a bad encoding conversion, making the intent of the notes unclear.
- **Proposed Fix**: Replace the corrupted markers with legible text (e.g., "FIX" or "TODO") so the comments accurately convey their purpose.

## Improve a test
- **Issue**: `ExampleUnitTest` only checks that `2 + 2` equals `4`, providing no coverage of app logic.
- **Proposed Fix**: Replace the placeholder with a focused unit test (e.g., verifying the viewer-count formatting logic in `StreamersAdaptor` or another non-Android helper) to start building meaningful coverage.
