/*
 * Copyright 2016 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.app.DialogFragment;
import android.os.Bundle;
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

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class SettingsDialog extends DialogFragment {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = SettingsDialog.class.getSimpleName();

    /**
     * Tag string to use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

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

        final boolean areLogsEnabled = AppPreferencesManager.areLogsEnabled();
        final CheckBox logsEnableCheckView =
                (CheckBox) view.findViewById(R.id.settings_dialog_enable_logs_check_view);
        logsEnableCheckView.setChecked(areLogsEnabled);
        processEnableCheckView(areLogsEnabled);
        logsEnableCheckView.setOnClickListener(
                new SafeOnClickListener<SettingsDialog>(this) {

                    @Override
                    public void safeOnClick(final SettingsDialog reference, final View view) {
                        final boolean checked = ((CheckBox) view).isChecked();
                        reference.processEnableCheckView(checked);
                    }
                }
        );

        final Button clearLogsBtn
                = (Button) view.findViewById(R.id.settings_dialog_clear_logs_btn_view);
        clearLogsBtn.setOnClickListener(

                new SafeOnClickListener<SettingsDialog>(this) {

                    @Override
                    public void safeOnClick(final SettingsDialog reference, final View view) {
                        final boolean result = AppLogger.deleteAllLogs(reference.getActivity());
                        String message = result
                                ? "All logs deleted"
                                : "Can not delete logs";
                        Toast.makeText(reference.getActivity(), message, Toast.LENGTH_LONG).show();
                    }
                }
        );

        final Button sendLogsBtn
                = (Button) view.findViewById(R.id.settings_dialog_send_logs_btn_view);
        sendLogsBtn.setOnClickListener(

                new SafeOnClickListener<SettingsDialog>(this) {

                    @Override
                    public void safeOnClick(final SettingsDialog reference, final View view) {

                    }
                }
        );
    }

    private void processEnableCheckView(final boolean isEnable) {
        final View view = getView();
        if (view == null) {
            return;
        }
        final Button sendLogsBtn
                = (Button) view.findViewById(R.id.settings_dialog_send_logs_btn_view);
        final Button clearLogsBtn
                = (Button) view.findViewById(R.id.settings_dialog_clear_logs_btn_view);
        sendLogsBtn.setEnabled(isEnable);
        clearLogsBtn.setEnabled(isEnable);

        AppPreferencesManager.setAreLogsEnabled(isEnable);
    }
}
