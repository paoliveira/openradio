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

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.fragment.app.DialogFragment;

import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.permission.PermissionChecker;
import com.yuriy.openradio.shared.service.LocationService;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.AppUtils;
import com.yuriy.openradio.shared.utils.ImageFilePath;
import com.yuriy.openradio.shared.utils.IntentsHelper;
import com.yuriy.openradio.shared.view.SafeToast;
import com.yuriy.openradio.shared.vo.RadioStation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Base dialog to use by Edit and Add dialogs.
 */
public abstract class BaseAddEditStationDialog extends DialogFragment {

    /**
     * Text view for Image Url.
     */
    protected EditText mImageUrlEdit;
    protected EditText mNameEdit;
    protected EditText mUrlEdit;
    protected Spinner mCountriesSpinner;
    protected Spinner mGenresSpinner;
    protected CheckBox mAddToFavCheckView;
    private ArrayAdapter<CharSequence> mGenresAdapter;
    private ArrayAdapter<String> mCountriesAdapter;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.dialog_add_edit_station, container, false);
        final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                (int) (AppUtils.getShortestScreenSize(getActivity()) * 0.8),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        final LinearLayout root = view.findViewById(R.id.add_edit_station_dialog_root);
        root.setLayoutParams(layoutParams);

        mNameEdit = view.findViewById(R.id.add_edit_station_name_edit);
        mUrlEdit = view.findViewById(R.id.add_edit_station_stream_url_edit);
        mImageUrlEdit = view.findViewById(R.id.add_edit_station_image_url_edit);

        final List<String> countries = new ArrayList<>(LocationService.COUNTRY_CODE_TO_NAME.values());
        Collections.sort(countries);

        mCountriesSpinner = view.findViewById(R.id.add_edit_station_country_spin);
        // Create an ArrayAdapter using the string array and a default spinner layout
        mCountriesAdapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                countries
        );
        // Specify the layout to use when the list of choices appears
        mCountriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the mCountriesAdapter to the spinner
        mCountriesSpinner.setAdapter(mCountriesAdapter);

        mGenresSpinner = view.findViewById(R.id.add_station_genre_spin);
        // Create an ArrayAdapter using the string array and a default spinner layout
        mGenresAdapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>(AppUtils.predefinedCategories())
        );
        // Specify the layout to use when the list of choices appears
        mGenresAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the mCountriesAdapter to the spinner
        mGenresSpinner.setAdapter(mGenresAdapter);

        final Button imageUrlBtn = view.findViewById(R.id.add_edit_station_image_browse_btn);
        imageUrlBtn.setOnClickListener(
                viewBtn -> {
                    final Intent galleryIntent = new Intent();
                    galleryIntent.setType("image/*");
                    galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                    // Chooser of filesystem options.
                    final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Image");

                    startActivityForResult(chooserIntent, IntentsHelper.REQUEST_CODE_FILE_SELECTED);
                }
        );

        mAddToFavCheckView = view.findViewById(R.id.add_to_fav_check_view);

        final Button addOrEditBtn = view.findViewById(R.id.add_edit_station_dialog_add_btn_view);
        addOrEditBtn.setOnClickListener(
                viewBtn -> processInputInternal(
                        mNameEdit.getText().toString(),
                        mUrlEdit.getText().toString(),
                        mImageUrlEdit.getText().toString(),
                        String.valueOf(mGenresSpinner.getSelectedItem()),
                        String.valueOf(mCountriesSpinner.getSelectedItem()),
                        mAddToFavCheckView.isChecked()
                )
        );

        final Button cancelBtn = view.findViewById(R.id.add_edit_station_dialog_cancel_btn_view);
        cancelBtn.setOnClickListener(
                viewBtn -> getDialog().dismiss()
        );

        return view;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Context applicationContext = getActivity().getApplicationContext();
        if (!PermissionChecker.isGranted(
                applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) {
            SafeToast.showAnyThread(
                    applicationContext,
                    applicationContext.getString(R.string.storage_permission_not_granted)
            );
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (data == null) {
            return;
        }

        switch (requestCode) {
            case IntentsHelper.REQUEST_CODE_FILE_SELECTED:
                final Uri selectedImageUri = data.getData();
                final Context applicationContext = getActivity().getApplicationContext();
                //MEDIA GALLERY
                final String selectedImagePath = ImageFilePath.getPath(
                        applicationContext, selectedImageUri
                );
                AppLogger.d("Image Path:" + selectedImagePath);
                if (selectedImagePath != null) {
                    mImageUrlEdit.setText(selectedImagePath);
                } else {
                    SafeToast.showAnyThread(
                            applicationContext,
                            applicationContext.getString(R.string.can_not_open_file)
                    );
                }
                break;
        }
    }

    /**
     * Abstraction to handle action once input is processed.
     *
     * @param name     Name of the Radio Station.
     * @param url      Url of the Stream associated with Radio Station.
     * @param imageUrl Url of the Image associated with Radio Station.
     * @param genre    Genre of the Radio Station.
     * @param country  Country of the Radio Station.
     * @param addToFav Whether or not add radio station to favorites.
     */
    protected abstract void processInput(final String name, final String url, final String imageUrl,
                                         final String genre, final String country, final boolean addToFav);

    /**
     * Return position of country in drop down list.
     *
     * @param country Country of the Radio Station.
     * @return Position of country.
     */
    protected int getCountryPosition(final String country) {
        if (mCountriesAdapter == null) {
            return -1;
        }
        return mCountriesAdapter.getPosition(country);
    }

    /**
     * Return position of genre in drop down list.
     *
     * @param genre Genre of the Radio Station.
     * @return Position of Genre.
     */
    protected int getGenrePosition(final String genre) {
        if (mGenresAdapter == null) {
            return -1;
        }
        return mGenresAdapter.getPosition(genre);
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
    private void processInputInternal(final String name, final String url, final String imageUrl,
                                      final String genre, final String country, final boolean addToFav) {
        final Context applicationContext = getActivity().getApplicationContext();
        if (TextUtils.isEmpty(name)) {
            SafeToast.showAnyThread(applicationContext, "Name is empty");
            return;
        }
        if (TextUtils.isEmpty(url)) {
            SafeToast.showAnyThread(applicationContext, "Stream URL is empty");
            return;
        }

        processInput(name, url, imageUrl, genre, country, addToFav);

        getDialog().dismiss();
    }
}
