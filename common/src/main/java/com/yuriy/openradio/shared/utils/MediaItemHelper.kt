/*
 * Copyright 2017-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.utils

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.vo.RadioStation
import com.yuriy.openradio.shared.vo.getStreamUrlFixed

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/4/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
object MediaItemHelper {

    private const val DRAWABLE_ID_UNDEFINED = MediaSessionCompat.QueueItem.UNKNOWN_ID
    private const val KEY_IS_FAVORITE = "KEY_IS_FAVORITE"
    private const val KEY_IS_LAST_PLAYED = "KEY_IS_LAST_PLAYED"
    private const val KEY_IS_LOCAL = "KEY_IS_LOCAL"
    private const val KEY_SORT_ID = "KEY_SORT_ID"
    private const val KEY_BITRATE = "KEY_BITRATE"
    private const val KEY_DRAWABLE_ID = "DRAWABLE_ID"

    fun setDrawableId(bundle: Bundle?, drawableId: Int) {
        if (bundle == null) {
            return
        }
        bundle.putInt(KEY_DRAWABLE_ID, drawableId)
    }

    fun getDrawableId(bundle: Bundle?): Int {
        return bundle?.getInt(KEY_DRAWABLE_ID, DRAWABLE_ID_UNDEFINED)
            ?: DRAWABLE_ID_UNDEFINED
    }

    fun isDrawableIdValid(drawableId: Int): Boolean {
        return drawableId != DRAWABLE_ID_UNDEFINED
    }

    fun updateBitrateField(bundle: Bundle, bitrate: Int) {
        bundle.putInt(KEY_BITRATE, bitrate)
    }

    fun getBitrateField(mediaItem: MediaBrowserCompat.MediaItem?): Int {
        if (mediaItem == null) {
            return 0
        }
        val mediaDescription = mediaItem.description
        val bundle = mediaDescription.extras
        return bundle?.getInt(KEY_BITRATE, 0) ?: 0
    }

    /**
     * Sets key that indicates Radio Station is in favorites.
     *
     * @param bundle
     * @param isLastPlayed Whether Media Item is known last played.
     */
    fun updateLastPlayedField(bundle: Bundle, isLastPlayed: Boolean) {
        bundle.putBoolean(KEY_IS_LAST_PLAYED, isLastPlayed)
    }

    /**
     * Sets key that indicates Radio Station is in favorites.
     *
     * @param mediaItem  [MediaBrowserCompat.MediaItem].
     * @param isFavorite Whether Item is in Favorites.
     */
    fun updateFavoriteField(
        mediaItem: MediaBrowserCompat.MediaItem?,
        isFavorite: Boolean
    ) {
        if (mediaItem == null) {
            return
        }
        val mediaDescription = mediaItem.description
        val bundle = mediaDescription.extras ?: return
        updateFavoriteField(bundle, isFavorite)
    }

    fun updateFavoriteField(bundle: Bundle, isFavorite: Boolean) {
        bundle.putBoolean(KEY_IS_FAVORITE, isFavorite)
    }

    /**
     * Sets key that indicates Radio Station is in Local Radio Stations.
     *
     * @param bundle
     * @param isLocal   Whether Item is in Local Radio Stations.
     */
    fun updateLocalRadioStationField(bundle: Bundle, isLocal: Boolean) {
        bundle.putBoolean(KEY_IS_LOCAL, isLocal)
    }

    fun updateSortIdField(bundle: Bundle, sortId: Int) {
        bundle.putInt(KEY_SORT_ID, sortId)
    }

    /**
     * Gets `true` if Item is Favorite, `false` - otherwise.
     *
     * @param mediaItem [MediaBrowserCompat.MediaItem].
     * @return `true` if Item is Favorite, `false` - otherwise.
     */
    fun isFavoriteField(mediaItem: MediaBrowserCompat.MediaItem?): Boolean {
        if (mediaItem == null) {
            return false
        }
        val mediaDescription = mediaItem.description
        val bundle = mediaDescription.extras
        return bundle != null && bundle.getBoolean(KEY_IS_FAVORITE, false)
    }

    /**
     * Extracts Sort Id field from the [MediaBrowserCompat.MediaItem].
     *
     * @param mediaItem [MediaBrowserCompat.MediaItem] to extract
     * Sort Id from.
     * @return Extracted Sort Id or -1.
     */
    fun getSortIdField(mediaItem: MediaBrowserCompat.MediaItem?): Int {
        return if (mediaItem == null) {
            MediaSessionCompat.QueueItem.UNKNOWN_ID
        } else getSortIdField(mediaItem.description)
    }

    /**
     * Extracts Sort Id field from the [MediaDescriptionCompat].
     *
     * @param mediaDescription [MediaDescriptionCompat] to extract Media Id from.
     * @return Extracted Sort Id or -1.
     */
    private fun getSortIdField(mediaDescription: MediaDescriptionCompat?): Int {
        if (mediaDescription == null) {
            return MediaSessionCompat.QueueItem.UNKNOWN_ID
        }
        val bundle = mediaDescription.extras
        return bundle?.getInt(KEY_SORT_ID, MediaSessionCompat.QueueItem.UNKNOWN_ID)
            ?: MediaSessionCompat.QueueItem.UNKNOWN_ID
    }

    /**
     * Build [android.media.MediaMetadata] from provided
     * [RadioStation].
     *
     * @param radioStation [RadioStation].
     * @return [android.media.MediaMetadata]
     */
    fun metadataFromRadioStation(
        context: Context,
        radioStation: RadioStation,
        streamTitle: String? = null
    ): MediaMetadataCompat {
        val title = radioStation.name
        var artist = streamTitle
        val genre = radioStation.genre
        val source = radioStation.getStreamUrlFixed()
        val id = radioStation.id
        val album = radioStation.country

        if (artist.isNullOrEmpty()) {
            artist = context.getString(R.string.media_description_default)
        }

        val imageUri = radioStation.imageUri.toString()

        // Adding the music source to the MediaMetadata (and consequently using it in the
        // mediaSession.setMetadata) is not a good idea for a real world music app, because
        // the session metadata can be accessed by notification listeners. This is done in this
        // sample for convenience only.
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, source)
            .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
            .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, imageUri)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, imageUri)
            // This is the way information display on Android Auto screen:
            // DisplayTitle
            // Artist
            // Album
            // METADATA_KEY_DISPLAY_TITLE is used to indicate whether METADATA_KEY_DISPLAY_DESCRIPTION
            // needs to be parsed as description.
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, imageUri)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, streamTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, streamTitle)
            .build()

        // Info: There is no other way to set custom values in the description's bundle ...
        // Use reflection to do this.
        val description = metadata.description
        var extras = description.extras
        if (extras == null) {
            extras = Bundle()
            updateExtras(description, extras)
        }
        setDrawableId(extras, R.drawable.ic_radio_station_empty)
        extras.putInt(KEY_SORT_ID, radioStation.sortId)
        return metadata
    }

    /**
     * Try to extract useful information from the media description. In good case - this is metadata associated
     * with the stream, subtitles otherwise, in worse case - default string.
     *
     * @param value Media description to parse.
     *
     * @return Display description.
     */
    fun getDisplayDescription(value: MediaDescriptionCompat, defaultValue: String): String {
        val descChars = value.subtitle ?: return defaultValue
        var result = descChars.toString()
        if (result.isNotEmpty()) {
            return result
        }
        val subTitleChars = value.description ?: return defaultValue
        result = subTitleChars.toString()
        if (result.isNotEmpty()) {
            return result
        }
        if (value.extras != null) {
            result = value.extras!!.getString(MediaMetadataCompat.METADATA_KEY_ARTIST, defaultValue)
        }
        return result.ifEmpty { defaultValue }
    }

    /**
     * Updates extras field of the Media Description object using reflection.
     *
     * @param description Media description to update extras field on.
     * @param extras      Extras field to apply to provided Media Description.
     */
    private fun updateExtras(
        description: MediaDescriptionCompat,
        extras: Bundle?
    ) {
        val clazz: Class<*> = description.javaClass
        try {
            val field = clazz.getDeclaredField("mExtras")
            field.isAccessible = true
            field[description] = extras
        } catch (e: Exception) {
            AppLogger.e("Can not set bundles to description", e)
        }
    }

    /**
     * Create [MediaMetadataCompat] for the empty category.
     *
     * @param context Context of the callee.
     * @return Object of the [MediaMetadataCompat] type.
     */
    fun buildMediaMetadataForEmptyCategory(
        context: Context,
        parentId: String?
    ): MediaMetadataCompat {
        val title = context.getString(R.string.category_empty)
        val artist = AppUtils.EMPTY_STRING
        val genre = AppUtils.EMPTY_STRING
        val source = AppUtils.EMPTY_STRING

        // Adding the music source to the MediaMetadata (and consequently using it in the
        // mediaSession.setMetadata) is not a good idea for a real world music app, because
        // the session metadata can be accessed by notification listeners. This is done in this
        // sample for convenience only.
        return MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, parentId)
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, source)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title) // Workaround to include bundles into build()
            .putLong(MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS, MediaDescriptionCompat.STATUS_NOT_DOWNLOADED)
            .build()
    }
}
