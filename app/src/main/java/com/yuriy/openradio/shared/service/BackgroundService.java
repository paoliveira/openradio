package com.yuriy.openradio.shared.service;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.yuriy.openradio.shared.utils.AppLogger;

public final class BackgroundService extends JobIntentService {

    private static final String CLASS_NAME = BackgroundService.class.getSimpleName() + " ";
    private static final int JOB_ID = 2000;
    private static final String KEY_CMD = "KEY_COMMAND";
    private static final String KEY_CMD_NAME_STOP_ORS = "KEY_CMD_NAME_STOP_ORS";

    public BackgroundService() {
        super();
    }

    /**
     * Factory method to make the desired Intent.
     */
    private static Intent makeIntent(final Context context) {
        // Create an intent associated with the Background Service class.
        return new Intent(context, BackgroundService.class);
    }

    /**
     * Factory method to create intent to stop {@link OpenRadioService}.
     *
     * @param context Context of callee.
     */
    public static void makeStopServiceIntent(final Context context) {
        // Create an Intent to start job in the background via a Service.
        final Intent intent = makeIntent(context);
        intent.putExtra(KEY_CMD, KEY_CMD_NAME_STOP_ORS);
        enqueueWork(context, BackgroundService.class, JOB_ID, intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLogger.d(CLASS_NAME + "on create");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AppLogger.d(CLASS_NAME + "on destroy");
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        AppLogger.d(CLASS_NAME + "on handle work");
        if (!intent.hasExtra(KEY_CMD)) {
            return;
        }
        switch (intent.getStringExtra(KEY_CMD)) {
            case KEY_CMD_NAME_STOP_ORS:
                startService(OpenRadioService.makeStopServiceIntent(getApplicationContext()));
                break;
        }
    }
}
