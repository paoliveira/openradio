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

package wseemann.media.jplaylistparser.parser.xspf;

import android.util.Log;

import com.yuriy.openradio.gabor.shared.utils.AppLogger;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import wseemann.media.jplaylistparser.mime.MediaType;
import wseemann.media.jplaylistparser.parser.AbstractParser;
import wseemann.media.jplaylistparser.playlist.Playlist;
import wseemann.media.jplaylistparser.playlist.PlaylistEntry;

public final class XSPFPlaylistParser extends AbstractParser {

    public final static String EXTENSION = ".xspf";

    private static final String LOCATION_ELEMENT = "LOCATION";
    private static final String TITLE_ELEMENT = "TITLE";
    private static final String TRACK_ELEMENT = "TRACK";
    private static final String TRACKLIST_ELEMENT = "TRACKLIST";

    private static int sNumberOfFiles = 0;

    private static final Set<MediaType> SUPPORTED_TYPES =
            Collections.singleton(MediaType.video("application/xspf+xml"));

    public XSPFPlaylistParser(final int timeout) {
        super(timeout);
    }

    @Override
    public Set<MediaType> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    /**
     * Retrieves the files listed in a .asx file
     *
     * @throws IOException
     */
    private void parsePlaylist(final InputStream stream, final Playlist playlist) throws IOException {
        final StringBuilder xml = new StringBuilder();
        String line;
        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        while ((line = reader.readLine()) != null) {
            xml.append(line);
        }

        reader.close();

        parseXML(xml.toString(), playlist);
    }

    private void parseXML(final String xml, final Playlist playlist) {
        final SAXBuilder builder = new SAXBuilder();
        Reader in;
        Document doc;
        Element root;

        try {
            in = new StringReader(xml);

            doc = builder.build(in);
            root = doc.getRootElement();
            final List<Element> children = castList(Element.class, root.getChildren());

            for (int i = 0; i < children.size(); i++) {
                final String tag = children.get(i).getName();

                if (tag != null && tag.equalsIgnoreCase(TRACKLIST_ELEMENT)) {
                    final List<Element> children2 = castList(Element.class, children.get(i).getChildren());

                    for (int j = 0; j < children2.size(); j++) {
                        final String attributeName = children2.get(j).getName();

                        if (attributeName.equalsIgnoreCase(TRACK_ELEMENT)) {
                            final List<Element> children3 = castList(Element.class, children2.get(j).getChildren());
                            buildPlaylistEntry(children3, playlist);
                        }
                    }
                }
            }
        } catch (final JDOMException e) {
            AppLogger.e("XSPF parse exception:" + Log.getStackTraceString(e));
        } catch (final IOException e) {
            AppLogger.e("XSPF parse exception:" + Log.getStackTraceString(e));
        } catch (final Exception e) {
            AppLogger.e("XSPF parse exception:" + Log.getStackTraceString(e));
        }
    }

    private void buildPlaylistEntry(final List<Element> children, final Playlist playlist) {
        final PlaylistEntry playlistEntry = new PlaylistEntry();

        for (int i = 0; i < children.size(); i++) {
            final String attributeName = children.get(i).getName();

            if (attributeName.equalsIgnoreCase(LOCATION_ELEMENT)) {
                String href = children.get(i).getValue();

                // TODO: add trim?
                playlistEntry.set(PlaylistEntry.URI, href);
            } else if (attributeName.equalsIgnoreCase(TITLE_ELEMENT)) {
                String title = children.get(i).getValue();

                if (title != null) {
                    playlistEntry.set(PlaylistEntry.PLAYLIST_METADATA, title);
                }
            }
        }

        sNumberOfFiles = sNumberOfFiles + 1;
        playlistEntry.set(PlaylistEntry.TRACK, String.valueOf(sNumberOfFiles));
        parseEntry(playlistEntry, playlist);
    }

    private <T> List<T> castList(final Class<? extends T> castClass, final List<?> c) {
        final List<T> list = new ArrayList<>(c.size());

        for (final Object o : c) {
            list.add(castClass.cast(o));
        }

        return list;
    }

    @Override
    public void parse(final String uri, final InputStream stream, final Playlist playlist) throws IOException {
        parsePlaylist(stream, playlist);
    }
}
