/*
 * Copyright 2015 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 4/27/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class LocationService {

    private static final String CLASS_NAME = LocationService.class.getSimpleName();

    /**
     *
     */
    private String mCountryCode = "";

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
     *
     * @return
     */
    public String getCountryCode() {
        return mCountryCode;
    }

    public void requestCountryCodeLastKnown(final Context context) {
        final LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        String locationProvider = LocationManager.NETWORK_PROVIDER;
        // Or use LocationManager.GPS_PROVIDER

        final Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        if (lastKnownLocation == null) {
            Log.w(CLASS_NAME, "LastKnownLocation unavailable");
            return;
        }

        try {
            mCountryCode = extractCountryCode(
                    context, lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()
            );
        } catch (IOException e) {
            mCountryCode = "";
        }

        Log.d(CLASS_NAME, "LastKnownLocation:" + mCountryCode);
    }

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
                    mCountryCode = extractCountryCode(
                            context, location.getLatitude(), location.getLongitude()
                    );
                } catch (IOException e) {
                    mCountryCode = "";
                }
                listener.onCountryCodeLocated(mCountryCode);
                locationManager.removeUpdates(this);
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

        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 0, 0, locationListener
        );
    }

    private static String extractCountryCode(final Context context, final double latitude,
                                             final double longitude) throws IOException {
        final Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        final List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

        if (addresses == null || addresses.isEmpty()) {
            return "";
        }

        return addresses.get(0).getCountryCode();
    }
}
