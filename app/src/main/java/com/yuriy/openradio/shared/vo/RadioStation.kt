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
package com.yuriy.openradio.shared.vo

import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import com.yuriy.openradio.shared.model.storage.LocalRadioStationsStorage
import com.yuriy.openradio.shared.service.LocationService
import com.yuriy.openradio.shared.utils.AnalyticsUtils
import com.yuriy.openradio.shared.vo.MediaStream.Companion.makeCopyInstance
import com.yuriy.openradio.shared.vo.MediaStream.Companion.makeDefaultInstance
import java.io.Serializable

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/16/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * [RadioStation] is a value object that holds information
 * about concrete Radio Station.
 */
class RadioStation : Serializable {
    private var mId: String? = null

    // TODO: Convert to enum
    var status = 0
    var name = ""
    var homePage = ""
    var lastCheckOkTime = ""
    var lastCheckOk = 0

    // TODO: Convert to enum
    private var mCountry = ""

    // TODO: Convert to enum
    var countryCode = ""
    var genre = ""
    var imageUrl = ""
    var thumbUrl = ""
    private val mMediaStream: MediaStream

    /**
     * Flag indicate that Radio Station has been added locally to the phone storage.
     */
    var isLocal = false
    var sortId = MediaSessionCompat.QueueItem.UNKNOWN_ID

    /**
     * Private constructor.
     * Disallow instantiation of this helper class.
     */
    private constructor(context: Context, id: String) {
        setId(context, id)
        mMediaStream = makeDefaultInstance()
    }

    /**
     * Copy constructor.
     *
     * @param radioStation Object to be copied.
     */
    private constructor(context: Context, radioStation: RadioStation) {
        mCountry = radioStation.mCountry
        countryCode = radioStation.countryCode
        genre = radioStation.genre
        setId(context, radioStation.mId)
        imageUrl = radioStation.imageUrl
        isLocal = radioStation.isLocal
        mMediaStream = makeCopyInstance(radioStation.mMediaStream)
        name = radioStation.name
        sortId = radioStation.sortId
        status = radioStation.status
        thumbUrl = radioStation.thumbUrl
        homePage = radioStation.homePage
    }

    val id: String
        get() = mId!!

    fun isMediaStreamEmpty(): Boolean {
        return mMediaStream.isEmpty
    }

    fun setIsLocal(value: Boolean) {
        isLocal = value
    }

    private fun setId(context: Context, value: String?) {
        mId = if (value.isNullOrEmpty()) {
            AnalyticsUtils.logException(IllegalArgumentException("Radio Station ID is invalid"))
            LocalRadioStationsStorage.getId(context)
        } else {
            value
        }
    }

    var country: String
        get() = mCountry
        set(value) {
            mCountry = if (LocationService.GB_WRONG == value) {
                LocationService.GB_CORRECT
            } else {
                value
            }
        }

    //TODO: Dangerous! MediaItem may be reference from the same RadioStation object!
    var mediaStream: MediaStream
        get() = mMediaStream
        set(value) {
            //TODO: Dangerous! MediaItem may be reference from the same RadioStation object!
            mMediaStream.clear()
            val size = value.variantsNumber
            for (i in 0 until size) {
                mMediaStream.setVariant(value.getVariant(i)!!.bitrate, value.getVariant(i)!!.url)
            }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val station = other as RadioStation
        return if (mId != station.mId) false else mMediaStream == station.mMediaStream
    }

    override fun hashCode(): Int {
        var result = mId.hashCode()
        result = 31 * result + mMediaStream.hashCode()
        return result
    }

    override fun toString(): String {
        return "RS " + hashCode() + " {" +
                "id=" + mId +
                ", status=" + status +
                ", lastCheckOk=" + lastCheckOk +
                ", lastCheckOkTime=" + lastCheckOkTime +
                ", name='" + name + '\'' +
                ", stream='" + mMediaStream + '\'' +
                ", webSite='" + homePage + '\'' +
                ", country='" + mCountry + '\'' +
                ", genre='" + genre + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", thumbUrl='" + thumbUrl + '\'' +
                ", isLocal=" + isLocal + '\'' +
                ", sortId=" + sortId +
                '}'
    }

    companion object {
        /**
         * Factory method to create instance of the [RadioStation].
         *
         * @return Instance of the [RadioStation].
         */
        @JvmStatic
        fun makeDefaultInstance(context: Context, id: String): RadioStation {
            return RadioStation(context, id)
        }

        /**
         * Factory method to create copy-instance of the [RadioStation].
         *
         * @param radioStation Object to be copied.
         * @return Copied instance of [RadioStation].
         */
        @JvmStatic
        fun makeCopyInstance(context: Context, radioStation: RadioStation): RadioStation {
            return RadioStation(context, radioStation)
        }
    }
}