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
package com.yuriy.openradio.shared.model.translation

import android.content.Context
import android.util.Log
import com.yuriy.openradio.shared.utils.AppLogger.e
import com.yuriy.openradio.shared.utils.JsonUtils.getBooleanValue
import com.yuriy.openradio.shared.utils.JsonUtils.getIntArray
import com.yuriy.openradio.shared.utils.JsonUtils.getIntValue
import com.yuriy.openradio.shared.utils.JsonUtils.getListValue
import com.yuriy.openradio.shared.utils.JsonUtils.getShortArray
import com.yuriy.openradio.shared.vo.EqualizerState
import org.json.JSONObject

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/9/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class EqualizerStateJsonDeserializer : EqualizerStateDeserializer {
    override fun deserialize(context: Context, value: String): EqualizerState {
        val state = EqualizerState()
        try {
            val jsonObject = JSONObject(value)
            state.isEnabled = getBooleanValue(jsonObject, EqualizerJsonHelper.KEY_ENABLED)
            state.currentPreset = getIntValue(jsonObject, EqualizerJsonHelper.KEY_CURRENT_PRESET).toShort()
            state.numOfBands = getIntValue(jsonObject, EqualizerJsonHelper.KEY_NUM_OF_BANDS).toShort()
            state.presets = getListValue(jsonObject, EqualizerJsonHelper.KEY_PRESETS)
            state.bandLevelRange = getShortArray(jsonObject, EqualizerJsonHelper.KEY_BAND_LEVEL_RANGE)
            state.bandLevels = getShortArray(jsonObject, EqualizerJsonHelper.KEY_BAND_LEVELS)
            state.centerFrequencies = getIntArray(jsonObject, EqualizerJsonHelper.KEY_CENTER_FREQUENCIES)
        } catch (e: Throwable) {
            /* Ignore this exception */
            e("""
    Error while de-marshall $value, exception:
    ${Log.getStackTraceString(e)}
    """.trimIndent())
        }
        return state
    }
}
