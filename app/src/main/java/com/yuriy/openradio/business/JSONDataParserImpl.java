/*
 * Copyright 2014 The "Open Radio" Project. Author: Chernyshov Yuriy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yuriy.openradio.business;

import android.content.Context;
import android.media.MediaMetadata;
import android.util.Log;

import com.yuriy.openradio.api.RadioStationVO;

import org.json.JSONException;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */


public class JSONDataParserImpl implements DataParser {

    private static final String CLASS_NAME = JSONDataParserImpl.class.getSimpleName();

    /**
     * JSON Keys
     */

    public static final String KEY_ID = "id";

    public static final String KEY_NAME = "name";

    public static final String KEY_DESCRIPTION = "description";

    public static final String KEY_AMOUNT = "amount";

    public static final String KEY_STREAM_URL = "streamurl";

    public static final String KEY_COUNTRY = "country";

    public static final String KEY_BIT_RATE = "bitrate";

    public static final String KEY_STATUS = "status";

    public static final String KEY_ADDED = "added";

    public static final String KEY_URL_ID = "urlid";

    public static final String KEY_WEBSITE = "website";

    public static final String KEY_SONG_HISTORY = "songhistory";

    public static final String KEY_TITLE = "title";

    public static final String KEY_TIME = "time";

    public static final String KEY_DIRECTORY = "directory";

    public static final String CUSTOM_METADATA_TRACK_SOURCE = "__SOURCE__";

    /**
     * Build {@link android.media.MediaMetadata} from provided
     * {@link com.yuriy.openradio.api.RadioStationVO}.
     *
     * @param radioStation {@link com.yuriy.openradio.api.RadioStationVO}.
     * @return {@link android.media.MediaMetadata}
     */
    public static MediaMetadata buildMediaMetadataFromRadioStation(
            final Context context,
            final RadioStationVO radioStation)
            throws JSONException {

        final String iconUrl = "android.resource://" +
                context.getPackageName() + "/drawable/ic_radio_station";

        final String title = radioStation.getName();
        //final String album = radioStation.getString(JSON_ALBUM);
        //final String artist = radioStation.getString(JSON_ARTIST);
        //final String genre = radioStation.getString(JSON_GENRE);
        final String source = radioStation.getStreamURL();
        //final String iconUrl = radioStation.getString(JSON_IMAGE);
        //final int trackNumber = radioStation.getInt(JSON_TRACK_NUMBER);
        //final int totalTrackCount = radioStation.getInt(JSON_TOTAL_TRACK_COUNT);
        //final int duration = radioStation.getInt(JSON_DURATION) * 1000; // ms
        final String id = String.valueOf(radioStation.getId());

        Log.d(CLASS_NAME, "Media Metadata for " + radioStation);

        // Adding the music source to the MediaMetadata (and consequently using it in the
        // mediaSession.setMetadata) is not a good idea for a real world music app, because
        // the session metadata can be accessed by notification listeners. This is done in this
        // sample for convenience only.
        return new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, id)
                .putString(CUSTOM_METADATA_TRACK_SOURCE, source)
                //.putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                //.putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                //.putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
                //.putString(MediaMetadata.METADATA_KEY_GENRE, genre)
                .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, iconUrl)
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                //.putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, trackNumber)
                //.putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, totalTrackCount)
                .build();
    }
}
