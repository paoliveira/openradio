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
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.yuriy.openradio.api.GeoAPI;
import com.yuriy.openradio.api.GeoAPIImpl;
import com.yuriy.openradio.business.location.GeoDataParser;
import com.yuriy.openradio.business.location.IPAPIDataParserJson;
import com.yuriy.openradio.business.storage.GeoAPIStorage;
import com.yuriy.openradio.net.Downloader;
import com.yuriy.openradio.net.HTTPDownloaderImpl;
import com.yuriy.openradio.net.UrlBuilder;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.utils.FabricUtils;
import com.yuriy.openradio.utils.PermissionChecker;
import com.yuriy.openradio.vo.Country;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 4/27/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class LocationService {

    private static final String CLASS_NAME = LocationService.class.getSimpleName();
    /**
     * Default value of the Country Code.
     */
    public static final String COUNTRY_CODE_DEFAULT = "CA";
    public static final String COUNTRY_NAME_DEFAULT = "Canada";

    public static final int COUNTRY_REQUEST_MIN_WAIT = 60000;

    /**
     * Obtained value of the Country Code.
     */
    private String mCountryCode = COUNTRY_CODE_DEFAULT;

    /**
     * Private constructor.
     */
    private LocationService() { }

    /**
     * Factory method to return default instance of the {@link LocationService}.
     *
     * @return Instance of the {@link LocationService}.
     */
    public static LocationService getInstance() {
        return new LocationService();
    }

    /**
     * @return Country code for the current user's location.
     */
    String getCountryCode() {
        return mCountryCode;
    }

    /**
     * Check whether location service is enabled on the phone. It dispatch appropriate local
     * event in case of negative result.
     *
     * @param context {@link Context} of the callee.
     */
    void checkLocationEnable(final Context context) {
        final LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        final String locationProvider = LocationManager.NETWORK_PROVIDER;

        if (locationManager.isProviderEnabled(locationProvider)) {
            return;
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(
                AppLocalBroadcastReceiver.createIntentLocationDisabled()
        );
    }

    /**
     * Request last know user's country code.
     *
     * @param context {@link Context} of the callee.
     */
    @SuppressWarnings("ResourceType")
    void requestCountryCodeLastKnownSync(final Context context,
                                         final ExecutorService executorService) {
        final LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            AppLogger.w(CLASS_NAME + " Location Manager unavailable");
            return;
        }

        final String locationProvider = LocationManager.NETWORK_PROVIDER;
        // Or use LocationManager.GPS_PROVIDER

        if (!PermissionChecker.isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            AppLogger.w("Location permission not granted");
            return;
        }

        final Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        if (lastKnownLocation == null) {
            AppLogger.w(CLASS_NAME + " Last known Location unavailable");
            return;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        executorService.submit(
                () -> {
                    try {
                        LocationService.this.mCountryCode = getCountryCodeGeocoder(
                                context, lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()
                        );
                    } finally {
                        latch.countDown();
                    }

                    AppLogger.d(CLASS_NAME + " Last known Location:" + LocationService.this.mCountryCode);
                }
        );
        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            LocationService.this.mCountryCode = COUNTRY_CODE_DEFAULT;
            FabricUtils.logException(e);
        }
    }

    @SuppressWarnings("ResourceType")
    void requestCountryCode(final Context context, final LocationServiceListener listener) {
        // Acquire a reference to the system Location Manager
        final LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (!PermissionChecker.isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            return;
        }

        final LocationListener locationListener = new LocationListenerImpl(
                this, listener, context, locationManager
        );
        // Register the listener with the Location Manager to receive location updates
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 0, 0, locationListener
            );
        } catch (final Exception e) {
            FabricUtils.logException(e);

            mCountryCode = COUNTRY_CODE_DEFAULT;
            listener.onCountryCodeLocated(mCountryCode);
        }
    }

    private static String getCountryCodeGeocoder(final Context context,
                                                 final double latitude,
                                                 final double longitude) {
        final Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
        } catch (final Exception exception) {
            final boolean isConnected = AppUtils.checkConnectivity(context);
            final String countryCode = getCountryCode(context, latitude, longitude);
            final String msg = "Can not get geocoder location for lat:" + latitude
                    + ", long:" + longitude + ", country by ip-api:" + countryCode
                    + ", connected:" + isConnected;
            FabricUtils.logException(new Exception(msg, exception));
            return countryCode;
        }

        if (addresses == null || addresses.isEmpty()) {
            return COUNTRY_CODE_DEFAULT;
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

        final GeoDataParser parser = new IPAPIDataParserJson();
        final GeoAPI geoAPI = new GeoAPIImpl(parser);
        final Downloader downloader = new HTTPDownloaderImpl();
        final Uri uri = UrlBuilder.getIPAPIUrl();
        country = geoAPI.getCountry(downloader, uri);

        GeoAPIStorage.setLastUseTime(currentTime, context);
        GeoAPIStorage.setLastKnownCountryName(country.getName(), context);
        GeoAPIStorage.setLastKnownCountryCode(country.getCode(), context);

        return country.getCode();
    }

    /**
     *  Define a listener that responds to location updates.
     */
    private static final class LocationListenerImpl implements LocationListener {

        private final WeakReference<LocationService> mReference;
        private final LocationServiceListener mListener;
        private final Context mContext;
        @NonNull
        private final LocationManager mLocationManager;

        private LocationListenerImpl(final LocationService reference,
                                     final LocationServiceListener listener,
                                     final Context context,
                                     @NonNull final LocationManager locationManager) {
            super();
            mReference = new WeakReference<>(reference);
            mListener = listener;
            mContext = context;
            mLocationManager = locationManager;
        }

        @Override
        public void onLocationChanged(final Location location) {

            if (mListener == null) {
                return;
            }
            final LocationService reference = mReference.get();
            if (reference == null) {
                return;
            }

            reference.mCountryCode = getCountryCodeGeocoder(
                    mContext, location.getLatitude(), location.getLongitude()
            );
            mListener.onCountryCodeLocated(reference.mCountryCode);

            if (!PermissionChecker.isGranted(mContext, Manifest.permission.ACCESS_FINE_LOCATION)) {
                return;
            }

            try {
                mLocationManager.removeUpdates(this);
            } catch (final IllegalArgumentException e) {
                FabricUtils.logException(e);
            }
        }

        @Override
        public void onStatusChanged(final String provider,
                                    final int status,
                                    final Bundle extras) {

        }

        @Override
        public void onProviderEnabled(final String provider) {

        }

        @Override
        public void onProviderDisabled(final String provider) {

        }
    }
}
