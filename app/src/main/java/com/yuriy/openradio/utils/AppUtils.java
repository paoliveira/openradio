package com.yuriy.openradio.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 11/29/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * {@link AppUtils} is a helper class which holds various help-methods
 */
public final class AppUtils {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = AppUtils.class.getSimpleName();

    /**
     * Private constructor
     */
    private AppUtils() {}

    /**
     * Read resource file as bytes array.
     *
     * @param id      Identifier of the resource.
     * @param context Application context.
     * @return Bytes array associated with a resource
     */
    public static byte[] getResource(final int id, final Context context) {
        final Resources resources = context.getResources();
        final InputStream is = resources.openRawResource(id);
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final byte[] readBuffer = new byte[4 * 1024];

        try {
            int read;
            do {
                read = is.read(readBuffer, 0, readBuffer.length);
                if(read == -1) {
                    break;
                }
                bout.write(readBuffer, 0, read);
            } while(true);

            return bout.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                /* Ignore */
            }
        }
        return new byte[0];
    }

    /**
     * Get application's version
     *
     * @param context Application context.
     * @return Application Version
     */
    public static String getApplicationVersion(final Context context) {
        final PackageInfo packageInfo = getPackageInfo(context);
        if (packageInfo != null) {
            return packageInfo.versionName;
        } else {
            Log.w(CLASS_NAME, "Can't get application version");
            return "?";
        }
    }

    /**
     * @return PackageInfo for the current application or null if the PackageManager could not be
     * contacted.
     */
    private static PackageInfo getPackageInfo(final Context context) {
        final PackageManager packageManager = context.getPackageManager();
        if (packageManager == null) {
            Log.w(CLASS_NAME, "Package manager is NULL");
            return null;
        }
        String packageName = "";
        try {
            packageName = context.getPackageName();
            return packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(CLASS_NAME, "Failed to find PackageInfo : " + packageName);
            return null;
        } catch (RuntimeException e) {
            // To catch RuntimeException("Package manager has died") that can occur on some
            // version of Android,
            // when the remote PackageManager is unavailable. I suspect this sometimes occurs
            // when the App is being reinstalled.
            Log.e(CLASS_NAME, "Package manager has died : " + packageName);
            return null;
        } catch (Throwable e) {
            Log.e(CLASS_NAME, "Package manager has Throwable : " + e);
            return null;
        }
    }
}
