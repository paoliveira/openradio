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

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.yuriy.openradio.R;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.utils.IntentsHelper;

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
                new FrameLayout.LayoutParams(
                        (int) (AppUtils.getShortestScreenSize(getActivity()) * 0.8),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
        final LinearLayout root = (LinearLayout) view.findViewById(R.id.add_station_dialog_root);
        root.setLayoutParams(layoutParams);

        final EditText nameEdit = (EditText) view.findViewById(R.id.add_station_name_edit);
        final EditText urlEdit = (EditText) view.findViewById(R.id.add_station_stream_url_edit);
        mImageUrlEdit = (EditText) view.findViewById(R.id.add_station_image_url_edit);

        final Spinner countriesSpinner = (Spinner) view.findViewById(R.id.add_station_country_spin);
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

        final Spinner genresSpinner = (Spinner) view.findViewById(R.id.add_station_genre_spin);
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

        final Button imageUrlBtn = (Button) view.findViewById(R.id.add_station_image_browse_btn);
        imageUrlBtn.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(final View viewBtn) {
                        // show Choose File dialog
                        startActivityForResult(
                                FileDialog.makeIntentToOpenFile(getActivity()),
                                IntentsHelper.REQUEST_CODE_FILE_SELECTED
                        );
                    }
                }
        );

        final Button addBtn = (Button) view.findViewById(R.id.add_station_dialog_add_btn_view);
        addBtn.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(final View viewBtn) {
                        processInput(
                                nameEdit.getText().toString(),
                                urlEdit.getText().toString(),
                                mImageUrlEdit.getText().toString(),
                                String.valueOf(genresSpinner.getSelectedItem()),
                                String.valueOf(countriesSpinner.getSelectedItem())
                        );
                    }
                }
        );

        final Button cancelBtn = (Button) view.findViewById(R.id.add_station_dialog_cancel_btn_view);
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

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == IntentsHelper.REQUEST_CODE_FILE_SELECTED) {
            final String filePath = FileDialog.getFilePath(data);
            if (filePath != null) {
                mImageUrlEdit.setText(filePath);
            }
        }
    }

    /**
     * Validate provided input in order to pass data farther to
     * generate {@link com.yuriy.openradio.api.RadioStationVO}.
     *
     * @param name     Name of the Radio Station.
     * @param url      Url of the Stream associated with Radio Station.
     * @param imageUrl Url of the Image associated with Radio Station.
     * @param genre    Genre of the Radio Station.
     * @param country  Country of the Radio Station.
     */
    private void processInput(final String name, final String url, final String imageUrl,
                              final String genre, final String country) {
        if (TextUtils.isEmpty(name)) {
            displayToast("Name is empty");
            return;
        }
        if (TextUtils.isEmpty(url)) {
            displayToast("Stream URL is empty");
            return;
        }

        ((MainActivity) getActivity()).processAddStationCallback(
                name, url, imageUrl, genre, country
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
