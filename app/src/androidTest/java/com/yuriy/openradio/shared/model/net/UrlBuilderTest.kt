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

package com.yuriy.openradio.shared.model.net

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yuriy.openradio.shared.model.net.UrlBuilder.preProcessIconUri
import com.yuriy.openradio.shared.model.net.UrlBuilder.preProcessIconUrl
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 29/12/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
@RunWith(AndroidJUnit4::class)
class UrlBuilderTest {

    @Test
    fun preProcessIconUrlNullInput() {
        val newUrl = preProcessIconUrl(null)
        MatcherAssert.assertThat(newUrl, Matchers.isEmptyString())
    }

    @Test
    fun preProcessIconUrlEmptyInput() {
        val newUrl = preProcessIconUrl("")
        MatcherAssert.assertThat(newUrl, Matchers.`is`(""))
    }

    @Test
    fun preProcessIconUriNullInput() {
        val newUri = preProcessIconUri(null)
        MatcherAssert.assertThat(newUri, Matchers.nullValue())
    }
}
