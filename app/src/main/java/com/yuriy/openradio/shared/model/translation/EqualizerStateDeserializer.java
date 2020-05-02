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

import android.content.Context;
import android.media.audiofx.Equalizer;

import com.yuriy.openradio.shared.vo.EqualizerState;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/9/15
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * {@link EqualizerStateSerializer} is an interface that provides common method to de-serialize
 * {@link Equalizer}'s state which is needed to apply to instance of {@link Equalizer}.
 */
public interface EqualizerStateDeserializer {

    /**
     * Deserialize {@link Equalizer}'s state.
     *
     * @param context Context of callee.
     * @param value   String representation of the {@link Equalizer}'s state.
     * @return State of {@link Equalizer} as {@link EqualizerState} object.
     */
    EqualizerState deserialize(final Context context, final String value);
}
