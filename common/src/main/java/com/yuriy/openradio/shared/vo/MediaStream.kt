/*
 * Copyright 2017 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import com.yuriy.openradio.shared.utils.AppUtils

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 4/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *
 * [MediaStream] is a value object that holds data associated with Radio Station's stream.
 */
class MediaStream private constructor() {

    private val mVariants = ArrayList<Variant>()

    fun setVariant(bitrate: Int, url: String) {
        mVariants.add(Variant(bitrate, url))
    }

    val variantsNumber: Int
        get() = mVariants.size

    val isEmpty: Boolean
        get() = mVariants.isEmpty()

    fun clear() {
        mVariants.clear()
    }

    fun getVariant(position: Int): Variant {
        if (position < 0) {
            return Variant(0, AppUtils.EMPTY_STRING)
        }
        return if (position >= variantsNumber) {
            Variant(0, AppUtils.EMPTY_STRING)
        } else mVariants[position].copy()
    }

    override fun toString(): String {
        return "MediaStream{variants=$mVariants}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as MediaStream
        return mVariants == that.mVariants
    }

    override fun hashCode(): Int {
        return mVariants.hashCode()
    }

    /**
     * Copy constructor.
     *
     * @param mediaStream Object to be copied.
     */
    private constructor(mediaStream: MediaStream) : this() {
        for (variant in mediaStream.mVariants) {
            mVariants.add(Variant(variant.bitrate, variant.url))
        }
    }

    class Variant constructor(val bitrate: Int, val url: String) {

        override fun toString(): String {
            return "Variant{url='$url', bitrate=$bitrate}"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val variant = other as Variant
            return if (bitrate != variant.bitrate) false else url == variant.url
        }

        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + bitrate
            return result
        }

        fun copy(): Variant {
            return Variant(bitrate, url)
        }
    }

    companion object {

        const val BIT_RATE_DEFAULT = 320
        /**
         * Factory method to create instance of the [MediaStream].
         *
         * @return Instance of the [MediaStream].
         */
        fun makeDefaultInstance(): MediaStream {
            return MediaStream()
        }

        /**
         * Factory method to create copy-instance of the [MediaStream].
         *
         * @param mediaStream Object to be copied.
         * @return Copied instance of [MediaStream].
         */
        fun makeCopyInstance(mediaStream: MediaStream): MediaStream {
            return MediaStream(mediaStream)
        }
    }
}
