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

package com.yuriy.openradio.shared.service

import android.annotation.SuppressLint
import android.app.Activity
import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.view.View
import androidx.core.app.JobIntentService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.permission.PermissionChecker
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.IntentUtils
import com.yuriy.openradio.shared.vo.Country
import de.westnordost.countryboundaries.CountryBoundaries
import java.io.IOException
import java.util.TreeMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 4/27/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class LocationService : JobIntentService() {

    companion object {
        private val TAG = LocationService::class.java.simpleName + " "

        /**
         * Map of the Countries Codes to Names.
         */
        val COUNTRY_CODE_TO_NAME = TreeMap<String, String>()

        /**
         * Map of the Countries Names to Codes.
         */
        val COUNTRY_NAME_TO_CODE = TreeMap<String, String>()
        const val GB_WRONG = "United Kingdom of Great Britain and Northern Irela"
        const val TW_WRONG_A = "Taiwan, Province of China"
        const val TW_WRONG_B = "Taiwan Province of China"
        const val GB_CORRECT = "United Kingdom of Great Britain and Northern Ireland"
        const val TW_CORRECT = "Taiwan"
        private const val JOB_ID = 1000
        private const val EXTRA_NAME_MESSENGER = "EXTRA_NAME_MESSENGER"
        private const val BUNDLE_KEY_COUNTRY_CODE = "BUNDLE_KEY_COUNTRY_CODE"
        const val MSG_ID_ON_LOCATION = 100

        fun doCancelWork(context: Context) {
            if (!AppUtils.hasVersionM()) {
                return
            }
            context.getSystemService(JobScheduler::class.java)?.let {
                AppLogger.i("$TAG cancelling the job.")
                it.cancel(JOB_ID)
            }
        }

        /**
         * Factory method to enqueue work for Location Service.
         *
         * @param context Context of callee.
         */
        fun doEnqueueWork(context: Context, messenger: Messenger) {
            doCancelWork(context)
            // Create an Intent to get Location in the background via a Service.
            val intent = makeIntent(context, messenger)
            enqueueWork(context, LocationService::class.java, JOB_ID, intent)
        }

        fun getCountries(): Array<Country> {
            val list = ArrayList<Country>()
            for ((name, code) in COUNTRY_NAME_TO_CODE) {
                list.add(Country(name, code))
            }
            list.sortWith(compareBy({ it.name }, { it.name }))
            return list.toTypedArray()
        }

        fun getCountriesWithLocation(context: Context): Array<Country> {
            val list = ArrayList<Country>()
            val locationStr = context.getString(R.string.default_country_use_location)
            for ((name, code) in COUNTRY_NAME_TO_CODE) {
                list.add(Country(name, code))
            }
            list.sortWith(compareBy({ it.name }, { it.name }))
            list.add(0, Country(locationStr, locationStr))
            return list.toTypedArray()
        }

        fun checkCountry(activity: Activity, mainLayout: View, messenger: Messenger, defaultCountry: String) {
            if (!isDefaultLocationEnabled(activity.applicationContext, defaultCountry)) {
                return
            }
            if (!AppUtils.hasLocation(activity.applicationContext)) {
                return
            }
            if (PermissionChecker.isLocationGranted(activity.applicationContext)) {
                doEnqueueWork(activity.applicationContext, messenger)
            } else {
                PermissionChecker.requestLocationPermission(activity, mainLayout)
            }
        }

        private fun isDefaultLocationEnabled(context: Context, defaultCountry: String): Boolean {
            return defaultCountry == context.getString(R.string.default_country_use_location)
        }

        /**
         * Factory method to make the Intent to start this service.
         */
        private fun makeIntent(context: Context, messenger: Messenger): Intent {
            // Create an intent associated with the Location Service class.
            val intent = Intent(context, LocationService::class.java)
            intent.putExtra(EXTRA_NAME_MESSENGER, messenger)
            return intent
        }

        private fun makeReplyMessage(countryCode: String): Message {
            val message = Message.obtain()
            message.arg1 = Activity.RESULT_OK
            val data = Bundle()
            data.putString(BUNDLE_KEY_COUNTRY_CODE, countryCode)
            message.data = data
            return message
        }

        fun getCountryCode(message: Message): String {
            val data = message.data
            val offers = data.getString(BUNDLE_KEY_COUNTRY_CODE, Country.COUNTRY_CODE_DEFAULT)
            return if (message.arg1 != Activity.RESULT_OK) Country.COUNTRY_CODE_DEFAULT else offers
        }

        // http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
        init {
            COUNTRY_CODE_TO_NAME["AD"] = "Andorra"
            COUNTRY_CODE_TO_NAME["AE"] = "United Arab Emirates"
            COUNTRY_CODE_TO_NAME["AF"] = "Afghanistan"
            COUNTRY_CODE_TO_NAME["AG"] = "Antigua and Barbuda"
            COUNTRY_CODE_TO_NAME["AL"] = "Albania"
            COUNTRY_CODE_TO_NAME["AM"] = "Armenia"
            COUNTRY_CODE_TO_NAME["AO"] = "Angola"
            COUNTRY_CODE_TO_NAME["AR"] = "Argentina"
            COUNTRY_CODE_TO_NAME["AT"] = "Austria"
            COUNTRY_CODE_TO_NAME["AU"] = "Australia"
            COUNTRY_CODE_TO_NAME["AZ"] = "Azerbaijan"
            COUNTRY_CODE_TO_NAME["BA"] = "Bosnia and Herzegovina"
            COUNTRY_CODE_TO_NAME["BB"] = "Barbados"
            COUNTRY_CODE_TO_NAME["BD"] = "Bangladesh"
            COUNTRY_CODE_TO_NAME["BE"] = "Belgium"
            COUNTRY_CODE_TO_NAME["BF"] = "Burkina Faso"
            COUNTRY_CODE_TO_NAME["BG"] = "Bulgaria"
            COUNTRY_CODE_TO_NAME["BH"] = "Bahrain"
            COUNTRY_CODE_TO_NAME["BI"] = "Burundi"
            COUNTRY_CODE_TO_NAME["BJ"] = "Benin"
            COUNTRY_CODE_TO_NAME["BN"] = "Brunei Darussalam"
            COUNTRY_CODE_TO_NAME["BO"] = "Bolivia"
            COUNTRY_CODE_TO_NAME["BR"] = "Brazil"
            COUNTRY_CODE_TO_NAME["BS"] = "Bahamas"
            COUNTRY_CODE_TO_NAME["BT"] = "Bhutan"
            COUNTRY_CODE_TO_NAME["BW"] = "Botswana"
            COUNTRY_CODE_TO_NAME["BY"] = "Belarus"
            COUNTRY_CODE_TO_NAME["BZ"] = "Belize"
            COUNTRY_CODE_TO_NAME["CA"] = "Canada"
            COUNTRY_CODE_TO_NAME["CD"] = "Congo, Democratic Republic"
            COUNTRY_CODE_TO_NAME["CF"] = "Central African Republic"
            COUNTRY_CODE_TO_NAME["CG"] = "Congo-Brazzaville"
            COUNTRY_CODE_TO_NAME["CH"] = "Switzerland"
            COUNTRY_CODE_TO_NAME["CI"] = "Côte d'Ivoire"
            COUNTRY_CODE_TO_NAME["CL"] = "Chile"
            COUNTRY_CODE_TO_NAME["CM"] = "Cameroon"
            COUNTRY_CODE_TO_NAME["CN"] = "China"
            COUNTRY_CODE_TO_NAME["CO"] = "Colombia"
            COUNTRY_CODE_TO_NAME["CR"] = "Costa Rica"
            COUNTRY_CODE_TO_NAME["CU"] = "Cuba"
            COUNTRY_CODE_TO_NAME["CV"] = "Cabo Verde"
            COUNTRY_CODE_TO_NAME["CY"] = "Cyprus"
            COUNTRY_CODE_TO_NAME["CZ"] = "Czech Republic"
            COUNTRY_CODE_TO_NAME["DE"] = "Germany"
            COUNTRY_CODE_TO_NAME["DJ"] = "Djibouti"
            COUNTRY_CODE_TO_NAME["DK"] = "Denmark"
            COUNTRY_CODE_TO_NAME["DM"] = "Dominica"
            COUNTRY_CODE_TO_NAME["DO"] = "Dominican Republic"
            COUNTRY_CODE_TO_NAME["DZ"] = "Algeria"
            COUNTRY_CODE_TO_NAME["EC"] = "Ecuador"
            COUNTRY_CODE_TO_NAME["EE"] = "Estonia"
            COUNTRY_CODE_TO_NAME["EG"] = "Egypt"
            COUNTRY_CODE_TO_NAME["ER"] = "Eritrea"
            COUNTRY_CODE_TO_NAME["ES"] = "Spain"
            COUNTRY_CODE_TO_NAME["ET"] = "Ethiopia"
            COUNTRY_CODE_TO_NAME["FI"] = "Finland"
            COUNTRY_CODE_TO_NAME["FJ"] = "Fiji"
            COUNTRY_CODE_TO_NAME["FM"] = "Micronesia"
            COUNTRY_CODE_TO_NAME["FO"] = "Faroe Islands"
            COUNTRY_CODE_TO_NAME["FR"] = "France"
            COUNTRY_CODE_TO_NAME["GA"] = "Gabon"
            COUNTRY_CODE_TO_NAME["GB"] = GB_CORRECT
            COUNTRY_CODE_TO_NAME["GD"] = "Grenada"
            COUNTRY_CODE_TO_NAME["GE"] = "Georgia"
            COUNTRY_CODE_TO_NAME["GH"] = "Ghana"
            COUNTRY_CODE_TO_NAME["GM"] = "Gambia"
            COUNTRY_CODE_TO_NAME["GN"] = "Guinea"
            COUNTRY_CODE_TO_NAME["GQ"] = "Equatorial Guinea"
            COUNTRY_CODE_TO_NAME["GR"] = "Greece"
            COUNTRY_CODE_TO_NAME["GT"] = "Guatemala"
            COUNTRY_CODE_TO_NAME["GW"] = "Guinea-Bissau"
            COUNTRY_CODE_TO_NAME["GY"] = "Guyana"
            COUNTRY_CODE_TO_NAME["HK"] = "Hong Kong"
            COUNTRY_CODE_TO_NAME["HN"] = "Honduras"
            COUNTRY_CODE_TO_NAME["HR"] = "Croatia"
            COUNTRY_CODE_TO_NAME["HT"] = "Haiti"
            COUNTRY_CODE_TO_NAME["HU"] = "Hungary"
            COUNTRY_CODE_TO_NAME["ID"] = "Indonesia"
            COUNTRY_CODE_TO_NAME["IE"] = "Ireland"
            COUNTRY_CODE_TO_NAME["IL"] = "Israel"
            COUNTRY_CODE_TO_NAME["IN"] = "India"
            COUNTRY_CODE_TO_NAME["IQ"] = "Iraq"
            COUNTRY_CODE_TO_NAME["IR"] = "Iran"
            COUNTRY_CODE_TO_NAME["IS"] = "Iceland"
            COUNTRY_CODE_TO_NAME["IT"] = "Italy"
            COUNTRY_CODE_TO_NAME["JM"] = "Jamaica"
            COUNTRY_CODE_TO_NAME["JO"] = "Jordan"
            COUNTRY_CODE_TO_NAME["JP"] = "Japan"
            COUNTRY_CODE_TO_NAME["KE"] = "Kenya"
            COUNTRY_CODE_TO_NAME["KG"] = "Kyrgyzstan"
            COUNTRY_CODE_TO_NAME["KH"] = "Cambodia"
            COUNTRY_CODE_TO_NAME["KI"] = "Kiribati"
            COUNTRY_CODE_TO_NAME["KM"] = "Comoros"
            COUNTRY_CODE_TO_NAME["KN"] = "Saint Kitts and Nevis"
            COUNTRY_CODE_TO_NAME["KP"] = "Korea, Democratic People's Republic of"
            COUNTRY_CODE_TO_NAME["KR"] = "Korea, Republic of"
            COUNTRY_CODE_TO_NAME["KW"] = "Kuwait"
            COUNTRY_CODE_TO_NAME["KZ"] = "Kazakhstan"
            COUNTRY_CODE_TO_NAME["LA"] = "Lao People's Democratic Republic"
            COUNTRY_CODE_TO_NAME["LB"] = "Lebanon"
            COUNTRY_CODE_TO_NAME["LC"] = "Saint Lucia"
            COUNTRY_CODE_TO_NAME["LI"] = "Liechtenstein"
            COUNTRY_CODE_TO_NAME["LK"] = "Sri Lanka"
            COUNTRY_CODE_TO_NAME["LR"] = "Liberia"
            COUNTRY_CODE_TO_NAME["LS"] = "Lesotho"
            COUNTRY_CODE_TO_NAME["LT"] = "Lithuania"
            COUNTRY_CODE_TO_NAME["LU"] = "Luxembourg"
            COUNTRY_CODE_TO_NAME["LV"] = "Latvia"
            COUNTRY_CODE_TO_NAME["LY"] = "Libya"
            COUNTRY_CODE_TO_NAME["MA"] = "Morocco"
            COUNTRY_CODE_TO_NAME["MC"] = "Monaco"
            COUNTRY_CODE_TO_NAME["MD"] = "Moldova"
            COUNTRY_CODE_TO_NAME["ME"] = "Montenegro"
            COUNTRY_CODE_TO_NAME["MG"] = "Madagascar"
            COUNTRY_CODE_TO_NAME["MH"] = "Marshall Islands"
            COUNTRY_CODE_TO_NAME["MK"] = "Macedonia"
            COUNTRY_CODE_TO_NAME["ML"] = "Mali"
            COUNTRY_CODE_TO_NAME["MM"] = "Myanmar"
            COUNTRY_CODE_TO_NAME["MN"] = "Mongolia"
            COUNTRY_CODE_TO_NAME["MR"] = "Mauritania"
            COUNTRY_CODE_TO_NAME["MT"] = "Malta"
            COUNTRY_CODE_TO_NAME["MU"] = "Mauritius"
            COUNTRY_CODE_TO_NAME["MV"] = "Maldives"
            COUNTRY_CODE_TO_NAME["MW"] = "Malawi"
            COUNTRY_CODE_TO_NAME["MX"] = "Mexico"
            COUNTRY_CODE_TO_NAME["MY"] = "Malaysia"
            COUNTRY_CODE_TO_NAME["MZ"] = "Mozambique"
            COUNTRY_CODE_TO_NAME["NA"] = "Namibia"
            COUNTRY_CODE_TO_NAME["NE"] = "Niger"
            COUNTRY_CODE_TO_NAME["NG"] = "Nigeria"
            COUNTRY_CODE_TO_NAME["NI"] = "Nicaragua"
            COUNTRY_CODE_TO_NAME["NL"] = "Netherlands"
            COUNTRY_CODE_TO_NAME["NO"] = "Norway"
            COUNTRY_CODE_TO_NAME["NP"] = "Nepal"
            COUNTRY_CODE_TO_NAME["NR"] = "Nauru"
            COUNTRY_CODE_TO_NAME["NZ"] = "New Zealand"
            COUNTRY_CODE_TO_NAME["OM"] = "Oman"
            COUNTRY_CODE_TO_NAME["PA"] = "Panama"
            COUNTRY_CODE_TO_NAME["PE"] = "Peru"
            COUNTRY_CODE_TO_NAME["PG"] = "Papua New Guinea"
            COUNTRY_CODE_TO_NAME["PH"] = "Philippines"
            COUNTRY_CODE_TO_NAME["PK"] = "Pakistan"
            COUNTRY_CODE_TO_NAME["PL"] = "Poland"
            COUNTRY_CODE_TO_NAME["PT"] = "Portugal"
            COUNTRY_CODE_TO_NAME["PW"] = "Palau"
            COUNTRY_CODE_TO_NAME["PY"] = "Paraguay"
            COUNTRY_CODE_TO_NAME["QA"] = "Qatar"
            COUNTRY_CODE_TO_NAME["RO"] = "Romania"
            COUNTRY_CODE_TO_NAME["RS"] = "Serbia"
            COUNTRY_CODE_TO_NAME["RU"] = "Russian Federation"
            COUNTRY_CODE_TO_NAME["RW"] = "Rwanda"
            COUNTRY_CODE_TO_NAME["SA"] = "Saudi Arabia"
            COUNTRY_CODE_TO_NAME["SB"] = "Solomon Islands"
            COUNTRY_CODE_TO_NAME["SC"] = "Seychelles"
            COUNTRY_CODE_TO_NAME["SD"] = "Sudan"
            COUNTRY_CODE_TO_NAME["SE"] = "Sweden"
            COUNTRY_CODE_TO_NAME["SG"] = "Singapore"
            COUNTRY_CODE_TO_NAME["SI"] = "Slovenia"
            COUNTRY_CODE_TO_NAME["SK"] = "Slovakia"
            COUNTRY_CODE_TO_NAME["SL"] = "Sierra Leone"
            COUNTRY_CODE_TO_NAME["SM"] = "San Marino"
            COUNTRY_CODE_TO_NAME["SN"] = "Senegal"
            COUNTRY_CODE_TO_NAME["SO"] = "Somalia"
            COUNTRY_CODE_TO_NAME["SR"] = "Suriname"
            COUNTRY_CODE_TO_NAME["ST"] = "Sao Tome and Principe"
            COUNTRY_CODE_TO_NAME["SV"] = "El Salvador"
            COUNTRY_CODE_TO_NAME["SY"] = "Syrian Arab Republic"
            COUNTRY_CODE_TO_NAME["SZ"] = "Swaziland"
            COUNTRY_CODE_TO_NAME["TD"] = "Chad"
            COUNTRY_CODE_TO_NAME["TG"] = "Togo"
            COUNTRY_CODE_TO_NAME["TH"] = "Thailand"
            COUNTRY_CODE_TO_NAME["TJ"] = "Tajikistan"
            COUNTRY_CODE_TO_NAME["TM"] = "Turkmenistan"
            COUNTRY_CODE_TO_NAME["TN"] = "Tunisia"
            COUNTRY_CODE_TO_NAME["TO"] = "Tonga"
            COUNTRY_CODE_TO_NAME["TR"] = "Turkey"
            COUNTRY_CODE_TO_NAME["TT"] = "Trinidad and Tobago"
            COUNTRY_CODE_TO_NAME["TV"] = "Tuvalu"
            COUNTRY_CODE_TO_NAME["TW"] = "Taiwan"
            COUNTRY_CODE_TO_NAME["TZ"] = "Tanzania"
            COUNTRY_CODE_TO_NAME["UA"] = "Ukraine"
            COUNTRY_CODE_TO_NAME["UG"] = "Uganda"
            COUNTRY_CODE_TO_NAME["US"] = "United States of America"
            COUNTRY_CODE_TO_NAME["UY"] = "Uruguay"
            COUNTRY_CODE_TO_NAME["UZ"] = "Uzbekistan"
            COUNTRY_CODE_TO_NAME["VA"] = "Holy See"
            COUNTRY_CODE_TO_NAME["VC"] = "Saint Vincent and the Grenadines"
            COUNTRY_CODE_TO_NAME["VE"] = "Venezuela"
            COUNTRY_CODE_TO_NAME["VN"] = "Viet Nam"
            COUNTRY_CODE_TO_NAME["VU"] = "Vanuatu"
            COUNTRY_CODE_TO_NAME["WS"] = "Samoa"
            COUNTRY_CODE_TO_NAME["YE"] = "Yemen"
            COUNTRY_CODE_TO_NAME["ZA"] = "South Africa"
            COUNTRY_CODE_TO_NAME["ZM"] = "Zambia"
            COUNTRY_CODE_TO_NAME["ZW"] = "Zimbabwe"
            COUNTRY_CODE_TO_NAME["AI"] = "Anguilla"
            COUNTRY_CODE_TO_NAME["AN"] = "Netherlands Antilles"
            COUNTRY_CODE_TO_NAME["AQ"] = "Antarctica"
            COUNTRY_CODE_TO_NAME["AS"] = "American Samoa"
            COUNTRY_CODE_TO_NAME["AW"] = "Aruba"
            COUNTRY_CODE_TO_NAME["AX"] = "Åland Islands"
            COUNTRY_CODE_TO_NAME["BL"] = "Saint Barthélemy"
            COUNTRY_CODE_TO_NAME["BM"] = "Bermuda"
            COUNTRY_CODE_TO_NAME["BQ"] = "Bonaire, Sint Eustatius and Saba"
            COUNTRY_CODE_TO_NAME["BV"] = "Bouvet Island"
            COUNTRY_CODE_TO_NAME["CC"] = "Cocos (Keeling) Islands"
            COUNTRY_CODE_TO_NAME["CK"] = "Cook Islands"
            COUNTRY_CODE_TO_NAME["CW"] = "Curaçao"
            COUNTRY_CODE_TO_NAME["CX"] = "Christmas Island"
            COUNTRY_CODE_TO_NAME["FK"] = "Falkland Islands (Malvinas)"
            COUNTRY_CODE_TO_NAME["GF"] = "French Guiana"
            COUNTRY_CODE_TO_NAME["GG"] = "Guernsey"
            COUNTRY_CODE_TO_NAME["GI"] = "Gibraltar"
            COUNTRY_CODE_TO_NAME["GL"] = "Greenland"
            COUNTRY_CODE_TO_NAME["GP"] = "Guadeloupe"
            COUNTRY_CODE_TO_NAME["GS"] = "South Georgia and the South Sandwich Islands"
            COUNTRY_CODE_TO_NAME["GU"] = "Guam"
            COUNTRY_CODE_TO_NAME["HM"] = "Heard Island and McDonald Islands"
            COUNTRY_CODE_TO_NAME["IM"] = "Isle of Man"
            COUNTRY_CODE_TO_NAME["IO"] = "British Indian Ocean Territory"
            COUNTRY_CODE_TO_NAME["JE"] = "Jersey"
            COUNTRY_CODE_TO_NAME["KY"] = "Cayman Islands"
            COUNTRY_CODE_TO_NAME["MF"] = "Saint Martin (French part)"
            COUNTRY_CODE_TO_NAME["MO"] = "Macao"
            COUNTRY_CODE_TO_NAME["MP"] = "Northern Mariana Islands"
            COUNTRY_CODE_TO_NAME["MQ"] = "Martinique"
            COUNTRY_CODE_TO_NAME["MS"] = "Montserrat"
            COUNTRY_CODE_TO_NAME["NC"] = "New Caledonia"
            COUNTRY_CODE_TO_NAME["NF"] = "Norfolk Island"
            COUNTRY_CODE_TO_NAME["NU"] = "Niue"
            COUNTRY_CODE_TO_NAME["PF"] = "French Polynesia"
            COUNTRY_CODE_TO_NAME["PM"] = "Saint Pierre and Miquelon"
            COUNTRY_CODE_TO_NAME["PN"] = "Pitcairn"
            COUNTRY_CODE_TO_NAME["PR"] = "Puerto Rico"
            COUNTRY_CODE_TO_NAME["PS"] = "Palestine, State of"
            COUNTRY_CODE_TO_NAME["RE"] = "Réunion"
            COUNTRY_CODE_TO_NAME["SH"] = "Saint Helena, Ascension and Tristan da Cunha"
            COUNTRY_CODE_TO_NAME["SJ"] = "Svalbard and Jan Mayen"
            COUNTRY_CODE_TO_NAME["SS"] = "South Sudan"
            COUNTRY_CODE_TO_NAME["SX"] = "Sint Maarten (Dutch part)"
            COUNTRY_CODE_TO_NAME["TC"] = "Turks and Caicos Islands"
            COUNTRY_CODE_TO_NAME["TF"] = "French Southern Territories"
            COUNTRY_CODE_TO_NAME["TK"] = "Tokelau"
            COUNTRY_CODE_TO_NAME["TL"] = "Timor-Leste"
            COUNTRY_CODE_TO_NAME["UM"] = "United States Minor Outlying Islands"
            COUNTRY_CODE_TO_NAME["VG"] = "Virgin Islands, British"
            COUNTRY_CODE_TO_NAME["VI"] = "Virgin Islands, U.S."
            COUNTRY_CODE_TO_NAME["WF"] = "Wallis and Futuna"
            COUNTRY_CODE_TO_NAME["YT"] = "Mayotte"
            for ((key, value) in COUNTRY_CODE_TO_NAME) {
                COUNTRY_NAME_TO_CODE[value] = key
            }
        }
    }

    private var mFusedLocationClient: FusedLocationProviderClient? = null

    override fun onCreate() {
        super.onCreate()
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.d("$TAG destroyed")
    }

    /**
     * Hook method called each time the Location Service is sent an
     * Intent via startService() to retrieve the country code and
     * reply to the client via the Messenger sent with the
     * Intent.
     */
    override fun onHandleWork(intent: Intent) {
        AppLogger.d("$TAG intent:$intent")
        requestCountryCode(
            applicationContext,
            object : LocationServiceListener {
                override fun onCountryCodeLocated(countryCode: String) {
                    val messenger = IntentUtils.getParcelableExtra<Messenger>(EXTRA_NAME_MESSENGER, intent)
                    val message = makeReplyMessage(countryCode)
                    try {
                        message.what = MSG_ID_ON_LOCATION
                        messenger?.send(message)
                    } catch (e: RemoteException) {
                        AppLogger.e("$TAG can't send on location message", e)
                    }
                }
            }
        )
    }

    /**
     * Do requests country code from fuse client.
     * Before call this service, permission check is done at Activity.
     *
     * @param context Context of callee.
     * @param listener Listener to the location event.
     */
    @SuppressLint("MissingPermission")
    private fun requestCountryCode(context: Context, listener: LocationServiceListener) {
        val locationListener = LocationListenerImpl(
            listener, context, mFusedLocationClient!!
        )
        val interval = 100L
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval).build()
        mFusedLocationClient?.requestLocationUpdates(
            request,
            locationListener,
            Looper.getMainLooper()
        )
    }

    /**
     * Define a listener that responds to location updates.
     */
    private class LocationListenerImpl(
        private var mListener: LocationServiceListener,
        context: Context,
        fusedLocationClient: FusedLocationProviderClient
    ) : LocationCallback() {
        private var mContext: Context?
        private var mFusedLocationClient: FusedLocationProviderClient?
        private val mCounter: AtomicInteger
        private var mCountryBoundaries: CountryBoundaries? = null
        override fun onLocationAvailability(availability: LocationAvailability) {
            super.onLocationAvailability(availability)
            if (!availability.isLocationAvailable) {
                clear()
            }
        }

        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if (mCounter.getAndIncrement() < MAX_COUNT) {
                return
            }
            if (mContext == null) {
                return
            }
            var countryCode = Country.COUNTRY_CODE_DEFAULT
            if (mCountryBoundaries == null) {
                mListener.onCountryCodeLocated(countryCode)
                clear()
                return
            }
            countryCode = try {
                val location = result.lastLocation
                extractCountryCode(
                    mCountryBoundaries?.getIds(
                        location?.longitude ?: Country.LONG_DEFAULT,
                        location?.latitude ?: Country.LAT_DEFAULT
                    )
                )
            } catch (e: Exception) {
                Country.COUNTRY_CODE_DEFAULT
            }
            mListener.onCountryCodeLocated(countryCode)
            clear()
        }

        private fun extractCountryCode(data: List<String>?): String {
            var result = Country.COUNTRY_CODE_DEFAULT
            if (data == null) {
                return result
            }
            if (data.isEmpty()) {
                return result
            }
            for (id in data) {
                // Need to get ISO standard only.
                if (COUNTRY_CODE_TO_NAME.containsKey(id)) {
                    // Do not break here, let's print all codes.
                    result = id
                }
            }
            return result
        }

        private fun clear() {
            if (mContext == null) {
                return
            }
            mFusedLocationClient?.removeLocationUpdates(this)
            mContext = null
            mFusedLocationClient = null
        }

        companion object {
            private const val MAX_COUNT = 0
        }

        init {
            mContext = context
            mFusedLocationClient = fusedLocationClient
            mCounter = AtomicInteger(0)
            try {
                mCountryBoundaries = CountryBoundaries.load(mContext?.assets?.open("boundaries.ser"))
            } catch (e: IOException) {
                // Ignore up to now ...
                AppLogger.e("$TAG can not load boundaries.ser", e)
            }
        }
    }
}
