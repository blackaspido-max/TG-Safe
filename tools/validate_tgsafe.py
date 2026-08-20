#!/usr/bin/env python3
"""Static TG Safe safety-contract validation.

This intentionally checks the exact upstream source anchors used by
`tgsafe.gradle`. If Telegram changes one of those anchors, validation fails and
we review the upstream change before producing another trusted Safe build.
"""

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def read(rel: str) -> str:
    path = ROOT / rel
    if not path.is_file():
        raise AssertionError(f"missing file: {rel}")
    return path.read_text(encoding="utf-8").replace("\r\n", "\n").replace("\r", "\n")


def require(rel: str, needle: str) -> None:
    text = read(rel)
    if needle not in text:
        raise AssertionError(f"required anchor missing in {rel}: {needle}")


def require_one(rel: str, needle: str) -> None:
    text = read(rel)
    count = text.count(needle)
    if count != 1:
        raise AssertionError(f"expected exactly one anchor in {rel}, got {count}: {needle}")


def forbid(rel: str, needle: str) -> None:
    text = read(rel)
    if needle in text:
        raise AssertionError(f"forbidden text present in {rel}: {needle}")


def main() -> int:
    enter = "TMessagesProj/src/main/java/org/telegram/ui/Components/ChatActivityEnterView.java"
    send = "TMessagesProj/src/main/java/org/telegram/messenger/SendMessagesHelper.java"
    loc = "TMessagesProj/src/main/java/org/telegram/messenger/LocationController.java"
    cam = "TMessagesProj/src/main/java/org/telegram/messenger/camera/CameraController.java"
    voip = "TMessagesProj/src/main/java/org/telegram/messenger/voip/VoIPService.java"
    pre = "TMessagesProj/src/main/java/org/telegram/messenger/voip/VoIPPreNotificationService.java"

    # Exact source anchors consumed by tgsafe.gradle.
    require_one(enter, "private Runnable recordAudioVideoRunnable = new Runnable() {\n        @Override\n        public void run() {")
    require_one(enter, "private Runnable onFinishInitCameraRunnable = new Runnable() {\n        @Override\n        public void run() {")

    require_one(send, "public void sendCurrentLocation(final MessageObject messageObject, final TLRPC.KeyboardButton button) {")
    require_one(send, "private void sendLocation(Location location) {")
    require_one(send, "public void sendMessage(SendMessageParams sendMessageParams) {")

    for anchor in (
        "public void startFusedLocationRequest(boolean permissionsGranted) {",
        "private void broadcastLastKnownLocation(boolean cancelCurrent) {",
        "protected void addSharingLocation(TLRPC.Message message) {",
        "private void loadSharingLocations() {",
        "private void startService() {",
        "public void setMapLocation(Location location, boolean first) {",
        "private void start() {",
    ):
        require_one(loc, anchor)

    require_one(cam, "public boolean takePicture(final File path, final boolean ignoreOrientation, final Object sessionObject, final Utilities.Callback<Integer> callback) {")
    require_one(cam, "public void recordVideo(final Object sessionObject, final File path, boolean mirror, final VideoTakeCallback callback, final Runnable onVideoStartRecord, ICameraView cameraView, boolean createThumbnail) {")

    require_one(voip, "public void acceptIncomingCall() {")
    require_one(pre, "public static void answer(Context context) {")

    # Central policy and build wiring.
    require("TMessagesProj/src/main/java/org/telegram/messenger/SafeMode.java", "public static final boolean ENABLED = true;")
    require("TMessagesProj/src/main/java/org/telegram/messenger/SafeMode.java", "blockLocationSharing()")
    require("build.gradle", "apply from: 'tgsafe.gradle'")
    require("gradle.properties", "APP_PACKAGE=org.telegram.messenger.tgsafe")
    require("TMessagesProj_App/src/main/res/values/tgsafe.xml", "TG Safe 🛡️")

    # App-level hard rail must remove capture/location permissions.
    main_manifest = "TMessagesProj_App/src/main/AndroidManifest.xml"
    for permission in (
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.ACCESS_MEDIA_LOCATION",
    ):
        require(main_manifest, f'android:name="{permission}" tools:node="remove"')

    # afatDebug uses this higher-priority manifest. It must not re-add actual
    # device-location permissions after main-manifest removal markers.
    debug_manifest = "TMessagesProj/config/debug/AndroidManifest_SDK23.xml"
    for permission in (
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_MEDIA_LOCATION",
        "android.permission.FOREGROUND_SERVICE_LOCATION",
    ):
        forbid(debug_manifest, f'android:name="{permission}"')

    # Dummy google-services config must at least match the local Safe package
    # so the Google Services Gradle plugin can generate resources for debug.
    require("TMessagesProj_App/google-services.json", '"package_name": "org.telegram.messenger.tgsafe.beta"')

    print("TG Safe static validation: PASS")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"TG Safe static validation: FAIL: {exc}", file=sys.stderr)
        raise SystemExit(1)
