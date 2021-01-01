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

package com.yuriy.openradio.shared.utils

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yuriy.openradio.shared.utils.NetUtils.checkResource
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NetUtilsTest {

    @Test
    fun testCheckImage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val url = "https://djradio.gr/wp-content/uploads/2019/05/djradio-logo-smaller-white-1.png"
        val result = checkResource(context, url)
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))
    }

    @Test
    fun testUri() {
        val uriStr = "https://urbanhitrapfr.ice.infomaniak.ch/urbanhitrapfr-128.mp3"
        val uri = Uri.parse(uriStr)
        MatcherAssert.assertThat(uri.toString(), CoreMatchers.`is`(uriStr))
    }
}
