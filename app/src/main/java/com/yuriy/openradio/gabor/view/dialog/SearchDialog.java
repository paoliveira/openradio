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

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yuriy.openradio.gabor.R;
import com.yuriy.openradio.gabor.shared.view.BaseDialogFragment;
import com.yuriy.openradio.gabor.view.activity.MainActivity;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class SearchDialog extends BaseDialogFragment {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = SearchDialog.class.getSimpleName();

    /**
     * Tag string to use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    @Override
    @NonNull
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final MainActivity activity = (MainActivity) getActivity();

        final View view = getInflater().inflate(
                R.layout.dialog_search,
                activity.findViewById(R.id.dialog_search_root)
        );

        setWindowDimensions(view, 0.8f, 0.4f);
        final EditText searchEditView = view.findViewById(R.id.search_dialog_edit_txt_view);
        final Button searchBtn = view.findViewById(R.id.search_dialog_btn_view);
        searchBtn.setOnClickListener(
                viewBtn -> {
                    if (searchEditView != null) {
                        activity.onSearchDialogClick(searchEditView.getText().toString().trim());
                    }
                    getDialog().dismiss();
                }
        );

        return createAlertDialog(view);
    }
}
