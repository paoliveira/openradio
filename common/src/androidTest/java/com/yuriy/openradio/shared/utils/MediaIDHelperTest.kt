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

import com.yuriy.openradio.shared.utils.MediaIdHelper.getCountryCode
import com.yuriy.openradio.shared.utils.MediaIdHelper.getId
import junit.framework.TestCase
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/13/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class MediaIDHelperTest : TestCase() {

    fun testGetId() {
        val id = MediaIdHelper.MEDIA_ID_CHILD_CATEGORIES
        val startsWith = MediaIdHelper.MEDIA_ID_CHILD_CATEGORIES + "11"
        MatcherAssert.assertThat(getId(startsWith), Matchers.`is`(id))
    }

    fun testGetValidCountryCode() {
        val id = MediaIdHelper.MEDIA_ID_COUNTRIES_LIST + "BR"
        MatcherAssert.assertThat(getCountryCode(id), Matchers.`is`("BR"))
    }

    fun testGetInvalidCountryCodeFromValidValueSameSubName() {
        val id = MediaIdHelper.MEDIA_ID_COUNTRIES_LIST
        MatcherAssert.assertThat(getCountryCode(id), Matchers.nullValue())
    }

    fun testGetInvalidCountryCodeFromValidValueDifferentSubName() {
        val id = MediaIdHelper.MEDIA_ID_SEARCH_FROM_APP
        MatcherAssert.assertThat(getCountryCode(id), Matchers.nullValue())
    }

    fun testGetInvalidCountryCodeFromNullValue() {
        val id: String? = null
        MatcherAssert.assertThat(getCountryCode(id), Matchers.nullValue())
    }

    fun testStartsWithAndEquals() {
        val name = MediaIdHelper.MEDIA_ID_COUNTRIES_LIST
        val id = MediaIdHelper.MEDIA_ID_COUNTRIES_LIST
        MatcherAssert.assertThat(name.startsWith(id), Matchers.`is`(true))
        MatcherAssert.assertThat(name == id, Matchers.`is`(true))
    }
}
