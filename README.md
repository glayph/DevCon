# PhoneDeck

PhoneDeck is an Android device-management console. It provides an on-device control surface and an optional local web interface for managing a phone that is owned or administered by the person using it.

> [!IMPORTANT]
> ## Consent and privacy notice
>
> PhoneDeck must only be installed and used with the device owner's **clear, informed, and revocable consent**. Do not use it to secretly monitor a girlfriend, boyfriend, family member, employee, or any other person. Remote access, screen casting, notifications, files, location, contacts, SMS, call information, app usage, microphone, camera, and other sensitive capabilities must be enabled only when the device owner understands and approves the specific access. Follow applicable law and organizational policy.

## What it does

Depending on the permissions explicitly granted on the managed device, PhoneDeck can provide remote administration features such as:

- Device status and connectivity information
- Permission and service status management
- App management and installation workflows
- Notification and event visibility
- File and media access
- Optional live screen casting
- Device commands and action logs

These capabilities are sensitive. The app's visible indicators, Android permission dialogs, notification surfaces, and access logs should remain enabled so the device owner can see and revoke access at any time.

## Build locally

**Prerequisites:** Android Studio or a compatible JDK, Android SDK Platform 36.1, and the Android SDK Build Tools.

```bash
./gradlew assembleDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Configuration

1. Open the project in Android Studio, or use the included Gradle wrapper.
2. Create a local `.env` file only if your deployment requires the variables documented in `.env.example`.
3. Do not commit secrets, API keys, signing keys, or private endpoint credentials.
4. Install the APK only on a device whose owner has approved the installation and every requested permission.

## Security guidance

- Keep the web interface bound to a trusted network and protect it with authentication.
- Do not expose the management port directly to the public internet.
- Review action logs and revoke permissions that are no longer needed.
- Do not attempt to conceal the app from the device owner or bypass Android security warnings such as Google Play Protect.
- For organizations, use a documented, consent-based mobile-device-management policy.

## License

Add the project's intended license before distributing or deploying PhoneDeck publicly.
