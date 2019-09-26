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
import android.location.Address;
import android.location.Geocoder;
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
import com.yuriy.openradio.broadcast.ConnectivityReceiver;
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
import com.yuriy.openradio.utils.ConcurrentUtils;
import com.yuriy.openradio.utils.FabricUtils;
import com.yuriy.openradio.vo.Country;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 4/27/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class LocationService extends IntentService {

    private static final String CLASS_NAME = LocationService.class.getSimpleName() + " ";

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

    // TODO - quick and dirty solution to test
    public static String sCountryCode;

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
        final String[] result = new String[1];

        final CountDownLatch latch = new CountDownLatch(1);
        ConcurrentUtils.LOCATION_EXECUTOR.submit(
                () -> {
                    Looper.prepare();
                    requestCountryCode(
                            getApplicationContext(),
                            countryCode -> {
                                // Get current country code.
                                result[0] = countryCode;

                                sCountryCode = countryCode;

                                Looper.myLooper().quit();
                                latch.countDown();
                            },
                            Looper.myLooper()
                    );
                    Looper.loop();
                }
        );

        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
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

    void requestCountryCode(final Context context, final LocationServiceListener listener,
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

    private static String getCountryCodeGeocoder(final Context context,
                                                 final double latitude,
                                                 final double longitude) {
        final Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
        } catch (final Exception exception) {
            final boolean isConnected = ConnectivityReceiver.checkConnectivity(context);
            final String countryCode = getCountryCode(context, latitude, longitude);
            final String msg = "Can not get geocoder location for lat:" + latitude
                    + ", long:" + longitude + ", country by geo api:" + countryCode
                    + ", connected:" + isConnected;
            FabricUtils.logException(new Exception(msg, exception));
            return countryCode;
        }

        if (addresses == null || addresses.isEmpty()) {
            return Country.COUNTRY_CODE_DEFAULT;
        }

        return addresses.get(0).getCountryCode();
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

            final Location location = result.getLastLocation();
            if (location == null) {
                return;
            }

            String countryCode = getCountryCodeGeocoder(
                    mContext, location.getLatitude(), location.getLongitude()
            );
            AppLogger.d(
                    "On Location changed (" + mCounter.get() + "):" + countryCode
            );
            if (mListener != null) {
                mListener.onCountryCodeLocated(countryCode);
            }
        }
    }
}
