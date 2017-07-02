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

package com.yuriy.openradio.view;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.yuriy.openradio.R;
import com.yuriy.openradio.utils.AppLogger;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class SaveToGoogleDriveActivity extends AppCompatActivity {

    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 300;

    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;

    public SaveToGoogleDriveActivity() {
        super();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_to_google_drive);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(
                            new GoogleApiClient.ConnectionCallbacks() {
                                @Override
                                public void onConnected(@Nullable Bundle bundle) {
                                    AppLogger.i("On Connected:" + bundle);
                                }

                                @Override
                                public void onConnectionSuspended(int i) {
                                    AppLogger.i("On Connection suspended:" + i);
                                }
                            }
                    )
                    .addOnConnectionFailedListener(
                            connectionResult -> {
                                AppLogger.i("On Connection failed:" + connectionResult);
                                if (connectionResult.hasResolution()) {
                                    try {
                                        connectionResult.startResolutionForResult(
                                                this,
                                                RESOLVE_CONNECTION_REQUEST_CODE
                                        );
                                    } catch (IntentSender.SendIntentException e) {
                                        // Unable to resolve, message user appropriately
                                        AppLogger.e("On Connection failed:" + e);
                                    }
                                } else {
                                    GooglePlayServicesUtil.getErrorDialog(
                                            connectionResult.getErrorCode(), this, 0
                                    ).show();
                                }
                            }
                    )
                    .build();
        }
        mGoogleApiClient.connect();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        AppLogger.i("OnActivityResult: request:" + requestCode + " result:" + resultCode);
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    if (mGoogleApiClient != null) {
                        mGoogleApiClient.connect();
                    }
                } else {
                    if (mGoogleApiClient != null) {
                        AppLogger.i("Is connected:" + mGoogleApiClient.isConnected() + " " + mGoogleApiClient.isConnecting());
                    }
                }
                break;
        }
    }
}
