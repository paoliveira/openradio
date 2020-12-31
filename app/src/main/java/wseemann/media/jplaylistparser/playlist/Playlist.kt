/*
 * Copyright 2014 William Seemann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wseemann.media.jplaylistparser.playlist

import java.util.*

class Playlist {

    private val mPlaylistEntries: MutableList<PlaylistEntry>

    fun add(playlistEntry: PlaylistEntry) {
        mPlaylistEntries.add(playlistEntry)
    }

    /**
     * @return the PlaylistEntries
     */
    val playlistEntries: List<PlaylistEntry>
        get() = mPlaylistEntries

    /**
     * Constructs a new, empty playlist.
     */
    init {
        mPlaylistEntries = ArrayList()
    }
}