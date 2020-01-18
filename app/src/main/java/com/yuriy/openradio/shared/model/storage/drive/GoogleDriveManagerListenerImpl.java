package com.yuriy.openradio.shared.model.storage.drive;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.common.ConnectionResult;
import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.view.SafeToast;
import com.yuriy.openradio.shared.view.dialog.GoogleDriveDialog;

public class GoogleDriveManagerListenerImpl implements GoogleDriveManager.Listener {

    public interface Listener {
        FragmentManager getSupportFragmentManager();
        void onAccountRequested();
        void requestGoogleDriveSignIn(final ConnectionResult connectionResult);
        void onComplete();
    }

    private Context mContext;
    private final Handler mHandler;
    private final Thread mUiThread;
    private final Listener mListener;

    public GoogleDriveManagerListenerImpl(final Context context, final Listener listener) {
        super();
        mUiThread = Thread.currentThread();
        mHandler = new Handler();
        mContext = context;
        mListener = listener;
    }

    @Override
    public void onConnectionFailed(@Nullable final ConnectionResult connectionResult) {
        if (connectionResult != null) {
            mListener.requestGoogleDriveSignIn(connectionResult);
        } else {
            SafeToast.showAnyThread(
                    mContext, mContext.getString(R.string.google_drive_conn_error)
            );
        }

        runOnUiThread(() -> {
            final GoogleDriveDialog dialog = GoogleDriveDialog.findGoogleDriveDialog(
                    mListener.getSupportFragmentManager()
            );
            if (dialog != null) {
                dialog.hideTitleProgress();
            }
        });
    }

    @Override
    public void onStart(final GoogleDriveManager.Command command) {
        runOnUiThread(() -> {
            final GoogleDriveDialog dialog = GoogleDriveDialog.findGoogleDriveDialog(
                    mListener.getSupportFragmentManager()
            );
            if (dialog != null) {
                dialog.showProgress(command);
            }
        });
    }

    @Override
    public void onSuccess(final GoogleDriveManager.Command command) {
        String message = null;
        switch (command) {
            case UPLOAD:
                message = mContext.getString(R.string.google_drive_data_saved);
                break;
            case DOWNLOAD:
                message = mContext.getString(R.string.google_drive_data_read);
                mListener.onComplete();
                break;
        }
        if (!TextUtils.isEmpty(message)) {
            SafeToast.showAnyThread(mContext, message);
        }

        runOnUiThread(() -> {
            final GoogleDriveDialog dialog = GoogleDriveDialog.findGoogleDriveDialog(
                    mListener.getSupportFragmentManager()
            );
            if (dialog != null) {
                dialog.hideProgress(command);
            }
        });
    }

    @Override
    public void onError(final GoogleDriveManager.Command command, final GoogleDriveError error) {
        String message = null;
        switch (command) {
            case UPLOAD:
                message = mContext.getString(R.string.google_drive_error_when_save);
                break;
            case DOWNLOAD:
                message = mContext.getString(R.string.google_drive_error_when_read);
                break;
        }
        if (!TextUtils.isEmpty(message)) {
            SafeToast.showAnyThread(mContext, message);
        }

        runOnUiThread(() -> {
            final GoogleDriveDialog dialog = GoogleDriveDialog.findGoogleDriveDialog(
                    mListener.getSupportFragmentManager()
            );
            if (dialog != null) {
                dialog.hideProgress(command);
            }
        });
    }

    @Override
    public void onConnect() {
        runOnUiThread(() -> {
            final GoogleDriveDialog dialog = GoogleDriveDialog.findGoogleDriveDialog(
                    mListener.getSupportFragmentManager()
            );
            if (dialog != null) {
                dialog.showTitleProgress();
            }
        });
    }

    @Override
    public void onConnected() {
        runOnUiThread(() -> {
            final GoogleDriveDialog dialog = GoogleDriveDialog.findGoogleDriveDialog(
                    mListener.getSupportFragmentManager()
            );
            if (dialog != null) {
                dialog.hideTitleProgress();
            }
        });
    }

    @Override
    public void onAccountRequested() {
        mListener.onAccountRequested();
    }

    private void runOnUiThread(final Runnable action) {
        if (Thread.currentThread() != mUiThread) {
            mHandler.post(action);
        } else {
            action.run();
        }
    }
}
