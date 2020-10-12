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

package com.yuriy.openradio.shared.view.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.drive.DriveScopes;
import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.model.storage.drive.GoogleDriveManager;
import com.yuriy.openradio.shared.model.storage.drive.GoogleDriveManagerListenerImpl;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.AppUtils;
import com.yuriy.openradio.shared.view.BaseDialogFragment;
import com.yuriy.openradio.shared.view.SafeToast;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class GoogleDriveDialog extends BaseDialogFragment {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = GoogleDriveDialog.class.getSimpleName();

    /**
     * Tag string to use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    private static final int ACCOUNT_REQUEST_CODE = 400;

    private ProgressBar mProgressBarUpload;
    private ProgressBar mProgressBarDownload;
    private ProgressBar mProgressBarTitle;

    /**
     *
     */
    private GoogleDriveManager mGoogleDriveManager;

    public GoogleDriveDialog() {
        super();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getContext();
        final FragmentActivity activity = getActivity();

        mGoogleDriveManager = new GoogleDriveManager(
                context,
                new GoogleDriveManagerListenerImpl(
                        context,
                        new GoogleDriveManagerListenerImpl.Listener() {

                            @Override
                            public FragmentManager getSupportFragmentManager() {
                                return activity.getSupportFragmentManager();
                            }

                            @Override
                            public void onAccountRequested() {
                                final GoogleSignInClient client = buildGoogleSignInClient();
                                if (!AppUtils.startActivityForResultSafe(
                                        activity,
                                        client.getSignInIntent(),
                                        ACCOUNT_REQUEST_CODE)) {
                                    GoogleDriveDialog.this.mGoogleDriveManager.connect(null);
                                }
                            }

                            @Override
                            public void onComplete() {
                                //GoogleDriveDialog.this.updateListAfterDownloadFromGoogleDrive();
                            }
                        }
                )
        );
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final View view = getInflater().inflate(
                R.layout.dialog_google_drive,
                getActivity().findViewById(R.id.dialog_google_drive_root)
        );

        setWindowDimensions(view, 0.9f, 0.9f);

        final Button uploadTo = view.findViewById(R.id.upload_to_google_drive_btn);
        uploadTo.setOnClickListener(v -> uploadRadioStationsToGoogleDrive());

        final Button downloadFrom = view.findViewById(R.id.download_from_google_drive_btn);
        downloadFrom.setOnClickListener(v -> downloadRadioStationsFromGoogleDrive());

        mProgressBarUpload = view.findViewById(R.id.upload_to_google_drive_progress);
        mProgressBarDownload = view.findViewById(R.id.download_to_google_drive_progress);
        mProgressBarTitle = view.findViewById(R.id.google_drive_title_progress);

        hideProgress(GoogleDriveManager.Command.UPLOAD);
        hideProgress(GoogleDriveManager.Command.DOWNLOAD);
        hideTitleProgress();

        return createAlertDialog(view);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // TODO: Save state and continue with Google Drive if procedure was interrupted by device rotation.
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        AppLogger.d(CLASS_NAME + " OnActivityResult: request:" + requestCode + " result:" + resultCode);
        if (requestCode == ACCOUNT_REQUEST_CODE) {
            GoogleSignIn.getSignedInAccountFromIntent(data)
                    .addOnSuccessListener(googleAccount -> {
                                AppLogger.d("Signed in as " + googleAccount.getEmail());
                                mGoogleDriveManager.connect(googleAccount.getAccount());
                            }
                    )
                    .addOnFailureListener(exception -> {
                        AppLogger.e("Can't do sign in:" + Log.getStackTraceString(exception));
                        SafeToast.showAnyThread(
                                        getContext(), getString(R.string.can_not_get_account_name)
                                );
                            }
                    );
        }
    }

    @Nullable
    public static GoogleDriveDialog findGoogleDriveDialog(@Nullable final FragmentManager fragmentManager) {
        if (fragmentManager == null) {
            return null;
        }
        final Fragment fragment = fragmentManager.findFragmentByTag(DIALOG_TAG);
        if (fragment instanceof GoogleDriveDialog) {
            return (GoogleDriveDialog) fragment;
        }
        return null;
    }

    /**
     *
     */
    private void uploadRadioStationsToGoogleDrive() {
        mGoogleDriveManager.uploadRadioStations();
    }

    /**
     *
     */
    private void downloadRadioStationsFromGoogleDrive() {
        mGoogleDriveManager.downloadRadioStations();
    }

    public void showProgress(final GoogleDriveManager.Command command) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        switch (command) {
            case UPLOAD:
                if (mProgressBarUpload != null) {
                    activity.runOnUiThread(() -> mProgressBarUpload.setVisibility(View.VISIBLE));
                }
                break;
            case DOWNLOAD:
                if (mProgressBarDownload != null) {
                    activity.runOnUiThread(() -> mProgressBarDownload.setVisibility(View.VISIBLE));
                }
                break;
        }
    }

    public void hideProgress(final GoogleDriveManager.Command command) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        switch (command) {
            case UPLOAD:
                if (mProgressBarUpload != null) {
                    activity.runOnUiThread(() -> mProgressBarUpload.setVisibility(View.GONE));
                }
                break;
            case DOWNLOAD:
                if (mProgressBarDownload != null) {
                    activity.runOnUiThread(() -> mProgressBarDownload.setVisibility(View.GONE));
                }
                break;
        }
    }

    public void showTitleProgress() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(() -> mProgressBarTitle.setVisibility(View.VISIBLE));
    }

    public void hideTitleProgress() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(() -> mProgressBarTitle.setVisibility(View.GONE));
    }

    private GoogleSignInClient buildGoogleSignInClient() {
        final GoogleSignInOptions options =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .build();
        return GoogleSignIn.getClient(getContext(), options);
    }
}
