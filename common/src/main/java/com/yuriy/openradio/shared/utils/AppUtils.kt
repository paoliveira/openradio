/*
 * Copyright 2017-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import androidx.fragment.app.FragmentActivity
import com.google.android.exoplayer2.util.Util
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import java.util.*

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 11/29/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * [AppUtils] is a helper class which holds various help-methods
 */
object AppUtils {

    /**
     * Time out for the stream to decide whether there is response or not, ms.
     */
    const val TIME_OUT = 2000
    const val UTF8 = "UTF-8"
    const val EMPTY_STRING = ""

    private val ANDROID_AUTO_PACKAGE_NAMES = arrayOf(
            "com.google.android.projection.gearhead",
            "com.android.car"
    )

    private const val KEY_SEARCH_QUERY = "KEY_SEARCH_QUERY"

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
        AppLogger.i("Has Location:$result")
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
            AppLogger.w("$CLASS_NAME Can't get application version")
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
            AppLogger.w("$CLASS_NAME Package manager is NULL")
            return null
        }
        val packageName: String
        return try {
            packageName = context.packageName
            packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            AppLogger.e("Get pck info", e)
            null
        } catch (e: RuntimeException) {
            // To catch RuntimeException("Package manager has died") that can occur on some
            // version of Android,
            // when the remote PackageManager is unavailable. I suspect this sometimes occurs
            // when the App is being reinstalled.
            AppLogger.e("Get pck info", e)
            null
        } catch (e: Throwable) {
            AppLogger.e("$CLASS_NAME Package manager", e)
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

    @JvmStatic
    fun getApplicationVersionName(context: Context): String {
        val packageInfo = getPackageInfo(context)
        return if (packageInfo != null) {
            packageInfo.versionName
        } else {
            AppLogger.w("Can't get application version")
            "?"
        }
    }

    @JvmStatic
    fun getApplicationVersionCode(context: Context): Int {
        val packageInfo = getPackageInfo(context)
        return if (packageInfo != null) {
            packageInfo.versionCode
        } else {
            AppLogger.w("Can't get application code")
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
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val simCountry = tm.simCountryIso
            if (simCountry != null && simCountry.length == 2) {
                // SIM country code is available
                return simCountry.lowercase(Locale.US)
            } else if (tm.phoneType != TelephonyManager.PHONE_TYPE_CDMA) {
                // device is not 3G (would be unreliable)
                val networkCountry = tm.networkCountryIso
                if (networkCountry != null && networkCountry.length == 2) {
                    // network country code is available
                    return networkCountry.lowercase(Locale.US)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("GetUserCountry", e)
        }
        return null
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

    @JvmStatic
    fun makeSearchQueryBundle(queryString: String): Bundle {
        val bundle = Bundle()
        bundle.putString(KEY_SEARCH_QUERY, queryString)
        return bundle
    }

    @JvmStatic
    fun getSearchQueryFromBundle(queryBundle: Bundle): String {
        return queryBundle.getString(KEY_SEARCH_QUERY, EMPTY_STRING)
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
        val value: String = when (densityDpi) {
            DisplayMetrics.DENSITY_LOW -> "LDPI"
            DisplayMetrics.DENSITY_MEDIUM -> "MDPI"
            DisplayMetrics.DENSITY_TV, DisplayMetrics.DENSITY_HIGH -> "HDPI"
            DisplayMetrics.DENSITY_XHIGH, DisplayMetrics.DENSITY_280 -> "XHDPI"
            DisplayMetrics.DENSITY_XXHIGH,
            DisplayMetrics.DENSITY_360, DisplayMetrics.DENSITY_400, DisplayMetrics.DENSITY_420 -> "XXHDPI"
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
                AppLogger.e("Can not start activity", e)
                return false
            }
            return true
        }
        return false
    }

    @JvmStatic
    fun startActivityForResultSafe(context: Activity?, intent: Intent, resultCode: Int): Boolean {
        if (context == null) {
            return false
        }
        // Verify that the intent will resolve to an activity
        if (intent.resolveActivity(context.packageManager) != null) {
            try {
                context.startActivityForResult(intent, resultCode)
            } catch (e: Exception) {
                AppLogger.e("Can not start activity for result", e)
                return false
            }
            return true
        }
        return false
    }
}
