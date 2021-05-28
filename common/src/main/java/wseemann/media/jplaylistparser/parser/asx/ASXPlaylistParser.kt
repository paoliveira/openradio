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

package wseemann.media.jplaylistparser.parser.asx

import android.util.Log
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.JDOMException
import org.jdom2.input.JDOMParseException
import org.jdom2.input.SAXBuilder
import wseemann.media.jplaylistparser.mime.MediaType
import wseemann.media.jplaylistparser.mime.MediaType.Companion.video
import wseemann.media.jplaylistparser.parser.AbstractParser
import wseemann.media.jplaylistparser.parser.AutoDetectParser
import wseemann.media.jplaylistparser.playlist.Playlist
import wseemann.media.jplaylistparser.playlist.PlaylistEntry
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import java.util.*

class ASXPlaylistParser(timeout: Int) : AbstractParser(timeout) {

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

    @Throws(JDOMException::class, IOException::class)
    private fun validateXML(xml: String, builder: SAXBuilder): String {
        var xmlCpy = xml
        var reader: Reader
        var i = 0
        xmlCpy = xmlCpy.replace("&".toRegex(), "&amp;")
        while (i < 5) {
            reader = StringReader(xmlCpy)
            try {
                builder.build(reader)
                break
            } catch (e: JDOMParseException) {
                val message = e.message
                xmlCpy = if (message!!.matches("^.*.The element type.*.must be terminated by the matching end-tag.*".toRegex())) {
                    val tag = message.substring(message.lastIndexOf("type") + 6, message.lastIndexOf("must") - 2)
                    xmlCpy.replace("(?i)</" + tag + ">".toRegex(), "</$tag>")
                } else {
                    break
                }
                i++
            }
        }
        return xmlCpy
    }

    private fun parseXML(xml: String, playlist: Playlist) {
        var xmlCpy = xml
        val builder = SAXBuilder()
        val reader: Reader
        val doc: Document
        val root: Element
        try {
            xmlCpy = validateXML(xmlCpy, builder)
            reader = StringReader(xmlCpy)
            doc = builder.build(reader)
            root = doc.rootElement
            val children = castList(Element::class.java, root.children)
            for (i in children.indices) {
                val tag = children[i].name
                if (tag != null && tag.equals(ENTRY_ELEMENT, ignoreCase = true)) {
                    val children2 = castList(Element::class.java, children[i].children)
                    buildPlaylistEntry(children2, playlist)
                } else if (tag != null && tag.equals(ENTRYREF_ELEMENT, ignoreCase = true)) {
                    var url: URL
                    var conn: HttpURLConnection? = null
                    var inputStream: InputStream? = null
                    try {
                        var href = children[i].getAttributeValue(HREF_ATTRIBUTE)
                        if (href == null) {
                            href = children[i].getAttributeValue(HREF_ATTRIBUTE.uppercase(Locale.ROOT))
                        }
                        if (href == null) {
                            href = children[i].value
                        }
                        url = URL(href)
                        conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = AppUtils.TIME_OUT
                        conn.readTimeout = AppUtils.TIME_OUT
                        conn.requestMethod = "GET"
                        val contentType = conn.contentType
                        inputStream = conn.inputStream
                        val parser = AutoDetectParser(AppUtils.TIME_OUT)
                        parser.parse(url.toString(), contentType, inputStream, playlist)
                    } catch (e: MalformedURLException) {
                        AppLogger.e("ASX parse exception:" + Log.getStackTraceString(e))
                    } catch (e: SocketTimeoutException) {
                        AppLogger.e("ASX parse exception:" + Log.getStackTraceString(e))
                    } catch (e: IOException) {
                        AppLogger.e("ASX parse exception:" + Log.getStackTraceString(e))
                    } finally {
                        conn?.disconnect()
                        if (inputStream != null) {
                            try {
                                inputStream.close()
                            } catch (e: IOException) {
                                /* Ignore */
                            }
                        }
                    }
                }
            }
        } catch (e: JDOMException) {
            AppLogger.e("ASX parse exception:" + Log.getStackTraceString(e))
        } catch (e: IOException) {
            AppLogger.e("ASX parse exception:" + Log.getStackTraceString(e))
        } catch (e: Exception) {
            AppLogger.e("ASX parse exception:" + Log.getStackTraceString(e))
        }
    }

    private fun buildPlaylistEntry(children: List<Element>, playlist: Playlist?) {
        val playlistEntry = PlaylistEntry()
        for (i in children.indices) {
            val attributeName = children[i].name
            if (attributeName.equals(ABSTRACT_ELEMENT, ignoreCase = true)) {
            } else if (attributeName.equals(ASX_ELEMENT, ignoreCase = true)) {
            } else if (attributeName.equals(AUTHOR_ELEMENT, ignoreCase = true)) {
            } else if (attributeName.equals(BASE_ELEMENT, ignoreCase = true)) {
            } else if (attributeName.equals(COPYRIGHT_ELEMENT, ignoreCase = true)) {
            } else if (attributeName.equals(DURATION_ELEMENT, ignoreCase = true)) {
            } else if (attributeName.equals(ENDMARKER_ELEMENT, ignoreCase = true)) {
            } else if (attributeName.equals(ENTRY_ELEMENT, ignoreCase = true)) {
            } else if (attributeName.equals(ENTRYREF_ELEMENT, ignoreCase = true)) {
            } else if (attributeName.equals(EVENT_ELEMENT, ignoreCase = true)) {
            } else if (attributeName.equals(MOREINFO_ELEMENT, ignoreCase = true)) {
            } else if (attributeName.equals(PARAM_ELEMENT, ignoreCase = true)) {
            } else if (attributeName.equals(REF_ELEMENT, ignoreCase = true)) {
                var href = children[i].getAttributeValue(HREF_ATTRIBUTE)
                if (href == null) {
                    href = children[i].getAttributeValue(HREF_ATTRIBUTE.uppercase(Locale.ROOT))
                }
                if (href == null) {
                    href = children[i].value
                }

                // TODO: add trim?
                playlistEntry[PlaylistEntry.URI] = href
            } else if (attributeName.equals(REPEAT_ELEMENT, ignoreCase = true)) {
            } else if (attributeName.equals(STARTMARKER_ELEMENT, ignoreCase = true)) {
            } else if (attributeName.equals(STARTTIME_ELEMENT, ignoreCase = true)) {
            } else if (attributeName.equals(TITLE_ELEMENT, ignoreCase = true)) {
                val title = children[i].value
                if (title != null) {
                    playlistEntry[PlaylistEntry.PLAYLIST_METADATA] = title
                }
            }
        }
        sNumberOfFiles += 1
        playlistEntry[PlaylistEntry.TRACK] = sNumberOfFiles.toString()
        parseEntry(playlistEntry, playlist!!)
    }

    private fun <T> castList(castClass: Class<out T>, c: List<*>): List<T> {
        val list: MutableList<T> = ArrayList(c.size)
        for (o in c) {
            castClass.cast(o)?.let { list.add(it) }
        }
        return list
    }

    override val supportedTypes: Set<MediaType?>
        get() = setOf(video("x-ms-asf"))

    @Throws(IOException::class)
    override fun parse(uri: String, stream: InputStream, playlist: Playlist) {
        parsePlaylist(stream, playlist)
    }

    companion object {
        const val EXTENSION = ".asx"
        private const val ABSTRACT_ELEMENT = "ABSTRACT"
        private const val ASX_ELEMENT = "ASX"
        private const val AUTHOR_ELEMENT = "AUTHOR"
        private const val BASE_ELEMENT = "BASE"
        private const val COPYRIGHT_ELEMENT = "COPYRIGHT"
        private const val DURATION_ELEMENT = "DURATION"
        private const val ENDMARKER_ELEMENT = "ENDMARKER"
        private const val ENTRY_ELEMENT = "ENTRY"
        private const val ENTRYREF_ELEMENT = "ENTRYREF"
        private const val EVENT_ELEMENT = "EVENT"
        private const val MOREINFO_ELEMENT = "MOREINFO"
        private const val PARAM_ELEMENT = "PARAM"
        private const val REF_ELEMENT = "REF"
        private const val REPEAT_ELEMENT = "REPEAT"
        private const val STARTMARKER_ELEMENT = "STARTMARKER"
        private const val STARTTIME_ELEMENT = "STARTTIME"
        private const val TITLE_ELEMENT = "TITLE"
        private var sNumberOfFiles = 0
        private const val HREF_ATTRIBUTE = "href"
    }
}
