# PhoneDeck Remote Access & Registration Guide

## Zero-Account Public Tunnel (Any Network)

- **Android Device = Server**: `HttpBridgeServer` runs directly on your Android phone inside `RemoteServerService` on port `8080`.
- **Zero Cloudflare / Account Setup**: Anyone installing PhoneDeck automatically connects over an outbound pure-JVM SSH reverse tunnel (`free.pinggy.io` over port `443`, with `localhost.run` fallback) to receive a live public `https://` URL across any Wi-Fi or mobile data network.
- **Single Switch**: Turn on the **PhoneDeck** switch in the top bar to start both the Localhost server and Public tunnel together.
- **Reconnects Automatically**: Handles Wi-Fi ↔ mobile data transitions and anonymous tunnel session refreshes automatically in the background.

## First-Time Desktop Sign Up & Login

1. Open either your live **Public** `https://` URL or `http://localhost:8080` in a desktop browser.
2. On **Sign up** (`1 / 2`), enter **User**, **Password**, and **Confirm Password**, then tap **Continue**.
3. PhoneDeck sends a **one-time 4-digit OTP notification** directly to the Android device's OS notification shade (never shown inside the PhoneDeck app UI or mirrored over notifications).
4. Enter that 4-digit **OTP** on Page `2 / 2` and tap **Confirm** to finalize your account.
5. Future browser sessions sign in once with **User + Password** and stay authenticated via an HMAC-signed persistent cookie.
