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

package com.yuriy.openradio.shared.model.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yuriy.openradio.shared.model.storage.RadioStationsStorage.Companion.merge
import com.yuriy.openradio.shared.vo.MediaStream.Companion.makeDefaultInstance
import com.yuriy.openradio.shared.vo.RadioStation
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 19/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
@RunWith(AndroidJUnit4::class)
class RadioStationsStorageTest {

    @Test
    fun merge() {
        val listA: MutableList<RadioStation> = ArrayList()
        val listB: MutableList<RadioStation> = ArrayList()
        val radioStations = arrayOfNulls<RadioStation>(10)
        for (i in radioStations.indices) {
            val radioStation = RadioStation.makeDefaultInstance(i.toString())
            val stream = makeDefaultInstance()
            stream.setVariant(125, "Url" + 1)
            radioStation.mediaStream = stream
            radioStations[i] = radioStation
        }
        listA.add(radioStations[0]!!)
        listA.add(radioStations[1]!!)
        listA.add(radioStations[2]!!)
        listA.add(radioStations[3]!!)
        listA.add(radioStations[4]!!)
        listB.add(radioStations[3]!!)
        listB.add(radioStations[4]!!)
        listB.add(radioStations[5]!!)
        merge(listA, listB)
        MatcherAssert.assertThat(listA.size, CoreMatchers.`is`(6))
        for (i in 0..5) {
            MatcherAssert.assertThat(listA[i].id, CoreMatchers.`is`(i.toString()))
        }
    }
}
