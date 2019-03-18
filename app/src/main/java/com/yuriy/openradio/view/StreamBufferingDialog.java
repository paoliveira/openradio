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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.exoplayer2.DefaultLoadControl;
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

    private EditText mMinBuffer;
    private EditText mMaxBuffer;
    private EditText mPlayBuffer;
    private EditText mPlayBufferRebuffer;

    @SuppressLint("StringFormatInvalid")
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final MainActivity activity = (MainActivity) getActivity();

        final View view = getInflater().inflate(
                R.layout.settings_stream_buffering,
                activity.findViewById(R.id.settings_stream_buffering_root)
        );

        setWindowDimensions(view, 0.9f, 0.9f);

        final String titleText = getString(R.string.stream_buffering_label);
        final TextView title = view.findViewById(R.id.stream_buffering_label_view);
        title.setText(titleText);

        final Context context = activity.getApplicationContext();

        final TextView descView = view.findViewById(R.id.stream_buffering_desc_view);
        try {
            descView.setText(
                    String.format(
                            getResources().getString(R.string.stream_buffering_descr),
                            getResources().getInteger(R.integer.min_buffer_val),
                            getResources().getInteger(R.integer.max_buffer_val),
                            getResources().getInteger(R.integer.min_buffer_sec),
                            getResources().getInteger(R.integer.max_buffer_min)
                    )
            );
        } catch (final Exception e) {
            /* Ignore */
        }

        mMinBuffer = view.findViewById(R.id.min_buffer_edit_view);
        mMinBuffer.setText(String.valueOf(AppPreferencesManager.getMinBuffer(context)));
        mMaxBuffer = view.findViewById(R.id.max_buffer_edit_view);
        mMaxBuffer.setText(String.valueOf(AppPreferencesManager.getMaxBuffer(context)));
        mPlayBuffer = view.findViewById(R.id.play_buffer_edit_view);
        mPlayBuffer.setText(String.valueOf(AppPreferencesManager.getPlayBuffer(context)));
        mPlayBufferRebuffer = view.findViewById(R.id.play_buffer_after_rebuffer_edit_view);
        mPlayBufferRebuffer.setText(String.valueOf(AppPreferencesManager.getPlayBufferRebuffer(context)));

        final Button restoreBtn = view.findViewById(R.id.buffering_restore_btn);
        restoreBtn.setOnClickListener(
                v -> {
                    mMinBuffer.setText(String.valueOf(DefaultLoadControl.DEFAULT_MIN_BUFFER_MS));
                    mMaxBuffer.setText(String.valueOf(DefaultLoadControl.DEFAULT_MAX_BUFFER_MS));
                    mPlayBuffer.setText(String.valueOf(DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS));
                    mPlayBufferRebuffer.setText(
                            String.valueOf(DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                    );
                }
        );

        return createAlertDialog(view);
    }

    @Override
    public void onPause() {
        super.onPause();

        final Context context = getActivity().getApplicationContext();
        if (context == null) {
            return;
        }
        final String erroMsgBase = getString(R.string.invalid_buffer_desc);
        if (mMinBuffer != null) {
            final String minBufferStr = mMinBuffer.getText().toString().trim();
            if (validateInput(minBufferStr)) {
                AppPreferencesManager.setMinBuffer(context, Integer.valueOf(minBufferStr));
            } else {
                SafeToast.showAnyThread(context, erroMsgBase + minBufferStr);
            }
        }
        if (mMaxBuffer != null) {
            final String maxBufferStr = mMaxBuffer.getText().toString().trim();
            if (validateInput(maxBufferStr)) {
                AppPreferencesManager.setMaxBuffer(context, Integer.valueOf(maxBufferStr));
            } else {
                SafeToast.showAnyThread(context, erroMsgBase + maxBufferStr);
            }
        }
        if (mPlayBuffer != null) {
            final String playBufferStr = mPlayBuffer.getText().toString().trim();
            if (validateInput(playBufferStr)) {
                AppPreferencesManager.setPlayBuffer(context, Integer.valueOf(playBufferStr));
            } else {
                SafeToast.showAnyThread(context, erroMsgBase + playBufferStr);
            }
        }
        if (mPlayBufferRebuffer != null) {
            final String playBufferRebufferStr = mPlayBufferRebuffer.getText().toString().trim();
            if (validateInput(playBufferRebufferStr)) {
                AppPreferencesManager.setPlayBufferRebuffer(
                        context, Integer.valueOf(playBufferRebufferStr)
                );
            } else {
                SafeToast.showAnyThread(context, erroMsgBase + playBufferRebufferStr);
            }
        }
    }

    private boolean validateInput(final String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        if (!TextUtils.isDigitsOnly(value)) {
            return false;
        }
        int valueInt;
        try {
            valueInt = Integer.valueOf(value);
        } catch (final NumberFormatException e) {
            return false;
        }
        if (valueInt > getResources().getInteger(R.integer.max_buffer_val)) {
            return false;
        }
        return valueInt >= getResources().getInteger(R.integer.min_buffer_val);
    }
}
