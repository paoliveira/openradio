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

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
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
import android.widget.Toast;

import com.yuriy.openradio.R;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.utils.ImageFilePath;
import com.yuriy.openradio.utils.IntentsHelper;
import com.yuriy.openradio.utils.PermissionChecker;
import com.yuriy.openradio.vo.RadioStation;

import java.util.ArrayList;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class AddStationDialog extends DialogFragment {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = AddStationDialog.class.getSimpleName();

    /**
     * Tag string to use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    /**
     * View for the selected Image Url.
     */
    private EditText mImageUrlEdit;

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
                (int) (AppUtils.getShortestScreenSize(getActivity()) * 0.8),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        final LinearLayout root = view.findViewById(R.id.add_station_dialog_root);
        root.setLayoutParams(layoutParams);

        final EditText nameEdit = view.findViewById(R.id.add_station_name_edit);
        final EditText urlEdit = view.findViewById(R.id.add_station_stream_url_edit);
        mImageUrlEdit = view.findViewById(R.id.add_station_image_url_edit);

        final Spinner countriesSpinner = view.findViewById(R.id.add_station_country_spin);
        // Create an ArrayAdapter using the string array and a default spinner layout
        final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                new ArrayList<CharSequence>(AppUtils.COUNTRY_CODE_TO_NAME.values())
        );
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        countriesSpinner.setAdapter(adapter);

        final Spinner genresSpinner = view.findViewById(R.id.add_station_genre_spin);
        // Create an ArrayAdapter using the string array and a default spinner layout
        final ArrayAdapter<CharSequence> genresAdapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                new ArrayList<CharSequence>(AppUtils.predefinedCategories())
        );
        // Specify the layout to use when the list of choices appears
        genresAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        genresSpinner.setAdapter(genresAdapter);

        final Button imageUrlBtn = view.findViewById(R.id.add_station_image_browse_btn);
        imageUrlBtn.setOnClickListener(
                viewBtn -> {
                    final Intent galleryIntent = new Intent();
                    galleryIntent.setType("image/*");
                    galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                    // Chooser of filesystem options.
                    final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Image");

                    startActivityForResult(chooserIntent , IntentsHelper.REQUEST_CODE_FILE_SELECTED);
                }
        );

        final CheckBox addToFavCheckView = view.findViewById(R.id.add_to_fav_check_view);

        final Button addBtn = view.findViewById(R.id.add_station_dialog_add_btn_view);
        addBtn.setOnClickListener(
                viewBtn -> processInput(
                        nameEdit.getText().toString(),
                        urlEdit.getText().toString(),
                        mImageUrlEdit.getText().toString(),
                        String.valueOf(genresSpinner.getSelectedItem()),
                        String.valueOf(countriesSpinner.getSelectedItem()),
                        addToFavCheckView.isChecked()
                )
        );

        final Button cancelBtn = view.findViewById(R.id.add_station_dialog_cancel_btn_view);
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

        switch(requestCode) {
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
    private void processInput(final String name, final String url, final String imageUrl,
                              final String genre, final String country, final boolean addToFav) {
        if (TextUtils.isEmpty(name)) {
            displayToast("Name is empty");
            return;
        }
        if (TextUtils.isEmpty(url)) {
            displayToast("Stream URL is empty");
            return;
        }

        ((MainActivity) getActivity()).processAddStationCallback(
                name, url, imageUrl, genre, country, addToFav
        );

        getDialog().dismiss();
    }

    /**
     * Display Toast message.
     *
     * @param text message to be displayed.
     */
    private void displayToast(final String text) {
        final Toast toast = Toast.makeText(getActivity(), text, Toast.LENGTH_LONG);
        toast.show();
    }
}
