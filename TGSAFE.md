# TG Safe 🛡️

TG Safe is a separate Android Telegram fork intended to coexist with the normal Telegram app. The normal app remains the place for intentional recording/capture; TG Safe is the everyday fail-closed client.

## Blocked in TG Safe

- recording voice messages;
- recording round video messages;
- taking photos with Telegram's camera;
- recording video with Telegram's camera;
- accepting incoming Telegram audio/video calls;
- sending current location;
- sending a selected map point as location media;
- starting or continuing live location;
- Telegram background/live-location acquisition and broadcast.

## Still allowed

- text chats;
- received voice/video-message playback;
- received photos/videos;
- gallery attachments;
- files, stickers, GIFs and reactions;
- viewing locations sent by other people;
- outgoing calls are not intentionally disabled by the TG Safe policy.

## Defence in depth

TG Safe does not rely on hidden buttons alone.

1. `SafeMode.java` is the central fail-closed policy.
2. Build-time source guards stop voice/video-message recording, camera capture, incoming-call answering and location send/update paths.
3. The TG Safe application manifest removes Android coarse/fine/background location permissions as a second hard rail.
4. `tgsafe.gradle` is intentionally strict: if an upstream Telegram update changes a source anchor, the build fails instead of silently skipping a safety guard.

## Separate installation

The application ID is:

`org.telegram.messenger.tgsafe`

Debug builds use Android's normal `.beta` suffix, so they do not replace the official Telegram installation.

The visible application name is `TG Safe 🛡️`.

## Windows build

From the `tgsafe-core` branch run:

```powershell
powershell -ExecutionPolicy Bypass -File .\BUILD_TG_SAFE.ps1
```

The script updates submodules, applies/verifies all guards and builds `TMessagesProj_App:assembleAfatDebug`. On success it copies the resulting APK to:

`TG-Safe.apk`

## Upstream build requirements

Telegram's upstream README currently specifies Android Studio 2025.1.4, Android SDK 35 and Android NDK 27.2.12479018. The upstream repository also requires developers to use their own `api_id` and to replace the dummy signing/Firebase configuration before publishing an APK.

For a private local test build, the fork keeps upstream dummy build configuration except for package-name matching. Do not treat that configuration as production credentials.

## Safety acceptance test

Before relying on the APK, test all of these on a disposable/private chat/account setup:

- [ ] long-press the voice button: no voice recording begins;
- [ ] voice-record gestures do not start recording;
- [ ] switch to round video and hold: no round-video recording begins;
- [ ] Telegram camera cannot capture a photo;
- [ ] Telegram camera cannot start video recording;
- [ ] photo/video chosen from gallery can still be sent;
- [ ] incoming audio call cannot be answered from in-app UI;
- [ ] incoming audio call cannot be answered from Android notification UI;
- [ ] incoming video call cannot be answered;
- [ ] current location cannot be sent;
- [ ] a manually selected point cannot be sent as location media;
- [ ] live location cannot be started;
- [ ] existing live-location machinery cannot broadcast coordinates;
- [ ] bot location-request buttons cannot ultimately transmit location;
- [ ] locations received from other people can still be viewed;
- [ ] received voice messages and round videos still play;
- [ ] ordinary text/files/stickers/reactions still work.

Do not merge an upstream update into a trusted TG Safe APK until the build-time safety guard task passes again and this checklist has been rerun.
