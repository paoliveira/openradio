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

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.yuriy.openradio.gabor.R;
import com.yuriy.openradio.gabor.shared.broadcast.RSAddValidatedReceiver;
import com.yuriy.openradio.gabor.shared.broadcast.RSAddValidatedReceiverListener;
import com.yuriy.openradio.gabor.shared.permission.PermissionChecker;
import com.yuriy.openradio.gabor.shared.service.LocationService;
import com.yuriy.openradio.gabor.shared.utils.AppLogger;
import com.yuriy.openradio.gabor.shared.utils.AppUtils;
import com.yuriy.openradio.gabor.shared.utils.ImageFilePath;
import com.yuriy.openradio.gabor.shared.utils.IntentUtils;
import com.yuriy.openradio.gabor.shared.view.BaseDialogFragment;
import com.yuriy.openradio.gabor.shared.view.SafeToast;
import com.yuriy.openradio.gabor.shared.vo.RadioStation;
import com.yuriy.openradio.gabor.shared.vo.RadioStationToAdd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * Base dialog to use by Edit and Add dialogs.
 */
public abstract class BaseAddEditStationDialog extends BaseDialogFragment {

    /**
     * Text view for Image Url.
     */
    EditText mImageLocalUrlEdit;
    private EditText mImageWebUrlEdit;
    EditText mNameEdit;
    EditText mHomePageEdit;
    EditText mUrlEdit;
    Spinner mCountriesSpinner;
    Spinner mGenresSpinner;
    CheckBox mAddToFavCheckView;
    ProgressBar mProgressView;
    private CheckBox mAddToSrvrCheckView;
    private ArrayAdapter<CharSequence> mGenresAdapter;
    private ArrayAdapter<String> mCountriesAdapter;
    private RSAddValidatedReceiver mRsAddValidatedReceiver;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.dialog_add_edit_station, container, false);
        final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                (int) (AppUtils.getShortestScreenSize(getActivity()) * 0.8),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        mRsAddValidatedReceiver = new RSAddValidatedReceiver(
                new RSAddValidatedReceiverListener() {
                    @Override
                    public void onSuccess(final String message) {
                        mProgressView.setVisibility(View.INVISIBLE);
                        SafeToast.showAnyThread(getContext(), message);
                        getDialog().dismiss();
                    }

                    @Override
                    public void onFailure(final String reason) {
                        mProgressView.setVisibility(View.INVISIBLE);
                        SafeToast.showAnyThread(getContext(), reason);
                    }
                }
        );
        mRsAddValidatedReceiver.register(getContext());

        final LinearLayout root = view.findViewById(R.id.add_edit_station_dialog_root);
        root.setLayoutParams(layoutParams);

        mHomePageEdit = view.findViewById(R.id.add_edit_station_home_page_edit);
        mNameEdit = view.findViewById(R.id.add_edit_station_name_edit);
        mUrlEdit = view.findViewById(R.id.add_edit_station_stream_url_edit);
        mImageLocalUrlEdit = view.findViewById(R.id.add_edit_station_image_url_edit);
        mImageWebUrlEdit = view.findViewById(R.id.add_edit_station_web_image_url_edit);
        mProgressView = view.findViewById(R.id.add_edit_station_dialog_progress_bar_view);

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

                    AppUtils.startActivityForResultSafe(
                            getActivity(),
                            chooserIntent,
                            IntentUtils.REQUEST_CODE_FILE_SELECTED
                    );
                }
        );

        mAddToFavCheckView = view.findViewById(R.id.add_to_fav_check_view);
        mAddToSrvrCheckView = view.findViewById(R.id.add_to_srvr_check_view);
        mAddToSrvrCheckView.setOnCheckedChangeListener(
                (buttonView, isChecked) -> toggleWebImageView(view, isChecked)
        );

        final Button addOrEditBtn = view.findViewById(R.id.add_edit_station_dialog_add_btn_view);
        addOrEditBtn.setOnClickListener(
                viewBtn -> {
                    mProgressView.setVisibility(View.VISIBLE);
                    processInputInternal(
                            mNameEdit.getText().toString(),
                            mUrlEdit.getText().toString(),
                            mImageLocalUrlEdit.getText().toString(),
                            mImageWebUrlEdit.getText().toString(),
                            mHomePageEdit.getText().toString(),
                            String.valueOf(mGenresSpinner.getSelectedItem()),
                            String.valueOf(mCountriesSpinner.getSelectedItem()),
                            mAddToFavCheckView.isChecked(),
                            mAddToSrvrCheckView.isChecked()
                    );
                }
        );

        final Button cancelBtn = view.findViewById(R.id.add_edit_station_dialog_cancel_btn_view);
        cancelBtn.setOnClickListener(
                viewBtn -> getDialog().dismiss()
        );

        mProgressView.setVisibility(View.INVISIBLE);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mProgressView.setVisibility(View.INVISIBLE);
        mRsAddValidatedReceiver.unregister(getContext());
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Context context = getActivity();
        if (!PermissionChecker.isGranted(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        ) {
            SafeToast.showAnyThread(
                    context,
                    context.getString(R.string.storage_permission_not_granted)
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
            case IntentUtils.REQUEST_CODE_FILE_SELECTED:
                final Uri selectedImageUri = data.getData();
                final Context context = getActivity();
                //MEDIA GALLERY
                final String selectedImagePath = ImageFilePath.getPath(
                        context, selectedImageUri
                );
                AppLogger.d("Image Path:" + selectedImagePath);
                if (selectedImagePath != null) {
                    mImageLocalUrlEdit.setText(selectedImagePath);
                } else {
                    SafeToast.showAnyThread(
                            context,
                            context.getString(R.string.can_not_open_file)
                    );
                }
                break;
        }
    }

    /**
     * Abstraction to handle action once input is processed.
     *
     * @param radioStationToAdd Data to add as radio station.
     */
    protected abstract void processInput(final RadioStationToAdd radioStationToAdd);

    /**
     * Return position of country in drop down list.
     *
     * @param country Country of the Radio Station.
     * @return Position of country.
     */
    int getCountryPosition(final String country) {
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
    int getGenrePosition(final String genre) {
        if (mGenresAdapter == null) {
            return -1;
        }
        return mGenresAdapter.getPosition(genre);
    }

    /**
     * Validate provided input in order to pass data farther to
     * generate {@link RadioStation}.
     *
     * @param name          Name of the Radio Station.
     * @param url           Url of the Stream associated with Radio Station.
     * @param imageLocalUrl Local Url of the Image associated with Radio Station.
     * @param imageWebUrl   Web Url of the Image associated with Radio Station.
     * @param homePage      Web Url of Radio Station's home page.
     * @param genre         Genre of the Radio Station.
     * @param country       Country of the Radio Station.
     * @param addToFav      Whether or not add radio station to favorites.
     * @param addToServer   Whether or not add radio station to the server.
     */
    private void processInputInternal(final String name, final String url, final String imageLocalUrl,
                                      final String imageWebUrl, final String homePage, final String genre,
                                      final String country, final boolean addToFav, final boolean addToServer) {
        final RadioStationToAdd rsToAdd = new RadioStationToAdd(
                name, url, imageLocalUrl, imageWebUrl, homePage, genre, country, addToFav, addToServer
        );

        processInput(rsToAdd);
    }

    private void toggleWebImageView(final View view, final boolean enabled) {
        if (view == null) {
            return;
        }

        final TextView label = view.findViewById(R.id.add_edit_station_web_image_url_label);
        final EditText edit = view.findViewById(R.id.add_edit_station_web_image_url_edit);

        label.setEnabled(enabled);
        edit.setEnabled(enabled);
    }
}
