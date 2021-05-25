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
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.utils.AppLogger.e
import com.yuriy.openradio.shared.vo.MediaItemListEnded
import com.yuriy.openradio.shared.vo.RadioStation
import java.util.*

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

    private fun updateBitrateField(bundle: Bundle, bitrate: Int) {
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
     * @param mediaItem    [MediaBrowserCompat.MediaItem].
     * @param isLastPlayed Whether Media Item is known last played.
     */
    private fun updateLastPlayedField(bundle: Bundle, isLastPlayed: Boolean) {
        bundle.putBoolean(KEY_IS_LAST_PLAYED, isLastPlayed)
    }

    /**
     * Sets key that indicates Radio Station is in favorites.
     *
     * @param mediaItem  [MediaBrowserCompat.MediaItem].
     * @param isFavorite Whether Item is in Favorites.
     */
    fun updateFavoriteField(mediaItem: MediaBrowserCompat.MediaItem?,
                            isFavorite: Boolean) {
        if (mediaItem == null) {
            return
        }
        val mediaDescription = mediaItem.description
        val bundle = mediaDescription.extras ?: return
        updateFavoriteField(bundle, isFavorite)
    }

    private fun updateFavoriteField(bundle: Bundle, isFavorite: Boolean) {
        bundle.putBoolean(KEY_IS_FAVORITE, isFavorite)
    }

    /**
     * Sets key that indicates Radio Station is in Local Radio Stations.
     *
     * @param mediaItem [MediaBrowserCompat.MediaItem].
     * @param isLocal   Whether Item is in Local Radio Stations.
     */
    private fun updateLocalRadioStationField(bundle: Bundle, isLocal: Boolean) {
        bundle.putBoolean(KEY_IS_LOCAL, isLocal)
    }

    /**
     * @param mediaItem
     * @param sortId
     */
    private fun updateSortIdField(bundle: Bundle, sortId: Int) {
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
    @JvmStatic
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
    @JvmOverloads
    fun metadataFromRadioStation(context: Context?,
                                 radioStation: RadioStation?,
                                 streamTitle: String? = null): MediaMetadataCompat? {
        if (radioStation == null) {
            return null
        }
        var iconUrl = AppUtils.EMPTY_STRING
        if (radioStation.imageUrl.isNotEmpty() && !radioStation.imageUrl.equals("null", ignoreCase = true)) {
            iconUrl = radioStation.imageUrl
        }
        val title = radioStation.name
        var artist = streamTitle
        val genre = radioStation.genre
        val source = radioStation.mediaStream.getVariant(0)!!.url
        val id = radioStation.id
        val album = radioStation.country

        if (artist.isNullOrEmpty()) {
            artist = context?.getString(R.string.media_description_default)
        }

        // Adding the music source to the MediaMetadata (and consequently using it in the
        // mediaSession.setMetadata) is not a good idea for a real world music app, because
        // the session metadata can be accessed by notification listeners. This is done in this
        // sample for convenience only.
        val mediaMetadataCompat = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, source)
            .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, iconUrl)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, iconUrl)
            // This is the way information display on Android Auto screen:
            // DisplayTitle
            // Artist
            // Album
            // METADATA_KEY_DISPLAY_TITLE is used to indicate whether METADATA_KEY_DISPLAY_DESCRIPTION
            // needs to be parsed as description.
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, iconUrl)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, streamTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, streamTitle)
            .build()

        // Info: There is no other way to set custom values in the description's bundle ...
        // Use reflection to do this.
        val description = mediaMetadataCompat.description
        var extras = description.extras
        if (extras == null) {
            extras = Bundle()
            updateExtras(description, extras)
        }
        setDrawableId(extras, R.drawable.ic_radio_station_empty)
        extras.putInt(KEY_SORT_ID, radioStation.sortId)
        return mediaMetadataCompat
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
        val descChars = value.description ?: return defaultValue
        var result = descChars.toString()
        if (result.isNotEmpty()) {
            return result
        }
        val subTitleChars = value.subtitle ?: return defaultValue
        result = subTitleChars.toString()
        if (result.isNotEmpty()) {
            return result
        }
        if (value.extras != null) {
            result = value.extras!!.getString(MediaMetadataCompat.METADATA_KEY_ARTIST, defaultValue)
        }
        return if (result.isNotEmpty()) {
            result
        } else defaultValue
    }

    /**
     * Updates extras field of the Media Description object using reflection.
     *
     * @param description Media description to update extras field on.
     * @param extras      Extras field to apply to provided Media Description.
     */
    private fun updateExtras(description: MediaDescriptionCompat,
                             extras: Bundle?) {
        val clazz: Class<*> = description.javaClass
        try {
            val field = clazz.getDeclaredField("mExtras")
            field.isAccessible = true
            field[description] = extras
        } catch (e: Exception) {
            e("Can not set bundles to description:$e")
        }
    }

    /**
     * Build [MediaDescriptionCompat] from provided
     * [RadioStation].
     *
     * @param radioStation [RadioStation].
     * @return [MediaDescriptionCompat]
     */
    fun buildMediaDescriptionFromRadioStation(radioStation: RadioStation,
                                              sortId: Int = MediaSessionCompat.QueueItem.UNKNOWN_ID,
                                              isFavorite: Boolean = false,
                                              isLocal: Boolean = false,
                                              isUpdateLastPlayedField: Boolean = false): MediaDescriptionCompat {
        var iconUrl = AppUtils.EMPTY_STRING
        if (radioStation.imageUrl.isNotEmpty() && !radioStation.imageUrl.equals("null", ignoreCase = true)) {
            iconUrl = radioStation.imageUrl
        }
        val title = radioStation.name
        val country = radioStation.country
        val genre = radioStation.genre
        val id = radioStation.id
        val bundle = Bundle()
        updateBitrateField(bundle, radioStation.mediaStream.getVariant(0)!!.bitrate)
        updateFavoriteField(bundle, isFavorite)
        updateSortIdField(bundle, sortId)
        updateLocalRadioStationField(bundle, isLocal)
        updateLastPlayedField(bundle, isUpdateLastPlayedField)
        setDrawableId(bundle, R.drawable.ic_radio_station_empty)
        return MediaDescriptionCompat.Builder()
            .setDescription(genre)
            .setMediaId(id)
            .setTitle(title)
            .setSubtitle(country)
            .setExtras(bundle)
            .setIconUri(Uri.parse(iconUrl))
            .build()
    }

    /**
     * Create [MediaMetadataCompat] for the empty category.
     *
     * @param context Context of the callee.
     * @return Object of the [MediaMetadataCompat] type.
     */
    fun buildMediaMetadataForEmptyCategory(context: Context,
                                           parentId: String?): MediaMetadataCompat {
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

    /**
     * @return
     */
    fun createListEndedResult(): List<MediaBrowserCompat.MediaItem> {
        return ArrayList<MediaBrowserCompat.MediaItem>(listOf(MediaItemListEnded()))
    }

    /**
     * @param list
     * @return
     */
    fun isEndOfList(list: List<MediaBrowserCompat.MediaItem?>?): Boolean {
        return (list == null
            || list.size == 1
            && (list[0] == null || list[0] is MediaItemListEnded))
    }

    fun playbackStateToString(state: PlaybackStateCompat?): String {
        return if (state == null) {
            "UNDEFINED"
        } else playbackStateToString(state.state)
    }

    fun playbackStateToString(state: Int): String {
        return when (state) {
            PlaybackStateCompat.STATE_STOPPED -> "STOPPED"
            PlaybackStateCompat.STATE_PAUSED -> "PAUSED"
            PlaybackStateCompat.STATE_PLAYING -> "PLAYING"
            PlaybackStateCompat.STATE_FAST_FORWARDING -> "FAST_FORWARDING"
            PlaybackStateCompat.STATE_REWINDING -> "REWINDING"
            PlaybackStateCompat.STATE_BUFFERING -> "BUFFERING"
            PlaybackStateCompat.STATE_ERROR -> "ERROR"
            PlaybackStateCompat.STATE_CONNECTING -> "CONNECTING"
            PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS -> "SKIPPING_TO_PREVIOUS"
            PlaybackStateCompat.STATE_SKIPPING_TO_NEXT -> "SKIPPING_TO_NEXT"
            PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM -> "SKIPPING_TO_QUEUE_ITEM"
            PlaybackStateCompat.STATE_NONE -> "NONE"
            else -> "UNDEFINED{$state}"
        }
    }
}
