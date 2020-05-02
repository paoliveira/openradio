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

package com.yuriy.openradio.shared.model.translation;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.JsonUtils;
import com.yuriy.openradio.shared.vo.EqualizerState;

import org.json.JSONObject;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/9/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class EqualizerStateJsonDeserializer implements EqualizerStateDeserializer {

    /**
     * Default constructor.
     */
    public EqualizerStateJsonDeserializer() {
        super();
    }

    @Override
    public final EqualizerState deserialize(@NonNull final Context context, @NonNull final String value) {
        final EqualizerState state = new EqualizerState();
        try {
            final JSONObject jsonObject = new JSONObject(value);
            state.setEnabled(JsonUtils.getBooleanValue(jsonObject, EqualizerJsonHelper.KEY_ENABLED));
            state.setCurrentPreset((short) JsonUtils.getIntValue(jsonObject, EqualizerJsonHelper.KEY_CURRENT_PRESET));
            state.setNumOfBands((short) JsonUtils.getIntValue(jsonObject, EqualizerJsonHelper.KEY_NUM_OF_BANDS));
            state.setPresets(JsonUtils.getListValue(jsonObject, EqualizerJsonHelper.KEY_PRESETS));
            state.setBandLevelRange(JsonUtils.getShortArray(jsonObject, EqualizerJsonHelper.KEY_BAND_LEVEL_RANGE));
            state.setBandLevels(JsonUtils.getShortArray(jsonObject, EqualizerJsonHelper.KEY_BAND_LEVELS));
            state.setCenterFrequencies(JsonUtils.getIntArray(jsonObject, EqualizerJsonHelper.KEY_CENTER_FREQUENCIES));
        } catch (final Throwable e) {
            /* Ignore this exception */
            AppLogger.e("Error while de-marshall " + value + ", exception:\n" + Log.getStackTraceString(e));
        }
        return state;
    }
}
