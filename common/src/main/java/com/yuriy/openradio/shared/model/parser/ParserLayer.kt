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

package com.yuriy.openradio.shared.model.parser

import com.yuriy.openradio.shared.model.translation.MediaIdBuilder
import com.yuriy.openradio.shared.vo.Category
import com.yuriy.openradio.shared.vo.RadioStation

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 * [ParserLayer] is an interface which provides common methods
 * for the data parsing. Different implementation can parse raw string data into JSON,
 * XML or any other format.
 */
interface ParserLayer {

    fun getRadioStation(data: String, mediaIdBuilder: MediaIdBuilder): RadioStation

    fun getRadioStations(data: String, mediaIdBuilder: MediaIdBuilder): List<RadioStation>

    fun getCategories(data: String): List<Category>
}
