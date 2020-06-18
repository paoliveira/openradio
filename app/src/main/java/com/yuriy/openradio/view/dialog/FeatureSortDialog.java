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

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager;
import com.yuriy.openradio.shared.view.BaseDialogFragment;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/3/15
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * {@link FeatureSortDialog} is a Dialog to inform user about how to use Sort feature.
 */
public final class FeatureSortDialog extends BaseDialogFragment {

    /**
     * Tag string to use in the debugging messages.
     */
    private static final String LOG_TAG = FeatureSortDialog.class.getSimpleName();

    /**
     * The tag for this fragment, as per {@link android.app.FragmentTransaction#add}.
     */
    public final static String DIALOG_TAG = LOG_TAG + "Tag";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final View view = getInflater().inflate(R.layout.feature_sort_dialog,
                 getActivity().findViewById(R.id.feature_sort_dialog_root));

        final Button okBtn
                = view.findViewById(R.id.feature_sort_ok_btn_view);
        okBtn.setOnClickListener(v -> {
                    AppPreferencesManager.setSortDialogShown(getActivity(), true);
                    dismiss();
                }
        );

        setWindowDimensions(view, 0.9f, 0.9f);

        final AlertDialog.Builder builder = createAlertDialogBuilder(getActivity());
        builder.setTitle(getActivity().getString(R.string.feature_sort_title));
        builder.setView(view);
        return builder.create();
    }
}
