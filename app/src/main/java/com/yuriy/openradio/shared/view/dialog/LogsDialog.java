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

package com.yuriy.openradio.shared.view.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.yuriy.openradio.BuildConfig;
import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager;
import com.yuriy.openradio.shared.utils.AnalyticsUtils;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.AppUtils;
import com.yuriy.openradio.shared.view.BaseDialogFragment;
import com.yuriy.openradio.shared.view.SafeToast;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class LogsDialog extends BaseDialogFragment {

    /**
     * Tag string mTo use in logging message.
     */
    private static final String CLASS_NAME = LogsDialog.class.getSimpleName();

    /**
     * Tag string mTo use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    private static final int LOGS_EMAIL_REQUEST_CODE = 4762;

    private static final String SUPPORT_MAIL = "chernyshov.yuriy@gmail.com";

    private SendLogEmailTask mSendLogMailTask;

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        final View view = getInflater().inflate(
                R.layout.dialog_settings_logs,
                getActivity().findViewById(R.id.dialog_settings_logs_root)
        );

        setWindowDimensions(view, 0.8f, 0.8f);

        final String titleText = getString(R.string.app_logs_label);
        final TextView title = view.findViewById(R.id.settings_logs_label_view);
        title.setText(titleText);

        final Context context = getActivity();

        final boolean areLogsEnabled = AppPreferencesManager.areLogsEnabled(context);
        final CheckBox logsEnableCheckView = view.findViewById(R.id.settings_dialog_enable_logs_check_view);
        logsEnableCheckView.setChecked(areLogsEnabled);
        processEnableCheckView(context, view, areLogsEnabled);
        logsEnableCheckView.setOnClickListener(
                view1 -> {
                    final boolean checked = ((CheckBox) view1).isChecked();
                    processEnableCheckView(context, view, checked);
                }
        );

        final Button clearLogsBtn = view.findViewById(R.id.settings_dialog_clear_logs_btn_view);
        clearLogsBtn.setOnClickListener(

                view12 -> {
                    final boolean result = AppLogger.deleteAllLogs(context);
                    String message = result
                            ? "All logs deleted"
                            : "Can not delete logs";
                    SafeToast.showAnyThread(context, message);
                    AppLogger.initLogger(context);
                }
        );

        final Button sendLogsBtn = view.findViewById(R.id.settings_dialog_send_logs_btn_view);
        sendLogsBtn.setOnClickListener(
                view13 -> sendLogMailTask()
        );

        return createAlertDialog(view);
    }

    private void processEnableCheckView(final Context context, final View view, final boolean isEnable) {
        if (view == null) {
            return;
        }
        final Button sendLogsBtn = view.findViewById(R.id.settings_dialog_send_logs_btn_view);
        final Button clearLogsBtn = view.findViewById(R.id.settings_dialog_clear_logs_btn_view);
        sendLogsBtn.setEnabled(isEnable);
        clearLogsBtn.setEnabled(isEnable);

        AppPreferencesManager.setLogsEnabled(context, isEnable);
        AppLogger.setLoggingEnabled(isEnable);
    }

    private synchronized void sendLogMailTask() {
        //attempt of run task one more time
        if (!checkRunningTasks()) {
            AppLogger.w("Send Logs task is running, return");
            return;
        }

        AppLogger.deleteZipFile(getActivity());
        try {
            AppLogger.zip(getActivity());
        } catch (final IOException e) {
            SafeToast.showAnyThread(getActivity(), "Can not ZIP Logs");
            AnalyticsUtils.logException(e);
            return;
        }

        mSendLogMailTask = new SendLogEmailTask(this);

        final String subj = "Logs report Open Radio, "
                + "v:" + AppUtils.getApplicationVersion(getActivity())
                + "." + AppUtils.getApplicationVersionCode(getActivity());
        final String bodyHeader = "Archive with logs is in attachment.";
        mSendLogMailTask.execute(new MailInfo(SUPPORT_MAIL, subj, bodyHeader));
    }

    private boolean checkRunningTasks() {
        return !(mSendLogMailTask != null && mSendLogMailTask.getStatus() == AsyncTask.Status.RUNNING);
    }

    private static final class SendLogEmailTask extends AsyncTask<MailInfo, Void, Intent> {

        private final WeakReference<LogsDialog> mContext;

        private SendLogEmailTask(final LogsDialog context) {
            super();
            mContext = new WeakReference<>(context);
        }

        @Override
        protected Intent doInBackground(final MailInfo... mailInfoArray) {
            final LogsDialog dialog = mContext.get();
            if (dialog == null) {
                return null;
            }
            final Activity activity = dialog.getActivity();
            if (activity == null) {
                return null;
            }
            final MailInfo mailInfo = mailInfoArray[0];

            // Prepare email intent
            final Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{mailInfo.mTo});
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, mailInfo.mSubj);
            sendIntent.putExtra(Intent.EXTRA_TEXT, mailInfo.mMailBody + "\r\n");
            sendIntent.setType("vnd.android.cursor.dir/email");

            try {
                final Uri path = FileProvider.getUriForFile(
                        activity,
                        BuildConfig.APPLICATION_ID + ".provider",
                        AppLogger.getLogsZipFile(activity)
                );
                sendIntent.putExtra(Intent.EXTRA_STREAM, path);
            } catch (final Exception e) {
                AnalyticsUtils.logException(e);
                return null;
            }

            return sendIntent;
        }

        @Override
        protected void onPostExecute(final Intent intent) {
            super.onPostExecute(intent);
            if (intent == null) {
                return;
            }
            final LogsDialog dialog = mContext.get();
            if (dialog == null) {
                return;
            }
            final Activity activity = dialog.getActivity();
            if (activity == null) {
                return;
            }
            try {
                final Intent intent1 = Intent.createChooser(
                        intent,
                        activity.getString(R.string.send_logs_chooser_title)
                );
                intent1.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activity.startActivityForResult(
                        intent1,
                        LOGS_EMAIL_REQUEST_CODE
                );
            } catch (final ActivityNotFoundException e) {
                SafeToast.showAnyThread(
                        activity,
                        activity.getString(R.string.cant_start_activity)
                );
                AnalyticsUtils.logException(e);
            }
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AppLogger.d(CLASS_NAME + "OnActivityResult: request:" + requestCode + " result:" + resultCode);

        final Context context = getContext();
        if (context == null) {
            return;
        }

        if (requestCode == LOGS_EMAIL_REQUEST_CODE) {
            SafeToast.showAnyThread(context, context.getString(R.string.logs_sent_msg));
        }
    }

    @Nullable
    public static LogsDialog findLogsDialog(@Nullable final FragmentManager fragmentManager) {
        if (fragmentManager == null) {
            return null;
        }
        final Fragment fragment = fragmentManager.findFragmentByTag(DIALOG_TAG);
        if (fragment instanceof LogsDialog) {
            return (LogsDialog) fragment;
        }
        return null;
    }

    private static final class MailInfo {

        private final String mTo;
        private final String mSubj;
        private final String mMailBody;

        private MailInfo(@NonNull final String to, @NonNull final String subj, @NonNull final String mailBody) {
            super();
            mTo = to;
            mSubj = subj;
            mMailBody = mailBody;
        }
    }
}
