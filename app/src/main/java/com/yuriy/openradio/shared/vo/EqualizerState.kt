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
package com.yuriy.openradio.shared.vo

import android.media.audiofx.Equalizer
import com.yuriy.openradio.shared.utils.AnalyticsUtils
import com.yuriy.openradio.shared.utils.AppLogger
import java.util.*

/**
 * Class that represents state of selected Equalizer.
 */
class EqualizerState() {

    var isEnabled = true
    var numOfBands: Short = 0
    private var mCurrentPreset: Short = 0
    private var mBandLevelRange: ShortArray
    private var mCenterFrequencies: IntArray
    private var mBandLevels: ShortArray
    private val mPresets: MutableList<String>

    /**
     * Copy constructor.
     *
     * @param equalizer Instance to copy from.
     */
    constructor(equalizer: Equalizer) : this() {
        isEnabled = equalizer.enabled
        mCurrentPreset = equalizer.currentPreset
        numOfBands = equalizer.numberOfBands
        if (equalizer.bandLevelRange.size == 2) {
            mBandLevelRange[0] = equalizer.bandLevelRange[0]
            mBandLevelRange[1] = equalizer.bandLevelRange[1]
        } else {
            AppLogger.e("Num of bands of eq is not 2")
        }
        val numOfPresets = equalizer.numberOfPresets.toInt()
        for (i in 0 until numOfPresets) {
            mPresets.add(equalizer.getPresetName(i.toShort()))
        }
        mCenterFrequencies = IntArray(numOfBands.toInt())
        mBandLevels = ShortArray(numOfBands.toInt())
        for (i in 0 until numOfBands) {
            mBandLevels[i] = equalizer.getBandLevel(i.toShort())
            mCenterFrequencies[i] = equalizer.getCenterFreq(i.toShort())
        }
    }

    var currentPreset: Short
        get() = mCurrentPreset
        set(value) {
            mCurrentPreset = if (value < 0) {
                AppLogger.e("$CLASS_NAME invalid curr preset id:$value, convert to 0")
                0
            } else {
                value
            }
        }
    var bandLevelRange: ShortArray
        get() = if (mBandLevelRange.size == 2) {
            mBandLevelRange.copyOf(mBandLevelRange.size)
        } else shortArrayOf(1500, -1500)
        set(value) {
            mBandLevelRange = value.copyOf(value.size)
        }
    var centerFrequencies: IntArray
        get() = mCenterFrequencies.copyOf(mCenterFrequencies.size)
        set(value) {
            mCenterFrequencies = value.copyOf(value.size)
        }
    var bandLevels: ShortArray
        get() = mBandLevels.copyOf(mBandLevels.size)
        set(value) {
            mBandLevels = value.copyOf(value.size)
        }
    var presets: List<String>
        get() = ArrayList(mPresets)
        set(value) {
            mPresets.clear()
            mPresets.addAll(value)
        }

    fun printState() {
        AppLogger.d("Eqlsr level rng:" + mBandLevelRange.contentToString() + ", milliBel")
        if (mPresets.isNotEmpty()) {
            AppLogger.d("Eqlsr cur preset:" + mPresets[currentPreset.toInt()])
        } else {
            AppLogger.d("Eqlsr cur preset unknown")
        }
        for (i in 0 until numOfBands) {
            val lvl = mBandLevels[i]
            val cntFq = mCenterFrequencies[i]
            AppLogger.d("Eqlsr level:$lvl, cnt fq:$cntFq")
        }
    }

    companion object {
        private val CLASS_NAME = EqualizerState::class.java.simpleName + " "

        @JvmStatic
        fun createState(equalizer: Equalizer): EqualizerState {
            return EqualizerState(equalizer)
        }

        @JvmStatic
        fun applyState(equalizer: Equalizer, state: EqualizerState) {
            try {
                if (!state.isEnabled) {
                    return
                }
                equalizer.enabled = false
                equalizer.enabled = true
                equalizer.usePreset(state.currentPreset)
            } catch (e: Exception) {
                AnalyticsUtils.logMessage("Apply eq state:$equalizer")
                AnalyticsUtils.logException(RuntimeException("Can not apply eq state:$e"))
            }
        }
    }

    /**
     * Default constructor.
     */
    init {
        mBandLevelRange = ShortArray(2)
        mCenterFrequencies = IntArray(0)
        mBandLevels = ShortArray(0)
        mPresets = ArrayList()
    }
}
