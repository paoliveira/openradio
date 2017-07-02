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
import android.app.DialogFragment;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
public final class SaveToGoogleDriveDialog extends DialogFragment {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = SaveToGoogleDriveDialog.class.getSimpleName();

    /**
     * Tag string to use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 300;

    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Create a new instance of {@link SaveToGoogleDriveDialog}
     */
    @SuppressWarnings("all")
    public static SaveToGoogleDriveDialog newInstance() {
        final SaveToGoogleDriveDialog dialog = new SaveToGoogleDriveDialog();
        // provide here an arguments, if any
        return dialog;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        getDialog().setTitle("Save to Google Drive");

        final View view = inflater.inflate(R.layout.dialog_search, container, false);
        final MainActivity activity = (MainActivity) getActivity();

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(activity.getApplicationContext())
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(
                            new GoogleApiClient.ConnectionCallbacks() {
                                @Override
                                public void onConnected(@Nullable Bundle bundle) {
                                    AppLogger.d("On Connected:" + bundle);
                                }

                                @Override
                                public void onConnectionSuspended(int i) {
                                    AppLogger.d("On Connection suspended:" + i);
                                }
                            }
                    )
                    .addOnConnectionFailedListener(
                            connectionResult -> {
                                AppLogger.e("On Connection failed:" + connectionResult);
                                if (connectionResult.hasResolution()) {
                                    try {
                                        connectionResult.startResolutionForResult(
                                                activity,
                                                RESOLVE_CONNECTION_REQUEST_CODE
                                        );
                                    } catch (IntentSender.SendIntentException e) {
                                        // Unable to resolve, message user appropriately
                                        AppLogger.e("On Connection failed:" + e);
                                    }
                                } else {
                                    GooglePlayServicesUtil.getErrorDialog(
                                            connectionResult.getErrorCode(), activity, 0
                                    ).show();
                                }
                            }
                    )
                    .build();
        }
        mGoogleApiClient.connect();

        return view;
    }

    @Override
    public void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }



    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        AppLogger.d("OnActivityResult: request:" + requestCode + " result:" + resultCode);
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    if (mGoogleApiClient != null) {
                        mGoogleApiClient.connect();
                    }
                }
                break;
        }
    }
}
