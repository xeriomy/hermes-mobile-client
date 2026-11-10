# Hermes Mobile (Android, Kotlin)

Native Android client for a Hermes Agent, speaking the `hermes-mobile`
protocol v1 over WebSocket. Kotlin + Jetpack Compose, no Expo.

```
phone  ⇄  ws://<agent-host>:8787/ws  ⇄  Hermes gateway (or the Rust bridge)  ⇄  agent
```

## Build

Builds run in GitHub Actions (this machine has no Android SDK):

```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest  # JVM unit tests
```

The CI workflow (`.github/workflows/ci.yml`) runs the unit tests on every push
and PR, builds the debug APK, and uploads both as artifacts.

## Run

1. Install `app-debug.apk` on a device or emulator.
2. Open the app → it starts on **Settings**.
3. Enter:
   - WebSocket URL: `ws://127.0.0.1:8787/ws` (same device) or `wss://<host>/ws`
   - App token: the `MOBILE_TOKEN` value from `~/.hermes/.env`
4. **Save and connect** → the status line reads **agent online**.

The device id shown on the settings screen is stable per install. It is the
Hermes session (`mobile:<id>`) and the cron address:

```bash
hermes send --to mobile:<device-id> "remember to water the plants"
```

## Architecture

```
app/src/main/java/com/hermes/mobile/
  data/
    Protocol.kt     # protocol v1 frames: parse inbound, build outbound
    ChatState.kt    # pure chat-list reducers (append vs edit)
    HermesClient.kt # OkHttp WebSocket: hello/ready, backoff reconnect, fatal auth
    SettingsStore.kt# URL/token + stable device id (SharedPreferences)
  vm/
    ChatViewModel.kt# UiState + listener wiring
  ui/
    ChatScreen.kt       # LazyColumn of bubbles, cards, composer
    SettingsScreen.kt   # URL + token entry
    Theme.kt            # Material 3 light/dark
```

**Message model.** One list of `Item`s keyed by `id`. Streaming chunks
(`edit == false`) concatenate onto the bubble; `edit == true` frames replace
it. Both are upserts — never appends — so a streamed reply can never produce a
duplicate bubble. `ChatState` holds that logic as pure functions and is unit
tested.

**Transport.** `HermesClient` owns hello/ready, exponential-backoff reconnect,
and the fatal `auth_failed` stop. Unknown frame types are ignored, never fatal.

**Security.** The token travels only in the `hello` frame — never in the URL,
so it cannot leak into reverse-proxy or access logs. `wss://` is enforced in
production by convention; `ws://` is permitted for local development.

## Tests

`app/src/test/java/com/hermes/mobile/ProtocolTest.kt` covers frame parsing
(ready, message, prompt, error, unknown, malformed) and the chat-list reducers
(chunk concatenation, edit replacement, separate bubbles). These are plain JVM
tests, so they run in CI without an emulator.

## Status

**Phase 1 — done:** connect, settings, text in, streamed text out, typing
indicator, avatar state, prompt cards (rendered, answerable).

**Phase 2 — next:** media attachments, cron delivery to the device, and a
release build with signing.
