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

package wseemann.media.jplaylistparser.parser.m3u

import com.yuriy.openradio.shared.utils.AppUtils
import wseemann.media.jplaylistparser.exception.JPlaylistParserException
import wseemann.media.jplaylistparser.mime.MediaType
import wseemann.media.jplaylistparser.mime.MediaType.Companion.audio
import wseemann.media.jplaylistparser.parser.AbstractParser
import wseemann.media.jplaylistparser.playlist.Playlist
import wseemann.media.jplaylistparser.playlist.PlaylistEntry
import java.io.IOException
import java.io.InputStream

class M3UPlaylistParser(timeout: Int) : AbstractParser(timeout) {

    private var mNumberOfFiles = 0
    private var processingEntry = false

    override val supportedTypes: Set<MediaType?>
        get() = setOf(audio("x-mpegurl"))

    @Throws(IOException::class)
    override fun parse(uri: String, stream: InputStream, playlist: Playlist) {
        parsePlaylist(stream, playlist)
    }

    /**
     * Retrieves the files listed in a .m3u file
     * @throws IOException
     */
    @Throws(IOException::class, JPlaylistParserException::class)
    private fun parsePlaylist(stream: InputStream, playlist: Playlist) {
        var playlistEntry = PlaylistEntry()
        stream.bufferedReader().forEachLine { it ->
            if (!(it.equals(EXTENDED_INFO_TAG, ignoreCase = true) || it.trim { it <= ' ' } == AppUtils.EMPTY_STRING)) {
                if (it.matches(RECORD_TAG)) {
                    playlistEntry = PlaylistEntry()
                    playlistEntry[PlaylistEntry.PLAYLIST_METADATA] = it.replace("^(.*?),".toRegex(), AppUtils.EMPTY_STRING)
                    processingEntry = true
                } else {
                    if (!processingEntry) {
                        playlistEntry = PlaylistEntry()
                    }
                    playlistEntry[PlaylistEntry.URI] = it.trim { it <= ' ' }
                    savePlaylistFile(playlistEntry, playlist)
                }
            }
        }
    }

    private fun savePlaylistFile(playlistEntry: PlaylistEntry, playlist: Playlist) {
        mNumberOfFiles += 1
        playlistEntry[PlaylistEntry.TRACK] = mNumberOfFiles.toString()
        parseEntry(playlistEntry, playlist)
        processingEntry = false
    }

    companion object {
        const val EXTENSION = ".m3u"
        private const val EXTENDED_INFO_TAG = "#EXTM3U"
        private val RECORD_TAG = "^[#][E|e][X|x][T|t][I|i][N|n][F|f].*".toRegex()
    }
}
