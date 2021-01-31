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

package wseemann.media.jplaylistparser.parser

import wseemann.media.jplaylistparser.exception.JPlaylistParserException
import wseemann.media.jplaylistparser.playlist.Playlist
import wseemann.media.jplaylistparser.playlist.PlaylistEntry
import java.io.IOException

abstract class AbstractParser(private val mTimeout: Int) : Parser {

    protected fun parseEntry(playlistEntry: PlaylistEntry, playlist: Playlist) {
        val parser = AutoDetectParser(mTimeout)
        try {
            if (mLastEntry != null && mLastEntry == playlistEntry) {
                throw RuntimeException("Cycle detected for $playlistEntry")
            }
            mLastEntry = playlistEntry
            parser.parse(playlistEntry[PlaylistEntry.URI], playlist)
        } catch (e: IOException) {
            playlist.add(playlistEntry)
        } catch (e: JPlaylistParserException) {
            playlist.add(playlistEntry)
        }
    }

    companion object {
        private var mLastEntry: PlaylistEntry? = null
    }
}
