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
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.FabricUtils;
import com.yuriy.openradio.utils.PermissionChecker;

import java.io.IOException;
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
public class LocationService {

    private static final String CLASS_NAME = LocationService.class.getSimpleName();
    /**
     * Default value of the Country Code.
     */
    private static final String COUNTRY_CODE_DEFAULT = "CA";

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
    public String getCountryCode() {
        return mCountryCode;
    }

    /**
     * Check whether location service is enabled on the phone. It dispatch appropriate local
     * event in case of negative result.
     *
     * @param context {@link Context} of the callee.
     */
    public void checkLocationEnable(final Context context) {
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
    public void requestCountryCodeLastKnownSync(final Context context, final ExecutorService executorService) {
        final LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        final String locationProvider = LocationManager.NETWORK_PROVIDER;
        // Or use LocationManager.GPS_PROVIDER

        if (!PermissionChecker.isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            return;
        }

        final Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        if (lastKnownLocation == null) {
            AppLogger.w(CLASS_NAME + " LastKnownLocation unavailable");
            return;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        executorService.submit(
                () -> {
                    try {
                        LocationService.this.mCountryCode = extractCountryCode(
                                context, lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()
                        );
                    } catch (final IOException e) {
                        LocationService.this.mCountryCode = COUNTRY_CODE_DEFAULT;
                        FabricUtils.logException(e);
                    } finally {
                        latch.countDown();
                    }

                    AppLogger.d(CLASS_NAME + " LastKnownLocation:" + LocationService.this.mCountryCode);
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
    public void requestCountryCode(final Context context, final LocationServiceListener listener) {
        // Acquire a reference to the system Location Manager
        final LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        final LocationListener locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {

                if (listener == null) {
                    return;
                }

                try {
                    LocationService.this.mCountryCode = extractCountryCode(
                            context, location.getLatitude(), location.getLongitude()
                    );
                } catch (final IOException e) {
                    LocationService.this.mCountryCode = COUNTRY_CODE_DEFAULT;
                    FabricUtils.logException(e);
                }
                listener.onCountryCodeLocated(LocationService.this.mCountryCode);

                if (!PermissionChecker.isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    return;
                }

                try {
                    locationManager.removeUpdates(this);
                } catch (final IllegalArgumentException e) {
                    FabricUtils.logException(e);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        if (!PermissionChecker.isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            return;
        }

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

    private static String extractCountryCode(final Context context, final double latitude,
                                             final double longitude) throws IOException {
        final Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        final List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

        if (addresses == null || addresses.isEmpty()) {
            return COUNTRY_CODE_DEFAULT;
        }

        return addresses.get(0).getCountryCode();
    }
}
