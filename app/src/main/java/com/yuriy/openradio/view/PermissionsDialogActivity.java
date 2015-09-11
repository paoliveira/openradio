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

package com.yuriy.openradio.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;

public final class PermissionsDialogActivity extends Activity {

    private static final String KEY_PERMISSION_NAME = "KEY_PERMISSION_NAME";

    private static final int PERMISSIONS_REQUEST_CODE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String permissionName = getPermissionName(getIntent());

        if (TextUtils.isEmpty(permissionName)) {
            return;
        }

        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionName)) {
            // Explain to the user why we need to read the contacts
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{permissionName},
                PERMISSIONS_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           final String[] permissions,
                                           final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        finish();

        // Restart main activity
        final Intent intent = getBaseContext()
                .getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public static Intent getIntent(final Context context, final String permissionName) {
        final Intent intent = new Intent(context, PermissionsDialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(KEY_PERMISSION_NAME, permissionName);
        return intent;
    }

    private static String getPermissionName(final Intent intent) {
        if (intent == null) {
            return null;
        }
        return intent.getStringExtra(KEY_PERMISSION_NAME);
    }
}
