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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.yuriy.openradio.R;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.utils.BitmapHelper;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/2/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class FlagOverlayImpl implements FlagOverlay {

    /**
     * Path to the directory with Flags, under app's folder.
     */
    private static final String FLAGS_DIR = "/img/flags";

    private static final String CLASS_NAME = FlagOverlayImpl.class.getSimpleName();

    /**
     * Private constructor.
     */
    private FlagOverlayImpl() { }

    /**
     * Factory method to make default instance of the {@link FlagOverlay}.
     * @return Default instance of the {@link FlagOverlay}.
     */
    public static FlagOverlay getInstance() {
        return new FlagOverlayImpl();
    }

    @Override
    public final Bitmap getFlag(final Context context, final String countryCode,
                                final Bitmap baseBitmap) {
        final int identifier = context.getResources().getIdentifier(
                "flag_" + countryCode.toLowerCase(),
                "drawable", context.getPackageName()
        );

        if (identifier == 0) {
            Log.w(CLASS_NAME, "No flag found for the:" + countryCode);
            return baseBitmap;
        }

        final Bitmap flag = BitmapFactory.decodeResource(
                context.getResources(),
                identifier
        );

        return BitmapHelper.overlayWithFlag(baseBitmap, flag);
    }

    /**
     * Build url to the county's flag.
     *
     * @param context     Callee context.
     * @param countryCode Country code.
     * @return Url to the flag image of the country.
     */
    private String getFlagUrl(final Context context, final String countryCode) {
        // Create directory if such does nor exists
        AppUtils.createDirIfNeeded(AppUtils.getExternalStorageDir(context) + FLAGS_DIR);
        // return a path to the file
        return AppUtils.getExternalStorageDir(context) + FLAGS_DIR + "/flag_" + countryCode + ".jpg";
    }
}
