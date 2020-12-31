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

package wseemann.media.jplaylistparser.parser.xspf

import android.util.Log
import com.yuriy.openradio.shared.utils.AppLogger
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.JDOMException
import org.jdom2.input.SAXBuilder
import wseemann.media.jplaylistparser.mime.MediaType
import wseemann.media.jplaylistparser.parser.AbstractParser
import wseemann.media.jplaylistparser.playlist.Playlist
import wseemann.media.jplaylistparser.playlist.PlaylistEntry
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.io.StringReader
import java.util.*

class XSPFPlaylistParser(timeout: Int) : AbstractParser(timeout) {
    /**
     * Retrieves the files listed in a .asx file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun parsePlaylist(stream: InputStream, playlist: Playlist) {
        val xml = StringBuilder()
        stream.bufferedReader().forEachLine {
            xml.append(it)
        }
        parseXML(xml.toString(), playlist)
    }

    private fun parseXML(xml: String, playlist: Playlist) {
        val builder = SAXBuilder()
        val reader: Reader
        val doc: Document
        val root: Element
        try {
            reader = StringReader(xml)
            doc = builder.build(reader)
            root = doc.rootElement
            val children = castList(Element::class.java, root.children)
            for (i in children.indices) {
                val tag = children[i].name
                if (tag != null && tag.equals(TRACKLIST_ELEMENT, ignoreCase = true)) {
                    val children2 = castList(Element::class.java, children[i].children)
                    for (j in children2.indices) {
                        val attributeName = children2[j].name
                        if (attributeName.equals(TRACK_ELEMENT, ignoreCase = true)) {
                            val children3 = castList(Element::class.java, children2[j].children)
                            buildPlaylistEntry(children3, playlist)
                        }
                    }
                }
            }
        } catch (e: JDOMException) {
            AppLogger.e("XSPF parse exception:" + Log.getStackTraceString(e))
        } catch (e: IOException) {
            AppLogger.e("XSPF parse exception:" + Log.getStackTraceString(e))
        } catch (e: Exception) {
            AppLogger.e("XSPF parse exception:" + Log.getStackTraceString(e))
        }
    }

    private fun buildPlaylistEntry(children: List<Element>, playlist: Playlist) {
        val playlistEntry = PlaylistEntry()
        for (i in children.indices) {
            val attributeName = children[i].name
            if (attributeName.equals(LOCATION_ELEMENT, ignoreCase = true)) {
                val href = children[i].value
                // TODO: add trim?
                playlistEntry[PlaylistEntry.URI] = href
            } else if (attributeName.equals(TITLE_ELEMENT, ignoreCase = true)) {
                val title = children[i].value
                if (title != null) {
                    playlistEntry[PlaylistEntry.PLAYLIST_METADATA] = title
                }
            }
        }
        sNumberOfFiles += 1
        playlistEntry[PlaylistEntry.TRACK] = sNumberOfFiles.toString()
        parseEntry(playlistEntry, playlist)
    }

    private fun <T> castList(castClass: Class<out T>, c: List<*>): List<T> {
        val list: MutableList<T> = ArrayList(c.size)
        for (o in c) {
            list.add(castClass.cast(o))
        }
        return list
    }

    override val supportedTypes: Set<MediaType?>
        get() = setOf(MediaType.video("application/xspf+xml"))

    @Throws(IOException::class)
    override fun parse(uri: String, stream: InputStream, playlist: Playlist) {
        parsePlaylist(stream, playlist)
    }

    companion object {
        const val EXTENSION = ".xspf"
        private const val LOCATION_ELEMENT = "LOCATION"
        private const val TITLE_ELEMENT = "TITLE"
        private const val TRACK_ELEMENT = "TRACK"
        private const val TRACKLIST_ELEMENT = "TRACKLIST"
        private var sNumberOfFiles = 0
    }
}