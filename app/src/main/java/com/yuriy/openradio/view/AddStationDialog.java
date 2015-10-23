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
import com.yuriy.openradio.utils.IntentsHelper;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class AddStationDialog extends DialogFragment {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = AddStationDialog.class.getSimpleName();

    /**
     * Tag string to use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    /**
     * Create a new instance of {@link AddStationDialog}
     */
    @SuppressWarnings("all")
    public static AddStationDialog newInstance() {
        final AddStationDialog addStationDialog = new AddStationDialog();
        // provide here an arguments, if any
        return addStationDialog;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        getDialog().setTitle(R.string.add_station_dialog_title);

        final View view = inflater.inflate(R.layout.dialog_add_station, container, false);
        final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                new FrameLayout.LayoutParams(
                        (int) (AppUtils.getShortestScreenSize(getActivity()) * 0.8),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
        final LinearLayout root = (LinearLayout) view.findViewById(R.id.add_station_dialog_root);
        root.setLayoutParams(layoutParams);

        final Button addBtn = (Button) view.findViewById(R.id.add_station_dialog_add_btn_view);
        addBtn.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        getDialog().dismiss();
                    }
                }
        );

        return view;
    }
}
