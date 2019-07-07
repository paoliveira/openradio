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
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.yuriy.openradio.R;
import com.yuriy.openradio.model.storage.drive.GoogleDriveManager;

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

    private ProgressBar mProgressBarUpload;

    private ProgressBar mProgressBarDownload;

    private ProgressBar mProgressBarTitle;

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final MainActivity activity = (MainActivity) getActivity();

        final View view = getInflater().inflate(
                R.layout.dialog_google_drive,
                activity.findViewById(R.id.dialog_google_drive_root)
        );

        setWindowDimensions(view, 0.9f, 0.9f);

        final Button uploadTo = view.findViewById(R.id.upload_to_google_drive_btn);
        uploadTo.setOnClickListener(v -> activity.uploadRadioStationsToGoogleDrive());

        final Button downloadFrom = view.findViewById(R.id.download_from_google_drive_btn);
        downloadFrom.setOnClickListener(v -> activity.downloadRadioStationsFromGoogleDrive());

        mProgressBarUpload = view.findViewById(R.id.upload_to_google_drive_progress);
        mProgressBarDownload = view.findViewById(R.id.download_to_google_drive_progress);
        mProgressBarTitle = view.findViewById(R.id.google_drive_title_progress);

        hideProgress(GoogleDriveManager.Command.UPLOAD);
        hideProgress(GoogleDriveManager.Command.DOWNLOAD);
        hideTitleProgress();

        return createAlertDialog(view);
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
}
