# PhoneDeck Remote Access Setup

## Architecture

The Android device runs the PhoneDeck HTTP bridge locally on port `8080`. When PhoneDeck is enabled, the app opens an outbound secure WebSocket connection to the deployed Cloudflare Worker:

```text
wss://phonedeck-relay.glayph-phonedeck.workers.dev/ws/device
```

The Worker uses the device ID to route browser requests to the correct device through an isolated Durable Object.

The Android device remains the authoritative host for the dashboard, authentication, passwords, OTP verification, and device operations. The Worker only terminates HTTPS and forwards request/response envelopes over the WebSocket tunnel.

## Cloudflare configuration

- Account ID: `09bb87bf827bd3bcc5097dd2eaa80aeb`
- Workers subdomain: `glayph-phonedeck.workers.dev`
- Worker name: `phonedeck-relay`
- Health endpoint: `https://phonedeck-relay.glayph-phonedeck.workers.dev/health`
- Durable Object binding: `PHONEDECK_TUNNEL`

The relay source and Durable Object configuration are in [`relay-worker/`](relay-worker/). API tokens are deployment credentials only. They must not be stored in source code, the APK, or this documentation.

## Stable URL for each device

Each installation generates a device ID in the form:

```text
pd_<hex-id>
```

The Android app stores the device ID and tunnel secret in encrypted Android secure storage. Once connected, the dashboard URL is:

```text
https://phonedeck-relay.glayph-phonedeck.workers.dev/d/<device-id>/
```

For example:

```text
https://phonedeck-relay.glayph-phonedeck.workers.dev/d/pd_a1b2c3d4e5/
```

The URL remains the same after app restarts, tunnel reconnects, and Wi-Fi/mobile-data changes. It changes when Android app data is cleared or the app is uninstalled and reinstalled, because the app then generates a new device identity.

## Enable remote access

1. Install the Cloudflare-enabled PhoneDeck APK.
2. Open PhoneDeck on the Android device.
3. Grant the permissions required by the features you want to use.
4. Turn on the PhoneDeck/Remote Access switch.
5. Wait for the status to show **Connected**.
6. Copy the public URL shown in the dashboard or open it from the app.

If the phone is offline, the Worker shows an offline response. When the app reconnects, the same device URL becomes available again.

## First-time browser registration

1. Open the device URL in a browser.
2. On the **Sign up** page, enter a user name, password, and password confirmation.
3. Select **Continue**.
4. PhoneDeck sends a one-time four-digit OTP to the Android device's notification shade.
5. Enter the OTP on the verification page and select **Confirm**.
6. Future browser sessions use the registered user name and password and receive an authenticated session cookie.

The OTP is delivered to the Android device's operating-system notification shade. It is not displayed inside the PhoneDeck dashboard or forwarded as a mirrored notification.

## Troubleshooting

- **Local access:** while the phone and browser are on the same network, try `http://<phone-ip>:8080`.
- **Relay connecting:** keep the PhoneDeck switch enabled and verify that the phone has Wi-Fi or mobile data access.
- **Offline page:** the Worker is reachable, but the Android WebSocket tunnel is not currently connected.
- **New URL after reinstall:** this is expected if app data was cleared or the app was uninstalled. The new installation has a new device ID.
