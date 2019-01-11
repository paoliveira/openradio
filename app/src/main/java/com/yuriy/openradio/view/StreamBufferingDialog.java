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

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
public final class StreamBufferingDialog extends DialogFragment {

    /**
     * Tag string mTo use in logging message.
     */
    private static final String CLASS_NAME = StreamBufferingDialog.class.getSimpleName();

    /**
     * Tag string mTo use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    /**
     * Create a new instance of {@link StreamBufferingDialog}
     */
    @SuppressWarnings("all")
    public static StreamBufferingDialog newInstance() {
        final StreamBufferingDialog aboutDialog = new StreamBufferingDialog();
        // provide here an arguments, if any
        return aboutDialog;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.settings_stream_buffering, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final String titleText = "Stream Buffering";
        final TextView title = view.findViewById(R.id.stream_buffering_label_view);
        title.setText(titleText);

        final Context context = getActivity().getApplicationContext();

        final EditText minBuffer = getView().findViewById(R.id.min_buffer_edit_view);
        minBuffer.setText(String.valueOf(AppPreferencesManager.getMinBuffer(context)));
        final EditText maxBuffer = getView().findViewById(R.id.max_buffer_edit_view);
        maxBuffer.setText(String.valueOf(AppPreferencesManager.getMaxBuffer(context)));
        final EditText playBuffer = getView().findViewById(R.id.play_buffer_edit_view);
        playBuffer.setText(String.valueOf(AppPreferencesManager.getPlayBuffer(context)));
        final EditText playBufferRebuffer = getView().findViewById(R.id.play_buffer_after_rebuffer_edit_view);
        playBufferRebuffer.setText(String.valueOf(AppPreferencesManager.getPlayBufferRebuffer(context)));
    }

    @Override
    public void onPause() {
        super.onPause();

    }
}
