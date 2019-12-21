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

package wseemann.media.jplaylistparser.playlist;

import java.util.ArrayList;
import java.util.List;

public final class Playlist {

	private final List<PlaylistEntry> mPlaylistEntries;
	
    /**
     * Constructs a new, empty playlist.
     */
	public Playlist() {
		super();
		mPlaylistEntries = new ArrayList<>();
	}

	public final void add(final PlaylistEntry playlistEntry) {
		mPlaylistEntries.add(playlistEntry);
	}
	
	/**
	 * @return the PlaylistEntries
	 */
	public final List<PlaylistEntry> getPlaylistEntries() {
		return mPlaylistEntries;
	}
}