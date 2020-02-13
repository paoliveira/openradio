/*
 * Copyright 2019 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.view.BaseDialogFragment;
import com.yuriy.openradio.shared.view.dialog.GoogleDriveDialog;
import com.yuriy.openradio.shared.view.dialog.LogsDialog;
import com.yuriy.openradio.view.dialog.AddStationDialog;
import com.yuriy.openradio.view.dialog.TvSettingsDialog;
import com.yuriy.openradio.view.fragment.TvMainFragment;

import java.util.List;

/*
 * Main TV Activity class that loads main TV fragment.
 */
public final class TvMainActivity extends FragmentActivity {

    private static final String CLASS_NAME = TvMainActivity.class.getSimpleName();

    private ImageView mBackBtn;
    /**
     * Progress Bar view to indicate that data is loading.
     */
    private ProgressBar mProgressBar;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_main);

        mProgressBar = findViewById(R.id.progress_bar_tv_view);

        setUpAddBtn();
        setUpBackBtn();
        setUpSearchBtn();
        setUpSettingsBtn();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        AppLogger.d(CLASS_NAME + "OnActivityResult: request:" + requestCode + " result:" + resultCode);
        final GoogleDriveDialog gDriveDialog = GoogleDriveDialog.findGoogleDriveDialog(getSupportFragmentManager());
        if (gDriveDialog != null) {
            gDriveDialog.onActivityResult(requestCode, resultCode, data);
        }

        final LogsDialog logsDialog = LogsDialog.findLogsDialog(getSupportFragmentManager());
        if (logsDialog != null) {
            logsDialog.onActivityResult(requestCode, resultCode, data);
        }

        switch (requestCode) {
            case TvSearchActivity.SEARCH_TV_ACTIVITY_REQUEST_CODE:
                onSearchDialogClick();
                break;
        }
    }

    /**
     * Process call back from the Search Dialog.
     */
    public void onSearchDialogClick() {
        final TvMainFragment fragment = getMainTvFragment();
        if (fragment != null) {
            fragment.onSearchDialogClick();
        }
    }

    /**
     * Show progress bar.
     */
    public void showProgressBar() {
        if (mProgressBar == null) {
            return;
        }
        mProgressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Hide progress bar.
     */
    public void hideProgressBar() {
        if (mProgressBar == null) {
            return;
        }
        mProgressBar.setVisibility(View.GONE);
    }

    public void onDataLoaded() {
        int numItemsInStack = 0;
        final FragmentManager manager = getSupportFragmentManager();
        final List<Fragment> list = manager.getFragments();
        for (final Fragment fragment : list) {
            if (!(fragment instanceof TvMainFragment)) {
                continue;
            }
            numItemsInStack = ((TvMainFragment) fragment).getNumItemsInStack();
            break;
        }
        if (numItemsInStack > 1) {
            showBackBtn();
        } else {
            hideBackBtn();
        }
    }

    private TvMainFragment getMainTvFragment() {
        final FragmentManager manager = getSupportFragmentManager();
        final List<Fragment> list = manager.getFragments();
        for (final Fragment fragment : list) {
            if (fragment instanceof TvMainFragment) {
                return (TvMainFragment) fragment;
            }
        }
        return null;
    }

    private void showBackBtn() {
        if (mBackBtn == null) {
            return;
        }
        mBackBtn.setVisibility(View.VISIBLE);
    }

    private void hideBackBtn() {
        if (mBackBtn == null) {
            return;
        }
        mBackBtn.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onBackPressed() {
        final TvMainFragment fragment = getMainTvFragment();
        if (fragment != null) {
            if (fragment.handleBackButton()) {
                super.onBackPressed();
            }
        }
    }

    private void setUpBackBtn() {
        mBackBtn = findViewById(R.id.tv_back_btn);
        if (mBackBtn == null) {
            return;
        }
        mBackBtn.setOnClickListener(
                v -> {
                    final TvMainFragment fragment = getMainTvFragment();
                    if (fragment != null) {
                        fragment.handleBackButton();
                    }
                }
        );
    }

    private void setUpSettingsBtn() {
        final ImageView button = findViewById(R.id.tv_settings_btn);
        if (button == null) {
            return;
        }
        button.setOnClickListener(v -> showTvSettings());
    }

    private void setUpSearchBtn() {
        final ImageView button = findViewById(R.id.tv_search_btn);
        if (button == null) {
            return;
        }
        button.setOnClickListener(
                v -> startActivityForResult(
                        TvSearchActivity.makeStartIntent(getApplicationContext()),
                        TvSearchActivity.SEARCH_TV_ACTIVITY_REQUEST_CODE
                )
        );
    }

    private void setUpAddBtn() {
        final ImageView button = findViewById(R.id.tv_add_btn);
        if (button == null) {
            return;
        }
        button.setOnClickListener(
                (view) -> {
                    // Show Add Station Dialog
                    final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    final DialogFragment dialog = AddStationDialog.newInstance();
                    dialog.show(transaction, AddStationDialog.DIALOG_TAG);
                }
        );

    }

    private void showTvSettings() {
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        final Fragment fragment = getSupportFragmentManager().findFragmentByTag(TvSettingsDialog.DIALOG_TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        // Show Settings Dialog
        final DialogFragment dialogFragment = BaseDialogFragment.newInstance(
                TvSettingsDialog.class.getName()
        );
        dialogFragment.show(transaction, TvSettingsDialog.DIALOG_TAG);
    }
}
