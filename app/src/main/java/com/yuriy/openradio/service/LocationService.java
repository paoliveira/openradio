package com.yuriy.openradio.service;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

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

    public LocationService() {

    }

    public static void getCountry(final Context context, final LocationServiceListener listener) {
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
                    listener.onCountryCodeLocated(
                            extractCountryCode(
                                    context, location.getLatitude(), location.getLongitude()
                            )
                    );
                } catch (IOException e) {
                    listener.onCountryCodeLocated("");
                }

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
