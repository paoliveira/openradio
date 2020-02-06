package com.yuriy.openradio.shared.view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.AppUtils;

/**
 * @author Yurii Chernyshov
 * @version 1.0
 * @since 2016-01-29
 *
 * Helper class to provide ability to display {@link Toast} from any Thread.
 */
public final class SafeToast {

    /**
     * default constructor.
     */
    private SafeToast() {
        super();
    }

    /**
     * Show {@link Toast}. This method can be invoked from any Thread.
     *
     * @param context Context of the Application.
     * @param text    Message to display in the {@link Toast}.
     */
    public static void showAnyThread(final Context context, final CharSequence text) {
        if (AppUtils.isUiThread()) {
            // We are already in UI thread, it's safe to show Toast
            showToastUIThread(context, text);
        } else {
            final Handler handler = new Handler(Looper.getMainLooper());
            // We are NOT in UI thread, so scheduling task in handler
            handler.post(() -> {
                // Show Toast
                showToastUIThread(context, text);
            });
        }
    }

    /**
     * Show {@link Toast} in the UI Thread.
     *
     * @param context Context of the Application.
     * @param text    Message to display in the {@link Toast}.
     */
    private static void showToastUIThread(Context context, final CharSequence text) {
        if (context == null) {
            AppLogger.w("Can not display Toast, Context is null");
            return;
        }
        if (TextUtils.isEmpty(text)) {
            AppLogger.w("Can not display Toast, Text is null or empty");
            return;
        }
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
        AppLogger.i("Toast:" + text.toString());
    }
}
