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

package com.yuriy.openradio.shared.utils

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.Player
import com.yuriy.openradio.shared.vo.MediaItemListEnded
import java.util.*

object PlayerUtils {

    /**
     * @param list
     * @return
     */
    fun isEndOfList(list: List<MediaBrowserCompat.MediaItem?>?): Boolean {
        return (list == null
            || list.size == 1
            && (list[0] == null || list[0] is MediaItemListEnded))
    }

    /**
     * @return
     */
    fun createListEndedResult(): List<MediaBrowserCompat.MediaItem> {
        return ArrayList<MediaBrowserCompat.MediaItem>(listOf(MediaItemListEnded()))
    }

    fun playbackStateToString(state: PlaybackStateCompat?): String {
        return if (state == null) {
            "UNDEFINED"
        } else playbackStateToString(state.state)
    }

    fun playbackStateToString(state: Int): String {
        return when (state) {
            PlaybackStateCompat.STATE_STOPPED -> "STOPPED"
            PlaybackStateCompat.STATE_PAUSED -> "PAUSED"
            PlaybackStateCompat.STATE_PLAYING -> "PLAYING"
            PlaybackStateCompat.STATE_FAST_FORWARDING -> "FAST_FORWARDING"
            PlaybackStateCompat.STATE_REWINDING -> "REWINDING"
            PlaybackStateCompat.STATE_BUFFERING -> "BUFFERING"
            PlaybackStateCompat.STATE_ERROR -> "ERROR"
            PlaybackStateCompat.STATE_CONNECTING -> "CONNECTING"
            PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS -> "SKIPPING_TO_PREVIOUS"
            PlaybackStateCompat.STATE_SKIPPING_TO_NEXT -> "SKIPPING_TO_NEXT"
            PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM -> "SKIPPING_TO_QUEUE_ITEM"
            PlaybackStateCompat.STATE_NONE -> "NONE"
            else -> "UNDEFINED{$state}"
        }
    }

    fun playerStateToString(state: Int): String {
        return when (state) {
            Player.STATE_IDLE -> "STATE_IDLE"
            Player.STATE_BUFFERING -> "STATE_BUFFERING"
            Player.STATE_READY -> "STATE_READY"
            Player.STATE_ENDED -> "STATE_ENDED"
            else -> "UNDEFINED{$state}"
        }
    }
}
