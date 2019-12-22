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

package com.yuriy.openradio.shared.view.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.yuriy.openradio.shared.utils.AppLogger;

import java.util.Arrays;

public final class PermissionsDialogActivity extends Activity {

    private static final String CLASS_NAME = PermissionsDialogActivity.class.getSimpleName();
    private static final String KEY_PERMISSION_NAME = "KEY_PERMISSION_NAME";
    private static final int PERMISSIONS_REQUEST_CODE = 1234;
    private static final String PERMISSION_DENIED_KEY = "PERMISSION_DENIED_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String permissionName = getPermissionName(getIntent());

        if (TextUtils.isEmpty(permissionName)) {
            return;
        }

        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionName)) {
            // Explain to the user why we need this permission.
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{permissionName},
                PERMISSIONS_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AppLogger.d(
                CLASS_NAME + " permissions:" + Arrays.toString(permissions)
                        + ", results:" + Arrays.toString(grantResults)
        );

        finish();

        boolean isDenied = true;
        if (isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION, permissions, grantResults)) {
            isDenied = false;
        }

        // Restart main activity
        final Intent intent = getBaseContext()
                .getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(PERMISSION_DENIED_KEY, isDenied);
        startActivity(intent);
    }

    public static boolean isLocationDenied(final Intent intent) {
        if (intent == null) {
            return false;
        }
        return intent.getBooleanExtra(PERMISSION_DENIED_KEY, false);
    }

    /**
     * @param context
     * @param permissionName
     * @return
     */
    public static Intent getIntent(final Context context, final String permissionName) {
        final Intent intent = new Intent(context, PermissionsDialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(KEY_PERMISSION_NAME, permissionName);
        return intent;
    }

    /**
     * @param intent
     * @return
     */
    private static String getPermissionName(final Intent intent) {
        if (intent == null) {
            return null;
        }
        return intent.getStringExtra(KEY_PERMISSION_NAME);
    }

    /**
     * @param name
     * @param permissions
     * @param results
     * @return
     */
    private static boolean isPermissionGranted(final String name, final String[] permissions, final int[] results) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        int length = permissions.length;
        if (length == 0) {
            return false;
        }

        boolean isGranted = false;
        String permission;
        int result;
        for (int i = 0; i < length; i++) {
            permission = permissions[i];
            result = results[i];
            if (TextUtils.equals(name, permission) && result == PackageManager.PERMISSION_GRANTED) {
                isGranted = true;
                break;
            }
        }
        return isGranted;
    }
}
