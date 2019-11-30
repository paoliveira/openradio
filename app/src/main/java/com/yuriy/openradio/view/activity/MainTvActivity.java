package com.yuriy.openradio.view.activity;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.yuriy.openradio.R;
import com.yuriy.openradio.model.storage.drive.GoogleDriveManager;
import com.yuriy.openradio.model.storage.drive.GoogleDriveManagerAction;
import com.yuriy.openradio.model.storage.drive.GoogleDriveManagerListenerImpl;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.FabricUtils;
import com.yuriy.openradio.view.BaseDialogFragment;
import com.yuriy.openradio.view.SafeToast;
import com.yuriy.openradio.view.dialog.SettingsTvDialog;
import com.yuriy.openradio.view.fragment.MainTvFragment;

import java.util.List;

/*
 * Main TV Activity class that loads main TV fragment.
 */
public final class MainTvActivity
        extends FragmentActivity
        implements GoogleDriveManagerAction {

    private static final String CLASS_NAME = MainTvActivity.class.getSimpleName();
    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 300;
    private static final int ACCOUNT_REQUEST_CODE = 400;

    private ImageView mBackBtn;
    /**
     *
     */
    private GoogleDriveManager mGoogleDriveManager;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_tv);

        final Context context = getApplicationContext();

        mGoogleDriveManager = new GoogleDriveManager(
                context,
                new GoogleDriveManagerListenerImpl(
                        context,
                        new GoogleDriveManagerListenerImpl.Listener() {

                            @Override
                            public FragmentManager getSupportFragmentManager() {
                                return MainTvActivity.this.getSupportFragmentManager();
                            }

                            @Override
                            public void onAccountRequested() {
                                try {
                                    MainTvActivity.this.startActivityForResult(
                                            AccountPicker.newChooseAccountIntent(
                                                    null,
                                                    null,
                                                    new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},
                                                    true, null, null, null, null
                                            ),
                                            ACCOUNT_REQUEST_CODE
                                    );
                                } catch (final ActivityNotFoundException e) {
                                    FabricUtils.logException(e);
                                    MainTvActivity.this.mGoogleDriveManager.connect(null);
                                }
                            }

                            @Override
                            public void requestGoogleDriveSignIn(final ConnectionResult connectionResult) {
                                MainTvActivity.this.requestGoogleDriveSignIn(connectionResult);
                            }

                            @Override
                            public void onComplete() {
                                //MainTvActivity.this.updateListAfterDownloadFromGoogleDrive();
                            }
                        }
                )
        );

        setUpBackBtn();
        setUpSettingsBtn();
    }

    @Override
    protected void onPause() {

        mGoogleDriveManager.disconnect();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mGoogleDriveManager != null) {
            mGoogleDriveManager.release();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AppLogger.d(CLASS_NAME + "OnActivityResult: request:" + requestCode + " result:" + resultCode);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                mGoogleDriveManager.connect();
                break;
            case ACCOUNT_REQUEST_CODE:
                final String email = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if (TextUtils.isEmpty(email)) {
                    SafeToast.showAnyThread(
                            getApplicationContext(), getString(R.string.can_not_get_account_name)
                    );
                    break;
                }
                mGoogleDriveManager.connect(email);
                break;
        }
    }

    /**
     *
     */
    @Override
    public void uploadRadioStationsToGoogleDrive() {
        mGoogleDriveManager.uploadRadioStations();
    }

    /**
     *
     */
    @Override
    public void downloadRadioStationsFromGoogleDrive() {
        mGoogleDriveManager.downloadRadioStations();
    }

    /**
     * @param connectionResult
     */
    private void requestGoogleDriveSignIn(@NonNull final ConnectionResult connectionResult) {
        try {
            connectionResult.startResolutionForResult(
                    this,
                    RESOLVE_CONNECTION_REQUEST_CODE
            );
        } catch (IntentSender.SendIntentException e) {
            // Unable to resolve, message user appropriately
            AppLogger.e(CLASS_NAME + "Google Drive unable to resolve failure:" + e);
        }
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
