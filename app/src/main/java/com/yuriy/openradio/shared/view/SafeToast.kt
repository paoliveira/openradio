package com.yuriy.openradio.shared.view

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils

/**
 * @author Yurii Chernyshov
 * @version 1.0
 * @since 2016-01-29
 *
 * Helper class to provide ability to display [Toast] from any Thread.
 */
object SafeToast {
    /**
     * Show [Toast]. This method can be invoked from any Thread.
     *
     * @param context Context of the Application.
     * @param text    Message to display in the [Toast].
     */
    @JvmStatic
    fun showAnyThread(context: Context?, text: CharSequence) {
        if (AppUtils.isUiThread()) {
            // We are already in UI thread, it's safe to show Toast
            showToastUIThread(context, text)
        } else {
            val handler = Handler(Looper.getMainLooper())
            // We are NOT in UI thread, so scheduling task in handler
            handler.post {
                // Show Toast
                showToastUIThread(context, text)
            }
        }
    }

    /**
     * Show [Toast] in the UI Thread.
     *
     * @param context Context of the Application.
     * @param text    Message to display in the [Toast].
     */
    private fun showToastUIThread(context: Context?, text: CharSequence) {
        if (context == null) {
            AppLogger.w("Can not display Toast, Context is null")
            return
        }
        if (text.isEmpty()) {
            AppLogger.w("Can not display Toast, Text is null or empty")
            return
        }
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        AppLogger.i("Toast:$text")
    }
}
