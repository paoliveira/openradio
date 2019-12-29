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

package wseemann.media.jplaylistparser.parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

import wseemann.media.jplaylistparser.exception.JPlaylistParserException;
import wseemann.media.jplaylistparser.mime.MediaType;
import wseemann.media.jplaylistparser.parser.asx.ASXPlaylistParser;
import wseemann.media.jplaylistparser.parser.m3u.M3UPlaylistParser;
import wseemann.media.jplaylistparser.parser.m3u8.M3U8PlaylistParser;
import wseemann.media.jplaylistparser.parser.pls.PLSPlaylistParser;
import wseemann.media.jplaylistparser.parser.xspf.XSPFPlaylistParser;
import wseemann.media.jplaylistparser.playlist.Playlist;

public final class AutoDetectParser {

    private final int mTimeout;

    public AutoDetectParser(final int timeout) {
        super();
        mTimeout = timeout;
    }

    public void parse(
            final String uri,
            String mimeType,
            final InputStream stream,
            final Playlist playlist)
            throws IOException, JPlaylistParserException {
        Parser parser;
        String extension;

        if (uri == null) {
            throw new IllegalArgumentException("URI cannot be NULL");
        }

        if (stream == null) {
            throw new IllegalArgumentException("Stream cannot be NULL");
        }

        if (mimeType == null) {
            mimeType = "";
        }

        if (mimeType.split("\\;").length > 0) {
            mimeType = mimeType.split("\\;")[0];
        }

        final AbstractParser m3uPlaylistParser = new M3UPlaylistParser(mTimeout);
        final AbstractParser m3u8PlaylistParser = new M3U8PlaylistParser(mTimeout);
        final AbstractParser plsPlaylistParser = new PLSPlaylistParser(mTimeout);
        final AbstractParser xspfPlaylistParser = new XSPFPlaylistParser(mTimeout);
        final AbstractParser asxPlaylistParser = new ASXPlaylistParser(mTimeout);

        extension = getFileExtension(uri);

        if (extension.equalsIgnoreCase(M3UPlaylistParser.EXTENSION)
                || (m3uPlaylistParser.getSupportedTypes().contains(MediaType.parse(mimeType)) &&
                !extension.equalsIgnoreCase(M3U8PlaylistParser.EXTENSION))) {
            parser = m3uPlaylistParser;
        } else if (extension.equalsIgnoreCase(M3U8PlaylistParser.EXTENSION)
                || m3uPlaylistParser.getSupportedTypes().contains(MediaType.parse(mimeType))) {
            parser = m3u8PlaylistParser;
        } else if (extension.equalsIgnoreCase(PLSPlaylistParser.EXTENSION)
                || plsPlaylistParser.getSupportedTypes().contains(MediaType.parse(mimeType))) {
            parser = plsPlaylistParser;
        } else if (extension.equalsIgnoreCase(XSPFPlaylistParser.EXTENSION)
                || xspfPlaylistParser.getSupportedTypes().contains(MediaType.parse(mimeType))) {
            parser = xspfPlaylistParser;
        } else if (extension.equalsIgnoreCase(ASXPlaylistParser.EXTENSION)
                || asxPlaylistParser.getSupportedTypes().contains(MediaType.parse(mimeType))) {
            parser = asxPlaylistParser;
        } else {
            throw new JPlaylistParserException("Unsupported format:" + uri);
        }

        parser.parse(uri, stream, playlist);
    }

    void parse(final String uri, final Playlist playlist) throws IOException, JPlaylistParserException {
        Parser parser;
        String extension;

        if (uri == null) {
            throw new IllegalArgumentException("URI cannot be NULL");
        }

        final AbstractParser m3uPlaylistParser = new M3UPlaylistParser(mTimeout);
        final AbstractParser m3u8PlaylistParser = new M3U8PlaylistParser(mTimeout);
        final AbstractParser plsPlaylistParser = new PLSPlaylistParser(mTimeout);
        final AbstractParser xspfPlaylistParser = new XSPFPlaylistParser(mTimeout);
        final AbstractParser asxPlaylistParser = new ASXPlaylistParser(mTimeout);

        extension = getFileExtension(uri);

        if (extension.equalsIgnoreCase(M3UPlaylistParser.EXTENSION)
                && !extension.equalsIgnoreCase(M3U8PlaylistParser.EXTENSION)) {
            parser = m3uPlaylistParser;
        } else if (extension.equalsIgnoreCase(M3U8PlaylistParser.EXTENSION)) {
            parser = m3u8PlaylistParser;
        } else if (extension.equalsIgnoreCase(PLSPlaylistParser.EXTENSION)) {
            parser = plsPlaylistParser;
        } else if (extension.equalsIgnoreCase(XSPFPlaylistParser.EXTENSION)) {
            parser = xspfPlaylistParser;
        } else if (extension.equalsIgnoreCase(ASXPlaylistParser.EXTENSION)) {
            parser = asxPlaylistParser;
        } else {
            throw new JPlaylistParserException("Unsupported format:" + uri);
        }

        HttpURLConnection conn = null;
        InputStream is = null;

        try {
            final URL url = new URL(URLDecoder.decode(uri, "UTF-8"));
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(mTimeout);
            conn.setReadTimeout(mTimeout);
            conn.setRequestMethod("GET");

            is = conn.getInputStream();

            parser.parse(url.toString(), is, playlist);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    /**/
                }
            }
        }
    }

    String getFileExtension(final String uri) {
        String fileExtension = "";
        int beginIndex = uri.lastIndexOf(".");
        if (beginIndex != -1) {
            int endIndex = uri.length();
            fileExtension = uri.substring(beginIndex, endIndex);
            // Keep this order the same!
            if (fileExtension.startsWith(PLSPlaylistParser.EXTENSION)) {
                endIndex = PLSPlaylistParser.EXTENSION.length();
            } else if (fileExtension.startsWith(M3U8PlaylistParser.EXTENSION)) {
                endIndex = M3U8PlaylistParser.EXTENSION.length();
            } else if (fileExtension.startsWith(M3UPlaylistParser.EXTENSION)) {
                endIndex = M3UPlaylistParser.EXTENSION.length();
            } else if (fileExtension.startsWith(XSPFPlaylistParser.EXTENSION)) {
                endIndex = XSPFPlaylistParser.EXTENSION.length();
            } else if (fileExtension.startsWith(ASXPlaylistParser.EXTENSION)) {
                endIndex = ASXPlaylistParser.EXTENSION.length();
            } else {
                endIndex = fileExtension.length();
            }
            beginIndex = 0;
            fileExtension = fileExtension.substring(beginIndex, endIndex);
        }
        return fileExtension;
    }
}
