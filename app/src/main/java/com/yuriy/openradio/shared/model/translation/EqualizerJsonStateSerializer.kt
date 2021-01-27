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
package com.yuriy.openradio.shared.model.translation

import android.util.Log
import com.yuriy.openradio.shared.utils.AppLogger.e
import com.yuriy.openradio.shared.vo.EqualizerState
import org.json.JSONObject

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/4/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * [EqualizerJsonStateSerializer] is implementation of the [EqualizerStateSerializer]
 * interface that serialize [android.media.audiofx.Equalizer] into JSON's String.
 */
class EqualizerJsonStateSerializer : EqualizerStateSerializer {

    override fun serialize(state: EqualizerState): String {
        val jsonObject = JSONObject()
        try {
            val builder = StringBuilder()
            val presets = state.presets
            for (i in presets.indices) {
                builder.append(presets[i])
                if (i < presets.size - 1) {
                    builder.append(",")
                }
            }
            jsonObject.put(EqualizerJsonHelper.KEY_PRESETS, builder.toString())
            jsonObject.put(EqualizerJsonHelper.KEY_CURRENT_PRESET, state.currentPreset.toInt())
            jsonObject.put(EqualizerJsonHelper.KEY_ENABLED, state.isEnabled)
            jsonObject.put(EqualizerJsonHelper.KEY_NUM_OF_BANDS, state.numOfBands.toInt())
            if (state.bandLevelRange.size == 2) {
                val bandLevelRangeStr = state.bandLevelRange[0].toString() + "," + state.bandLevelRange[1]
                jsonObject.put(EqualizerJsonHelper.KEY_BAND_LEVEL_RANGE, bandLevelRangeStr)
            }
            builder.setLength(0)
            val centerFrequencies = state.centerFrequencies
            for (i in centerFrequencies.indices) {
                builder.append(centerFrequencies[i])
                if (i < centerFrequencies.size - 1) {
                    builder.append(",")
                }
            }
            jsonObject.put(EqualizerJsonHelper.KEY_CENTER_FREQUENCIES, builder.toString())
            builder.setLength(0)
            val getBandLevels = state.bandLevels
            for (i in getBandLevels.indices) {
                builder.append(getBandLevels[i])
                if (i < getBandLevels.size - 1) {
                    builder.append(",")
                }
            }
            jsonObject.put(EqualizerJsonHelper.KEY_BAND_LEVELS, builder.toString())
        } catch (e: Exception) {
            /* Ignore this exception */
            e("Error while marshall $state, exception:${Log.getStackTraceString(e)}")
        }
        return jsonObject.toString()
    }
}
