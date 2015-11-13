/*
 * Copyright 2015 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuriy.openradio.R;
import com.yuriy.openradio.utils.AppUtils;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class RemoveStationDialog extends DialogFragment {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = RemoveStationDialog.class.getSimpleName();

    /**
     * Tag string to use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    /**
     * Key for the Media Id value.
     */
    private static final String KEY_MEDIA_ID = "KEY_MEDIA_ID";

    /**
     * Key for the Name value.
     */
    private static final String KEY_NAME = "KEY_NAME";

    /**
     * Create a new instance of {@link RemoveStationDialog}
     */
    @SuppressWarnings("all")
    public static RemoveStationDialog newInstance(final String mediaId, final String name) {
        final Bundle bundle = new Bundle();
        bundle.putString(KEY_MEDIA_ID, mediaId);
        bundle.putString(KEY_NAME, name);

        final RemoveStationDialog dialog = new RemoveStationDialog();
        dialog.setArguments(bundle);
        return dialog;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        getDialog().setTitle(R.string.remove_station_dialog_title);

        final View view = inflater.inflate(R.layout.dialog_remove_station, container, false);
        final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                new FrameLayout.LayoutParams(
                        (int) (AppUtils.getShortestScreenSize(getActivity()) * 0.8),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
        final LinearLayout root = (LinearLayout) view.findViewById(R.id.remove_station_dialog_root);
        root.setLayoutParams(layoutParams);

        final String mediaId = getArgument(getArguments(), KEY_MEDIA_ID);
        final String name = getArgument(getArguments(), KEY_NAME);

        final TextView textView = (TextView) view.findViewById(R.id.remove_station_text_view);
        textView.setText(getString(R.string.remove_station_dialog_main_text, name));

        final Button removeBtn = (Button) view.findViewById(R.id.remove_station_dialog_add_btn_view);
        removeBtn.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(final View viewBtn) {
                        ((MainActivity) getActivity()).processRemoveStationCallback(mediaId);
                        getDialog().dismiss();
                    }
                }
        );

        final Button cancelBtn = (Button) view.findViewById(R.id.remove_station_dialog_cancel_btn_view);
        cancelBtn.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(final View viewBtn) {
                        getDialog().dismiss();
                    }
                }
        );

        return view;
    }

    /**
     * Extract argument from the Bundle.
     *
     * @param bundle Arguments {@link Bundle}.
     * @param key    Key of the argument.
     *
     * @return Value associated with the provided key, or an empty string.
     */
    private static String getArgument(final Bundle bundle, final String key) {
        if (bundle == null) {
            return "";
        }
        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        return "";
    }
}
