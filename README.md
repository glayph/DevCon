# PhoneDeck

PhoneDeck turns an Android device into a remotely accessible control dashboard. The Android app runs the local HTTP bridge, serves the dashboard, and connects to the PhoneDeck Cloudflare Worker relay for a stable public URL.

## Features

- Local dashboard served from the Android device on port `8080`.
- Stable Cloudflare relay URLs in the form:

  ```text
  https://phonedeck-relay.glayph-phonedeck.workers.dev/d/<device-id>/
  ```

- Per-device identity and tunnel secret stored in Android secure storage.
- Automatic Cloudflare WebSocket reconnects after network changes.
- Dashboard authentication with password login and one-time OTP confirmation.
- Device controls, notifications, screen capture, application management, and MCP endpoints.

## Requirements

- Android Studio or the Android command-line tools.
- JDK 21.
- Android SDK Platform 36 and Build Tools 36.0.0.
- A device running Android 7.0 or later (API 24+).

## Build the debug APK

From the project root:

```bash
./gradlew :app:assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build the release APK

Release builds require a signing keystore. Do not commit the keystore or its passwords.

Set these environment variables before building:

```bash
export KEYSTORE_PATH=/absolute/path/to/my-upload-key.jks
export STORE_PASSWORD='your-keystore-password'
export KEY_PASSWORD='your-key-password'
./gradlew :app:assembleRelease
```

The release APK is written to:

```text
app/build/outputs/apk/release/app-release.apk
```

## Cloudflare Worker

The relay Worker source is in [`relay-worker/`](relay-worker/). The deployed Worker uses the Cloudflare Workers subdomain `glayph-phonedeck.workers.dev` and the Worker name `phonedeck-relay`.

The Worker requires a Durable Object binding named `PHONEDECK_TUNNEL`. Deploy it with Wrangler after authenticating in your own browser:

```bash
cd relay-worker
npx wrangler login
npx wrangler deploy
```

The API token used for deployment must never be placed in this repository or inside the APK. The Android app only contains the public Worker URL. Each installed device generates and securely stores its own device ID and tunnel secret.

## Stable device URLs

After the app connects to the relay, its dashboard URL is:

```text
https://phonedeck-relay.glayph-phonedeck.workers.dev/d/<device-id>/
```

The URL remains stable across app restarts, reconnects, and Wi-Fi/mobile-data changes. It changes if Android app data is cleared or the app is uninstalled and reinstalled, because a new device identity is generated.

## Installation and first use

1. Install the APK on the Android device.
2. Open PhoneDeck and grant the permissions requested by the features you want to use.
3. Enable the PhoneDeck/Remote Access switch.
4. Wait for the relay status to show **Connected**.
5. Open the displayed public URL in a browser.
6. Complete the first-time account registration and OTP confirmation.

## Security notes

The Android device remains the authoritative server and authentication host. The Cloudflare Worker forwards requests to the device but does not contain the device passwords or tunnel secrets. Do not commit API tokens, keystores, passwords, `local.properties`, or generated build output.
