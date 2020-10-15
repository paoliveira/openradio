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

package wseemann.media.jplaylistparser.parser.asx;

import android.util.Log;

import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.AppUtils;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.JDOMParseException;
import org.jdom2.input.SAXBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import wseemann.media.jplaylistparser.mime.MediaType;
import wseemann.media.jplaylistparser.parser.AbstractParser;
import wseemann.media.jplaylistparser.parser.AutoDetectParser;
import wseemann.media.jplaylistparser.playlist.Playlist;
import wseemann.media.jplaylistparser.playlist.PlaylistEntry;

public final class ASXPlaylistParser extends AbstractParser {
    public final static String EXTENSION = ".asx";

    private static final String ABSTRACT_ELEMENT = "ABSTRACT";
    private static final String ASX_ELEMENT = "ASX";
    private static final String AUTHOR_ELEMENT = "AUTHOR";
    private static final String BASE_ELEMENT = "BASE";
    private static final String COPYRIGHT_ELEMENT = "COPYRIGHT";
    private static final String DURATION_ELEMENT = "DURATION";
    private static final String ENDMARKER_ELEMENT = "ENDMARKER";
    private static final String ENTRY_ELEMENT = "ENTRY";
    private static final String ENTRYREF_ELEMENT = "ENTRYREF";
    private static final String EVENT_ELEMENT = "EVENT";
    private static final String MOREINFO_ELEMENT = "MOREINFO";
    private static final String PARAM_ELEMENT = "PARAM";
    private static final String REF_ELEMENT = "REF";
    private static final String REPEAT_ELEMENT = "REPEAT";
    private static final String STARTMARKER_ELEMENT = "STARTMARKER";
    private static final String STARTTIME_ELEMENT = "STARTTIME";
    private static final String TITLE_ELEMENT = "TITLE";

    private final String HREF_ATTRIBUTE = "href";

    private static int sNumberOfFiles = 0;

    private static final Set<MediaType> SUPPORTED_TYPES =
            Collections.singleton(MediaType.video("x-ms-asf"));

    public Set<MediaType> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    public ASXPlaylistParser(int timeout) {
        super(timeout);
    }

    /**
     * Retrieves the files listed in a .asx file
     *
     * @throws IOException
     */
    private void parsePlaylist(InputStream stream, Playlist playlist) throws IOException {
        final StringBuilder xml = new StringBuilder();
        String line;
        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        while ((line = reader.readLine()) != null) {
            xml.append(line);
        }

        reader.close();

        parseXML(xml.toString(), playlist);
    }

    private String validateXML(String xml, SAXBuilder builder) throws JDOMException, IOException {
        Reader in;
        int i = 0;

        xml = xml.replaceAll("\\&", "&amp;");

        while (i < 5) {
            in = new StringReader(xml);

            try {
                builder.build(in);
                break;
            } catch (JDOMParseException e) {
                String message = e.getMessage();
                if (message.matches("^.*.The element type.*.must be terminated by the matching end-tag.*")) {
                    String tag = message.substring(message.lastIndexOf("type") + 6, message.lastIndexOf("must") - 2);
                    xml = xml.replaceAll("(?i)</" + tag + ">", "</" + tag + ">");
                } else {
                    break;
                }

                i++;
            }
        }

        return xml;
    }

    private void parseXML(String xml, Playlist playlist) {
        SAXBuilder builder = new SAXBuilder();
        Reader in;
        Document doc;
        Element root;

        try {
            xml = validateXML(xml, builder);
            in = new StringReader(xml);

            doc = builder.build(in);
            root = doc.getRootElement();
            List<Element> children = castList(Element.class, root.getChildren());

            for (int i = 0; i < children.size(); i++) {
                String tag = children.get(i).getName();

                if (tag != null && tag.equalsIgnoreCase(ENTRY_ELEMENT)) {
                    List<Element> children2 = castList(Element.class, children.get(i).getChildren());

                    buildPlaylistEntry(children2, playlist);
                } else if (tag != null && tag.equalsIgnoreCase(ENTRYREF_ELEMENT)) {
                    URL url;
                    HttpURLConnection conn = null;
                    InputStream is = null;

                    try {
                        String href = children.get(i).getAttributeValue(HREF_ATTRIBUTE);

                        if (href == null) {
                            href = children.get(i).getAttributeValue(HREF_ATTRIBUTE.toUpperCase());
                        }

                        if (href == null) {
                            href = children.get(i).getValue();
                        }

                        url = new URL(href);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(AppUtils.TIME_OUT);
                        conn.setReadTimeout(AppUtils.TIME_OUT);
                        conn.setRequestMethod("GET");

                        String contentType = conn.getContentType();
                        is = conn.getInputStream();

                        AutoDetectParser parser = new AutoDetectParser(AppUtils.TIME_OUT);
                        parser.parse(url.toString(), contentType, is, playlist);
                    } catch (MalformedURLException e) {
                        AppLogger.e("ASX parse exception:" + Log.getStackTraceString(e));
                    } catch (SocketTimeoutException e) {
                        AppLogger.e("ASX parse exception:" + Log.getStackTraceString(e));
                    } catch (IOException e) {
                        AppLogger.e("ASX parse exception:" + Log.getStackTraceString(e));
                    } finally {
                        if (conn != null) {
                            conn.disconnect();
                        }

                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException e) {
                                /* Ignore */
                            }
                        }
                    }
                }
            }
        } catch (JDOMException e) {
            AppLogger.e("ASX parse exception:" + Log.getStackTraceString(e));
        } catch (IOException e) {
            AppLogger.e("ASX parse exception:" + Log.getStackTraceString(e));
        } catch (Exception e) {
            AppLogger.e("ASX parse exception:" + Log.getStackTraceString(e));
        }
    }

    private void buildPlaylistEntry(List<Element> children, Playlist playlist) {
        PlaylistEntry playlistEntry = new PlaylistEntry();

        for (int i = 0; i < children.size(); i++) {
            String attributeName = children.get(i).getName();

            if (attributeName.equalsIgnoreCase(ABSTRACT_ELEMENT)) {
            } else if (attributeName.equalsIgnoreCase(ASX_ELEMENT)) {
            } else if (attributeName.equalsIgnoreCase(AUTHOR_ELEMENT)) {
            } else if (attributeName.equalsIgnoreCase(BASE_ELEMENT)) {
            } else if (attributeName.equalsIgnoreCase(COPYRIGHT_ELEMENT)) {
            } else if (attributeName.equalsIgnoreCase(DURATION_ELEMENT)) {
            } else if (attributeName.equalsIgnoreCase(ENDMARKER_ELEMENT)) {
            } else if (attributeName.equalsIgnoreCase(ENTRY_ELEMENT)) {
            } else if (attributeName.equalsIgnoreCase(ENTRYREF_ELEMENT)) {
            } else if (attributeName.equalsIgnoreCase(EVENT_ELEMENT)) {
            } else if (attributeName.equalsIgnoreCase(MOREINFO_ELEMENT)) {
            } else if (attributeName.equalsIgnoreCase(PARAM_ELEMENT)) {
            } else if (attributeName.equalsIgnoreCase(REF_ELEMENT)) {
                String href = children.get(i).getAttributeValue(HREF_ATTRIBUTE);

                if (href == null) {
                    href = children.get(i).getAttributeValue(HREF_ATTRIBUTE.toUpperCase());
                }

                if (href == null) {
                    href = children.get(i).getValue();
                }

                // TODO: add trim?
                playlistEntry.set(PlaylistEntry.URI, href);
            } else if (attributeName.equalsIgnoreCase(REPEAT_ELEMENT)) {
            } else if (attributeName.equalsIgnoreCase(STARTMARKER_ELEMENT)) {
            } else if (attributeName.equalsIgnoreCase(STARTTIME_ELEMENT)) {
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

    private <T> List<T> castList(Class<? extends T> castClass, List<?> c) {
        List<T> list = new ArrayList<>(c.size());

        for (Object o : c) {
            list.add(castClass.cast(o));
        }

        return list;
    }

    @Override
    public void parse(String uri, InputStream stream, Playlist playlist) throws IOException {
        parsePlaylist(stream, playlist);
    }
}
