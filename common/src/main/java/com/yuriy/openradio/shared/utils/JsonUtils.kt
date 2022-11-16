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

package com.yuriy.openradio.shared.utils

import org.json.JSONException
import org.json.JSONObject

object JsonUtils {

    @Throws(JSONException::class)
    fun getShortArray(jsonObject: JSONObject, key: String): ShortArray {
        if (jsonObject.has(key)) {
            val obj = jsonObject.getString(key)
            val array = obj.split(",".toRegex()).toTypedArray()
            if (array.size <= 1) {
                return shortArrayOf()
            }
            val list = ShortArray(array.size)
            for (i in list.indices) {
                list[i] = array[i].toShort()
            }
            return list
        }
        return shortArrayOf()
    }

    @Throws(JSONException::class)
    fun getIntArray(jsonObject: JSONObject, key: String): IntArray {
        if (jsonObject.has(key)) {
            val obj = jsonObject.getString(key)
            val array = obj.split(",".toRegex()).toTypedArray()
            if (array.size <= 1) {
                return intArrayOf()
            }
            val list = IntArray(array.size)
            for (i in list.indices) {
                list[i] = array[i].toInt()
            }
            return list
        }
        return intArrayOf()
    }

    @Throws(JSONException::class)
    fun <T> getListValue(jsonObject: JSONObject, key: String): List<T> {
        if (jsonObject.has(key)) {
            val obj = jsonObject.getString(key)
            val list = ArrayList<T>()
            val array = obj.split(",".toRegex()).toTypedArray()
            for (o in array) {
                list.add(o as T)
            }
            return list
        }
        return ArrayList()
    }

    fun getStringValue(jsonObject: JSONObject, key: String): String {
        return getStringValue(jsonObject, key, AppUtils.EMPTY_STRING)
    }

    fun getStringValue(jsonObject: JSONObject, key: String, defaultValue: String): String {
        return if (jsonObject.has(key)) {
            jsonObject.getString(key)
        } else defaultValue
    }

    fun getIntValue(jsonObject: JSONObject, key: String): Int {
        return getIntValue(jsonObject, key, 0)
    }

    fun getIntValue(jsonObject: JSONObject, key: String, defaultValue: Int): Int {
        return if (jsonObject.has(key)) {
            jsonObject.getInt(key)
        } else defaultValue
    }

    fun getBooleanValue(jsonObject: JSONObject, key: String): Boolean {
        return jsonObject.has(key) && jsonObject.getBoolean(key)
    }
}
