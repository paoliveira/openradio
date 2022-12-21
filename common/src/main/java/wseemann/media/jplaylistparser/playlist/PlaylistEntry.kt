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
package wseemann.media.jplaylistparser.playlist

import com.yuriy.openradio.shared.utils.AppUtils

/**
 * A multi-valued metadata container.
 */
class PlaylistEntry {
    /**
     * A map of all metadata attributes.
     */
    private val metadata = HashMap<String, Array<String>>()

    /**
     * Returns an array of the names contained in the metadata.
     *
     * @return Metadata names
     */
    private fun names(): Array<String> {
        return metadata.keys.toTypedArray()
    }

    /**
     * Get the value associated to a metadata name. If many values are assiociated
     * to the specified name, then the first one is returned.
     *
     * @param name
     * of the metadata.
     * @return the value associated to the specified metadata name.
     */
    operator fun get(name: String): String {
        val values = metadata[name]
        return values?.get(0) ?: AppUtils.EMPTY_STRING
    }

    private fun getValuesInternal(name: String): Array<String> {
        var values = metadata[name]
        if (values == null) {
            values = Array(0) { AppUtils.EMPTY_STRING }
        }
        return values
    }

    /**
     * Set metadata name/value. Associate the specified value to the specified
     * metadata name. If some previous values were associated to this name, they
     * are removed.
     *
     * @param name
     * the metadata name.
     * @param value
     * the metadata value.
     */
    operator fun set(name: String, value: String) {
        metadata[name] = arrayOf(value)
    }

    /**
     * Returns the number of metadata names in this metadata.
     *
     * @return number of metadata names
     */
    fun size(): Int {
        return metadata.size
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        val otherCpy = try {
            other as PlaylistEntry
        } catch (cce: ClassCastException) {
            return false
        }
        if (otherCpy.size() != size()) {
            return false
        }
        val names = names()
        for (name in names) {
            val otherValues = otherCpy.getValuesInternal(name)
            val thisValues = getValuesInternal(name)
            if (otherValues.size != thisValues.size) {
                return false
            }
            for (j in otherValues.indices) {
                if (otherValues[j] != thisValues[j]) {
                    return false
                }
            }
        }
        return true
    }

    override fun toString(): String {
        val builder = StringBuilder()
        val names = names()
        for (name in names) {
            val values = getValuesInternal(name)
            for (value in values) {
                builder.append(name).append("=").append(value).append(" ")
            }
        }
        return builder.toString()
    }

    override fun hashCode(): Int {
        return metadata.hashCode()
    }

    companion object {
        const val TRACK = "track"
        const val URI = "uri"
        const val PLAYLIST_METADATA = "playlist_metadata"
    }
}
