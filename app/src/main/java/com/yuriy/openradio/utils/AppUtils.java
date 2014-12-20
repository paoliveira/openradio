package com.yuriy.openradio.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
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

    public static boolean hasFroyo() {
        // Can use static final constants like FROYO, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static int gettHeightScreenSize(FragmentActivity context) {
        // Fetch screen height and width, to use as our max size when loading images as this
        // activity runs full screen
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        return displayMetrics.heightPixels;
    }

    public static int getWidthScreenSize(FragmentActivity context) {
        // Fetch screen height and width, to use as our max size when loading images as this
        // activity runs full screen
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        return displayMetrics.widthPixels;
    }

    public static int getLongestScreenSize(FragmentActivity context) {
        // Fetch screen height and width, to use as our max size when loading images as this
        // activity runs full screen
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;

        // For this sample we'll use half of the longest width to resize our images. As the
        // image scaling ensures the image is larger than this, we should be left with a
        // resolution that is appropriate for both portrait and landscape. For best image quality
        // we shouldn't divide by 2, but this will use more memory and require a larger memory
        // cache.
        // TODO

        return height > width ? height : width;
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
