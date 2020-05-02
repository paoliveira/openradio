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

package com.yuriy.openradio.shared.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class JsonUtils {

    private JsonUtils() {
        super();
    }

    public static short[] getShortArray(final JSONObject jsonObject, final String key) throws JSONException {
        if (jsonObject == null) {
            return new short[]{};
        }
        if (jsonObject.has(key)) {
            final String obj = jsonObject.getString(key);
            final String[] array = obj.split(",");
            if (array.length <= 1) {
                return new short[]{};
            }
            final short[] list = new short[array.length];
            for (int i = 0; i < list.length; ++i) {
                list[i] = Short.parseShort(array[i]);
            }
            return list;
        }
        return new short[]{};
    }

    public static int[] getIntArray(final JSONObject jsonObject, final String key) throws JSONException {
        if (jsonObject == null) {
            return new int[]{};
        }
        if (jsonObject.has(key)) {
            final String obj = jsonObject.getString(key);
            final String[] array = obj.split(",");
            if (array.length <= 1) {
                return new int[]{};
            }
            final int[] list = new int[array.length];
            for (int i = 0; i < list.length; ++i) {
                list[i] = Integer.parseInt(array[i]);
            }
            return list;
        }
        return new int[]{};
    }

    public static <T> List<T> getListValue(final JSONObject jsonObject, final String key) throws JSONException {
        if (jsonObject == null) {
            return new ArrayList<>();
        }
        if (jsonObject.has(key)) {
            final String obj = jsonObject.getString(key);
            final List<T> list = new ArrayList<>();
            final Object[] array = obj.split(",");
            for (final Object o : array) {
                list.add((T) o);
            }
            return list;
        }
        return new ArrayList<>();
    }

    public static String getStringValue(final JSONObject jsonObject, final String key) throws JSONException {
        return getStringValue(jsonObject, key, "");
    }

    public static String getStringValue(final JSONObject jsonObject, final String key, final String defaultValue)
            throws JSONException {
        if (jsonObject == null) {
            return defaultValue;
        }
        if (jsonObject.has(key)) {
            return jsonObject.getString(key);
        }
        return defaultValue;
    }

    public static int getIntValue(final JSONObject jsonObject, final String key) throws JSONException {
        return getIntValue(jsonObject, key, 0);
    }

    public static int getIntValue(final JSONObject jsonObject,
                            final String key, final int defaultValue) throws JSONException {
        if (jsonObject == null) {
            return defaultValue;
        }
        if (jsonObject.has(key)) {
            return jsonObject.getInt(key);
        }
        return defaultValue;
    }

    public static boolean getBooleanValue(final JSONObject jsonObject, final String key) throws JSONException {
        return jsonObject != null && jsonObject.has(key) && jsonObject.getBoolean(key);
    }
}
