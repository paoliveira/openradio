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
     * Create a new instance of {@link RemoveStationDialog}
     */
    @SuppressWarnings("all")
    public static RemoveStationDialog newInstance() {
        final RemoveStationDialog dialog = new RemoveStationDialog();
        // provide here an arguments, if any
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

        final Button removeBtn = (Button) view.findViewById(R.id.remove_station_dialog_add_btn_view);
        removeBtn.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(final View viewBtn) {

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
}
