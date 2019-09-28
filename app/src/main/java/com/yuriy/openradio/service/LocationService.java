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

package com.yuriy.openradio.service;

import android.Manifest;
import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.yuriy.openradio.model.api.GeoAPI;
import com.yuriy.openradio.model.api.GeoAPIImpl;
import com.yuriy.openradio.model.net.Downloader;
import com.yuriy.openradio.model.net.HTTPDownloaderImpl;
import com.yuriy.openradio.model.net.UrlBuilder;
import com.yuriy.openradio.model.parser.GeoDataParser;
import com.yuriy.openradio.model.parser.GoogleGeoDataParserJson;
import com.yuriy.openradio.model.storage.GeoAPIStorage;
import com.yuriy.openradio.permission.PermissionChecker;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.vo.Country;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import de.westnordost.countryboundaries.CountryBoundaries;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 4/27/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class LocationService extends IntentService {

    private static final String CLASS_NAME = LocationService.class.getSimpleName() + " ";

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

    public static final int COUNTRY_REQUEST_MIN_WAIT = 60000;

    /**
     * String constant used to extract the Messenger "extra" from an intent.
     */
    private static final String MESSENGER = "MESSENGER";

    /**
     * String constant used to extract the country code "extra" from an intent.
     */
    private static final String COUNTRY_CODE = "COUNTRY_CODE";

    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Easy and fast way to cache country code.
     */
    private static String sCountryCode;

    private CountryBoundaries mCountryBoundaries;

    /**
     * Private constructor.
     */
    public LocationService() {
        super("LocationService");
    }

    /**
     * Factory method to make the desired Intent.
     */
    public static Intent makeIntent(final Context context, final Handler handler) {
        // Create an intent associated with the Location Service class.
        return new Intent(context, LocationService.class)
                // Create and pass a Messenger as an "extra" so the
                // Location Service can send back the Location.
                .putExtra(MESSENGER, new Messenger(handler));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            mCountryBoundaries = CountryBoundaries.load(getAssets().open("boundaries.ser"));
        } catch (final IOException e) {
            //
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
    }

    /**
     * Hook method called each time the Location Service is sent an
     * Intent via startService() to retrieve the country code and
     * reply to the client via the Messenger sent with the
     * Intent.
     */
    @Override
    public void onHandleIntent(final Intent intent) {
        AppLogger.d(CLASS_NAME + "Handle Location intent:" + intent);
        final String[] result = new String[1];
        // Max time to await for country code to be detected, in sec.
        final int maxAwaitTime = 2;
        // Delay prior to request country code, in millisec.
        // It is necessary to give time to Looper to start running prior to call location APIs.
        final int delay = 100;
        final CountDownLatch latch = new CountDownLatch(1);
        // Use simple thread here and not executor's API because executor can handle new call in the same thread.
        // While this is good resource keeper, Loop handling will be more complicated. Keep things simple - create
        // new thread on each request. The good news is - new request is only happening on app start up.
        final Thread thread = new Thread(
                () -> {
                    Looper.prepare();
                    final Handler handler = new Handler();
                    handler.postDelayed(
                            () -> requestCountryCode(
                                    getApplicationContext(),
                                    countryCode -> {
                                        // Get current country code.
                                        result[0] = countryCode;

                                        sCountryCode = countryCode;

                                        Looper.myLooper().quit();
                                        latch.countDown();
                                    },
                                    Looper.myLooper()
                            ), delay
                    );
                    Looper.loop();
                }
        );
        thread.start();

        try {
            latch.await(maxAwaitTime, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            //
        }
        // Send the country code back to client.
        sendCountryCode(intent, result[0]);
    }

    /**
     * Send the country code back to the client via the
     * messenger that's stored in the intent.
     */
    private void sendCountryCode(final Intent intent, final String countryCode) {
        // Extract the Messenger.
        final Messenger messenger = (Messenger) intent.getExtras().get(MESSENGER);

        // Call factory method to create Message.
        final Message message = makeReplyMessage(countryCode);

        try {
            // Send country code to back to the client.
            messenger.send(message);
        } catch (final RemoteException e) {
            AppLogger.e(CLASS_NAME + "Exception while sending Location back to client:" + e);
        }
    }

    /**
     * A factory method that creates a Message to return to the client with the country code.
     */
    private Message makeReplyMessage(final String countryCode) {
        final Message message = Message.obtain();
        // Return the result to indicate whether the get country code succeeded or failed.
        if (countryCode != null) {
            message.arg1 = Activity.RESULT_OK;
            final Bundle data = new Bundle();

            // Current user's country code.
            data.putString(COUNTRY_CODE, countryCode);
            message.setData(data);
            AppLogger.d(CLASS_NAME + "put country code into message");
        } else {
            message.arg1 = Activity.RESULT_CANCELED;
        }

        return message;
    }

    /**
     * @return Cached country code.
     */
    public static String getCountryCode() {
        return sCountryCode;
    }

    /**
     * Helper method that returns country code if succeeded.
     */
    public static String getCountryCode(final Message message) {
        // Extract the data from Message, which is in the form
        // of a Bundle that can be passed across processes.
        final Bundle data = message.getData();

        // Extract the country code from the Bundle.
        final String countryCode = data.getString(COUNTRY_CODE);

        // Check to see if the get Location succeeded.
        if (message.arg1 != Activity.RESULT_OK || countryCode == null) {
            return null;
        } else {
            return countryCode;
        }
    }

    private void requestCountryCode(final Context context, final LocationServiceListener listener,
                                    final Looper looper) {
        final LocationCallback locationListener = new LocationListenerImpl(
                this, listener, context, mFusedLocationClient
        );
        mFusedLocationClient.requestLocationUpdates(
                createLocationRequest(),
                locationListener,
                looper
        );
    }

    /**
     * Call Geo API to get current country.
     *
     * @param context   Context of the application.
     * @param latitude  Latitude of the location.
     * @param longitude Longitude of the location.
     * @return Country code.
     */
    private static String getCountryCode(final Context context,
                                         final double latitude, final double longitude) {
        Country country;

        final long lastUsedTime = GeoAPIStorage.getLastUseTime(context);
        final long currentTime = System.currentTimeMillis();
        if (lastUsedTime != GeoAPIStorage.LAST_USE_TIME_DEFAULT
                && currentTime - lastUsedTime < COUNTRY_REQUEST_MIN_WAIT) {
            country = new Country(
                    GeoAPIStorage.getLastKnownCountryName(context),
                    GeoAPIStorage.getLastKnownCountryCode(context)
            );
            return country.getCode();
        }

        final GeoDataParser parser = new GoogleGeoDataParserJson();
        final GeoAPI geoAPI = new GeoAPIImpl(parser);
        final Downloader downloader = new HTTPDownloaderImpl();
        final Uri uri = UrlBuilder.getGoogleGeoAPIUrl(latitude, longitude);
        country = geoAPI.getCountry(downloader, uri);

        GeoAPIStorage.setLastUseTime(currentTime, context);
        GeoAPIStorage.setLastKnownCountryName(country.getName(), context);
        GeoAPIStorage.setLastKnownCountryCode(country.getCode(), context);

        return country.getCode();
    }

    private LocationRequest createLocationRequest() {
        final LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(0);
        locationRequest.setFastestInterval(0);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    /**
     * Define a listener that responds to location updates.
     */
    private static final class LocationListenerImpl extends LocationCallback {

        private final WeakReference<LocationService> mReference;
        @Nullable
        private final LocationServiceListener mListener;
        private final Context mContext;
        @NonNull
        private final FusedLocationProviderClient mFusedLocationClient;
        private final AtomicInteger mCounter;
        private static final int MAX_COUNT = 0;

        private LocationListenerImpl(final LocationService reference,
                                     @Nullable final LocationServiceListener listener,
                                     final Context context,
                                     @NonNull final FusedLocationProviderClient fusedLocationClient) {
            super();
            mReference = new WeakReference<>(reference);
            mListener = listener;
            mContext = context;
            mFusedLocationClient = fusedLocationClient;
            mCounter = new AtomicInteger(0);
        }

        @Override
        public void onLocationResult(final LocationResult result) {
            super.onLocationResult(result);
            AppLogger.d(
                    "On Location changed (" + mCounter.get() + "):" + result.getLastLocation()
            );
            if (mCounter.getAndIncrement() < MAX_COUNT) {
                return;
            }
            if (PermissionChecker.isGranted(mContext, Manifest.permission.ACCESS_FINE_LOCATION)) {
                mFusedLocationClient.removeLocationUpdates(this);
            }

            final LocationService reference = mReference.get();
            if (reference == null) {
                return;
            }

            String countryCode = Country.COUNTRY_CODE_DEFAULT;

            final Location location = result.getLastLocation();
            if (location == null) {
                if (mListener != null) {
                    mListener.onCountryCodeLocated(countryCode);
                }
                return;
            }

            if (reference.mCountryBoundaries == null) {
                if (mListener != null) {
                    mListener.onCountryCodeLocated(countryCode);
                }
                return;
            }

            countryCode = extractCountryCode(
                    reference.mCountryBoundaries.getIds(
                            location.getLongitude(), location.getLatitude()
                    )
            );

            if (mListener != null) {
                mListener.onCountryCodeLocated(countryCode);
            }
        }

        private String extractCountryCode(final List<String> data) {
            String result = Country.COUNTRY_CODE_DEFAULT;
            if (data == null) {
                return result;
            }
            if (data.isEmpty()) {
                return result;
            }
            AppLogger.d(CLASS_NAME + "Found " + data.size() + " boundaries");
            for (final String id : data) {
                AppLogger.d(CLASS_NAME + "  " + id);
                // Need to get ISO standard only.
                if (COUNTRY_CODE_TO_NAME.containsKey(id)) {
                    // Do not break here, let's print all codes.
                    result = id;
                }
            }
            return result;
        }
    }
}
