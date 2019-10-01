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

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.yuriy.openradio.R;
import com.yuriy.openradio.view.BaseDialogFragment;
import com.yuriy.openradio.view.activity.MainActivity;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class RemoveStationDialog extends BaseDialogFragment {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = RemoveStationDialog.class.getSimpleName();

    /**
     * Tag string to use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    /**
     * Key for the Media Id value.
     */
    private static final String KEY_MEDIA_ID = "KEY_MEDIA_ID";

    /**
     * Key for the Name value.
     */
    private static final String KEY_NAME = "KEY_NAME";

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final MainActivity activity = (MainActivity) getActivity();

        final View view = getInflater().inflate(
                R.layout.dialog_remove_station,
                activity.findViewById(R.id.remove_station_dialog_root)
        );

        setWindowDimensions(view, 0.8f, 0.2f);

        final String mediaId = getArgument(getArguments(), KEY_MEDIA_ID);
        final String name = getArgument(getArguments(), KEY_NAME);

        final TextView textView = view.findViewById(R.id.remove_station_text_view);
        textView.setText(getString(R.string.remove_station_dialog_main_text, name));

        final Button removeBtn = view.findViewById(R.id.remove_station_dialog_add_btn_view);
        removeBtn.setOnClickListener(
                viewBtn -> {
                    activity.processRemoveStationCallback(
                            mediaId
                    );
                    getDialog().dismiss();
                }
        );

        final Button cancelBtn = view.findViewById(R.id.remove_station_dialog_cancel_btn_view);
        cancelBtn.setOnClickListener(
                viewBtn -> getDialog().dismiss()
        );

        return createAlertDialog(view);
    }

    public static Bundle createBundle(final String mediaId, final String name) {
        final Bundle bundle = new Bundle();
        bundle.putString(KEY_MEDIA_ID, mediaId);
        bundle.putString(KEY_NAME, name);
        return bundle;
    }

    /**
     * Extract argument from the Bundle.
     *
     * @param bundle Arguments {@link Bundle}.
     * @param key    Key of the argument.
     *
     * @return Value associated with the provided key, or an empty string.
     */
    private static String getArgument(final Bundle bundle, final String key) {
        if (bundle == null) {
            return "";
        }
        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        return "";
    }
}
