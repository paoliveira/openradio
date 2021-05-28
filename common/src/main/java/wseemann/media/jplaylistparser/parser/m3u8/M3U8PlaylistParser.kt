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
package wseemann.media.jplaylistparser.parser.m3u8

import android.text.TextUtils
import com.yuriy.openradio.shared.utils.AppUtils
import wseemann.media.jplaylistparser.exception.JPlaylistParserException
import wseemann.media.jplaylistparser.mime.MediaType
import wseemann.media.jplaylistparser.mime.MediaType.Companion.audio
import wseemann.media.jplaylistparser.parser.AbstractParser
import wseemann.media.jplaylistparser.playlist.Playlist
import wseemann.media.jplaylistparser.playlist.PlaylistEntry
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException

class M3U8PlaylistParser(timeout: Int) : AbstractParser(timeout) {

    private var mNumberOfFiles = 0
    private var processingEntry = false

    override val supportedTypes: Set<MediaType?>
        get() = setOf(audio("x-mpegurl"))

    @Throws(IOException::class)
    override fun parse(uri: String, stream: InputStream, playlist: Playlist) {
        parsePlaylist(uri, stream, playlist)
    }

    /**
     * Retrieves the files listed in a .m3u file
     *
     * @throws IOException
     */
    @Throws(IOException::class, JPlaylistParserException::class)
    private fun parsePlaylist(uri: String, stream: InputStream, playlist: Playlist) {
        val host = getHost(uri)
        if (TextUtils.isEmpty(host)) {
            throw IOException("Provided URI '$uri' is invalid")
        }
        var playlistEntry = PlaylistEntry()
        stream.bufferedReader().forEachLine { it ->
            if (!(it.equals(EXTENDED_INFO_TAG, ignoreCase = true) ||
                            it.matches(INFO_TAG) || it.trim { it <= ' ' } == AppUtils.EMPTY_STRING)) {
                if (it.matches(RECORD_TAG)) {
                    playlistEntry = PlaylistEntry()
                    playlistEntry[PlaylistEntry.PLAYLIST_METADATA] = it.replace("^(.*?),".toRegex(), AppUtils.EMPTY_STRING)
                    processingEntry = true
                } else {
                    if (!processingEntry) {
                        playlistEntry = PlaylistEntry()
                    }
                    playlistEntry[PlaylistEntry.URI] = generateUri(it.trim { it <= ' ' }, host)
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

    private fun getHost(uri: String): String {
        var hostString = AppUtils.EMPTY_STRING
        try {
            var host = URI(uri)
            var path: String
            if (host.path.also { path = it } == null || path.trim { it <= ' ' } == AppUtils.EMPTY_STRING) {
                return "$uri/"
            }
            val index = path.lastIndexOf('/')
            if (index > -1) {
                host = URI(
                        host.scheme, null, host.host, host.port, path.substring(0, index + 1),
                        null, null
                )
            }
            hostString = host.toString()
        } catch (e: URISyntaxException) {
            /**/
        }
        return hostString
    }

    private fun generateUri(uri: String, host: String): String {
        return if (uri.matches(PROTOCOL)) {
            uri
        } else host + uri
    }

    companion object {
        const val EXTENSION = ".m3u8"
        private const val EXTENDED_INFO_TAG = "#EXTM3U"
        private val INFO_TAG = "^[#][E|e][X|x][T|t][-][X|x][-].*".toRegex()
        private val RECORD_TAG = "^[#][E|e][X|x][T|t][I|i][N|n][F|f].*".toRegex()
        private val PROTOCOL = "^[H|h][T|t][T|t][P|p].*".toRegex()
    }
}
