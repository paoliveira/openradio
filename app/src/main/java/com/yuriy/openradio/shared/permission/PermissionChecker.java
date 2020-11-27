/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;
import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.utils.AppUtils;

/**
 * {@link PermissionChecker} is a helper class that designed to manage permissions changes
 * introduced in API 23.
 */
public final class PermissionChecker {

    private PermissionChecker() {
        super();
    }

    public static boolean isLocationGranted(@NonNull final Context context) {
        return PermissionChecker.isGranted(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        );
    }

    /**
     * Checks whether provided permission is granted or not.
     *
     * @param context    Application's context.
     * @param permission Permission name to check.
     * @return <b>TRUE</b> in case of provided permission is granted,
     * <b>FALSE</b> otherwise.
     */
    public static boolean isGranted(final Context context, final String permission) {
        if (!AppUtils.hasVersionM()) {
            return true;
        }
        return context != null
                && !TextUtils.isEmpty(permission)
                && ActivityCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     *
     */
    public static void requestLocationPermission(final Activity activity, final View layout, final int requestCode) {
        requestPermission(
                activity, layout,
                Manifest.permission.ACCESS_FINE_LOCATION, activity.getString(R.string.location_access_proposed),
                requestCode
        );
    }

    /**
     *
     */
    private static void requestPermission(final Activity activity, final View layout,
                                          final String permissionName, final String permissionMessage,
                                          final int requestCode) {
        // Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permissionName)) {
            Snackbar.make(layout, permissionMessage,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok_label, view -> {
                        // Request the permission
                        ActivityCompat.requestPermissions(
                                activity,
                                new String[]{permissionName},
                                requestCode
                        );
                    }
            ).show();
        } else {
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{permissionName}, requestCode
            );
        }
    }
}
