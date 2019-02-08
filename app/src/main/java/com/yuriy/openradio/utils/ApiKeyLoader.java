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

package com.yuriy.openradio.utils;

import android.content.Context;
import android.text.TextUtils;

import com.yuriy.openradio.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link com.yuriy.openradio.utils.ApiKeyLoader} is a helper class to provide
 * Dirble API key
 */
public final class ApiKeyLoader {

    /**
     * Cashed value pf the Api key.
     */
    private static String sCashedKey = "";

    /**
     * Load API key from the resources.
     *
     * @param context Context of the application.
     * @return API key.
     */
    public static String getApiKey(final Context context) {
        if (!TextUtils.isEmpty(sCashedKey)) {
            return sCashedKey;
        }
        final int resourceId;
        if (AppLogger.isDebug(context)) {
            AppLogger.i("API key is debug");
            resourceId = R.raw.api_key_debug;
        } else {
            AppLogger.i("API key is release");
            resourceId = R.raw.api_key;
        }
        try (InputStream stream = context.getResources().openRawResource(resourceId)) {
            final Writer writer = new StringWriter();
            final char[] buffer = new char[1024];
            final Reader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            int length;
            while ((length = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, length);
            }
            sCashedKey = writer.toString();
        } catch (final IOException e) {
            FabricUtils.logException(e);
        }
        return sCashedKey;
    }
}
