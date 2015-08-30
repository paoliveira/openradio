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
import android.widget.EditText;

import com.yuriy.openradio.R;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class SearchDialog extends DialogFragment {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = SearchDialog.class.getSimpleName();

    /**
     * Tag string to use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    /**
     * Create a new instance of {@link SearchDialog}
     */
    @SuppressWarnings("all")
    public static SearchDialog newInstance() {
        final SearchDialog aboutDialog = new SearchDialog();
        // provide here an arguments, if any
        return aboutDialog;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        getDialog().setTitle(getActivity().getString(R.string.search_label));

        final View view = inflater.inflate(R.layout.dialog_search, container, false);
        final MainActivity activity = (MainActivity) getActivity();
        final EditText searchEditView = (EditText) view.findViewById(R.id.search_dialog_edit_txt_view);
        final Button searchBtn = (Button) view.findViewById(R.id.search_dialog_btn_view);
        searchBtn.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (activity != null && searchEditView != null) {
                            activity.onSearchDialogClick(searchEditView.getText().toString().trim());
                        }
                        getDialog().dismiss();
                    }
                }
        );

        return view;
    }
}
