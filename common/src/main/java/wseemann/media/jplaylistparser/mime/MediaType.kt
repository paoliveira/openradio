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

package wseemann.media.jplaylistparser.mime

import com.yuriy.openradio.shared.utils.AppUtils
import java.util.Locale
import java.util.SortedMap
import java.util.TreeMap
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Internet media type.
 */
class MediaType : Comparable<MediaType> {

    /**
     * Canonical string representation of this media type.
     */
    private var mString = AppUtils.EMPTY_STRING

    private constructor(type: String?, subtype: String?, parameters: Map<String, String>) {
        if (type.isNullOrEmpty()) {
            return
        }
        if (subtype.isNullOrEmpty()) {
            return
        }
        var typeCpy = type
        var subtypeCpy = subtype
        typeCpy = typeCpy.trim { it <= ' ' }.lowercase(Locale.ENGLISH)
        subtypeCpy = subtypeCpy.trim { it <= ' ' }.lowercase(Locale.ENGLISH)
        if (parameters.isEmpty()) {
            mString = "$typeCpy/$subtypeCpy"
        } else {
            val builder = StringBuilder()
            builder.append(typeCpy)
            builder.append('/')
            builder.append(subtypeCpy)
            val map: SortedMap<String, String> = TreeMap()
            for ((key1, value) in parameters) {
                val key = key1.trim { it <= ' ' }.lowercase(Locale.ENGLISH)
                map[key] = value
            }
            for ((key, value) in map) {
                builder.append("; ")
                builder.append(key)
                builder.append("=")
                if (SPECIAL_OR_WHITESPACE.matcher(value).find()) {
                    builder.append('"')
                    builder.append(SPECIAL.matcher(value).replaceAll("\\\\$0"))
                    builder.append('"')
                } else {
                    builder.append(value)
                }
            }
            mString = builder.toString()
        }
    }

    private constructor(string: String, slash: Int) {
        mString = string
    }

    override fun toString(): String {
        return mString
    }

    override fun equals(other: Any?): Boolean {
        return if (other is MediaType) {
            mString == other.mString
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return mString.hashCode()
    }

    override fun compareTo(other: MediaType): Int {
        return mString.compareTo(other.mString)
    }

    companion object {
        private val SPECIAL = Pattern.compile("[()<>@,;:\\\\\"/\\[\\]?=]")
        private val SPECIAL_OR_WHITESPACE = Pattern.compile("[()<>@,;:\\\\\"/\\[\\]?=\\s]")

        /**
         * See http://www.ietf.org/rfc/rfc2045.txt for valid mime-type characters.
         */
        private const val VALID_CHARS = "([^\\c\\()<>@,;:\\\\\"/\\[\\]?=\\s]+)"
        private val TYPE_PATTERN = Pattern.compile(
                "(?s)\\s*$VALID_CHARS\\s*/\\s*$VALID_CHARS\\s*($|;.*)"
        )
        private val CHARSET_FIRST_PATTERN = Pattern.compile(
                "(?is)\\s*(charset\\s*=\\s*[^\\c;\\s]+)\\s*;\\s*"
                        + VALID_CHARS + "\\s*/\\s*" + VALID_CHARS + "\\s*"
        )

        /**
         * Set of basic types with normalized "type/subtype" names.
         * Used to optimize type lookup and to avoid having too many
         * [MediaType] instances in memory.
         */
        private val SIMPLE_TYPES = HashMap<String, MediaType>()

        fun audio(type: String): MediaType? {
            return parse("audio/$type")
        }

        fun video(type: String): MediaType? {
            return parse("video/$type")
        }

        /**
         * Parses the given string to a media type. The string is expected
         * to be of the form "type/subtype(; parameter=...)*" as defined in
         * RFC 2045, though we also handle "charset=xxx; type/subtype" for
         * broken web servers.
         *
         * @param string media type string to be parsed
         * @return parsed media type, or `null` if parsing fails
         */
        fun parse(string: String?): MediaType? {
            if (string == null) {
                return null
            }
            // Optimization for the common cases
            synchronized(SIMPLE_TYPES) {
                var type = SIMPLE_TYPES[string]
                if (type == null) {
                    val slash = string.indexOf('/')
                    if (slash == -1) {
                        return null
                    } else if (SIMPLE_TYPES.size < 10000 && isSimpleName(string.substring(0, slash))
                            && isSimpleName(string.substring(slash + 1))) {
                        type = MediaType(string, slash)
                        SIMPLE_TYPES[string] = type
                    }
                }
                if (type != null) {
                    return type
                }
            }
            var matcher: Matcher = TYPE_PATTERN.matcher(string)
            if (matcher.matches()) {
                return MediaType(
                        matcher.group(1), matcher.group(2),
                        parseParameters(matcher.group(3)))
            }
            matcher = CHARSET_FIRST_PATTERN.matcher(string)
            return if (matcher.matches()) {
                MediaType(
                        matcher.group(2), matcher.group(3),
                        parseParameters(matcher.group(1)))
            } else null
        }

        private fun isSimpleName(name: String): Boolean {
            for (element in name) {
                if (element != '-'
                        && element != '+'
                        && element != '.'
                        && element != '_'
                        && element !in '0'..'9'
                        && element !in 'a'..'z') {
                    return false
                }
            }
            return name.isNotEmpty()
        }

        private fun parseParameters(string: String?): Map<String, String> {
            var stringCpy: String? = string ?: return emptyMap()
            // Extracts k1=v1, k2=v2 from mime/type; k1=v1; k2=v2
            // Note - this logic isn't fully RFC2045 compliant yet, as it
            //  doesn't fully handle quoted keys or values (eg containing ; or =)
            val parameters = HashMap<String, String>()
            while (stringCpy!!.isNotEmpty()) {
                var key = stringCpy
                var value = AppUtils.EMPTY_STRING
                val semicolon = stringCpy.indexOf(';')
                if (semicolon != -1) {
                    key = stringCpy.substring(0, semicolon)
                    stringCpy = stringCpy.substring(semicolon + 1)
                } else {
                    stringCpy = AppUtils.EMPTY_STRING
                }
                val equals = key.indexOf('=')
                if (equals != -1) {
                    value = key.substring(equals + 1)
                    key = key.substring(0, equals)
                }
                key = key.trim { it <= ' ' }
                if (key.isNotEmpty()) {
                    parameters[key] = unquote(value.trim { it <= ' ' })
                }
            }
            return parameters
        }

        private fun unquote(s: String): String {
            if (s.startsWith("\"") && s.endsWith("\"")) {
                return s.substring(1, s.length - 1)
            }
            return if (s.startsWith("'") && s.endsWith("'")) {
                s.substring(1, s.length - 1)
            } else s
        }
    }
}
