/*
 * Copyright 2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.model.translation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yuriy.openradio.shared.vo.RadioStationTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RadioStationJsonSerializerTest {

    @Test
    fun serialization() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val serializer: RadioStationSerializer = RadioStationJsonSerializer()
        val radioStation = RadioStationTest.makeTestInstance(context, "id-1", 0)
        val js = serializer.serialize(radioStation)

        MatcherAssert.assertThat(js, CoreMatchers.notNullValue())
    }
}