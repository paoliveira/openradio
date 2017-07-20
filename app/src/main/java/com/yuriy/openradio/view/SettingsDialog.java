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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.yuriy.openradio.R;
import com.yuriy.openradio.business.AppPreferencesManager;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.utils.AsyncTask;
import com.yuriy.openradio.utils.CrashlyticsUtils;

import java.io.IOException;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class SettingsDialog extends DialogFragment {

    /**
     * Tag string mTo use in logging message.
     */
    private static final String CLASS_NAME = SettingsDialog.class.getSimpleName();

    /**
     * Tag string mTo use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    private static final int LOGS_EMAIL_REQUEST_CODE = 1000;

    private static final String SUPPORT_MAIL = "chernyshov.yuriy@gmail.com";

    private SendLogEmailTask mSendLogMailTask;

    /**
     * Create a new instance of {@link SettingsDialog}
     */
    @SuppressWarnings("all")
    public static SettingsDialog newInstance() {
        final SettingsDialog aboutDialog = new SettingsDialog();
        // provide here an arguments, if any
        return aboutDialog;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.settings_about, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final String titleText = getActivity().getString(R.string.app_settings_title);
        final TextView title = (TextView) view.findViewById(R.id.dialog_settings_title_view);
        title.setText(titleText);

        final Context context = getActivity().getApplicationContext();

        final boolean lastKnownRadioStationEnabled = AppPreferencesManager.lastKnownRadioStationEnabled(context);
        final CheckBox lastKnownRadioStationEnableCheckView = (CheckBox) view.findViewById(
                R.id.settings_dialog_enable_last_known_radio_station_check_view
        );
        lastKnownRadioStationEnableCheckView.setChecked(lastKnownRadioStationEnabled);
        lastKnownRadioStationEnableCheckView.setOnClickListener(
                view1 -> {
                    final boolean checked = ((CheckBox) view1).isChecked();
                    AppPreferencesManager.lastKnownRadioStationEnabled(context, checked);
                }
        );

        final boolean areLogsEnabled = AppPreferencesManager.areLogsEnabled(context);
        final CheckBox logsEnableCheckView = (CheckBox) view.findViewById(R.id.settings_dialog_enable_logs_check_view);
        logsEnableCheckView.setChecked(areLogsEnabled);
        processEnableCheckView(context, areLogsEnabled);
        logsEnableCheckView.setOnClickListener(
                view1 -> {
                    final boolean checked = ((CheckBox) view1).isChecked();
                    processEnableCheckView(context, checked);
                }
        );

        final Button clearLogsBtn = (Button) view.findViewById(R.id.settings_dialog_clear_logs_btn_view);
        clearLogsBtn.setOnClickListener(

                view12 -> {
                    final Activity activity = getActivity();
                    AppLogger.deleteZipFile(activity);
                    AppLogger.deleteLogcatFile(activity);
                    final boolean result = AppLogger.deleteAllLogs(activity);
                    String message = result
                            ? "All logs deleted"
                            : "Can not delete logs";
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show();

                    AppLogger.initLogger(activity);
                }
        );

        final Button sendLogsBtn = (Button) view.findViewById(R.id.settings_dialog_send_logs_btn_view);
        sendLogsBtn.setOnClickListener(
                view13 -> sendLogMailTask()
        );
    }

    private void processEnableCheckView(final Context context, final boolean isEnable) {
        final View view = getView();
        if (view == null) {
            return;
        }
        final Button sendLogsBtn = (Button) view.findViewById(R.id.settings_dialog_send_logs_btn_view);
        final Button clearLogsBtn = (Button) view.findViewById(R.id.settings_dialog_clear_logs_btn_view);
        sendLogsBtn.setEnabled(isEnable);
        clearLogsBtn.setEnabled(isEnable);

        AppPreferencesManager.setLogsEnabled(context, isEnable);
        AppLogger.setIsLoggingEnabled(isEnable);
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
            CrashlyticsUtils.logException(e);
            return;
        }

        mSendLogMailTask = new SendLogEmailTask(getActivity());

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

        private final Activity mContext;

        private SendLogEmailTask(final Activity context) {
            super();
            mContext = context;
        }

        @Override
        protected Intent doInBackground(final MailInfo... mailInfoArray) {
            if (mailInfoArray == null) {
                throw new NullPointerException("mailInfoArray");
            }
            if (mailInfoArray.length != 1) {
                throw new IllegalArgumentException("mailInfo");
            }
            final MailInfo mailInfo = mailInfoArray[0];
            if (mailInfo == null) {
                throw new NullPointerException("mailInfo");
            }

            // Prepare email intent
            final Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{mailInfo.mTo});
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, mailInfo.mSubj);
            sendIntent.putExtra(Intent.EXTRA_TEXT, mailInfo.mMailBody + "\r\n" );
            sendIntent .setType("vnd.android.cursor.dir/email");

            try {
                final Uri path = Uri.fromFile(AppLogger.getLogsZipFile(mContext.getApplicationContext()));
                sendIntent .putExtra(Intent.EXTRA_STREAM, path);
            } catch (final Exception e) {
                CrashlyticsUtils.logException(e);
                return null;
            }

            return sendIntent;
        }

        @Override
        protected void onPostExecute(final Intent intent) {
            super.onPostExecute(intent);

            if (intent != null) {
                try {
                    mContext.startActivityForResult(
                            Intent.createChooser(
                                    intent, mContext.getString(R.string.send_logs_chooser_title)
                            ),
                            LOGS_EMAIL_REQUEST_CODE
                    );
                } catch (final ActivityNotFoundException e) {
                    SafeToast.showAnyThread(
                            mContext.getApplicationContext(), mContext.getString(R.string.cant_start_activity)
                    );
                    CrashlyticsUtils.logException(e);
                }
            } else {
                SafeToast.showAnyThread(
                        mContext.getApplicationContext(), mContext.getString(R.string.cant_send_logs)
                );
            }
        }
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
