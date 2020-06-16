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

package com.yuriy.openradio.shared.vo;

import android.media.audiofx.Equalizer;

import androidx.annotation.NonNull;

import com.yuriy.openradio.shared.utils.AnalyticsUtils;
import com.yuriy.openradio.shared.utils.AppLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class that represents state of selected Equalizer.
 */
public final class EqualizerState {

    private boolean mEnabled;
    private short mNumOfBands;
    private short mCurrentPreset;
    private short[] mBandLevelRange;
    private int[] mCenterFrequencies;
    private short[] mBandLevels;
    private final List<String> mPresets;

    /**
     * Default constructor.
     */
    public EqualizerState() {
        super();
        mEnabled = true;
        mCurrentPreset = 0;
        mNumOfBands = 0;
        mBandLevelRange = new short[2];
        mCenterFrequencies = new int[0];
        mBandLevels = new short[0];
        mPresets = new ArrayList<>();
    }

    /**
     * Copy constructor.
     *
     * @param equalizer Instance to copy from.
     */
    public EqualizerState(@NonNull final Equalizer equalizer) {
        this();
        mEnabled = equalizer.getEnabled();
        mCurrentPreset = equalizer.getCurrentPreset();
        mNumOfBands = equalizer.getNumberOfBands();
        if (equalizer.getBandLevelRange().length == 2) {
            mBandLevelRange[0] = equalizer.getBandLevelRange()[0];
            mBandLevelRange[1] = equalizer.getBandLevelRange()[1];
        } else {
            AppLogger.e("Num of bands of eq is not 2");
        }

        final int numOfPresets = equalizer.getNumberOfPresets();
        for (short i = 0; i < numOfPresets; ++i) {
            mPresets.add(equalizer.getPresetName(i));
        }
        mCenterFrequencies = null;
        mCenterFrequencies = new int[mNumOfBands];
        mBandLevels = null;
        mBandLevels = new short[mNumOfBands];
        for (short i = 0; i < mNumOfBands; ++i) {
            mBandLevels[i] = equalizer.getBandLevel(i);
            mCenterFrequencies[i] = equalizer.getCenterFreq(i);
        }
    }

    public void setEnabled(final boolean value) {
        mEnabled = value;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public short getNumOfBands() {
        return mNumOfBands;
    }

    public void setNumOfBands(final short value) {
        mNumOfBands = value;
    }

    public void setCurrentPreset(final short value) {
        mCurrentPreset = value;
    }

    public short getCurrentPreset() {
        return mCurrentPreset;
    }

    @NonNull
    public short[] getBandLevelRange() {
        if (mBandLevelRange.length == 2) {
            return Arrays.copyOf(mBandLevelRange, mBandLevelRange.length);
        }
        return new short[]{1500, -1500};
    }

    public void setBandLevelRange(@NonNull final short[] value) {
        mBandLevelRange = Arrays.copyOf(value, value.length);
    }

    @NonNull
    public int[] getCenterFrequencies() {
        return Arrays.copyOf(mCenterFrequencies, mCenterFrequencies.length);
    }

    public void setCenterFrequencies(@NonNull final int[] value) {
        mCenterFrequencies = Arrays.copyOf(value, value.length);
    }

    @NonNull
    public short[] getBandLevels() {
        return Arrays.copyOf(mBandLevels, mBandLevels.length);
    }

    public void setBandLevels(@NonNull final short[] value) {
        mBandLevels = Arrays.copyOf(value, value.length);
    }

    public void setPresets(@NonNull final List<String> value) {
        mPresets.clear();
        mPresets.addAll(value);
    }

    public List<String> getPresets() {
        return new ArrayList<>(mPresets);
    }

    @NonNull
    public static EqualizerState createState(@NonNull final Equalizer equalizer) {
        return new EqualizerState(equalizer);
    }

    public static void applyState(@NonNull final Equalizer equalizer, @NonNull final EqualizerState state) {
        try {
            if (!state.isEnabled()) {
                return;
            }
            equalizer.setEnabled(false);
            equalizer.setEnabled(true);
            equalizer.usePreset(state.getCurrentPreset());
        } catch (final Exception e) {
            AnalyticsUtils.logMessage("Apply eq state:" + equalizer);
            AnalyticsUtils.logException(new RuntimeException("Can not apply eq state:" + e));
        }
    }

    public void printState() {
        AppLogger.d("Eqlsr level rng:" + Arrays.toString(mBandLevelRange) + ", milliBel");
        AppLogger.d("Eqlsr cur preset:" + mPresets.get(getCurrentPreset()));
        for (short i = 0; i < mNumOfBands; ++i) {
            short lvl = mBandLevels[i];
            int cntFq = mCenterFrequencies[i];
            AppLogger.d("Eqlsr level:" + lvl + ", cnt fq:" + cntFq);
        }
    }
}
