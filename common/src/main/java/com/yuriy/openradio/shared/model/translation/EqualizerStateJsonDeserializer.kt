/*
 * Copyright 2020-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.model.translation

import android.content.Context
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.JsonUtils
import com.yuriy.openradio.shared.vo.EqualizerState
import org.json.JSONException
import org.json.JSONObject

class EqualizerStateJsonDeserializer : EqualizerStateDeserializer {

    override fun deserialize(context: Context, value: String): EqualizerState {
        val state = EqualizerState()
        val jsonObject: JSONObject = try {
             JSONObject(value)
        } catch (e: JSONException) {
            AppLogger.e("Error while de-marshall $value", e)
            return state
        }
        state.isEnabled = JsonUtils.getBooleanValue(jsonObject, EqualizerJsonHelper.KEY_ENABLED)
        state.currentPreset = JsonUtils.getIntValue(jsonObject, EqualizerJsonHelper.KEY_CURRENT_PRESET).toShort()
        state.numOfBands = JsonUtils.getIntValue(jsonObject, EqualizerJsonHelper.KEY_NUM_OF_BANDS).toShort()
        state.presets = JsonUtils.getListValue(jsonObject, EqualizerJsonHelper.KEY_PRESETS)
        state.bandLevelRange = JsonUtils.getShortArray(jsonObject, EqualizerJsonHelper.KEY_BAND_LEVEL_RANGE)
        state.bandLevels = JsonUtils.getShortArray(jsonObject, EqualizerJsonHelper.KEY_BAND_LEVELS)
        state.centerFrequencies = JsonUtils.getIntArray(jsonObject, EqualizerJsonHelper.KEY_CENTER_FREQUENCIES)
        return state
    }
}
