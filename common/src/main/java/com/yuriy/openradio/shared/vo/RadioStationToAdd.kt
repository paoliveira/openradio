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
package com.yuriy.openradio.shared.vo

import java.io.Serializable

/**
 * This class contains data to validate prior to add to data set (either local or remote server).
 *
 * @param name          Name of the Radio Station.
 * @param url           Url of the Stream associated with Radio Station.
 * @param imageLocalUrl Local Url of the Image associated with Radio Station.
 * @param imageWebUrl   Web Url of the Image associated with Radio Station.
 * @param homePage      Web Url of Radio Station's home page.
 * @param genre         Genre of the Radio Station.
 * @param country       Country of the Radio Station.
 * @param isAddToFav      Whether or not add radio station to favorites.
 * @param isAddToServer   Whether or not add radio station to the server.
 */
class RadioStationToAdd(val name: String, val url: String, val imageLocalUrl: String,
                        val imageWebUrl: String, val homePage: String, val genre: String,
                        val country: String, val isAddToFav: Boolean, val isAddToServer: Boolean) : Serializable {
    override fun toString(): String {
        return "RadioStationToAdd{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", imageLocalUrl='" + imageLocalUrl + '\'' +
                ", imageWebUrl='" + imageWebUrl + '\'' +
                ", homePage='" + homePage + '\'' +
                ", genre='" + genre + '\'' +
                ", country='" + country + '\'' +
                ", addToFav=" + isAddToFav +
                ", addToServer=" + isAddToServer +
                '}'
    }
}
