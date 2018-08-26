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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yuriy.openradio.BuildConfig;
import com.yuriy.openradio.R;
import com.yuriy.openradio.business.storage.AppPreferencesManager;
import com.yuriy.openradio.service.AppLocalBroadcast;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.utils.FabricUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;

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
    private EditText mUserAgentEditView;
    private CheckBox mUserAgentCheckView;

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
        final TextView title = view.findViewById(R.id.dialog_settings_title_view);
        title.setText(titleText);

        final Context context = getActivity().getApplicationContext();

        final boolean lastKnownRadioStationEnabled = AppPreferencesManager.lastKnownRadioStationEnabled(context);
        final CheckBox lastKnownRadioStationEnableCheckView = view.findViewById(
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
        final CheckBox logsEnableCheckView = view.findViewById(R.id.settings_dialog_enable_logs_check_view);
        logsEnableCheckView.setChecked(areLogsEnabled);
        processEnableCheckView(context, areLogsEnabled);
        logsEnableCheckView.setOnClickListener(
                view1 -> {
                    final boolean checked = ((CheckBox) view1).isChecked();
                    processEnableCheckView(context, checked);
                }
        );

        final Button clearLogsBtn = view.findViewById(R.id.settings_dialog_clear_logs_btn_view);
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

        final Button sendLogsBtn = view.findViewById(R.id.settings_dialog_send_logs_btn_view);
        sendLogsBtn.setOnClickListener(
                view13 -> sendLogMailTask()
        );

        mUserAgentEditView = view.findViewById(R.id.user_agent_input_view);
        mUserAgentEditView.setText(AppPreferencesManager.getCustomUserAgent(context));

        mUserAgentCheckView = view.findViewById(R.id.user_agent_check_view);
        mUserAgentCheckView.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    AppPreferencesManager.isCustomUserAgent(context, isChecked);
                    mUserAgentEditView.setEnabled(isChecked);
                }
        );

        final boolean isCustomUserAgent = AppPreferencesManager.isCustomUserAgent(context);
        mUserAgentCheckView.setChecked(isCustomUserAgent);
        mUserAgentEditView.setEnabled(isCustomUserAgent);

        final SeekBar masterVolumeSeekBar = view.findViewById(R.id.master_vol_seek_bar);
        masterVolumeSeekBar.setProgress(AppPreferencesManager.getMasterVolume(context));
        masterVolumeSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(final SeekBar seekBar,
                                                  final int progress,
                                                  final boolean fromUser) {}

                    @Override
                    public void onStartTrackingTouch(final SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(final SeekBar seekBar) {
                        AppPreferencesManager.setMasterVolume(context, seekBar.getProgress());
                        LocalBroadcastManager.getInstance(context).sendBroadcast(
                                AppLocalBroadcast.createIntentMasterVolumeChanged()
                        );
                    }
                }
        );
    }

    @Override
    public void onPause() {
        super.onPause();
        saveCustomUserAgent();
    }

    private void saveCustomUserAgent() {
        if (mUserAgentEditView == null) {
            return;
        }
        final Context context = getActivity().getApplicationContext();
        if (context == null) {
            return;
        }

        final String userAgent = mUserAgentEditView.getText().toString().trim();
        AppPreferencesManager.setCustomUserAgent(context, userAgent);
    }

    private void processEnableCheckView(final Context context, final boolean isEnable) {
        final View view = getView();
        if (view == null) {
            return;
        }
        final Button sendLogsBtn = view.findViewById(R.id.settings_dialog_send_logs_btn_view);
        final Button clearLogsBtn = view.findViewById(R.id.settings_dialog_clear_logs_btn_view);
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
            FabricUtils.logException(e);
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

        private final WeakReference<SettingsDialog> mContext;

        private SendLogEmailTask(final SettingsDialog context) {
            super();
            mContext = new WeakReference<>(context);
        }

        @Override
        protected Intent doInBackground(final MailInfo... mailInfoArray) {
            final SettingsDialog dialog = mContext.get();
            if (dialog == null) {
                return null;
            }
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
            sendIntent.setType("vnd.android.cursor.dir/email");

            try {
                final Uri path = FileProvider.getUriForFile(
                        dialog.getActivity().getApplication(),
                        BuildConfig.APPLICATION_ID + ".provider",
                        AppLogger.getLogsZipFile(dialog.getActivity().getApplicationContext())
                );
                sendIntent.putExtra(Intent.EXTRA_STREAM, path);
            } catch (final Exception e) {
                FabricUtils.logException(e);
                return null;
            }

            return sendIntent;
        }

        @Override
        protected void onPostExecute(final Intent intent) {
            super.onPostExecute(intent);
            final SettingsDialog dialog = mContext.get();
            if (dialog == null) {
                return;
            }

            if (intent != null) {
                try {
                    final Intent intent1 = Intent.createChooser(
                            intent,
                            dialog.getActivity().getString(R.string.send_logs_chooser_title)
                    );
                    intent1.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    dialog.getActivity().startActivityForResult(
                            intent1,
                            LOGS_EMAIL_REQUEST_CODE
                    );
                } catch (final ActivityNotFoundException e) {
                    SafeToast.showAnyThread(
                            dialog.getActivity().getApplicationContext(),
                            dialog.getActivity().getString(R.string.cant_start_activity)
                    );
                    FabricUtils.logException(e);
                }
            } else {
                SafeToast.showAnyThread(
                        dialog.getActivity().getApplicationContext(),
                        dialog.getActivity().getString(R.string.cant_send_logs)
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
