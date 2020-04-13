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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.model.storage.FavoritesStorage;
import com.yuriy.openradio.shared.model.storage.LocalRadioStationsStorage;
import com.yuriy.openradio.shared.view.SafeToast;
import com.yuriy.openradio.shared.vo.RadioStation;
import com.yuriy.openradio.shared.vo.RadioStationToAdd;
import com.yuriy.openradio.view.activity.MainActivity;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * Dialog to provide components to Edit Radio Station.
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

    /**
     * Key to keep Media Id's value in Bundle.
     */
    private static final String MEDIA_ID_KEY = "MEDIA_ID_KEY";

    /**
     * Media Id associated with current Radio Station.
     */
    private String mMediaId;

    /**
     * Create a new instance of {@link EditStationDialog}
     *
     * @param mediaId Media id associated with Radio Station.
     */
    public static EditStationDialog newInstance(final String mediaId) {
        final EditStationDialog editStationDialog = new EditStationDialog();
        final Bundle bundle = new Bundle();
        bundle.putString(MEDIA_ID_KEY, mediaId);
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

        final Context context = getActivity().getApplicationContext();
        if (mMediaId != null) {
            final RadioStation radioStation = LocalRadioStationsStorage.get(mMediaId, context);
            if (radioStation != null) {
                handleUI(radioStation, context);
            } else {
                handleInvalidRadioStation(context, addOrEditBtn);
            }
        } else {
            handleInvalidRadioStation(context, addOrEditBtn);
        }

        return view;
    }

    /**
     * Validate provided input in order to pass data farther to generate {@link RadioStation}.
     */
    @Override
    protected void processInput(final RadioStationToAdd radioStationToAdd) {
        ((MainActivity) getActivity()).processEditStationCallback(
                mMediaId, radioStationToAdd
        );
    }

    /**
     * Handles UI in case of error while trying to edit Radio Station.
     *
     * @param context      Context of a callee.
     * @param addOrEditBtn Edit button.
     */
    private void handleInvalidRadioStation(@NonNull final Context context, @NonNull final Button addOrEditBtn) {
        SafeToast.showAnyThread(context, context.getString(R.string.can_not_edit_station_label));
        addOrEditBtn.setEnabled(false);
    }

    /**
     * Update UI with Radio Station loaded from storage.
     *
     * @param radioStation Radio Station.
     * @param context      Context of a callee.
     */
    private void handleUI(@NonNull final RadioStation radioStation, final Context context) {
        mNameEdit.setText(radioStation.getName());
        mUrlEdit.setText(radioStation.getMediaStream().getVariant(0).getUrl());
        mImageLocalUrlEdit.setText(radioStation.getImageUrl());
        mCountriesSpinner.setSelection(getCountryPosition(radioStation.getCountry()));
        mGenresSpinner.setSelection(getGenrePosition(radioStation.getGenre()));
        mAddToFavCheckView.setChecked(FavoritesStorage.isFavorite(radioStation, context));
    }

    /**
     * Extract media id from provided Bundle.
     *
     * @param bundle Bundle to handle.
     * @return Media Id or {@code null} if there is nothing to extract.
     */
    private static String getMediaId(@Nullable final Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        if (!bundle.containsKey(MEDIA_ID_KEY)) {
            return null;
        }
        return bundle.getString(MEDIA_ID_KEY);
    }
}
