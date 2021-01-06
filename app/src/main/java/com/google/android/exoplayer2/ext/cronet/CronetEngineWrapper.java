/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;

import org.chromium.net.CronetEngine;
import org.chromium.net.CronetProvider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.min;

/**
 * A wrapper class for a {@link CronetEngine}.
 */
public final class CronetEngineWrapper {

    private static final String TAG = "CronetEngineWrapper";

    @Nullable
    private final CronetEngine cronetEngine;

    /**
     * Creates a wrapper for a {@link CronetEngine} which automatically selects the most suitable
     * {@link CronetProvider}. Sets wrapper to prefer natively bundled Cronet over GMSCore Cronet
     * if both are available.
     *
     * @param context A context.
     */
    public CronetEngineWrapper(Context context) {
        this(context, false);
    }

    /**
     * Creates a wrapper for a {@link CronetEngine} which automatically selects the most suitable
     * {@link CronetProvider} based on user preference.
     *
     * @param context             A context.
     * @param preferGMSCoreCronet Whether Cronet from GMSCore should be preferred over natively
     *                            bundled Cronet if both are available.
     */
    public CronetEngineWrapper(Context context, boolean preferGMSCoreCronet) {
        CronetEngine cronetEngine = null;
        List<CronetProvider> cronetProviders = new ArrayList<>(CronetProvider.getAllProviders(context));
        // Remove disabled and fallback Cronet providers from list
        for (int i = cronetProviders.size() - 1; i >= 0; i--) {
            if (!cronetProviders.get(i).isEnabled()
                    || CronetProvider.PROVIDER_NAME_FALLBACK.equals(cronetProviders.get(i).getName())) {
                cronetProviders.remove(i);
            }
        }
        // Sort remaining providers by type and version.
        CronetProviderComparator providerComparator = new CronetProviderComparator(preferGMSCoreCronet);
        Collections.sort(cronetProviders, providerComparator);
        for (int i = 0; i < cronetProviders.size() && cronetEngine == null; i++) {
            String providerName = cronetProviders.get(i).getName();
            try {
                cronetEngine = cronetProviders.get(i).createBuilder().build();
                Log.d(TAG, "CronetEngine built using " + providerName);
            } catch (SecurityException e) {
                Log.w(TAG, "Failed to build CronetEngine. Please check if current process has "
                        + "android.permission.ACCESS_NETWORK_STATE.");
            } catch (UnsatisfiedLinkError e) {
                Log.w(TAG, "Failed to link Cronet binaries. Please check if native Cronet binaries are "
                        + "bundled into your app.");
            }
        }
        if (cronetEngine == null) {
            Log.w(TAG, "Cronet not available. Using fallback provider.");
        }
        this.cronetEngine = cronetEngine;
    }

    /**
     * Returns the wrapped {@link CronetEngine}.
     *
     * @return The CronetEngine, or null if no CronetEngine is available.
     */
    @Nullable
    /* package */ CronetEngine getCronetEngine() {
        return cronetEngine;
    }

    private static class CronetProviderComparator implements Comparator<CronetProvider> {

        @Nullable
        private final String gmsCoreCronetName;
        private final boolean preferGMSCoreCronet;

        // Multi-catch can only be used for API 19+ in this case.
        // Field#get(null) is blocked by the null-checker, but is safe because the field is static.
        @SuppressWarnings({"UseMultiCatch", "nullness:argument.type.incompatible"})
        public CronetProviderComparator(boolean preferGMSCoreCronet) {
            // GMSCore CronetProvider classes are only available in some configurations.
            // Thus, we use reflection to copy static name.
            String gmsCoreVersionString = null;
            try {
                Class<?> cronetProviderInstallerClass =
                        Class.forName("com.google.android.gms.net.CronetProviderInstaller");
                Field providerNameField = cronetProviderInstallerClass.getDeclaredField("PROVIDER_NAME");
                gmsCoreVersionString = (String) providerNameField.get(null);
            } catch (ClassNotFoundException e) {
                // GMSCore CronetProvider not available.
            } catch (NoSuchFieldException e) {
                // GMSCore CronetProvider not available.
            } catch (IllegalAccessException e) {
                // GMSCore CronetProvider not available.
            }
            gmsCoreCronetName = gmsCoreVersionString;
            this.preferGMSCoreCronet = preferGMSCoreCronet;
        }

        @Override
        public int compare(CronetProvider providerLeft, CronetProvider providerRight) {
            int typePreferenceLeft = evaluateCronetProviderType(providerLeft.getName());
            int typePreferenceRight = evaluateCronetProviderType(providerRight.getName());
            if (typePreferenceLeft != typePreferenceRight) {
                return typePreferenceLeft - typePreferenceRight;
            }
            return -compareVersionStrings(providerLeft.getVersion(), providerRight.getVersion());
        }

        public boolean isNativeProvider(String providerName) {
            return CronetProvider.PROVIDER_NAME_APP_PACKAGED.equals(providerName);
        }

        public boolean isGMSCoreProvider(String providerName) {
            return gmsCoreCronetName != null && gmsCoreCronetName.equals(providerName);
        }

        /**
         * Convert Cronet provider name into a sortable preference value.
         * Smaller values are preferred.
         */
        private int evaluateCronetProviderType(String providerName) {
            if (isNativeProvider(providerName)) {
                return 1;
            }
            if (isGMSCoreProvider(providerName)) {
                return preferGMSCoreCronet ? 0 : 2;
            }
            // Unknown provider type.
            return -1;
        }

        /**
         * Compares version strings of format "12.123.35.23".
         */
        private static int compareVersionStrings(String versionLeft, String versionRight) {
            if (versionLeft == null || versionRight == null) {
                return 0;
            }
            String[] versionStringsLeft = Util.split(versionLeft, "\\.");
            String[] versionStringsRight = Util.split(versionRight, "\\.");
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
