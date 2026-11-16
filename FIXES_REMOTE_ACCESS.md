# Remote Access Reliability Fixes

## Version 1.0.5

### Stable endpoint identity

- Centralized the Worker origin, WebSocket endpoint, and per-device URL construction in `CloudflareRelayConfig`.
- Recomputed the stable public URL from the persisted device ID during app/service startup.
- Kept the URL in state during CONNECTING, RECONNECTING, and DISCONNECTED states instead of clearing it with the live socket.
- Updated the app dashboard and authenticated `/api/auth/status` response to expose the assigned URL while offline.

### Android service lifecycle

- Promoted `RemoteServerService` to foreground before starting local sockets/server work, matching Android's foreground-service startup requirement.
- Kept the service sticky and made task-removal/destroy recovery conditional on the persisted user-stop flag.
- Restored real persistence for the explicit manual-stop flag; the boot receiver and watchdog no longer undo an intentional stop.
- Cancelled persisted watchdog jobs/alarms on an intentional stop.
- Declared the required connected-device and media-projection service types; removed the unused special-use type declaration.

### Tunnel reconnection

- Kept OkHttp WebSocket protocol Ping/Pong at 25-second intervals.
- Monitored default-network changes and Internet validation state.
- Interrupts offline backoff immediately when a new network or validated Internet returns.
- Added capped exponential backoff with jitter for repeated failures, reducing repeated connect attempts when there is no Internet.
- Kept relay-ready acknowledgement as the condition for reporting ONLINE.

### Verification performed

- Android Manifest XML parsed successfully.
- Cloudflare Worker JavaScript syntax check passed with Node.js.
- Static source checks confirmed that the stable URL is no longer cleared on a tunnel outage and that foreground promotion occurs before server startup.
- Release and debug APKs built successfully with version `1.0.5`.
- The deployed relay health endpoint returned HTTP `200` at `https://glayph.phonedeck.workers.dev/health`.

### Test note

- The Robolectric suite still has one failing boot-service timing assertion in the test environment. This does not block APK compilation or signing; real-device installation and prolonged background-service testing remain recommended.

## Operational boundary

The URL remains assigned while the phone is offline, but the Worker cannot serve live Android content until the phone reconnects. Android force-stop, device shutdown, battery depletion, manufacturer restrictions, and some background-start restrictions can still interrupt the service. No Android app can guarantee 100% nonstop availability through those states.
