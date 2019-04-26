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

import com.yuriy.openradio.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Random;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link ApiKeyLoader} is a helper class to provide Dirble API key.
 */
public final class ApiKeyLoader {

    private static final int[] IDS = new int[]{
            R.raw.api_key_1,
            R.raw.api_key_2,
            R.raw.api_key_3,
            R.raw.api_key_4
    };
    private static final String[] KEYS = new String[IDS.length];
    private static final Random RND = new Random();

    /**
     * Constructor.
     */
    private ApiKeyLoader() {
        super();
    }

    /**
     * Gets API key.
     *
     * @return API key.
     */
    public static String getApiKey() {
        final int idx = RND.nextInt(KEYS.length);
        AppLogger.i("Api key idx:" + idx);
        return KEYS[idx];
    }

    /**
     * Initialize API key loader.
     *
     * @param context Context of a callee.
     */
    public static void init(final Context context) {
        for (int i = 0; i < IDS.length; i++) {
            KEYS[i] = getApiKey(context, IDS[i]);
        }
    }

    /**
     * Load API key from the resource.
     *
     * @param context    Context of a callee.
     * @param resourceId Resource ID of the key.
     *
     * @return API key.
     */
    private static String getApiKey(final Context context, final int resourceId) {
        String key = "";
        try (final InputStream stream = context.getResources().openRawResource(resourceId)) {
            final Writer writer = new StringWriter();
            final char[] buffer = new char[1024];
            final Reader reader = new BufferedReader(new InputStreamReader(stream, AppUtils.UTF8));
            int length;
            while ((length = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, length);
            }
            key = writer.toString();
        } catch (final IOException e) {
            FabricUtils.logException(e);
        }
        return key;
    }
}
