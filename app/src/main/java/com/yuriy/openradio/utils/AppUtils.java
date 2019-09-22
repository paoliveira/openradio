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

package com.yuriy.openradio.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.exoplayer2.util.Util;
import com.yuriy.openradio.service.OpenRadioService;
import com.yuriy.openradio.view.activity.MainActivity;

import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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

    public static final String UTF8 = "UTF-8";

    /**
     * Map of the Countries Codes and Names.
     */
    public static final Map<String, String> COUNTRY_CODE_TO_NAME = new TreeMap<>();

    /**
     * Map of the Countries Names to Codes.
     */
    public static final Map<String, String> COUNTRY_NAME_TO_CODE = new TreeMap<>();

    // http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
    static {
        COUNTRY_CODE_TO_NAME.put("AD", "Andorra");
        COUNTRY_CODE_TO_NAME.put("AE", "United Arab Emirates");
        COUNTRY_CODE_TO_NAME.put("AF", "Afghanistan");
        COUNTRY_CODE_TO_NAME.put("AG", "Antigua and Barbuda");
        COUNTRY_CODE_TO_NAME.put("AL", "Albania");
        COUNTRY_CODE_TO_NAME.put("AM", "Armenia");
        COUNTRY_CODE_TO_NAME.put("AO", "Angola");
        COUNTRY_CODE_TO_NAME.put("AR", "Argentina");
        COUNTRY_CODE_TO_NAME.put("AT", "Austria");
        COUNTRY_CODE_TO_NAME.put("AU", "Australia");
        COUNTRY_CODE_TO_NAME.put("AZ", "Azerbaijan");
        COUNTRY_CODE_TO_NAME.put("BA", "Bosnia and Herzegovina");
        COUNTRY_CODE_TO_NAME.put("BB", "Barbados");
        COUNTRY_CODE_TO_NAME.put("BD", "Bangladesh");
        COUNTRY_CODE_TO_NAME.put("BE", "Belgium");
        COUNTRY_CODE_TO_NAME.put("BF", "Burkina Faso");
        COUNTRY_CODE_TO_NAME.put("BG", "Bulgaria");
        COUNTRY_CODE_TO_NAME.put("BH", "Bahrain");
        COUNTRY_CODE_TO_NAME.put("BI", "Burundi");
        COUNTRY_CODE_TO_NAME.put("BJ", "Benin");
        COUNTRY_CODE_TO_NAME.put("BN", "Brunei Darussalam");
        COUNTRY_CODE_TO_NAME.put("BO", "Bolivia, Plurinational State of");
        COUNTRY_CODE_TO_NAME.put("BR", "Brazil");
        COUNTRY_CODE_TO_NAME.put("BS", "Bahamas");
        COUNTRY_CODE_TO_NAME.put("BT", "Bhutan");
        COUNTRY_CODE_TO_NAME.put("BW", "Botswana");
        COUNTRY_CODE_TO_NAME.put("BY", "Belarus");
        COUNTRY_CODE_TO_NAME.put("BZ", "Belize");
        COUNTRY_CODE_TO_NAME.put("CA", "Canada");
        COUNTRY_CODE_TO_NAME.put("CD", "Congo, the Democratic Republic of the");
        COUNTRY_CODE_TO_NAME.put("CF", "Central African Republic");
        COUNTRY_CODE_TO_NAME.put("CG", "Congo");
        COUNTRY_CODE_TO_NAME.put("CH", "Switzerland");
        COUNTRY_CODE_TO_NAME.put("CI", "Côte d'Ivoire");
        COUNTRY_CODE_TO_NAME.put("CL", "Chile");
        COUNTRY_CODE_TO_NAME.put("CM", "Cameroon");
        COUNTRY_CODE_TO_NAME.put("CN", "China");
        COUNTRY_CODE_TO_NAME.put("CO", "Colombia");
        COUNTRY_CODE_TO_NAME.put("CR", "Costa Rica");
        COUNTRY_CODE_TO_NAME.put("CU", "Cuba");
        COUNTRY_CODE_TO_NAME.put("CV", "Cabo Verde");
        COUNTRY_CODE_TO_NAME.put("CY", "Cyprus");
        COUNTRY_CODE_TO_NAME.put("CZ", "Czech Republic");
        COUNTRY_CODE_TO_NAME.put("DE", "Germany");
        COUNTRY_CODE_TO_NAME.put("DJ", "Djibouti");
        COUNTRY_CODE_TO_NAME.put("DK", "Denmark");
        COUNTRY_CODE_TO_NAME.put("DM", "Dominica");
        COUNTRY_CODE_TO_NAME.put("DO", "Dominican Republic");
        COUNTRY_CODE_TO_NAME.put("DZ", "Algeria");
        COUNTRY_CODE_TO_NAME.put("EC", "Ecuador");
        COUNTRY_CODE_TO_NAME.put("EE", "Estonia");
        COUNTRY_CODE_TO_NAME.put("EG", "Egypt");
        COUNTRY_CODE_TO_NAME.put("ER", "Eritrea");
        COUNTRY_CODE_TO_NAME.put("ES", "Spain");
        COUNTRY_CODE_TO_NAME.put("ET", "Ethiopia");
        COUNTRY_CODE_TO_NAME.put("FI", "Finland");
        COUNTRY_CODE_TO_NAME.put("FJ", "Fiji");
        COUNTRY_CODE_TO_NAME.put("FM", "Micronesia, Federated States of");
        COUNTRY_CODE_TO_NAME.put("FO", "Faroe Islands");
        COUNTRY_CODE_TO_NAME.put("FR", "France");
        COUNTRY_CODE_TO_NAME.put("GA", "Gabon");
        COUNTRY_CODE_TO_NAME.put("GB", "United Kingdom of Great Britain and Northern Ireland");
        COUNTRY_CODE_TO_NAME.put("GD", "Grenada");
        COUNTRY_CODE_TO_NAME.put("GE", "Georgia");
        COUNTRY_CODE_TO_NAME.put("GH", "Ghana");
        COUNTRY_CODE_TO_NAME.put("GM", "Gambia");
        COUNTRY_CODE_TO_NAME.put("GN", "Guinea");
        COUNTRY_CODE_TO_NAME.put("GQ", "Equatorial Guinea");
        COUNTRY_CODE_TO_NAME.put("GR", "Greece");
        COUNTRY_CODE_TO_NAME.put("GT", "Guatemala");
        COUNTRY_CODE_TO_NAME.put("GW", "Guinea-Bissau");
        COUNTRY_CODE_TO_NAME.put("GY", "Guyana");
        COUNTRY_CODE_TO_NAME.put("HK", "Hong Kong");
        COUNTRY_CODE_TO_NAME.put("HN", "Honduras");
        COUNTRY_CODE_TO_NAME.put("HR", "Croatia");
        COUNTRY_CODE_TO_NAME.put("HT", "Haiti");
        COUNTRY_CODE_TO_NAME.put("HU", "Hungary");
        COUNTRY_CODE_TO_NAME.put("ID", "Indonesia");
        COUNTRY_CODE_TO_NAME.put("IE", "Ireland");
        COUNTRY_CODE_TO_NAME.put("IL", "Israel");
        COUNTRY_CODE_TO_NAME.put("IN", "India");
        COUNTRY_CODE_TO_NAME.put("IQ", "Iraq");
        COUNTRY_CODE_TO_NAME.put("IR", "Iran, Islamic Republic of");
        COUNTRY_CODE_TO_NAME.put("IS", "Iceland");
        COUNTRY_CODE_TO_NAME.put("IT", "Italy");
        COUNTRY_CODE_TO_NAME.put("JM", "Jamaica");
        COUNTRY_CODE_TO_NAME.put("JO", "Jordan");
        COUNTRY_CODE_TO_NAME.put("JP", "Japan");
        COUNTRY_CODE_TO_NAME.put("KE", "Kenya");
        COUNTRY_CODE_TO_NAME.put("KG", "Kyrgyzstan");
        COUNTRY_CODE_TO_NAME.put("KH", "Cambodia");
        COUNTRY_CODE_TO_NAME.put("KI", "Kiribati");
        COUNTRY_CODE_TO_NAME.put("KM", "Comoros");
        COUNTRY_CODE_TO_NAME.put("KN", "Saint Kitts and Nevis");
        COUNTRY_CODE_TO_NAME.put("KP", "Korea, Democratic People's Republic of");
        COUNTRY_CODE_TO_NAME.put("KR", "Korea, Republic of");
        COUNTRY_CODE_TO_NAME.put("KW", "Kuwait");
        COUNTRY_CODE_TO_NAME.put("KZ", "Kazakhstan");
        COUNTRY_CODE_TO_NAME.put("LA", "Lao People's Democratic Republic");
        COUNTRY_CODE_TO_NAME.put("LB", "Lebanon");
        COUNTRY_CODE_TO_NAME.put("LC", "Saint Lucia");
        COUNTRY_CODE_TO_NAME.put("LI", "Liechtenstein");
        COUNTRY_CODE_TO_NAME.put("LK", "Sri Lanka");
        COUNTRY_CODE_TO_NAME.put("LR", "Liberia");
        COUNTRY_CODE_TO_NAME.put("LS", "Lesotho");
        COUNTRY_CODE_TO_NAME.put("LT", "Lithuania");
        COUNTRY_CODE_TO_NAME.put("LU", "Luxembourg");
        COUNTRY_CODE_TO_NAME.put("LV", "Latvia");
        COUNTRY_CODE_TO_NAME.put("LY", "Libya");
        COUNTRY_CODE_TO_NAME.put("MA", "Morocco");
        COUNTRY_CODE_TO_NAME.put("MC", "Monaco");
        COUNTRY_CODE_TO_NAME.put("MD", "Moldova, Republic of");
        COUNTRY_CODE_TO_NAME.put("ME", "Montenegro");
        COUNTRY_CODE_TO_NAME.put("MG", "Madagascar");
        COUNTRY_CODE_TO_NAME.put("MH", "Marshall Islands");
        COUNTRY_CODE_TO_NAME.put("MK", "Macedonia, the former Yugoslav Republic of");
        COUNTRY_CODE_TO_NAME.put("ML", "Mali");
        COUNTRY_CODE_TO_NAME.put("MM", "Myanmar");
        COUNTRY_CODE_TO_NAME.put("MN", "Mongolia");
        COUNTRY_CODE_TO_NAME.put("MR", "Mauritania");
        COUNTRY_CODE_TO_NAME.put("MT", "Malta");
        COUNTRY_CODE_TO_NAME.put("MU", "Mauritius");
        COUNTRY_CODE_TO_NAME.put("MV", "Maldives");
        COUNTRY_CODE_TO_NAME.put("MW", "Malawi");
        COUNTRY_CODE_TO_NAME.put("MX", "Mexico");
        COUNTRY_CODE_TO_NAME.put("MY", "Malaysia");
        COUNTRY_CODE_TO_NAME.put("MZ", "Mozambique");
        COUNTRY_CODE_TO_NAME.put("NA", "Namibia");
        COUNTRY_CODE_TO_NAME.put("NE", "Niger");
        COUNTRY_CODE_TO_NAME.put("NG", "Nigeria");
        COUNTRY_CODE_TO_NAME.put("NI", "Nicaragua");
        COUNTRY_CODE_TO_NAME.put("NL", "Netherlands");
        COUNTRY_CODE_TO_NAME.put("NO", "Norway");
        COUNTRY_CODE_TO_NAME.put("NP", "Nepal");
        COUNTRY_CODE_TO_NAME.put("NR", "Nauru");
        COUNTRY_CODE_TO_NAME.put("NZ", "New Zealand");
        COUNTRY_CODE_TO_NAME.put("OM", "Oman");
        COUNTRY_CODE_TO_NAME.put("PA", "Panama");
        COUNTRY_CODE_TO_NAME.put("PE", "Peru");
        COUNTRY_CODE_TO_NAME.put("PG", "Papua New Guinea");
        COUNTRY_CODE_TO_NAME.put("PH", "Philippines");
        COUNTRY_CODE_TO_NAME.put("PK", "Pakistan");
        COUNTRY_CODE_TO_NAME.put("PL", "Poland");
        COUNTRY_CODE_TO_NAME.put("PT", "Portugal");
        COUNTRY_CODE_TO_NAME.put("PW", "Palau");
        COUNTRY_CODE_TO_NAME.put("PY", "Paraguay");
        COUNTRY_CODE_TO_NAME.put("QA", "Qatar");
        COUNTRY_CODE_TO_NAME.put("RO", "Romania");
        COUNTRY_CODE_TO_NAME.put("RS", "Serbia");
        COUNTRY_CODE_TO_NAME.put("RU", "Russian Federation");
        COUNTRY_CODE_TO_NAME.put("RW", "Rwanda");
        COUNTRY_CODE_TO_NAME.put("SA", "Saudi Arabia");
        COUNTRY_CODE_TO_NAME.put("SB", "Solomon Islands");
        COUNTRY_CODE_TO_NAME.put("SC", "Seychelles");
        COUNTRY_CODE_TO_NAME.put("SD", "Sudan");
        COUNTRY_CODE_TO_NAME.put("SE", "Sweden");
        COUNTRY_CODE_TO_NAME.put("SG", "Singapore");
        COUNTRY_CODE_TO_NAME.put("SI", "Slovenia");
        COUNTRY_CODE_TO_NAME.put("SK", "Slovakia");
        COUNTRY_CODE_TO_NAME.put("SL", "Sierra Leone");
        COUNTRY_CODE_TO_NAME.put("SM", "San Marino");
        COUNTRY_CODE_TO_NAME.put("SN", "Senegal");
        COUNTRY_CODE_TO_NAME.put("SO", "Somalia");
        COUNTRY_CODE_TO_NAME.put("SR", "Suriname");
        COUNTRY_CODE_TO_NAME.put("ST", "Sao Tome and Principe");
        COUNTRY_CODE_TO_NAME.put("SV", "El Salvador");
        COUNTRY_CODE_TO_NAME.put("SY", "Syrian Arab Republic");
        COUNTRY_CODE_TO_NAME.put("SZ", "Swaziland");
        COUNTRY_CODE_TO_NAME.put("TD", "Chad");
        COUNTRY_CODE_TO_NAME.put("TG", "Togo");
        COUNTRY_CODE_TO_NAME.put("TH", "Thailand");
        COUNTRY_CODE_TO_NAME.put("TJ", "Tajikistan");
        COUNTRY_CODE_TO_NAME.put("TM", "Turkmenistan");
        COUNTRY_CODE_TO_NAME.put("TN", "Tunisia");
        COUNTRY_CODE_TO_NAME.put("TO", "Tonga");
        COUNTRY_CODE_TO_NAME.put("TR", "Turkey");
        COUNTRY_CODE_TO_NAME.put("TT", "Trinidad and Tobago");
        COUNTRY_CODE_TO_NAME.put("TV", "Tuvalu");
        COUNTRY_CODE_TO_NAME.put("TW", "Taiwan, Province of China");
        COUNTRY_CODE_TO_NAME.put("TZ", "Tanzania, United Republic of");
        COUNTRY_CODE_TO_NAME.put("UA", "Ukraine");
        COUNTRY_CODE_TO_NAME.put("UG", "Uganda");
        COUNTRY_CODE_TO_NAME.put("US", "United States of America");
        COUNTRY_CODE_TO_NAME.put("UY", "Uruguay");
        COUNTRY_CODE_TO_NAME.put("UZ", "Uzbekistan");
        COUNTRY_CODE_TO_NAME.put("VA", "Holy See");
        COUNTRY_CODE_TO_NAME.put("VC", "Saint Vincent and the Grenadines");
        COUNTRY_CODE_TO_NAME.put("VE", "Venezuela, Bolivarian Republic of");
        COUNTRY_CODE_TO_NAME.put("VN", "Viet Nam");
        COUNTRY_CODE_TO_NAME.put("VU", "Vanuatu");
        COUNTRY_CODE_TO_NAME.put("WS", "Samoa");
        COUNTRY_CODE_TO_NAME.put("YE", "Yemen");
        COUNTRY_CODE_TO_NAME.put("ZA", "South Africa");
        COUNTRY_CODE_TO_NAME.put("ZM", "Zambia");
        COUNTRY_CODE_TO_NAME.put("ZW", "Zimbabwe");
        COUNTRY_CODE_TO_NAME.put("AI", "Anguilla");
        COUNTRY_CODE_TO_NAME.put("AN", "Netherlands Antilles");
        COUNTRY_CODE_TO_NAME.put("AQ", "Antarctica");
        COUNTRY_CODE_TO_NAME.put("AS", "American Samoa");
        COUNTRY_CODE_TO_NAME.put("AW", "Aruba");
        COUNTRY_CODE_TO_NAME.put("AX", "Åland Islands");
        COUNTRY_CODE_TO_NAME.put("BL", "Saint Barthélemy");
        COUNTRY_CODE_TO_NAME.put("BM", "Bermuda");
        COUNTRY_CODE_TO_NAME.put("BQ", "Bonaire, Sint Eustatius and Saba");
        COUNTRY_CODE_TO_NAME.put("BV", "Bouvet Island");
        COUNTRY_CODE_TO_NAME.put("CC", "Cocos (Keeling) Islands");
        COUNTRY_CODE_TO_NAME.put("CK", "Cook Islands");
        COUNTRY_CODE_TO_NAME.put("CW", "Curaçao");
        COUNTRY_CODE_TO_NAME.put("CX", "Christmas Island");
        COUNTRY_CODE_TO_NAME.put("EH", "Western Sahara");
        COUNTRY_CODE_TO_NAME.put("FK", "Falkland Islands (Malvinas)");
        COUNTRY_CODE_TO_NAME.put("GF", "French Guiana");
        COUNTRY_CODE_TO_NAME.put("GG", "Guernsey");
        COUNTRY_CODE_TO_NAME.put("GI", "Gibraltar");
        COUNTRY_CODE_TO_NAME.put("GL", "Greenland");
        COUNTRY_CODE_TO_NAME.put("GP", "Guadeloupe");
        COUNTRY_CODE_TO_NAME.put("GS", "South Georgia and the South Sandwich Islands");
        COUNTRY_CODE_TO_NAME.put("GU", "Guam");
        COUNTRY_CODE_TO_NAME.put("HM", "Heard Island and McDonald Islands");
        COUNTRY_CODE_TO_NAME.put("IM", "Isle of Man");
        COUNTRY_CODE_TO_NAME.put("IO", "British Indian Ocean Territory");
        COUNTRY_CODE_TO_NAME.put("JE", "Jersey");
        COUNTRY_CODE_TO_NAME.put("KY", "Cayman Islands");
        COUNTRY_CODE_TO_NAME.put("MF", "Saint Martin (French part)");
        COUNTRY_CODE_TO_NAME.put("MO", "Macao");
        COUNTRY_CODE_TO_NAME.put("MP", "Northern Mariana Islands");
        COUNTRY_CODE_TO_NAME.put("MQ", "Martinique");
        COUNTRY_CODE_TO_NAME.put("MS", "Montserrat");
        COUNTRY_CODE_TO_NAME.put("NC", "New Caledonia");
        COUNTRY_CODE_TO_NAME.put("NF", "Norfolk Island");
        COUNTRY_CODE_TO_NAME.put("NU", "Niue");
        COUNTRY_CODE_TO_NAME.put("PF", "French Polynesia");
        COUNTRY_CODE_TO_NAME.put("PM", "Saint Pierre and Miquelon");
        COUNTRY_CODE_TO_NAME.put("PN", "Pitcairn");
        COUNTRY_CODE_TO_NAME.put("PR", "Puerto Rico");
        COUNTRY_CODE_TO_NAME.put("PS", "Palestine, State of");
        COUNTRY_CODE_TO_NAME.put("RE", "Réunion");
        COUNTRY_CODE_TO_NAME.put("SH", "Saint Helena, Ascension and Tristan da Cunha");
        COUNTRY_CODE_TO_NAME.put("SJ", "Svalbard and Jan Mayen");
        COUNTRY_CODE_TO_NAME.put("SS", "South Sudan");
        COUNTRY_CODE_TO_NAME.put("SX", "Sint Maarten (Dutch part)");
        COUNTRY_CODE_TO_NAME.put("TC", "Turks and Caicos Islands");
        COUNTRY_CODE_TO_NAME.put("TF", "French Southern Territories");
        COUNTRY_CODE_TO_NAME.put("TK", "Tokelau");
        COUNTRY_CODE_TO_NAME.put("TL", "Timor-Leste");
        COUNTRY_CODE_TO_NAME.put("UM", "United States Minor Outlying Islands");
        COUNTRY_CODE_TO_NAME.put("VG", "Virgin Islands, British");
        COUNTRY_CODE_TO_NAME.put("VI", "Virgin Islands, U.S.");
        COUNTRY_CODE_TO_NAME.put("WF", "Wallis and Futuna");
        COUNTRY_CODE_TO_NAME.put("YT", "Mayotte");

        for (final Map.Entry<String, String> entry : COUNTRY_CODE_TO_NAME.entrySet()) {
            COUNTRY_NAME_TO_CODE.put(entry.getValue(), entry.getKey());
        }
    }

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
        String packageName = "";
        try {
            packageName = context.getPackageName();
            return packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            FabricUtils.logException(e);
            return null;
        } catch (RuntimeException e) {
            // To catch RuntimeException("Package manager has died") that can occur on some
            // version of Android,
            // when the remote PackageManager is unavailable. I suspect this sometimes occurs
            // when the App is being reinstalled.
            FabricUtils.logException(e);
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
     * Return {@link java.io.File} object legal to call on API 8.
     *
     * @param type    The type of files directory to return. May be null for the root of the
     *                files directory or one of the following Environment constants for a subdirectory:
     *                DIRECTORY_MUSIC, DIRECTORY_PODCASTS, DIRECTORY_RINGTONES, DIRECTORY_ALARMS,
     *                DIRECTORY_NOTIFICATIONS, DIRECTORY_PICTURES, or DIRECTORY_MOVIES.
     * @param context Context of the callee.
     * @return {@link java.io.File} object.
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
            FabricUtils.logException(e);
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

    public static boolean isVersionM() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.M;
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
     * from {@link MainActivity} to the
     * {@link OpenRadioService}
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
}
