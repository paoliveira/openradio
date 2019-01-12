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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.yuriy.openradio.R;
import com.yuriy.openradio.business.storage.AppPreferencesManager;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class StreamBufferingDialog extends BaseDialogFragment {

    /**
     * Tag string mTo use in logging message.
     */
    private static final String CLASS_NAME = StreamBufferingDialog.class.getSimpleName();

    /**
     * Tag string mTo use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final MainActivity activity = (MainActivity) getActivity();

        final View view = getInflater().inflate(
                R.layout.settings_stream_buffering,
                activity.findViewById(R.id.settings_stream_buffering_root)
        );

        setWindowDimensions(view, 0.9f, 0.9f);

        final String titleText = "Stream Buffering";
        final TextView title = view.findViewById(R.id.stream_buffering_label_view);
        title.setText(titleText);

        final Context context = activity.getApplicationContext();

        final EditText minBuffer = view.findViewById(R.id.min_buffer_edit_view);
        minBuffer.setText(String.valueOf(AppPreferencesManager.getMinBuffer(context)));
        final EditText maxBuffer = view.findViewById(R.id.max_buffer_edit_view);
        maxBuffer.setText(String.valueOf(AppPreferencesManager.getMaxBuffer(context)));
        final EditText playBuffer = view.findViewById(R.id.play_buffer_edit_view);
        playBuffer.setText(String.valueOf(AppPreferencesManager.getPlayBuffer(context)));
        final EditText playBufferRebuffer = view.findViewById(R.id.play_buffer_after_rebuffer_edit_view);
        playBufferRebuffer.setText(String.valueOf(AppPreferencesManager.getPlayBufferRebuffer(context)));

        return createAlertDialog(view);
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
