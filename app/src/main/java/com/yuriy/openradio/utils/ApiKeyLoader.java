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
import com.yuriy.openradio.business.storage.ApiKeyLoaderStorage;

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
     * Cashed value of the API key.
     */
    private String mCashedKey;
    private int mIndex;
    private int mMovesNum;
    private Context mContext;

    private static final int[] IDS = new int[]{R.raw.api_key_1, R.raw.api_key_2};

    public ApiKeyLoader(final Context context) {
        super();
        mCashedKey = "";
        mMovesNum = 0;
        mContext = context;
        mIndex = ApiKeyLoaderStorage.getLastIndex(mContext);
    }

    public boolean hasNext() {
        return mMovesNum < IDS.length - 1;
    }

    public boolean wasMovedToNext() {
        return mMovesNum > 0;
    }

    public void moveToNext() {
        mCashedKey = "";
        mIndex++;
        mMovesNum++;
        if (mIndex > IDS.length - 1) {
            mIndex = 0;
        }
        ApiKeyLoaderStorage.setLastIndex(mIndex, mContext);
    }

    /**
     * Load API key from the resources.
     *
     * @return API key.
     */
    public String getApiKey() {
        if (!TextUtils.isEmpty(mCashedKey)) {
            return mCashedKey;
        }
        final int resourceId = IDS[mIndex];
        try (final InputStream stream = mContext.getResources().openRawResource(resourceId)) {
            final Writer writer = new StringWriter();
            final char[] buffer = new char[1024];
            final Reader reader = new BufferedReader(new InputStreamReader(stream, AppUtils.UTF8));
            int length;
            while ((length = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, length);
            }
            mCashedKey = writer.toString();
        } catch (final IOException e) {
            FabricUtils.logException(e);
        }
        return mCashedKey;
    }
}
