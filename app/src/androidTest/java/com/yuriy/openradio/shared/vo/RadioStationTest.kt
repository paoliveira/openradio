/*
 * Copyright 2017-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.vo

class RadioStationTest {

    companion object {

        /**
         *
         * @param id
         * @param testIdx Should be unique index for a set of generated radio stations (if client need more then one
         * radio station in a single test).
         */
        fun makeTestInstance(id: String, testIdx: Int): RadioStation {
            val radioStation = RadioStation.makeDefaultInstance(id)
            radioStation.setVariant(MediaStream.BITRATE_DEFAULT, "http://www.stream-$testIdx.net")
            radioStation.imageUrl = "http://www.image-$testIdx.net"
            radioStation.countryCode = Country.COUNTRY_CODE_DEFAULT
            radioStation.genre = "Jazz"
            radioStation.name = "Radio Station - $testIdx"
            radioStation.homePage = "http://www.homepage-$testIdx.net"
            radioStation.sortId = testIdx
            return radioStation
        }
    }
}