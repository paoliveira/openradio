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

package com.yuriy.openradio.view.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yuriy.openradio.R;
import com.yuriy.openradio.view.activity.MainActivity;
import com.yuriy.openradio.shared.vo.RadioStation;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
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

    /**
     * Create a new instance of {@link AddStationDialog}.
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
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        getDialog().setTitle(R.string.add_station_dialog_title);
        return view;
    }

    /**
     * Validate provided input in order to pass data farther to
     * generate {@link RadioStation}.
     *
     * @param name     Name of the Radio Station.
     * @param url      Url of the Stream associated with Radio Station.
     * @param imageUrl Url of the Image associated with Radio Station.
     * @param genre    Genre of the Radio Station.
     * @param country  Country of the Radio Station.
     * @param addToFav Whether or not add radio station to favorites.
     */
    @Override
    protected void processInput(final String name, final String url, final String imageUrl,
                                final String genre, final String country, final boolean addToFav) {
        ((MainActivity) getActivity()).processAddStationCallback(
                name, url, imageUrl, genre, country, addToFav
        );
    }
}
