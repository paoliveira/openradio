/*
 * Copyright 2016 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;
import android.util.Log;

import com.yuriy.openradio.R;
import com.yuriy.openradio.api.RadioStationVO;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/4/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class MediaItemHelper {

    private static final String CLASS_NAME = MediaItemHelper.class.getSimpleName();

    private static final String KEY_IS_FAVORITE = "KEY_IS_FAVORITE";

    private static final String KEY_IS_LOCAL = "KEY_IS_LOCAL";

    private static final String KEY_CURRENT_STREAM_TITLE = "CURRENT_STREAM_TITLE";

    public static final String CUSTOM_METADATA_TRACK_SOURCE = "__SOURCE__";

    /**
     * Sets key that indicates Radio Station is in favorites.
     *
     * @param mediaItem  {@link MediaBrowserCompat.MediaItem}.
     * @param isFavorite Whether Item is in Favorites.
     */
    public static void updateFavoriteField(final MediaBrowserCompat.MediaItem mediaItem,
                                           final boolean isFavorite) {
        if (mediaItem == null) {
            return;
        }
        final MediaDescriptionCompat mediaDescription = mediaItem.getDescription();
        final Bundle bundle = mediaDescription.getExtras();
        if (bundle == null) {
            return;
        }
        bundle.putBoolean(KEY_IS_FAVORITE, isFavorite);
    }

    /**
     * Sets key that indicates Radio Station is in Local Radio Stations.
     *
     * @param mediaItem  {@link MediaBrowserCompat.MediaItem}.
     * @param isLocal     Whether Item is in Local Radio Stations.
     */
    public static void updateLocalRadioStationField(final MediaBrowserCompat.MediaItem mediaItem,
                                                    final boolean isLocal) {
        if (mediaItem == null) {
            return;
        }
        final MediaDescriptionCompat mediaDescription = mediaItem.getDescription();
        final Bundle bundle = mediaDescription.getExtras();
        if (bundle == null) {
            return;
        }
        bundle.putBoolean(KEY_IS_LOCAL, isLocal);
    }

    /**
     *
     *
     * @param mediaItem   {@link MediaBrowserCompat.MediaItem}.
     * @param streamTitle
     */
    public static void updateCurrentStreamTitleField(final MediaBrowserCompat.MediaItem mediaItem,
                                                     final String streamTitle) {
        if (mediaItem == null) {
            return;
        }
        final MediaDescriptionCompat mediaDescription = mediaItem.getDescription();
        final Bundle bundle = mediaDescription.getExtras();
        if (bundle == null) {
            return;
        }
        bundle.putString(KEY_CURRENT_STREAM_TITLE, streamTitle);
    }

    /**
     * Gets {@code true} if Item is Favorite, {@code false} - otherwise.
     * @param mediaItem {@link MediaBrowserCompat.MediaItem}.
     * @return {@code true} if Item is Favorite, {@code false} - otherwise.
     */
    public static boolean isFavoriteField(final MediaBrowserCompat.MediaItem mediaItem) {
        if (mediaItem == null) {
            return false;
        }
        final MediaDescriptionCompat mediaDescription = mediaItem.getDescription();
        final Bundle bundle = mediaDescription.getExtras();
        return bundle != null && bundle.getBoolean(KEY_IS_FAVORITE, false);
    }

    /**
     * Gets {@code true} if Item is Local Radio Station, {@code false} - otherwise.
     * @param mediaItem {@link MediaBrowserCompat.MediaItem}.
     * @return {@code true} if Item is Favorite, {@code false} - otherwise.
     */
    public static boolean isLocalRadioStationField(final MediaBrowserCompat.MediaItem mediaItem) {
        if (mediaItem == null) {
            return false;
        }
        final MediaDescriptionCompat mediaDescription = mediaItem.getDescription();
        final Bundle bundle = mediaDescription.getExtras();
        return bundle != null && bundle.getBoolean(KEY_IS_LOCAL, false);
    }

    /**
     *
     */
    public static String getCurrentStreamTitleField(final MediaBrowserCompat.MediaItem mediaItem) {
        if (mediaItem == null) {
            return "";
        }
        final MediaDescriptionCompat mediaDescription = mediaItem.getDescription();
        final Bundle bundle = mediaDescription.getExtras();
        if (bundle == null) {
            return "";
        }
        return bundle.getString(KEY_CURRENT_STREAM_TITLE, "");
    }

    /**
     * Build {@link android.media.MediaMetadata} from provided
     * {@link com.yuriy.openradio.api.RadioStationVO}.
     *
     * @param context      Context of the callee.
     * @param radioStation {@link com.yuriy.openradio.api.RadioStationVO}.
     *
     * @return {@link android.media.MediaMetadata}
     */
    public static MediaMetadataCompat buildMediaMetadataFromRadioStation(final Context context,
                                                                   final RadioStationVO radioStation) {
        return buildMediaMetadataFromRadioStation(context, radioStation, null);
    }

    /**
     * Build {@link android.media.MediaMetadata} from provided
     * {@link com.yuriy.openradio.api.RadioStationVO}.
     *
     * @param context      Context of the callee.
     * @param radioStation {@link com.yuriy.openradio.api.RadioStationVO}.
     * @param streamTitle  Title of the current stream.
     *
     * @return {@link android.media.MediaMetadata}
     */
    public static MediaMetadataCompat buildMediaMetadataFromRadioStation(final Context context,
                                                                         final RadioStationVO radioStation,
                                                                         final String streamTitle) {

        if (radioStation == null) {
            return null;
        }
        String iconUrl = "android.resource://" +
                context.getPackageName() + "/drawable/radio_station_alpha_bg";
        if (radioStation.getImageUrl() != null && !radioStation.getImageUrl().isEmpty()
                && !radioStation.getImageUrl().equalsIgnoreCase("null")) {
            iconUrl = radioStation.getImageUrl();
        }

        final String title = radioStation.getName();
        //final String album = radioStation.getString(JSON_ALBUM);
        final String artist = radioStation.getCountry();
        final String genre = radioStation.getGenre();
        final String source = radioStation.getStreamURL();
        //final String iconUrl = radioStation.getString(JSON_IMAGE);
        //final int trackNumber = radioStation.getInt(JSON_TRACK_NUMBER);
        //final int totalTrackCount = radioStation.getInt(JSON_TOTAL_TRACK_COUNT);
        //final int duration = radioStation.getInt(JSON_DURATION) * 1000; // ms
        final String id = String.valueOf(radioStation.getId());
        String subTitle = streamTitle;
        if (TextUtils.isEmpty(subTitle)) {
            subTitle = artist;
        }

        Log.d(CLASS_NAME, "Media Metadata for " + radioStation);

        // Adding the music source to the MediaMetadata (and consequently using it in the
        // mediaSession.setMetadata) is not a good idea for a real world music app, because
        // the session metadata can be accessed by notification listeners. This is done in this
        // sample for convenience only.
        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                .putString(CUSTOM_METADATA_TRACK_SOURCE, source)
                        //.putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, subTitle)
                        //.putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
                        //.putString(MediaMetadata.METADATA_KEY_ALBUM_ART, radioStation.getThumbUrl())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, iconUrl)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, subTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, subTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, subTitle)
                        //.putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, trackNumber)
                        //.putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, totalTrackCount)
                .build();
    }

    /**
     *  Build {@link MediaDescriptionCompat} from provided
     * {@link com.yuriy.openradio.api.RadioStationVO}.
     *
     * @param context      Context of the callee.
     * @param radioStation {@link com.yuriy.openradio.api.RadioStationVO}.
     * @return {@link MediaDescriptionCompat}
     */
    public static MediaDescriptionCompat buildMediaDescriptionFromRadioStation(final Context context,
                                                                         final RadioStationVO radioStation) {
        String iconUrl = "android.resource://" +
                context.getPackageName() + "/drawable/radio_station_alpha_bg";
        if (radioStation.getImageUrl() != null && !radioStation.getImageUrl().isEmpty()
                && !radioStation.getImageUrl().equalsIgnoreCase("null")) {
            iconUrl = radioStation.getImageUrl();
        }

        final String title = radioStation.getName();
        final String country = radioStation.getCountry();
        final String genre = radioStation.getGenre();
        final String id = String.valueOf(radioStation.getId());
        final Bundle bundle = new Bundle();

        Log.d(CLASS_NAME, "Media Description for " + radioStation);

        return new MediaDescriptionCompat.Builder()
                .setDescription(genre)
                .setMediaId(id)
                .setTitle(title)
                .setSubtitle(country)
                .setExtras(bundle)
                .setIconUri(Uri.parse(iconUrl))
                .build();
    }

    /**
     * Create {@link MediaMetadataCompat} for the empty category.
     *
     * @param context Context of the callee.
     * @return Object of the {@link MediaMetadataCompat} type.
     */
    public static MediaMetadataCompat buildMediaMetadataForEmptyCategory(final Context context,
                                                                         final String parentId) {

        final String iconUrl = "android.resource://" +
                context.getPackageName() + "/drawable/ic_radio_station_empty";

        final String title = context.getString(R.string.category_empty);
        final String artist = "";
        final String genre = "";
        final String source = "";

        // Adding the music source to the MediaMetadata (and consequently using it in the
        // mediaSession.setMetadata) is not a good idea for a real world music app, because
        // the session metadata can be accessed by notification listeners. This is done in this
        // sample for convenience only.
        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, parentId)
                .putString(CUSTOM_METADATA_TRACK_SOURCE, source)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, iconUrl)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .build();
    }
}
