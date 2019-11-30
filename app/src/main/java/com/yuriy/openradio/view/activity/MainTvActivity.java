package com.yuriy.openradio.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.yuriy.openradio.R;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.view.BaseDialogFragment;
import com.yuriy.openradio.view.dialog.GoogleDriveDialog;
import com.yuriy.openradio.view.dialog.SettingsTvDialog;
import com.yuriy.openradio.view.fragment.MainTvFragment;

import java.util.List;

/*
 * Main TV Activity class that loads main TV fragment.
 */
public final class MainTvActivity extends FragmentActivity {

    private static final String CLASS_NAME = MainTvActivity.class.getSimpleName();

    private ImageView mBackBtn;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_tv);

        setUpBackBtn();
        setUpSearchBtn();
        setUpSettingsBtn();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        AppLogger.d(CLASS_NAME + "OnActivityResult: request:" + requestCode + " result:" + resultCode);
        final GoogleDriveDialog googleDriveDialog = getGoogleDriveDialog();
        if (googleDriveDialog != null) {
            googleDriveDialog.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Nullable
    private GoogleDriveDialog getGoogleDriveDialog() {
        final Fragment fragment = getSupportFragmentManager().findFragmentByTag(GoogleDriveDialog.DIALOG_TAG);
        if (fragment instanceof GoogleDriveDialog) {
            return (GoogleDriveDialog) fragment;
        }
        return null;
    }

    public void onDataLoaded() {
        int numItemsInStack = 0;
        final FragmentManager manager = getSupportFragmentManager();
        final List<Fragment> list = manager.getFragments();
        for (final Fragment fragment : list) {
            if (!(fragment instanceof MainTvFragment)) {
                continue;
            }
            numItemsInStack = ((MainTvFragment)fragment).getNumItemsInStack();
            break;
        }
        if (numItemsInStack > 1) {
            showBackBtn();
        } else {
            hideBackBtn();
        }
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

    private void setUpBackBtn() {
        mBackBtn = findViewById(R.id.tv_back_btn);
        if (mBackBtn == null) {
            return;
        }
        mBackBtn.setOnClickListener(
                v -> {
                    final FragmentManager manager = getSupportFragmentManager();
                    final List<Fragment> list = manager.getFragments();
                    for (final Fragment fragment : list) {
                        if (!(fragment instanceof MainTvFragment)) {
                            continue;
                        }
                        ((MainTvFragment)fragment).handleBackButton();
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
        button.setOnClickListener(v -> startActivity(SearchTvActivity.makeStartIntent(getApplicationContext())));
    }

    private void showTvSettings() {
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        final Fragment fragment = getSupportFragmentManager().findFragmentByTag(SettingsTvDialog.DIALOG_TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        // Show Settings Dialog
        final DialogFragment dialogFragment = BaseDialogFragment.newInstance(
                SettingsTvDialog.class.getName()
        );
        dialogFragment.show(transaction, SettingsTvDialog.DIALOG_TAG);
    }
}
