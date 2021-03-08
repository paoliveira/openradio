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

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import com.yuriy.openradio.shared.model.storage.ImagesStore
import com.yuriy.openradio.shared.service.LocationService
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
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
    private var mId: String = ""

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
    private var imageUrl = ""
    private var imageUri = Uri.EMPTY
    var urlResolved = ""
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
    private constructor(id: String) {
        setId(id)
        mMediaStream = MediaStream.makeDefaultInstance()
    }

    /**
     * Copy constructor.
     *
     * @param radioStation Object to be copied.
     */
    private constructor(radioStation: RadioStation) {
        setId(radioStation.mId)
        mCountry = radioStation.mCountry
        countryCode = radioStation.countryCode
        genre = radioStation.genre
        imageUrl = radioStation.imageUrl
        isLocal = radioStation.isLocal
        mMediaStream = MediaStream.makeCopyInstance(radioStation.mMediaStream)
        name = radioStation.name
        sortId = radioStation.sortId
        status = radioStation.status
        homePage = radioStation.homePage
        urlResolved = radioStation.urlResolved
        lastCheckOk = radioStation.lastCheckOk
        lastCheckOkTime = radioStation.lastCheckOkTime
    }

    fun getImgUrl(): String {
        AppLogger.d("getImgUrl:$imageUrl")
        return imageUrl
    }

    fun getImgUri(): Uri {
        AppLogger.d("getImgUri:$imageUri")
        return imageUri
    }

    fun setImgUrl(context: Context, url: String?) {
        AppLogger.d("setImgUrl:$url")
        if (url.isNullOrEmpty()) {
            imageUri = Uri.parse("android.resource://com.yuriy.openradio/drawable/ic_radio_station_empty")
            return
        }
        if (!AppUtils.isWebUrl(url)) {
            imageUri = Uri.parse(url)
            return
        }
        if (imageUri != Uri.EMPTY) {
            return
        }
        val uri = ImagesStore.getUri(id)
        context.contentResolver.registerContentObserver(
                uri,
                true,
                object : ContentObserver(Handler(Looper.getMainLooper())) {

                    override fun onChange(selfChange: Boolean, uri: Uri?) {
                        super.onChange(selfChange, uri)
                        imageUri = ImagesStore.extractInsertedUri(uri)
                        context.contentResolver.unregisterContentObserver(this)
                    }
                }
        )

        context.contentResolver.insert(
                uri, ImagesStore.getContentValues(id, url)
        )
    }

    val id: String
        get() = mId

    fun isMediaStreamEmpty(): Boolean {
        return mMediaStream.isEmpty
    }

    fun setIsLocal(value: Boolean) {
        isLocal = value
    }

    private fun setId(value: String) {
        mId = value
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
                ", urlResolved='" + urlResolved + '\'' +
                ", webSite='" + homePage + '\'' +
                ", country='" + mCountry + '\'' +
                ", genre='" + genre + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
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
        fun makeDefaultInstance(id: String): RadioStation {
            return RadioStation(id)
        }

        /**
         * Factory method to create copy-instance of the [RadioStation].
         *
         * @param radioStation Object to be copied.
         * @return Copied instance of [RadioStation].
         */
        @JvmStatic
        fun makeCopyInstance(radioStation: RadioStation): RadioStation {
            return RadioStation(radioStation)
        }
    }
}
