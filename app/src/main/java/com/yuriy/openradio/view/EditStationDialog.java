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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.yuriy.openradio.R;
import com.yuriy.openradio.business.storage.FavoritesStorage;
import com.yuriy.openradio.business.storage.LocalRadioStationsStorage;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.vo.RadioStation;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class EditStationDialog extends BaseAddEditStationDialog {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = EditStationDialog.class.getSimpleName();

    /**
     * Tag string to use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    public static final String MEDIA_ID_TAG = "MEDIA_ID_TAG";

    private String mMediaId;

    /**
     * Create a new instance of {@link EditStationDialog}
     */
    @SuppressWarnings("all")
    public static EditStationDialog newInstance(final String mediaId) {
        final EditStationDialog editStationDialog = new EditStationDialog();
        final Bundle bundle = new Bundle();
        bundle.putString(MEDIA_ID_TAG, mediaId);
        editStationDialog.setArguments(bundle);
        return editStationDialog;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        getDialog().setTitle(R.string.edit_station_dialog_title);

        final Button addOrEditBtn = view.findViewById(R.id.add_edit_station_dialog_add_btn_view);
        addOrEditBtn.setText(R.string.edit_station_dialog_button_label);

        mMediaId = getMediaId(getArguments());

        if (mMediaId != null) {
            final Context context = getActivity().getApplicationContext();
            final RadioStation radioStation = LocalRadioStationsStorage.getFromLocal(mMediaId, context);
            if (radioStation != null) {
                handleUI(radioStation, context);
            } else {
                //TODO: Handle UI in case of no Radio Station found.
            }
        } else {
            //TODO: Handle UI in case of no ID found.
        }

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
        ((MainActivity) getActivity()).processEditStationCallback(
                mMediaId, name, url, imageUrl, genre, country, addToFav
        );
    }

    /**
     *
     * @param radioStation
     * @param context
     */
    private void handleUI(@NonNull final RadioStation radioStation, final Context context) {
        mNameEdit.setText(radioStation.getName());
        mUrlEdit.setText(radioStation.getStreamURL());
        mImageUrlEdit.setText(radioStation.getImageUrl());
        mCountriesSpinner.setSelection(getCountryPosition(radioStation.getCountry()));
        mGenresSpinner.setSelection(getGenrePosition(radioStation.getGenre()));
        mAddToFavCheckView.setChecked(FavoritesStorage.isFavorite(radioStation, context));
    }

    /**
     *
     * @param bundle
     * @return
     */
    private static String getMediaId(@Nullable final Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        if (!bundle.containsKey(MEDIA_ID_TAG)) {
            return null;
        }
        return bundle.getString(MEDIA_ID_TAG);
    }
}
