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
package com.yuriy.openradio.shared.utils

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import androidx.fragment.app.FragmentActivity
import com.google.android.exoplayer2.util.Util
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import com.yuriy.openradio.shared.utils.AnalyticsUtils.logException
import com.yuriy.openradio.shared.utils.AppLogger.e
import com.yuriy.openradio.shared.utils.AppLogger.i
import com.yuriy.openradio.shared.utils.AppLogger.w
import java.io.File
import java.math.BigInteger
import java.security.SecureRandom
import java.util.*

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 11/29/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *
 * [AppUtils] is a helper class which holds various help-methods
 */
object AppUtils {
    /**
     * Time out for the stream to decide whether there is response or not, ms.
     */
    const val TIME_OUT = 2000
    const val UTF8 = "UTF-8"

    private val ANDROID_AUTO_PACKAGE_NAMES = arrayOf(
            "com.google.android.projection.gearhead",
            "com.android.car"
    )

    /**
     * Tag string to use in logging message.
     */
    private val CLASS_NAME = AppUtils::class.java.simpleName

    /**
     * Whether or not device supports Location feature.
     *
     * @param context Context of the callee.
     * @return
     */
    @JvmStatic
    fun hasLocation(context: Context?): Boolean {
        if (context == null) {
            return false
        }
        val packageManager = context.packageManager ?: return false
        val result = packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION)
        i("Has Location:$result")
        return result
    }

    /**
     * Get application's version name.
     *
     * @param context Application context.
     * @return Application Version name.
     */
    @JvmStatic
    fun getApplicationVersion(context: Context): String {
        val packageInfo = getPackageInfo(context)
        return if (packageInfo != null) {
            packageInfo.versionName
        } else {
            w("$CLASS_NAME Can't get application version")
            "?"
        }
    }

    /**
     * @return PackageInfo for the current application or null if the PackageManager could not be
     * contacted.
     */
    private fun getPackageInfo(context: Context): PackageInfo? {
        val packageManager = context.packageManager
        if (packageManager == null) {
            w("$CLASS_NAME Package manager is NULL")
            return null
        }
        val packageName: String
        return try {
            packageName = context.packageName
            packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            logException(e)
            null
        } catch (e: RuntimeException) {
            // To catch RuntimeException("Package manager has died") that can occur on some
            // version of Android,
            // when the remote PackageManager is unavailable. I suspect this sometimes occurs
            // when the App is being reinstalled.
            logException(e)
            null
        } catch (e: Throwable) {
            e("$CLASS_NAME Package manager has Throwable : $e")
            null
        }
    }

    /**
     * This is a helper method with allows to prevent get a list of the predefined categories,
     * in order to do not show an empty category.
     *
     * @return Collection of the categories.
     */
    fun predefinedCategories(): Set<String> {
        val predefinedCategories: MutableSet<String> = TreeSet()
        predefinedCategories.add("Classical")
        predefinedCategories.add("Country")
        predefinedCategories.add("Decades")
        predefinedCategories.add("Electronic")
        predefinedCategories.add("Folk")
        predefinedCategories.add("International")
        predefinedCategories.add("Jazz")
        predefinedCategories.add("Misc")
        predefinedCategories.add("News")
        predefinedCategories.add("Pop")
        predefinedCategories.add("R&B/Urban")
        predefinedCategories.add("Rap")
        predefinedCategories.add("Reggae")
        predefinedCategories.add("Rock")
        predefinedCategories.add("Talk & Speech")
        predefinedCategories.add("University Radio")
        return predefinedCategories
    }

    @JvmStatic
    fun generateRandomHexToken(byteLength: Int): String {
        val secureRandom = SecureRandom()
        val token = ByteArray(byteLength)
        secureRandom.nextBytes(token)
        return BigInteger(1, token).toString(16)
    }

    /**
     * @return Persistent value of the User Agent.
     *
     * //TODO: Find a better way to handle this. This value is not changing often, need to cache it.
     */
    @JvmStatic
    fun getUserAgent(context: Context): String {
        val defaultValue = Util.getUserAgent(context, context.getString(R.string.app_name_user_agent))
        return if (AppPreferencesManager.isCustomUserAgent(context)) AppPreferencesManager.getCustomUserAgent(context, defaultValue) else defaultValue
    }

    /**
     * Return [File] object legal to call on API 8.
     *
     * @param type    The type of files directory to return. May be null for the root of the
     * files directory or one of the following Environment constants for a subdirectory:
     * DIRECTORY_MUSIC, DIRECTORY_PODCASTS, DIRECTORY_RINGTONES, DIRECTORY_ALARMS,
     * DIRECTORY_NOTIFICATIONS, DIRECTORY_PICTURES, or DIRECTORY_MOVIES.
     * @param context Context of the callee.
     * @return [File] object.
     */
    @TargetApi(8)
    private fun getExternalFilesDirAPI8(context: Context, type: String?): File? {
        return context.getExternalFilesDir(type)
    }

    fun getExternalStorageDir(context: Context): String? {
        val externalDir = getExternalFilesDirAPI8(context, null)
        return externalDir?.absolutePath
    }

    /**
     * @param context
     * @return
     */
    fun getShortestScreenSize(context: FragmentActivity): Int {
        val displayMetrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels
        return height.coerceAtMost(width)
    }

    fun externalStorageAvailable(): Boolean {
        val externalStorageAvailable: Boolean
        val externalStorageWriteable: Boolean
        when (Environment.getExternalStorageState()) {
            Environment.MEDIA_MOUNTED ->                 // We can read and write the media
            {
                externalStorageWriteable = true
                externalStorageAvailable = externalStorageWriteable
            }
            Environment.MEDIA_MOUNTED_READ_ONLY -> {
                // We can only read the media
                externalStorageAvailable = true
                externalStorageWriteable = false
            }
            else ->                 // Something else is wrong. It may be one of many other states, but all we need
                //  to know is we can neither read nor write
            {
                externalStorageWriteable = false
                externalStorageAvailable = externalStorageWriteable
            }
        }
        return externalStorageAvailable && externalStorageWriteable
    }

    @JvmStatic
    fun getApplicationVersionName(context: Context): String {
        val packageInfo = getPackageInfo(context)
        return if (packageInfo != null) {
            packageInfo.versionName
        } else {
            w("Can't get application version")
            "?"
        }
    }

    @JvmStatic
    fun getApplicationVersionCode(context: Context): Int {
        val packageInfo = getPackageInfo(context)
        return if (packageInfo != null) {
            packageInfo.versionCode
        } else {
            w("Can't get application code")
            0
        }
    }

    /**
     * Get ISO 3166-1 alpha-2 country code for this device (or null if not available).
     *
     * @param context Context reference to get the TelephonyManager instance from.
     * @return country code or null
     */
    @JvmStatic
    fun getUserCountry(context: Context): String? {
        try {
            val tm = context.getSystemService(
                    Context.TELEPHONY_SERVICE
            ) as TelephonyManager
            val simCountry = tm.simCountryIso
            if (simCountry != null && simCountry.length == 2) {
                // SIM country code is available
                return simCountry.toLowerCase(Locale.US)
            } else if (tm.phoneType != TelephonyManager.PHONE_TYPE_CDMA) {
                // device is not 3G (would be unreliable)
                val networkCountry = tm.networkCountryIso
                if (networkCountry != null && networkCountry.length == 2) {
                    // network country code is available
                    return networkCountry.toLowerCase(Locale.US)
                }
            }
        } catch (e: Exception) {
            logException(e)
        }
        return null
    }

    @JvmStatic
    fun isWebUrl(url: String): Boolean {
        return if (url.isEmpty()) {
            false
        } else url.toLowerCase(Locale.ROOT).startsWith("www")
                || url.toLowerCase(Locale.ROOT).startsWith("http")
    }

    @JvmStatic
    fun getPicassoCreator(uri: Uri): RequestCreator {
        return if (isWebUrl(uri.toString())) {
            Picasso.get().load(uri)
        } else {
            Picasso.get().load(File(uri.toString()))
        }
    }

    @JvmStatic
    fun hasVersionM(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    @JvmStatic
    fun hasVersionN(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }

    @JvmStatic
    fun hasVersionO(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    @JvmStatic
    fun hasVersionKitKat(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
    }

    @JvmStatic
    fun hasVersionLollipop(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }

    /**
     * See Apache utils for more details.
     * https://github.com/apache/commons-lang/blob/master/src/main/java/org/apache/commons/lang3/text/WordUtils.java
     */
    @JvmStatic
    fun capitalize(str: String): String {
        return capitalize(str, null)
    }

    /**
     * See Apache utils for more details.
     * https://github.com/apache/commons-lang/blob/master/src/main/java/org/apache/commons/lang3/text/WordUtils.java
     */
    private fun capitalize(str: String, delimiters: CharArray?): String {
        val delimLen = delimiters?.size ?: -1
        if (str.isEmpty() || delimLen == 0) {
            return str
        }
        val buffer = str.toCharArray()
        var capitalizeNext = true
        for (i in buffer.indices) {
            val ch = buffer[i]
            if (isDelimiter(ch, delimiters)) {
                capitalizeNext = true
            } else if (capitalizeNext) {
                buffer[i] = Character.toTitleCase(ch)
                capitalizeNext = false
            }
        }
        return String(buffer)
    }

    /**
     * See Apache utils for more details.
     * https://github.com/apache/commons-lang/blob/master/src/main/java/org/apache/commons/lang3/text/WordUtils.java
     */
    private fun isDelimiter(ch: Char, delimiters: CharArray?): Boolean {
        if (delimiters == null) {
            return Character.isWhitespace(ch)
        }
        for (delimiter in delimiters) {
            if (ch == delimiter) {
                return true
            }
        }
        return false
    }

    /**
     * Holder for the Search query. Up to now I found it as quick solution to pass query
     * from Activity to the Open Radio Service.
     */
    private val SEARCH_QUERY = StringBuilder()

    /**
     * @return Gets the Search query string.
     */
    @JvmStatic
    var searchQuery: String?
        get() = SEARCH_QUERY.toString()
        set(searchQuery) {
            SEARCH_QUERY.setLength(0)
            SEARCH_QUERY.append(searchQuery)
        }

    /**
     * Checks whether or not application runs on Automotive environment.
     *
     * @param clientPackageName The package name of the application which is
     * requesting access.
     * @return `true` in case of success, `false` otherwise.
     */
    @JvmStatic
    fun isAutomotive(clientPackageName: String): Boolean {
        for (pkg in ANDROID_AUTO_PACKAGE_NAMES) {
            if (clientPackageName.contains(pkg)) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun getDensity(context: Context): Array<String> {
        val densityDpi = context.resources.displayMetrics.densityDpi
        val value: String
        value = when (densityDpi) {
            DisplayMetrics.DENSITY_LOW -> "LDPI"
            DisplayMetrics.DENSITY_MEDIUM -> "MDPI"
            DisplayMetrics.DENSITY_TV, DisplayMetrics.DENSITY_HIGH -> "HDPI"
            DisplayMetrics.DENSITY_XHIGH, DisplayMetrics.DENSITY_280 -> "XHDPI"
            DisplayMetrics.DENSITY_XXHIGH, DisplayMetrics.DENSITY_360, DisplayMetrics.DENSITY_400, DisplayMetrics.DENSITY_420 -> "XXHDPI"
            DisplayMetrics.DENSITY_XXXHIGH, DisplayMetrics.DENSITY_560 -> "XXXHDPI"
            else -> "UNKNOWN"
        }
        return arrayOf(densityDpi.toString(), value)
    }

    fun startActivitySafe(context: Context?, intent: Intent): Boolean {
        if (context == null) {
            return false
        }
        // Verify that the intent will resolve to an activity
        if (intent.resolveActivity(context.packageManager) != null) {
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                e("Can not start activity:$e")
                return false
            }
            return true
        }
        return false
    }

    @JvmStatic
    fun startActivityForResultSafe(context: Activity?,
                                   intent: Intent,
                                   resultCode: Int): Boolean {
        if (context == null) {
            return false
        }
        // Verify that the intent will resolve to an activity
        if (intent.resolveActivity(context.packageManager) != null) {
            try {
                context.startActivityForResult(intent, resultCode)
            } catch (e: Exception) {
                e("Can not start activity for result:$e")
                return false
            }
            return true
        }
        return false
    }
}
