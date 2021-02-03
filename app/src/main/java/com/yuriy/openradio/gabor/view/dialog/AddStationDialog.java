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

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yuriy.openradio.gabor.R;
import com.yuriy.openradio.gabor.shared.service.OpenRadioService;
import com.yuriy.openradio.gabor.shared.vo.RadioStation;
import com.yuriy.openradio.gabor.shared.vo.RadioStationToAdd;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * Dialog view to handle Add Radio Station functionality.
 */
public final class AddStationDialog extends BaseAddEditStationDialog {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = AddStationDialog.class.getSimpleName();

    /**
     * Tag string to use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        getDialog().setTitle(R.string.add_station_dialog_title);
        return view;
    }

    /**
     * Validate provided input in order to pass data farther to
     * generate {@link RadioStation}.
     *
     * @param radioStationToAdd
     */
    @Override
    protected void processInput(final RadioStationToAdd radioStationToAdd) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.startService(
                OpenRadioService.makeAddRadioStationIntent(activity, radioStationToAdd)
        );
    }
}
