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

package com.yuriy.openradio.shared.model.translation;

import android.media.audiofx.Equalizer;
import android.util.Log;

import androidx.annotation.NonNull;

import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.vo.EqualizerState;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/4/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link EqualizerJsonStateSerializer} is implementation of the {@link EqualizerStateSerializer}
 * interface that serialize {@link Equalizer} into JSON's String.
 */
public final class EqualizerJsonStateSerializer implements EqualizerStateSerializer {

    /**
     * Default constructor.
     */
    public EqualizerJsonStateSerializer() {
        super();
    }

    @Override
    public final String serialize(@NonNull final EqualizerState state) {
        final JSONObject jsonObject = new JSONObject();
        try {
            StringBuilder builder = new StringBuilder();
            final List<String> presets = state.getPresets();
            for (int i = 0; i < presets.size(); i++) {
                builder.append(presets.get(i));
                if (i < presets.size() - 1) {
                    builder.append(",");
                }
            }
            jsonObject.put(EqualizerJsonHelper.KEY_PRESETS, builder.toString());

            jsonObject.put(EqualizerJsonHelper.KEY_CURRENT_PRESET, state.getCurrentPreset());
            jsonObject.put(EqualizerJsonHelper.KEY_ENABLED, state.isEnabled());
            jsonObject.put(EqualizerJsonHelper.KEY_NUM_OF_BANDS, state.getNumOfBands());

            if (state.getBandLevelRange().length == 2) {
                final String bandLevelRangeStr = state.getBandLevelRange()[0] + "," + state.getBandLevelRange()[1];
                jsonObject.put(EqualizerJsonHelper.KEY_BAND_LEVEL_RANGE, bandLevelRangeStr);
            }

            builder.setLength(0);
            int[] centerFrequencies = state.getCenterFrequencies();
            for (int i = 0; i < centerFrequencies.length; i++) {
                builder.append(centerFrequencies[i]);
                if (i < centerFrequencies.length - 1) {
                    builder.append(",");
                }
            }
            jsonObject.put(EqualizerJsonHelper.KEY_CENTER_FREQUENCIES, builder.toString());

            builder.setLength(0);
            short[] getBandLevels = state.getBandLevels();
            for (int i = 0; i < getBandLevels.length; i++) {
                builder.append(getBandLevels[i]);
                if (i < getBandLevels.length - 1) {
                    builder.append(",");
                }
            }
            jsonObject.put(EqualizerJsonHelper.KEY_BAND_LEVELS, builder.toString());
        } catch (final Exception e) {
            /* Ignore this exception */
            AppLogger.e("Error while marshall " + state + ", exception:\n" + Log.getStackTraceString(e));
        }
        return jsonObject.toString();
    }
}
