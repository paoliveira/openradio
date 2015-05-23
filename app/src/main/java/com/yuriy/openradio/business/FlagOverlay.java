/*
 * Copyright 2015 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.business;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/2/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */

import android.content.Context;
import android.graphics.Bitmap;

/**
 * {@link FlagOverlay} is an interface that provides method to overlay flag over image.
 */
public interface FlagOverlay {

    /**
     * Overlay bitmap of the flag of the provided country over provided image.
     *
     * @param context     Context of the callee.
     * @param countryCode Country code.
     * @param baseBitmap  Bitmap to overlay Flag over to.
     *
     * @return Bitmap of the base image overlayed by the flag.
     */
    Bitmap getFlag(final Context context, final String countryCode, final Bitmap baseBitmap);
}
