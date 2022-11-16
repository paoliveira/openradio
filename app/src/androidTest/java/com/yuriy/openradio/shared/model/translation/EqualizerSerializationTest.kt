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

package com.yuriy.openradio.shared.model.translation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yuriy.openradio.shared.vo.EqualizerState
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EqualizerSerializationTest {

    @Test
    fun testSerializationOfEqualizer() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val list = ArrayList<String>()
        list.add("One")
        list.add("Two")
        list.add("Three")
        val state = EqualizerState()
        state.presets = list
        val bandRangeLow: Short = -1500
        val bandRangeHigh: Short = 1500
        val bandRange = ShortArray(2)
        bandRange[0] = bandRangeLow
        bandRange[1] = bandRangeHigh
        state.bandLevelRange = bandRange
        val bandLevel1: Short = 100
        val bandLevel2: Short = 200
        val bandLevel3: Short = 300
        val bandLevel4: Short = 400
        val bandLevel5: Short = 500
        val bandLevels = ShortArray(5)
        bandLevels[0] = bandLevel1
        bandLevels[1] = bandLevel2
        bandLevels[2] = bandLevel3
        bandLevels[3] = bandLevel4
        bandLevels[4] = bandLevel5
        state.bandLevels = bandLevels
        val serializer = EqualizerJsonStateSerializer()
        val value = serializer.serialize(state)
        val deserializer = EqualizerStateJsonDeserializer()
        val newState = deserializer.deserialize(context, value)
        val newList = newState.presets
        MatcherAssert.assertThat(newList.size, CoreMatchers.`is`(3))
        MatcherAssert.assertThat(newList[0], CoreMatchers.`is`("One"))
        MatcherAssert.assertThat(newList[1], CoreMatchers.`is`("Two"))
        MatcherAssert.assertThat(newList[2], CoreMatchers.`is`("Three"))
        val newBandRange = newState.bandLevelRange
        MatcherAssert.assertThat(newBandRange.size, CoreMatchers.`is`(2))
        MatcherAssert.assertThat(newBandRange[0], CoreMatchers.`is`(bandRangeLow))
        MatcherAssert.assertThat(newBandRange[1], CoreMatchers.`is`(bandRangeHigh))
        val newBandLevels = newState.bandLevels
        MatcherAssert.assertThat(newBandLevels.size, CoreMatchers.`is`(5))
        MatcherAssert.assertThat(newBandLevels[0], CoreMatchers.`is`(bandLevel1))
        MatcherAssert.assertThat(newBandLevels[1], CoreMatchers.`is`(bandLevel2))
        MatcherAssert.assertThat(newBandLevels[2], CoreMatchers.`is`(bandLevel3))
        MatcherAssert.assertThat(newBandLevels[3], CoreMatchers.`is`(bandLevel4))
        MatcherAssert.assertThat(newBandLevels[4], CoreMatchers.`is`(bandLevel5))
    }
}
