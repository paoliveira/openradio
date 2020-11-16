/*
 * Copyright 2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.view.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.model.storage.EqualizerStorage;
import com.yuriy.openradio.shared.model.translation.EqualizerJsonStateSerializer;
import com.yuriy.openradio.shared.model.translation.EqualizerStateDeserializer;
import com.yuriy.openradio.shared.model.translation.EqualizerStateJsonDeserializer;
import com.yuriy.openradio.shared.model.translation.EqualizerStateSerializer;
import com.yuriy.openradio.shared.service.OpenRadioService;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.view.BaseDialogFragment;
import com.yuriy.openradio.shared.vo.EqualizerState;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class EqualizerDialog extends BaseDialogFragment {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = EqualizerDialog.class.getSimpleName();

    /**
     * Tag string to use in dialog transactions.
     */
    public static final String DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG";

    private LinearLayout mLinearLayout;

    @Override
    @NonNull
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final Activity activity = getActivity();

        final View view = getInflater().inflate(
                R.layout.dialog_eq,
                activity.findViewById(R.id.dialog_eq_root)
        );
        mLinearLayout = view.findViewById(R.id.eq_controls_view);

        setWindowDimensions(view, 0.8f, 0.6f);

        final Context context = getContext();
        final TextView notAvailableView = view.findViewById(R.id.eq_not_available_view);
        if (EqualizerStorage.isEmpty(context)) {
            notAvailableView.setVisibility(View.VISIBLE);
        } else {
            notAvailableView.setVisibility(View.GONE);
            handleEqualizer(context, view);
        }

        return createAlertDialog(view);
    }

    private void handleEqualizer(final Context context, final View view) {
        if (context == null) {
            return;
        }
        final EqualizerStateDeserializer deserializer = new EqualizerStateJsonDeserializer();
        final EqualizerState state = deserializer.deserialize(
                context, EqualizerStorage.loadEqualizerState(context)
        );
        final List<String> presets = state.getPresets();
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                presets
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        final Spinner spinner = view.findViewById(R.id.eq_presets_spinner);
        spinner.setAdapter(adapter);
        spinner.setSelection(state.getCurrentPreset());
        spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        AppLogger.d(CLASS_NAME + " use preset: " + presets.get(position));
                        state.setCurrentPreset((short) position);
                        final EqualizerStateSerializer serializer = new EqualizerJsonStateSerializer();
                        EqualizerStorage.saveEqualizerState(context, serializer.serialize(state));

                        final Activity activity = getActivity();
                        if (activity == null) {
                            AppLogger.e("Can not call equalizer update, activity is null");
                            return;
                        }
                        activity.startService(OpenRadioService.makeUpdateEqualizerIntent(context));

                        new Handler(Looper.getMainLooper()).postDelayed(
                                () -> {
                                    final EqualizerState state1 = deserializer.deserialize(
                                            context, EqualizerStorage.loadEqualizerState(context)
                                    );
                                    updateEqualizer(context, state1);
                                },
                                500
                        );
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

        updateEqualizer(context, state);
    }

    private void updateEqualizer(@NonNull final Context context,
                                 @NonNull final EqualizerState state) {
        mLinearLayout.removeAllViews();

        final short lowerEqualizerBandLevel = state.getBandLevelRange()[0];
        final short upperEqualizerBandLevel = state.getBandLevelRange()[1];
        for (short i = 0; i < state.getNumOfBands(); i++) {

            final TextView frequencyView = new TextView(context);
            frequencyView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            );
            frequencyView.setGravity(Gravity.CENTER_HORIZONTAL);
            final String msg0 = (state.getCenterFrequencies()[i] / 1000) + " Hz";
            frequencyView.setText(msg0);
            mLinearLayout.addView(frequencyView);

            final TextView lowerBandLevelView = new TextView(context);
            lowerBandLevelView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            );
            final String msg1 = (lowerEqualizerBandLevel / 100) + " dB";
            lowerBandLevelView.setText(msg1);
            final TextView upperBandLevelView = new TextView(context);
            upperBandLevelView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            );
            String msg2 = (upperEqualizerBandLevel / 100) + " dB";
            upperBandLevelView.setText(msg2);

            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT - 60, 120
            );
            params.weight = 1;

            final SeekBar seekBar = new SeekBar(context);
            seekBar.setEnabled(false);
            seekBar.setId(i);
            seekBar.setPadding(35, 15, 35, 15);
            seekBar.setLayoutParams(params);
            seekBar.setMax(upperEqualizerBandLevel - lowerEqualizerBandLevel);
            seekBar.setProgress((upperEqualizerBandLevel - lowerEqualizerBandLevel) / 2 + state.getBandLevels()[i]);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                    mEqualizer.setBandLevel(equalizerBandIndex, (short) (progress + lowerEqualizerBandLevel));
                }

                public void onStartTrackingTouch(SeekBar seekBar) {
                    //not used
                }

                public void onStopTrackingTouch(SeekBar seekBar) {
//                    properties.edit_preferences.putInt("seek_" + seek_id, seekBar.getProgress()).commit();
//                    properties.edit_preferences.putInt("position", 0).commit();
                }
            });
            seekBar.setProgressDrawable(new ColorDrawable(Color.rgb(56, 60, 62)));

            final LinearLayout.LayoutParams seekBarLayout = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            );
            seekBarLayout.weight = 1;
            seekBarLayout.setMargins(5, 0, 5, 0);

            final LinearLayout seekBarRowLayout = new LinearLayout(context);
            seekBarRowLayout.setOrientation(LinearLayout.HORIZONTAL);
            seekBarRowLayout.setLayoutParams(seekBarLayout);

            seekBarRowLayout.addView(lowerBandLevelView);
            seekBarRowLayout.addView(seekBar);
            seekBarRowLayout.addView(upperBandLevelView);

            mLinearLayout.addView(seekBarRowLayout);
        }
    }
}
