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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.broadcast.AppLocalBroadcast;
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager;
import com.yuriy.openradio.shared.view.BaseDialogFragment;
import com.yuriy.openradio.shared.view.SafeToast;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class GeneralSettingsDialog extends BaseDialogFragment {

    /**
     * Tag string mTo use in logging message.
     */
    private static final String CLASS_NAME = GeneralSettingsDialog.class.getSimpleName();

    /**
     * Tag string mTo use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    private EditText mUserAgentEditView;

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        final View view = getInflater().inflate(
                R.layout.dialog_general_settings,
                getActivity().findViewById(R.id.dialog_general_settings_root)
        );

        setWindowDimensions(view, 0.9f, 0.9f);

        final String titleText = getActivity().getString(R.string.app_settings_title);
        final TextView title = view.findViewById(R.id.dialog_settings_title_view);
        title.setText(titleText);

        final Context context = getActivity();

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

        mUserAgentEditView = view.findViewById(R.id.user_agent_input_view);
        mUserAgentEditView.setText(AppPreferencesManager.getCustomUserAgent(context));

        final CheckBox userAgentCheckView = view.findViewById(R.id.user_agent_check_view);
        userAgentCheckView.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    AppPreferencesManager.isCustomUserAgent(context, isChecked);
                    mUserAgentEditView.setEnabled(isChecked);
                }
        );

        final boolean isCustomUserAgent = AppPreferencesManager.isCustomUserAgent(context);
        userAgentCheckView.setChecked(isCustomUserAgent);
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

        final CheckBox btAutoRestart = view.findViewById(R.id.bt_auto_restart_check_view);
        btAutoRestart.setChecked(AppPreferencesManager.isBtAutoPlay(context));
        btAutoRestart.setOnCheckedChangeListener(
                (buttonView, isChecked) -> AppPreferencesManager.setBtAutoPlay(context, isChecked)
        );

        return createAlertDialog(view);
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
        final Context context = getActivity();
        if (context == null) {
            return;
        }

        final String userAgent = mUserAgentEditView.getText().toString().trim();
        if (TextUtils.isEmpty(userAgent)) {
            SafeToast.showAnyThread(context, getString(R.string.user_agent_empty_warning));
            return;
        }
        AppPreferencesManager.setCustomUserAgent(context, userAgent);
    }
}
