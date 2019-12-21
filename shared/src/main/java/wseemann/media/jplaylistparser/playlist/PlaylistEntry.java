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

import java.util.HashMap;
import java.util.Map;

/**
 * A multi-valued metadata container.
 */
public final class PlaylistEntry {

    /**
     * A map of all metadata attributes.
     */
    private Map<String, String[]> metadata = null;

    public static final String TRACK = "track";
    public static final String URI = "uri";
    public static final String PLAYLIST_METADATA = "playlist_metadata";

    /**
     * Constructs a new, empty playlist entry.
     */
    public PlaylistEntry() {
        metadata = new HashMap<>();
    }

    /**
     * Returns an array of the names contained in the metadata.
     *
     * @return Metadata names
     */
    private String[] names() {
        return metadata.keySet().toArray(new String[metadata.keySet().size()]);
    }

    /**
     * Get the value associated to a metadata name. If many values are assiociated
     * to the specified name, then the first one is returned.
     * 
     * @param name
     *          of the metadata.
     * @return the value associated to the specified metadata name.
     */
    public String get(final String name) {
        String[] values = metadata.get(name);
        if (values == null) {
            return null;
        } else {
            return values[0];
        }
    }

    private String[] _getValues(final String name) {
        String[] values = metadata.get(name);
        if (values == null) {
            values = new String[0];
        }
        return values;
    }

    /**
     * Set metadata name/value. Associate the specified value to the specified
     * metadata name. If some previous values were associated to this name, they
     * are removed.
     * 
     * @param name
     *          the metadata name.
     * @param value
     *          the metadata value.
     */
    public void set(final String name, final String value) {
        metadata.put(name, new String[] { value });
    }
    /**
     * Returns the number of metadata names in this metadata.
     * 
     * @return number of metadata names
     */
    public int size() {
        return metadata.size();
    }

    @Override
    public boolean equals(final Object o) {

        if (o == null) {
            return false;
        }

        PlaylistEntry other;
        try {
            other = (PlaylistEntry) o;
        } catch (ClassCastException cce) {
            return false;
        }

        if (other.size() != size()) {
            return false;
        }

        final String[] names = names();
        for (final String name : names) {
            final String[] otherValues = other._getValues(name);
            final String[] thisValues = _getValues(name);
            if (otherValues.length != thisValues.length) {
                return false;
            }
            for (int j = 0; j < otherValues.length; j++) {
                if (!otherValues[j].equals(thisValues[j])) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        final String[] names = names();
        for (final String name : names) {
            final String[] values = _getValues(name);
            for (final String value : values) {
                builder.append(name).append("=").append(value).append(" ");
            }
        }
        return builder.toString();
    }

}
