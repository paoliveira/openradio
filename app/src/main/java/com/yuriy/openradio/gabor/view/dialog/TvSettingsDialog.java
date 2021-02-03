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

package com.yuriy.openradio.gabor.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.yuriy.openradio.gabor.R;
import com.yuriy.openradio.gabor.shared.utils.AppLogger;
import com.yuriy.openradio.gabor.shared.view.BaseDialogFragment;
import com.yuriy.openradio.gabor.shared.view.dialog.AboutDialog;
import com.yuriy.openradio.gabor.shared.view.dialog.GeneralSettingsDialog;
import com.yuriy.openradio.gabor.shared.view.dialog.GoogleDriveDialog;
import com.yuriy.openradio.gabor.shared.view.dialog.LogsDialog;
import com.yuriy.openradio.gabor.shared.view.dialog.StreamBufferingDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class TvSettingsDialog extends BaseDialogFragment {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = TvSettingsDialog.class.getSimpleName();

    /**
     * Tag string to use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    private final AtomicBoolean mIsInstanceSaved;

    public TvSettingsDialog() {
        super();
        mIsInstanceSaved = new AtomicBoolean(false);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        mIsInstanceSaved.set(false);
        final View view = getInflater().inflate(
                R.layout.dialog_tv_settings,
                getActivity().findViewById(R.id.dialog_tv_settings_root)
        );

        setWindowDimensions(view, 0.9f, 0.9f);

        final Context context = getContext();
        final String titleText = context.getString(R.string.app_settings_title);
        final TextView title = view.findViewById(R.id.dialog_tv_settings_title_view);
        title.setText(titleText);

        final ListView listview = view.findViewById(R.id.settings_tv_list_view);
        // TODO: Refactor this and the same from activity_main_drawer to string resources
        final String[] values = new String[] {
                "General",
                "Stream Buffering",
                "Google Drive",
                "Logs",
                "About"
        };

        final List<String> list = new ArrayList<>();
        Collections.addAll(list, values);
        final ArrayAdapterExt adapter = new ArrayAdapterExt(
                getContext(),
                android.R.layout.simple_list_item_1, list
        );
        listview.setAdapter(adapter);
        listview.setOnItemClickListener(
                (parent, view1, position, id) -> {
                    AppLogger.d(CLASS_NAME + " click:" + values[position]);
                    if (mIsInstanceSaved.get()) {
                        return;
                    }
                    final FragmentTransaction fragmentTransaction = getActivity()
                            .getSupportFragmentManager().beginTransaction();
                    clearDialogs(fragmentTransaction);
                    switch (position) {
                        case 0:
                            // Show Search Dialog
                            final DialogFragment settingsDialog = BaseDialogFragment.newInstance(
                                    GeneralSettingsDialog.class.getName()
                            );
                            settingsDialog.show(fragmentTransaction, GeneralSettingsDialog.DIALOG_TAG);
                            break;
                        case 1:
                            // Show Stream Buffering Dialog
                            final DialogFragment streamBufferingDialog = BaseDialogFragment.newInstance(
                                    StreamBufferingDialog.class.getName()
                            );
                            streamBufferingDialog.show(fragmentTransaction, StreamBufferingDialog.DIALOG_TAG);
                            break;
                        case 2:
                            // Show Google Drive Dialog
                            final DialogFragment googleDriveDialog = BaseDialogFragment.newInstance(
                                    GoogleDriveDialog.class.getName()
                            );
                            googleDriveDialog.show(fragmentTransaction, GoogleDriveDialog.DIALOG_TAG);
                            break;
                        case 3:
                            // Show Application Logs Dialog
                            final DialogFragment applicationLogsDialog = BaseDialogFragment.newInstance(
                                    LogsDialog.class.getName()
                            );
                            applicationLogsDialog.show(fragmentTransaction, LogsDialog.DIALOG_TAG);
                            break;
                        case 4:
                            // Show About Dialog
                            final DialogFragment aboutDialog = BaseDialogFragment.newInstance(
                                    AboutDialog.class.getName()
                            );
                            aboutDialog.show(fragmentTransaction, AboutDialog.DIALOG_TAG);
                            break;
                        default:

                            break;
                    }
                }
        );

        return createAlertDialog(view);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mIsInstanceSaved.set(true);
    }

    /**
     *
     * @param fragmentTransaction
     */
    private void clearDialogs(final FragmentTransaction fragmentTransaction) {
        Fragment fragmentByTag = getActivity().getSupportFragmentManager().findFragmentByTag(AboutDialog.DIALOG_TAG);
        if (fragmentByTag != null) {
            fragmentTransaction.remove(fragmentByTag);
        }
        fragmentByTag = getActivity().getSupportFragmentManager().findFragmentByTag(SearchDialog.DIALOG_TAG);
        if (fragmentByTag != null) {
            fragmentTransaction.remove(fragmentByTag);
        }
        fragmentByTag = getActivity().getSupportFragmentManager().findFragmentByTag(GoogleDriveDialog.DIALOG_TAG);
        if (fragmentByTag != null) {
            fragmentTransaction.remove(fragmentByTag);
        }
        fragmentByTag = getActivity().getSupportFragmentManager().findFragmentByTag(GeneralSettingsDialog.DIALOG_TAG);
        if (fragmentByTag != null) {
            fragmentTransaction.remove(fragmentByTag);
        }
        fragmentTransaction.addToBackStack(null);
    }

    private static class ArrayAdapterExt extends ArrayAdapter<String> {

        private final Map<String, Integer> mMap = new HashMap<>();

        private ArrayAdapterExt(Context context, int textViewResourceId,
                                List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(final int position) {
            return mMap.get(getItem(position));
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
