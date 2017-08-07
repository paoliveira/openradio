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

import wseemann.media.jplaylistparser.exception.JPlaylistParserException;
import wseemann.media.jplaylistparser.playlist.Playlist;
import wseemann.media.jplaylistparser.playlist.PlaylistEntry;

public abstract class AbstractParser implements Parser {

	private final int mTimeout;

	public AbstractParser(final int timeout) {
		super();
		mTimeout = timeout;
	}

	protected void parseEntry(final PlaylistEntry playlistEntry, final Playlist playlist) {
		final AutoDetectParser parser = new AutoDetectParser(mTimeout);
		try {
		    parser.parse(playlistEntry.get(PlaylistEntry.URI), playlist);
		} catch (final IOException | JPlaylistParserException e) {
            playlist.add(playlistEntry);
		}
	}
}
