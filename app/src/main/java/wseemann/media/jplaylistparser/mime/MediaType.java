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

package wseemann.media.jplaylistparser.mime;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internet media type.
 */
public final class MediaType implements Comparable<MediaType> {

    private static final Pattern SPECIAL = Pattern.compile("[\\(\\)<>@,;:\\\\\"/\\[\\]\\?=]");

    private static final Pattern SPECIAL_OR_WHITESPACE = Pattern.compile("[\\(\\)<>@,;:\\\\\"/\\[\\]\\?=\\s]");

    /**
     * See http://www.ietf.org/rfc/rfc2045.txt for valid mime-type characters.
     */
    private static final String VALID_CHARS =  "([^\\c\\(\\)<>@,;:\\\\\"/\\[\\]\\?=\\s]+)";

    private static final Pattern TYPE_PATTERN = Pattern.compile(
                    "(?s)\\s*" + VALID_CHARS + "\\s*/\\s*" + VALID_CHARS + "\\s*($|;.*)"
    );

    private static final Pattern CHARSET_FIRST_PATTERN = Pattern.compile(
            "(?is)\\s*(charset\\s*=\\s*[^\\c;\\s]+)\\s*;\\s*"
            + VALID_CHARS + "\\s*/\\s*" + VALID_CHARS + "\\s*"
    );

    /**
     * Set of basic types with normalized "type/subtype" names.
     * Used to optimize type lookup and to avoid having too many
     * {@link MediaType} instances in memory.
     */
    private static final Map<String, MediaType> SIMPLE_TYPES = new HashMap<>();

    public static MediaType audio(String type) {
        return MediaType.parse("audio/" + type);
    }

    public static MediaType video(String type) {
        return MediaType.parse("video/" + type);
    }

    /**
     * Parses the given string to a media type. The string is expected
     * to be of the form "type/subtype(; parameter=...)*" as defined in
     * RFC 2045, though we also handle "charset=xxx; type/subtype" for
     * broken web servers.
     *
     * @param string media type string to be parsed
     * @return parsed media type, or <code>null</code> if parsing fails
     */
    public static MediaType parse(String string) {
        if (string == null) {
            return null;
        }

        // Optimization for the common cases
        synchronized (SIMPLE_TYPES) {
            MediaType type = SIMPLE_TYPES.get(string);
            if (type == null) {
                int slash = string.indexOf('/');
                if (slash == -1) {
                    return null;
                } else if (SIMPLE_TYPES.size() < 10000
                        && isSimpleName(string.substring(0, slash))
                        && isSimpleName(string.substring(slash + 1))) {
                    type = new MediaType(string, slash);
                    SIMPLE_TYPES.put(string, type);
                }
            }
            if (type != null) {
                return type;
            }
        }

        Matcher matcher;
        matcher = TYPE_PATTERN.matcher(string);
        if (matcher.matches()) {
            return new MediaType(
                    matcher.group(1), matcher.group(2),
                    parseParameters(matcher.group(3)));
        }
        matcher = CHARSET_FIRST_PATTERN.matcher(string);
        if (matcher.matches()) {
            return new MediaType(
                    matcher.group(2), matcher.group(3),
                    parseParameters(matcher.group(1)));
        }

        return null;
    }

    private static boolean isSimpleName(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c != '-' && c != '+' && c != '.' && c != '_'
                    && !('0' <= c && c <= '9')
                    && !('a' <= c && c <= 'z')) {
                return false;
            }
        }
        return name.length() > 0;
    }

    private static Map<String, String> parseParameters(String string) {
        if (string.length() == 0) {
            return Collections.emptyMap();
        }

        // Extracts k1=v1, k2=v2 from mime/type; k1=v1; k2=v2
        // Note - this logic isn't fully RFC2045 compliant yet, as it
        //  doesn't fully handle quoted keys or values (eg containing ; or =)
        Map<String, String> parameters = new HashMap<>();
        while (string.length() > 0) {
            String key = string;
            String value = "";

            int semicolon = string.indexOf(';');
            if (semicolon != -1) {
                key = string.substring(0, semicolon);
                string = string.substring(semicolon + 1);
            } else {
                string = "";
            }

            int equals = key.indexOf('=');
            if (equals != -1) {
                value = key.substring(equals + 1);
                key = key.substring(0, equals);
            }

            key = key.trim();
            if (key.length() > 0) {
                parameters.put(key, unquote(value.trim()));
            }
        }
        return parameters;
    }
    
    private static String unquote(String s) {
        if( s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        if( s.startsWith("'") && s.endsWith("'")) {
           return s.substring(1, s.length() - 1);
       }

        return s;
    }

    /**
     * Canonical string representation of this media type.
     */
    private final String string;

    private MediaType(String type, String subtype, Map<String, String> parameters) {
        type = type.trim().toLowerCase(Locale.ENGLISH);
        subtype = subtype.trim().toLowerCase(Locale.ENGLISH);

        if (parameters.isEmpty()) {
            this.string = type + '/' + subtype;
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append(type);
            builder.append('/');
            builder.append(subtype);

            SortedMap<String, String> map = new TreeMap<>();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                String key = entry.getKey().trim().toLowerCase(Locale.ENGLISH);
                map.put(key, entry.getValue());
            }
            for (Map.Entry<String, String> entry : map.entrySet()) {
                builder.append("; ");
                builder.append(entry.getKey());
                builder.append("=");
                String value = entry.getValue();
                if (SPECIAL_OR_WHITESPACE.matcher(value).find()) {
                    builder.append('"');
                    builder.append(SPECIAL.matcher(value).replaceAll("\\\\$0"));
                    builder.append('"');
                } else {
                    builder.append(value);
                }
            }

            this.string = builder.toString();
        }
    }

    private MediaType(String string, int slash) {
        assert slash != -1;
        assert string.charAt(slash) == '/';
        assert isSimpleName(string.substring(0, slash));
        assert isSimpleName(string.substring(slash + 1));
        this.string = string;
    }

    @Override
    public String toString() {
        return string;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof MediaType) {
            MediaType that = (MediaType) object;
            return string.equals(that.string);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return string.hashCode();
    }

    public int compareTo(@NonNull MediaType that) {
        return string.compareTo(that.string);
    }

}
