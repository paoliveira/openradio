/*
 * Copyright 2020-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.yuriy.openradio.R
import java.net.HttpURLConnection

class PlaybackStateError @JvmOverloads constructor(val msg: String? = null,
                                                   val code: Code = Code.NO_ERROR,
                                                   val exception: Throwable? = null) {

    enum class Code {
        NO_ERROR, GENERAL, UNRECOGNIZED_URL, PLAYBACK_ERROR
    }

    override fun toString(): String {
        return "PlaybackStateError{code='$code', msg=$msg, exception:$exception}"
    }

    companion object {

        fun isPlaybackStateError(context: Context, msg: String): Boolean {
            if (msg.isEmpty()) {
                return false
            }
            if (msg == context.getString(R.string.media_stream_error)
                    || msg == context.getString(R.string.media_stream_http_403)
                    || msg == context.getString(R.string.media_stream_http_404)) {
                return true
            }
            return false
        }

        fun toDisplayString(context: Context, error: PlaybackStateError): String {
            var msg = context.getString(R.string.media_stream_error)
            if (error.exception != null) {
                val cause = error.exception.cause
                if (cause is HttpDataSource.InvalidResponseCodeException) {
                    when (cause.responseCode) {
                        HttpURLConnection.HTTP_FORBIDDEN -> {
                            msg = context.getString(R.string.media_stream_http_403)
                        }
                        HttpURLConnection.HTTP_NOT_FOUND -> {
                            msg = context.getString(R.string.media_stream_http_404)
                        }
                    }
                }
            }
            return msg
        }
    }
}
