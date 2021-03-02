/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.model.storage.cache.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/04/19
 * E-Mail: chernyshov.yuriy@gmail.com
 */
@RunWith(AndroidJUnit4::class)
class PersistentApiCacheTest {

    private var mApiCache: ApiCache? = null

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        mApiCache = PersistentApiCache(context, DB_NAME)
        (mApiCache as PersistentApiCache).clear()
    }

    @After
    fun tearDown() {
        mApiCache!!.clear()
    }

    @Test
    @Throws(Exception::class)
    fun putAndGetData() {
        val in1 = getJsonArray(1)
        val in2 = getJsonArray(2)
        val key1 = "1234567890"
        val key2 = "0987654321"
        mApiCache!!.put(key1, in1)
        mApiCache!!.put(key2, in2)
        val out1 = mApiCache!![key1]
        val out2 = mApiCache!![key2]
        Assert.assertThat(out1.toString(), CoreMatchers.`is`(in1.toString()))
        Assert.assertThat(out2.toString(), CoreMatchers.`is`(in2.toString()))
    }

    @Test
    @Throws(Exception::class)
    fun deleteRaw() {
        val in1 = getJsonArray(1)
        val in2 = getJsonArray(2)
        val key1 = "1234567890"
        val key2 = "0987654321"
        mApiCache!!.put(key1, in1)
        mApiCache!!.put(key2, in2)
        mApiCache!!.remove(key1)
    }

    @Test
    @Throws(Exception::class)
    fun newRecordOverridePrevious() {
        val in1 = getJsonArray(1)
        val in2 = getJsonArray(2)
        val in3 = getJsonArray(3)
        val key = "1234567890"
        mApiCache!!.put(key, in1)
        mApiCache!!.remove(key)
        mApiCache!!.put(key, in2)
        mApiCache!!.remove(key)
        mApiCache!!.put(key, in3)
        Assert.assertThat((mApiCache as PersistentApiCache?)!!.getCount(key), CoreMatchers.`is`(1))
        Assert.assertThat(mApiCache!![key].toString() == in3.toString(), CoreMatchers.`is`(true))
    }

    @Throws(Exception::class)
    private fun getJsonArray(factor: Int): JSONArray {
        val student1 = JSONObject()
        student1.put("id", "3$factor")
        student1.put("name", "NAME OF STUDENT")
        student1.put("year", "3rd")
        student1.put("curriculum", "Arts$factor")
        student1.put("birthday", "5/5/1993")
        val student2 = JSONObject()
        student2.put("id", "2$factor")
        student2.put("name", "NAME OF STUDENT2")
        student2.put("year", "4rd")
        student2.put("curriculum", "Science$factor")
        student2.put("birthday", "5/5/1993")
        val jsonArray = JSONArray()
        jsonArray.put(student1)
        jsonArray.put(student2)
        return jsonArray
    }

    companion object {
        private const val DB_NAME = "ApiTestDb"
    }
}
