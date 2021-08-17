/*
 * Copyright 2021 The Android Open Source Project
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
package com.google.android.exoplayer2.ext.cronet;

import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;

import org.chromium.net.CronetEngine;
import org.chromium.net.CronetProvider;

import static java.lang.Math.min;

/**
 * Cronet utility methods.
 */
public final class CronetUtil {

    private static final String TAG = "CronetUtil";

    /**
     * Builds a {@link CronetEngine} suitable for use with ExoPlayer. When choosing a {@link
     * CronetProvider Cronet provider} to build the {@link CronetEngine}, disabled providers are not
     * considered. Neither are fallback providers, since it's more efficient to use {@link
     * DefaultHttpDataSource} than it is to use {@link CronetDataSource} with a fallback {@link
     * CronetEngine}.
     *
     * <p>Note that it's recommended for applications to create only one instance of {@link
     * CronetEngine}, so if your application already has an instance for performing other networking,
     * then that instance should be used and calling this method is unnecessary. See the <a
     * href="https://developer.android.com/guide/topics/connectivity/cronet/start">Android developer
     * guide</a> to learn more about using Cronet for network operations.
     *
     * @param context                  A context.
     * @param userAgent                A default user agent, or {@code null} to use a default user agent of the
     *                                 {@link CronetEngine}.
     * @param preferGooglePlayServices Whether Cronet from Google Play Services should be preferred
     *                                 over Cronet Embedded, if both are available.
     * @return The {@link CronetEngine}, or {@code null} if no suitable engine could be built.
     */
    @androidx.annotation.Nullable
    public static org.chromium.net.CronetEngine buildCronetEngine(
            android.content.Context context, @androidx.annotation.Nullable String userAgent, boolean preferGooglePlayServices) {
        java.util.List<org.chromium.net.CronetProvider> cronetProviders = new java.util.ArrayList<>(org.chromium.net.CronetProvider.getAllProviders(context));
        // Remove disabled and fallback Cronet providers from list.
        for (int i = cronetProviders.size() - 1; i >= 0; i--) {
            if (!cronetProviders.get(i).isEnabled()
                    || org.chromium.net.CronetProvider.PROVIDER_NAME_FALLBACK.equals(cronetProviders.get(i).getName())) {
                cronetProviders.remove(i);
            }
        }
        // Sort remaining providers by type and version.
        com.google.android.exoplayer2.ext.cronet.CronetUtil.CronetProviderComparator providerComparator =
                new com.google.android.exoplayer2.ext.cronet.CronetUtil.CronetProviderComparator(preferGooglePlayServices);
        java.util.Collections.sort(cronetProviders, providerComparator);
        for (int i = 0; i < cronetProviders.size(); i++) {
            String providerName = cronetProviders.get(i).getName();
            try {
                org.chromium.net.CronetEngine.Builder cronetEngineBuilder = cronetProviders.get(i).createBuilder();
                if (userAgent != null) {
                    cronetEngineBuilder.setUserAgent(userAgent);
                }
                org.chromium.net.CronetEngine cronetEngine = cronetEngineBuilder.build();
                com.google.android.exoplayer2.util.Log.d(TAG, "CronetEngine built using " + providerName);
                return cronetEngine;
            } catch (SecurityException e) {
                com.google.android.exoplayer2.util.Log.w(
                        TAG,
                        "Failed to build CronetEngine. Please check that the process has "
                                + "android.permission.ACCESS_NETWORK_STATE.");
            } catch (UnsatisfiedLinkError e) {
                com.google.android.exoplayer2.util.Log.w(
                        TAG,
                        "Failed to link Cronet binaries. Please check that native Cronet binaries are"
                                + "bundled into your app.");
            }
        }
        com.google.android.exoplayer2.util.Log.w(TAG, "CronetEngine could not be built.");
        return null;
    }

    private CronetUtil() {
    }

    private static class CronetProviderComparator implements java.util.Comparator<org.chromium.net.CronetProvider> {

        /*
         * Copy of com.google.android.gms.net.CronetProviderInstaller.PROVIDER_NAME. We have our own
         * copy because GMSCore CronetProvider classes are unavailable in some (internal to Google)
         * build configurations.
         */
        private static final String GOOGLE_PLAY_SERVICES_PROVIDER_NAME =
                "Google-Play-Services-Cronet-Provider";

        private final boolean preferGooglePlayServices;

        public CronetProviderComparator(boolean preferGooglePlayServices) {
            this.preferGooglePlayServices = preferGooglePlayServices;
        }

        @Override
        public int compare(org.chromium.net.CronetProvider providerLeft, org.chromium.net.CronetProvider providerRight) {
            int providerComparison = getPriority(providerLeft) - getPriority(providerRight);
            if (providerComparison != 0) {
                return providerComparison;
            }
            return -compareVersionStrings(providerLeft.getVersion(), providerRight.getVersion());
        }

        /**
         * Returns the priority score for a Cronet provider, where a smaller score indicates higher
         * priority.
         */
        private int getPriority(org.chromium.net.CronetProvider provider) {
            String providerName = provider.getName();
            if (org.chromium.net.CronetProvider.PROVIDER_NAME_APP_PACKAGED.equals(providerName)) {
                return 1;
            } else if (GOOGLE_PLAY_SERVICES_PROVIDER_NAME.equals(providerName)) {
                return preferGooglePlayServices ? 0 : 2;
            } else {
                return 3;
            }
        }

        /**
         * Compares version strings of format "12.123.35.23".
         */
        private static int compareVersionStrings(
                @androidx.annotation.Nullable String versionLeft, @androidx.annotation.Nullable String versionRight) {
            if (versionLeft == null || versionRight == null) {
                return 0;
            }
            String[] versionStringsLeft = com.google.android.exoplayer2.util.Util.split(versionLeft, "\\.");
            String[] versionStringsRight = com.google.android.exoplayer2.util.Util.split(versionRight, "\\.");
            int minLength = min(versionStringsLeft.length, versionStringsRight.length);
            for (int i = 0; i < minLength; i++) {
                if (!versionStringsLeft[i].equals(versionStringsRight[i])) {
                    try {
                        int versionIntLeft = Integer.parseInt(versionStringsLeft[i]);
                        int versionIntRight = Integer.parseInt(versionStringsRight[i]);
                        return versionIntLeft - versionIntRight;
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
            }
            return 0;
        }
    }
}
