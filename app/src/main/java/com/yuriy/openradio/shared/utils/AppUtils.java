/*
 * Copyright 2017 The "Open Radio" Project. Author: Chernyshov Yuriy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yuriy.openradio.shared.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.exoplayer2.util.Util;
import com.yuriy.openradio.shared.vo.RadioStation;

import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 11/29/14
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * {@link AppUtils} is a helper class which holds various help-methods
 */
public final class AppUtils {

    /**
     * Time out for the stream to decide whether there is response or not, ms.
     */
    public static final int TIME_OUT = 2000;

    public static final String UTF8 = "UTF-8";
    public static final String DRAWABLE_PATH = "android.resource://com.yuriy.openradio/drawable/";

    private static final String[] ANDROID_AUTO_PACKAGE_NAMES = new String[]{
            "com.google.android.projection.gearhead",
            "com.android.car"
    };

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = AppUtils.class.getSimpleName();

    /**
     * Private constructor.
     */
    private AppUtils() {
        super();
    }

    public static String listOfRadioStationsToString(final List<RadioStation> list) {
        if (list == null) {
            return "List is null";
        }
        if (list.isEmpty()) {
            return "List is empty";
        }
        final StringBuilder builder = new StringBuilder("{");
        for (final RadioStation station : list) {
            builder.append(station.toString()).append(",");
        }
        builder.append("}");
        return builder.toString();
    }

    /**
     * Get application's version name.
     *
     * @param context Application context.
     * @return Application Version name.
     */
    public static String getApplicationVersion(final Context context) {
        final PackageInfo packageInfo = getPackageInfo(context);
        if (packageInfo != null) {
            return packageInfo.versionName;
        } else {
            AppLogger.w(CLASS_NAME + " Can't get application version");
            return "?";
        }
    }

    /**
     * Get application's version code.
     *
     * @param context Application context.
     * @return Application Version code.
     */
    public static int getApplicationCode(final Context context) {
        final PackageInfo packageInfo = getPackageInfo(context);
        if (packageInfo != null) {
            return packageInfo.versionCode;
        } else {
            AppLogger.w(CLASS_NAME + " Can't get code version");
            return 0;
        }
    }

    /**
     * @return PackageInfo for the current application or null if the PackageManager could not be
     * contacted.
     */
    private static PackageInfo getPackageInfo(final Context context) {
        final PackageManager packageManager = context.getPackageManager();
        if (packageManager == null) {
            AppLogger.w(CLASS_NAME + " Package manager is NULL");
            return null;
        }
        String packageName;
        try {
            packageName = context.getPackageName();
            return packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            AnalyticsUtils.logException(e);
            return null;
        } catch (RuntimeException e) {
            // To catch RuntimeException("Package manager has died") that can occur on some
            // version of Android,
            // when the remote PackageManager is unavailable. I suspect this sometimes occurs
            // when the App is being reinstalled.
            AnalyticsUtils.logException(e);
            return null;
        } catch (Throwable e) {
            AppLogger.e(CLASS_NAME + " Package manager has Throwable : " + e);
            return null;
        }
    }

    /**
     * This is a helper method with allows to prevent get a list of the predefined categories,
     * in order to do not show an empty category.
     *
     * @return Collection of the categories.
     */
    public static Set<String> predefinedCategories() {
        final Set<String> predefinedCategories = new TreeSet<>();
        predefinedCategories.add("Classical");
        predefinedCategories.add("Country");
        predefinedCategories.add("Decades");
        predefinedCategories.add("Electronic");
        predefinedCategories.add("Folk");
        predefinedCategories.add("International");
        predefinedCategories.add("Jazz");
        predefinedCategories.add("Misc");
        predefinedCategories.add("Pop");
        predefinedCategories.add("R&B/Urban");
        predefinedCategories.add("Rap");
        predefinedCategories.add("Reggae");
        predefinedCategories.add("Rock");
        predefinedCategories.add("Talk & Speech");
        return predefinedCategories;
    }

    static String generateRandomHexToken(final int byteLength) {
        final SecureRandom secureRandom = new SecureRandom();
        final byte[] token = new byte[byteLength];
        secureRandom.nextBytes(token);
        return new BigInteger(1, token).toString(16);
    }

    public static String getDefaultUserAgent(@NonNull final Context context) {
        return Util.getUserAgent(context, "OpenRadio");
    }

    /**
     * Return {@link File} object legal to call on API 8.
     *
     * @param type    The type of files directory to return. May be null for the root of the
     *                files directory or one of the following Environment constants for a subdirectory:
     *                DIRECTORY_MUSIC, DIRECTORY_PODCASTS, DIRECTORY_RINGTONES, DIRECTORY_ALARMS,
     *                DIRECTORY_NOTIFICATIONS, DIRECTORY_PICTURES, or DIRECTORY_MOVIES.
     * @param context Context of the callee.
     * @return {@link File} object.
     */
    @TargetApi(8)
    private static File getExternalFilesDirAPI8(final Context context, final String type) {
        return context.getExternalFilesDir(type);
    }

    static String getExternalStorageDir(final Context context) {
        final File externalDir = getExternalFilesDirAPI8(context, null);
        return externalDir != null ? externalDir.getAbsolutePath() : null;
    }

    static int getLongestScreenSize(FragmentActivity context) {
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;
        return height > width ? height : width;
    }

    /**
     * @param context
     * @return
     */
    public static int getShortestScreenSize(final Activity context) {
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;
        return height < width ? height : width;
    }

    static boolean externalStorageAvailable() {
        boolean externalStorageAvailable;
        boolean externalStorageWriteable;
        final String state = Environment.getExternalStorageState();
        switch (state) {
            case Environment.MEDIA_MOUNTED:
                // We can read and write the media
                externalStorageAvailable = externalStorageWriteable = true;
                break;
            case Environment.MEDIA_MOUNTED_READ_ONLY:
                // We can only read the media
                externalStorageAvailable = true;
                externalStorageWriteable = false;
                break;
            default:
                // Something else is wrong. It may be one of many other states, but all we need
                //  to know is we can neither read nor write
                externalStorageAvailable = externalStorageWriteable = false;
                break;
        }
        return externalStorageAvailable && externalStorageWriteable;
    }

    public static String getApplicationVersionName(final Context context) {
        final PackageInfo packageInfo = getPackageInfo(context);
        if (packageInfo != null) {
            return packageInfo.versionName;
        } else {
            AppLogger.w("Can't get application version");
            return "?";
        }
    }

    public static int getApplicationVersionCode(final Context context) {
        final PackageInfo packageInfo = getPackageInfo(context);
        if (packageInfo != null) {
            return packageInfo.versionCode;
        } else {
            AppLogger.w("Can't get application code");
            return 0;
        }
    }

    /**
     * Get ISO 3166-1 alpha-2 country code for this device (or null if not available).
     *
     * @param context Context reference to get the TelephonyManager instance from.
     * @return country code or null
     */
    public static String getUserCountry(final Context context) {
        try {
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(
                    Context.TELEPHONY_SERVICE
            );
            final String simCountry = tm.getSimCountryIso();
            if (simCountry != null && simCountry.length() == 2) {
                // SIM country code is available
                return simCountry.toLowerCase(Locale.US);
            } else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
                // device is not 3G (would be unreliable)
                final String networkCountry = tm.getNetworkCountryIso();
                if (networkCountry != null && networkCountry.length() == 2) {
                    // network country code is available
                    return networkCountry.toLowerCase(Locale.US);
                }
            }
        } catch (final Exception e) {
            AnalyticsUtils.logException(e);
        }
        return null;
    }

    public static boolean isWebUrl(final String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        return url.toLowerCase().startsWith("www") || url.toLowerCase().startsWith("http");
    }

    static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static boolean hasVersionM() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * See Apache utils for more details.
     * https://github.com/apache/commons-lang/blob/master/src/main/java/org/apache/commons/lang3/text/WordUtils.java
     */
    public static String capitalize(final String str) {
        return capitalize(str, (char[]) null);
    }

    /**
     * See Apache utils for more details.
     * https://github.com/apache/commons-lang/blob/master/src/main/java/org/apache/commons/lang3/text/WordUtils.java
     */
    private static String capitalize(final String str, final char... delimiters) {
        final int delimLen = delimiters == null ? -1 : delimiters.length;
        if (TextUtils.isEmpty(str) || delimLen == 0) {
            return str;
        }
        final char[] buffer = str.toCharArray();
        boolean capitalizeNext = true;
        for (int i = 0; i < buffer.length; i++) {
            final char ch = buffer[i];
            if (isDelimiter(ch, delimiters)) {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer[i] = Character.toTitleCase(ch);
                capitalizeNext = false;
            }
        }
        return new String(buffer);
    }

    /**
     * See Apache utils for more details.
     * https://github.com/apache/commons-lang/blob/master/src/main/java/org/apache/commons/lang3/text/WordUtils.java
     */
    private static boolean isDelimiter(final char ch, final char[] delimiters) {
        if (delimiters == null) {
            return Character.isWhitespace(ch);
        }
        for (final char delimiter : delimiters) {
            if (ch == delimiter) {
                return true;
            }
        }
        return false;
    }

    /**
     * Holder for the Search query. Up to now I found it as quick solution to pass query
     * from Activity to the Open Radio Service.
     */
    private static StringBuilder sSearchQuery = new StringBuilder();

    /**
     * Save Search query string.
     *
     * @param searchQuery Search query string.
     */
    public static void setSearchQuery(final String searchQuery) {
        sSearchQuery.setLength(0);
        sSearchQuery.append(searchQuery);
    }

    /**
     * @return Gets the Search query string.
     */
    public static String getSearchQuery() {
        return sSearchQuery.toString();
    }

    /**
     * Checks whether or not application runs on Automotive environment.
     *
     * @param clientPackageName The package name of the application which is
     *                          requesting access.
     * @return {@code true} in case of success, {@code false} otherwise.
     */
    public static boolean isAutomotive(@NonNull final String clientPackageName) {
        for (final String pkg : ANDROID_AUTO_PACKAGE_NAMES) {
            if (clientPackageName.contains(pkg)) {
                return true;
            }
        }
        return false;
    }

    public static String[] getDensity(@NonNull final Context context) {
        final int densityDpi = context.getResources().getDisplayMetrics().densityDpi;
        final String value;
        switch (densityDpi) {
            case DisplayMetrics.DENSITY_LOW:
                // LDPI
                value = "LDPI";
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                // MDPI
                value = "MDPI";
                break;
            case DisplayMetrics.DENSITY_TV:
            case DisplayMetrics.DENSITY_HIGH:
                // HDPI
                value = "HDPI";
                break;
            case DisplayMetrics.DENSITY_XHIGH:
            case DisplayMetrics.DENSITY_280:
                // XHDPI
                value = "XHDPI";
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
            case DisplayMetrics.DENSITY_360:
            case DisplayMetrics.DENSITY_400:
            case DisplayMetrics.DENSITY_420:
                // XXHDPI
                value = "XXHDPI";
                break;
            case DisplayMetrics.DENSITY_XXXHIGH:
            case DisplayMetrics.DENSITY_560:
                // XXXHDPI
                value = "XXXHDPI";
                break;
            default:
                value = "UNKNOWN";
        }
        return new String[]{String.valueOf(densityDpi), value};
    }

    public static String intentBundleToString(@Nullable final Intent intent) {
        if (intent == null) {
            return "Intent[null]";
        }
        final Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return "Bundle[null]";
        }
        final StringBuilder builder = new StringBuilder("Bundle[");
        try {
            for (final String key : bundle.keySet()) {
                builder.append(key).append(":").append((bundle.get(key) != null ? bundle.get(key) : "NULL"));
                builder.append("|");
            }
            builder.delete(builder.length() - 1, builder.length());
        } catch (final Exception e) {
            AppLogger.e(CLASS_NAME + " bundle to string exception:" + e);
        }
        builder.append("]");
        return builder.toString();
    }
}
