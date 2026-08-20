package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.SafeMode;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;

/** Persistent visual indicator that this is the fail-closed TG Safe client. */
public final class SafeModeBadge {

    private static TextView badgeView;

    private SafeModeBadge() {
    }

    public static void install(FrameLayout parent, Context context, int account) {
        if (!SafeMode.ENABLED || parent == null || context == null) {
            return;
        }
        if (badgeView != null && badgeView.getParent() == parent) {
            update(account);
            return;
        }

        TextView badge = new TextView(context);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        badge.setTextColor(Color.WHITE);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(AndroidUtilities.dp(9), 0, AndroidUtilities.dp(9), 0);
        badge.setClickable(false);
        badge.setFocusable(false);
        badge.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        GradientDrawable background = new GradientDrawable();
        background.setColor(0xD9000000);
        background.setCornerRadius(AndroidUtilities.dp(12));
        background.setStroke(AndroidUtilities.dp(1), 0x66FFFFFF);
        badge.setBackground(background);
        badge.setElevation(AndroidUtilities.dp(100));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                AndroidUtilities.dp(24),
                Gravity.TOP | Gravity.CENTER_HORIZONTAL
        );
        lp.topMargin = AndroidUtilities.statusBarHeight + AndroidUtilities.dp(4);
        parent.addView(badge, lp);
        badgeView = badge;
        update(account);
    }

    public static void update(int account) {
        if (badgeView == null) {
            return;
        }

        String accountLabel = "аккаунт " + (account + 1);
        try {
            TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
            if (user != null) {
                String username = UserObject.getPublicUsername(user);
                if (!TextUtils.isEmpty(username)) {
                    accountLabel = "@" + username;
                }
            }
        } catch (Throwable ignore) {
        }

        badgeView.setText("TG SAFE 🛡️  ·  " + accountLabel);
    }
}
