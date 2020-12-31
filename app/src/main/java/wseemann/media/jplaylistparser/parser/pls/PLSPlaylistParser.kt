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
package wseemann.media.jplaylistparser.parser.pls

import com.yuriy.openradio.shared.utils.AppLogger
import wseemann.media.jplaylistparser.mime.MediaType
import wseemann.media.jplaylistparser.mime.MediaType.Companion.audio
import wseemann.media.jplaylistparser.parser.AbstractParser
import wseemann.media.jplaylistparser.playlist.Playlist
import wseemann.media.jplaylistparser.playlist.PlaylistEntry
import java.io.IOException
import java.io.InputStream

class PLSPlaylistParser(timeout: Int) : AbstractParser(timeout) {

    private var mNumberOfFiles = 0
    private var processingEntry = false

    override val supportedTypes: Set<MediaType?>
        get() = setOf(audio("x-scpls"))

    @Throws(IOException::class)
    override fun parse(uri: String, stream: InputStream, playlist: Playlist) {
        parsePlaylist(stream, playlist)
    }

    /**
     * Retrieves the files listed in a .pls file
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun parsePlaylist(stream: InputStream, playlist: Playlist) {
        var playlistEntry = PlaylistEntry()
        processingEntry = false
        stream.bufferedReader().forEachLine { it ->
            if (it.trim { it <= ' ' } == "") {
                if (processingEntry) {
                    savePlaylistFile(playlistEntry, playlist)
                }
                playlistEntry = PlaylistEntry()
                processingEntry = false
            } else {
                val index = it.indexOf('=')
                var parsedLine = arrayOfNulls<String>(0)
                if (index != -1) {
                    parsedLine = arrayOfNulls(2)
                    parsedLine[0] = it.substring(0, index)
                    parsedLine[1] = it.substring(index + 1)
                }
                if (parsedLine.size == 2) {
                    when {
                        parsedLine[0]!!.trim { it <= ' ' }.matches("[Ff][Ii][Ll][Ee].*".toRegex()) -> {
                            processingEntry = true
                            playlistEntry[PlaylistEntry.URI] = parsedLine[1]!!.trim { it <= ' ' }
                        }
                        parsedLine[0]!!.trim { it <= ' ' }.contains("Title") -> {
                            playlistEntry[PlaylistEntry.PLAYLIST_METADATA] = parsedLine[1]!!.trim { it <= ' ' }
                        }
                        parsedLine[0]!!.trim { it <= ' ' }.contains("Length") -> {
                            if (processingEntry) {
                                savePlaylistFile(playlistEntry, playlist)
                            }
                            playlistEntry = PlaylistEntry()
                            processingEntry = false
                        }
                    }
                }
            }
        }

        // added in case the file doesn't follow the standard pls
        // structure:
        // FileX:
        // TitleX:
        // LengthX:
        if (processingEntry) {
            savePlaylistFile(playlistEntry, playlist)
        }
    }

    private fun savePlaylistFile(playlistEntry: PlaylistEntry, playlist: Playlist) {
        mNumberOfFiles += 1
        playlistEntry[PlaylistEntry.TRACK] = mNumberOfFiles.toString()
        parseEntry(playlistEntry, playlist)
        processingEntry = false
    }

    companion object {
        const val EXTENSION = ".pls"
    }
}
