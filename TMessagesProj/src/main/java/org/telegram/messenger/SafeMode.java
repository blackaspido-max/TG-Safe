/*
 * TG Safe safety policy.
 *
 * This class is intentionally fail-closed. Dangerous actions are disabled in
 * the TG Safe build even if an upstream Telegram UI path still exposes a
 * button after an update.
 */
package org.telegram.messenger;

import android.content.Context;
import android.widget.Toast;

import org.telegram.tgnet.TLRPC;

public final class SafeMode {

    public static final boolean ENABLED = true;

    private SafeMode() {
    }

    public static boolean blockVoiceMessageRecording() {
        return ENABLED;
    }

    public static boolean blockVideoMessageRecording() {
        return ENABLED;
    }

    public static boolean blockVoiceOrVideoMessage(boolean video) {
        return video ? blockVideoMessageRecording() : blockVoiceMessageRecording();
    }

    public static boolean blockCameraCapture() {
        return ENABLED;
    }

    public static boolean blockIncomingCallAnswer() {
        return ENABLED;
    }

    public static boolean blockLocationSharing() {
        return ENABLED;
    }

    /**
     * Chat/message media types which disclose a geographic point. This is a
     * second line of defence behind the location UI guards.
     */
    public static boolean isLocationMedia(TLRPC.MessageMedia media) {
        return media instanceof TLRPC.TL_messageMediaGeo
                || media instanceof TLRPC.TL_messageMediaGeoLive
                || media instanceof TLRPC.TL_messageMediaVenue;
    }

    public static void showBlocked(Context context, String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
}
