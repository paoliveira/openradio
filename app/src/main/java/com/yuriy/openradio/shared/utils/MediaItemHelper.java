/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.vo.MediaItemListEnded;
import com.yuriy.openradio.shared.vo.RadioStation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/4/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class MediaItemHelper {

    private static int DRAWABLE_ID_UNDEFINED = -1;
    private static final String KEY_IS_FAVORITE = "KEY_IS_FAVORITE";
    private static final String KEY_IS_LAST_PLAYED = "KEY_IS_LAST_PLAYED";
    private static final String KEY_IS_LOCAL = "KEY_IS_LOCAL";
    private static final String KEY_SORT_ID = "KEY_SORT_ID";
    private static final String KEY_CURRENT_STREAM_TITLE = "CURRENT_STREAM_TITLE";
    private static final String KEY_BITRATE = "KEY_BITRATE";
    private static final String KEY_DRAWABLE_ID = "DRAWABLE_ID";

    /**
     * Default constructor.
     */
    private MediaItemHelper() {
        super();
    }

    public static void setDrawableId(@NonNull final Bundle bundle, final int drawableId) {
        bundle.putInt(KEY_DRAWABLE_ID, drawableId);
    }

    public static int getDrawableId(@Nullable final Bundle bundle) {
        if (bundle == null) {
            return DRAWABLE_ID_UNDEFINED;
        }
        return bundle.getInt(KEY_DRAWABLE_ID, DRAWABLE_ID_UNDEFINED);
    }

    public static boolean isDrawableIdValid(final int drawableId) {
        return drawableId != DRAWABLE_ID_UNDEFINED;
    }

    public static void updateBitrateField(@NonNull final Bundle bundle, final int bitrate) {
        bundle.putInt(KEY_BITRATE, bitrate);
    }

    public static int getBitrateField(final MediaBrowserCompat.MediaItem mediaItem) {
        if (mediaItem == null) {
            return 0;
        }
        final MediaDescriptionCompat mediaDescription = mediaItem.getDescription();
        final Bundle bundle = mediaDescription.getExtras();
        return bundle != null ? bundle.getInt(KEY_BITRATE, 0) : 0;
    }

    /**
     * Sets key that indicates Radio Station is in favorites.
     *
     * @param mediaItem    {@link MediaBrowserCompat.MediaItem}.
     * @param isLastPlayed Whether Media Item is known last played.
     */
    public static void updateLastPlayedField(final MediaBrowserCompat.MediaItem mediaItem,
                                             final boolean isLastPlayed) {
        if (mediaItem == null) {
            return;
        }
        final MediaDescriptionCompat mediaDescription = mediaItem.getDescription();
        final Bundle bundle = mediaDescription.getExtras();
        if (bundle == null) {
            return;
        }
        bundle.putBoolean(KEY_IS_LAST_PLAYED, isLastPlayed);
    }

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
     * @param mediaItem {@link MediaBrowserCompat.MediaItem}.
     * @param isLocal   Whether Item is in Local Radio Stations.
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
     * @param mediaItem
     * @param sortId
     */
    public static void updateSortIdField(final MediaBrowserCompat.MediaItem mediaItem,
                                         final int sortId) {
        if (mediaItem == null) {
            return;
        }
        final MediaDescriptionCompat mediaDescription = mediaItem.getDescription();
        final Bundle bundle = mediaDescription.getExtras();
        if (bundle == null) {
            return;
        }
        bundle.putInt(KEY_SORT_ID, sortId);
    }

    /**
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
     * Gets {@code true} if Media Item is known last played, {@code false} - otherwise.
     *
     * @param mediaItem {@link MediaBrowserCompat.MediaItem}.
     * @return {@code true} if Media Item is known last played, {@code false} - otherwise.
     */
    public static boolean isLastPlayedField(final MediaBrowserCompat.MediaItem mediaItem) {
        if (mediaItem == null) {
            return false;
        }
        final MediaDescriptionCompat mediaDescription = mediaItem.getDescription();
        final Bundle bundle = mediaDescription.getExtras();
        return bundle != null && bundle.getBoolean(KEY_IS_LAST_PLAYED, false);
    }

    /**
     * Gets {@code true} if Item is Favorite, {@code false} - otherwise.
     *
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
     *
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
     * Extracts Sort Id field from the {@link MediaBrowserCompat.MediaItem}.
     *
     * @param mediaItem {@link MediaBrowserCompat.MediaItem} to extract
     *                  Sort Id from.
     * @return Extracted Sort Id or -1.
     */
    public static int getSortIdField(final MediaBrowserCompat.MediaItem mediaItem) {
        if (mediaItem == null) {
            return -1;
        }
        return getSortIdField(mediaItem.getDescription());
    }

    /**
     * Extracts Sort Id field from the {@link MediaSessionCompat.QueueItem}.
     *
     * @param queueItem {@link MediaSessionCompat.QueueItem} to extract
     *                  Sort Id from.
     * @return Extracted Sort Id or -1.
     */
    public static int getSortIdField(final MediaSessionCompat.QueueItem queueItem) {
        if (queueItem == null) {
            return -1;
        }
        return getSortIdField(queueItem.getDescription());
    }

    /**
     * Extracts Sort Id field from the {@link MediaDescriptionCompat}.
     *
     * @param mediaDescription {@link MediaDescriptionCompat} to extract Media Id from.
     * @return Extracted Sort Id or -1.
     */
    private static int getSortIdField(final MediaDescriptionCompat mediaDescription) {
        if (mediaDescription == null) {
            return -1;
        }
        final Bundle bundle = mediaDescription.getExtras();
        if (bundle != null) {
            return bundle.getInt(KEY_SORT_ID, -1);
        }
        return -1;
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
     * {@link RadioStation}.
     *
     * @param radioStation {@link RadioStation}.
     * @return {@link android.media.MediaMetadata}
     */
    public static MediaMetadataCompat metadataFromRadioStation(final Context context,
                                                               final RadioStation radioStation) {
        return metadataFromRadioStation(context, radioStation, null);
    }

    /**
     * Build {@link android.media.MediaMetadata} from provided
     * {@link RadioStation}.
     *
     * @param radioStation {@link RadioStation}.
     * @param streamTitle  Title of the current stream.
     * @return {@link android.media.MediaMetadata}
     */
    public static MediaMetadataCompat metadataFromRadioStation(final Context context,
                                                               final RadioStation radioStation,
                                                               @Nullable final String streamTitle) {

        if (radioStation == null) {
            return null;
        }
        String iconUrl = "";
        if (radioStation.getImageUrl() != null && !radioStation.getImageUrl().isEmpty()
                && !radioStation.getImageUrl().equalsIgnoreCase("null")) {
            iconUrl = radioStation.getImageUrl();
        }

        final String title = radioStation.getName();
        final String artist = radioStation.getCountry();
        final String genre = radioStation.getGenre();
        final String source = radioStation.getMediaStream().getVariant(0).getUrl();
        final String id = radioStation.getId();
        String subTitle = TextUtils.isEmpty(streamTitle) ? radioStation.getCountry() : streamTitle;
        if (TextUtils.isEmpty(subTitle)) {
            subTitle = artist;
        }

        // Adding the music source to the MediaMetadata (and consequently using it in the
        // mediaSession.setMetadata) is not a good idea for a real world music app, because
        // the session metadata can be accessed by notification listeners. This is done in this
        // sample for convenience only.
        final MediaMetadataCompat mediaMetadataCompat = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, source)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, subTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
                .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, iconUrl)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, iconUrl)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, subTitle)
                .build();

        // Info: There is no other way to set custom values in the description's bundle ...
        // Use reflection to do this.
        final MediaDescriptionCompat description = mediaMetadataCompat.getDescription();
        Bundle extras = description.getExtras();
        if (extras == null) {
            extras = new Bundle();
            updateExtras(description, extras);
        }
        setDrawableId(extras, R.drawable.ic_radio_station_empty);
        extras.putInt(KEY_SORT_ID, radioStation.getSortId());

        return mediaMetadataCompat;
    }

    /**
     * Updates extras field of the Media Description object using reflection.
     *
     * @param description Media description to update extras field on.
     * @param extras      Extras field to apply to provided Media Description.
     */
    public static void updateExtras(final MediaDescriptionCompat description,
                                    final Bundle extras) {
        final Class<?> clazz = description.getClass();
        if (clazz != null) {
            try {
                final Field field = clazz.getDeclaredField("mExtras");
                field.setAccessible(true);
                field.set(description, extras);
            } catch (final Exception e) {
                AppLogger.e("Can not set bundles to description:" + e);
            }
        }
    }

    /**
     * Build {@link MediaDescriptionCompat} from provided
     * {@link RadioStation}.
     *
     * @param radioStation {@link RadioStation}.
     * @return {@link MediaDescriptionCompat}
     */
    public static MediaDescriptionCompat buildMediaDescriptionFromRadioStation(final Context context,
                                                                               final RadioStation radioStation) {
        String iconUrl = "";
        if (radioStation.getImageUrl() != null && !radioStation.getImageUrl().isEmpty()
                && !radioStation.getImageUrl().equalsIgnoreCase("null")) {
            iconUrl = radioStation.getImageUrl();
        }

        final String title = radioStation.getName();
        final String country = radioStation.getCountry();
        final String genre = radioStation.getGenre();
        final String id = radioStation.getId();
        final Bundle bundle = new Bundle();

        updateBitrateField(bundle, radioStation.getMediaStream().getVariant(0).getBitrate());
        setDrawableId(bundle, R.drawable.ic_radio_station_empty);

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

        final String title = context.getString(R.string.category_empty);
        final String artist = "";
        final String genre = "";
        final String source = "";

        // Adding the music source to the MediaMetadata (and consequently using it in the
        // mediaSession.setMetadata) is not a good idea for a real world music app, because
        // the session metadata can be accessed by notification listeners. This is done in this
        // sample for convenience only.

        final MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, parentId)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, source)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .build();
        setDrawableId(metadata.getBundle(), R.drawable.ic_radio_station_empty);
        return metadata;
    }

    /**
     * @return
     */
    public static List<MediaBrowserCompat.MediaItem> createListEndedResult() {
        return new ArrayList<>(Collections.singletonList(new MediaItemListEnded()));
    }

    /**
     * @param list
     * @return
     */
    public static boolean isEndOfList(final List<MediaBrowserCompat.MediaItem> list) {
        return list == null
                || list.size() == 1
                && (list.get(0) == null || list.get(0) instanceof MediaItemListEnded);
    }

    public static String playbackStateToString(@Nullable final PlaybackStateCompat state) {
        if (state == null) {
            return "UNDEFINED";
        }
        return playbackStateToString(state.getState());
    }

    public static String playbackStateToString(final int state) {
        switch (state) {
            case PlaybackStateCompat.STATE_STOPPED:
                return "STOPPED";
            case PlaybackStateCompat.STATE_PAUSED:
                return "PAUSED";
            case PlaybackStateCompat.STATE_PLAYING:
                return "PLAYING";
            case PlaybackStateCompat.STATE_FAST_FORWARDING:
                return "FAST_FORWARDING";
            case PlaybackStateCompat.STATE_REWINDING:
                return "REWINDING";
            case PlaybackStateCompat.STATE_BUFFERING:
                return "BUFFERING";
            case PlaybackStateCompat.STATE_ERROR:
                return "ERROR";
            case PlaybackStateCompat.STATE_CONNECTING:
                return "CONNECTING";
            case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                return "SKIPPING_TO_PREVIOUS";
            case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                return "SKIPPING_TO_NEXT";
            case PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM:
                return "SKIPPING_TO_QUEUE_ITEM";
            case PlaybackStateCompat.STATE_NONE:
                return "NONE";
            default:
                return "UNDEFINED{" + state + "}";
        }
    }
}
