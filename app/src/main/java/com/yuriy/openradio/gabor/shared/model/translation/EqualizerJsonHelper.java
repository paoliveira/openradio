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

package com.yuriy.openradio.gabor.shared.model.translation;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/9/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
final class EqualizerJsonHelper {

    private EqualizerJsonHelper() { super(); }

    /**
     * JSON's keys
     */

    static final String KEY_PRESETS = "Presets";
    static final String KEY_CURRENT_PRESET = "CurrentPreset";
    static final String KEY_ENABLED = "Enabed";
    static final String KEY_NUM_OF_BANDS = "NumOfBands";
    static final String KEY_BAND_LEVEL_RANGE = "BandLevelRange";
    static final String KEY_CENTER_FREQUENCIES = "CenterFrequencies";
    static final String KEY_BAND_LEVELS = "BandLevels";
}
