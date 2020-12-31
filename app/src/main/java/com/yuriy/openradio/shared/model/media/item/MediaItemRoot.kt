/*
 * Copyright 2015-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.model.media.item

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.text.TextUtils
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.model.media.item.MediaItemCommand.IUpdatePlaybackState
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import com.yuriy.openradio.shared.model.storage.FavoritesStorage
import com.yuriy.openradio.shared.model.storage.LatestRadioStationStorage
import com.yuriy.openradio.shared.model.storage.LocalRadioStationsStorage
import com.yuriy.openradio.shared.service.LocationService
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.MediaIdHelper
import com.yuriy.openradio.shared.utils.MediaItemHelper.buildMediaDescriptionFromRadioStation
import com.yuriy.openradio.shared.utils.MediaItemHelper.setDrawableId
import com.yuriy.openradio.shared.utils.MediaItemHelper.updateFavoriteField
import com.yuriy.openradio.shared.utils.MediaItemHelper.updateLastPlayedField
import com.yuriy.openradio.shared.vo.RadioStation

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * [MediaItemRoot] is concrete implementation of the [MediaItemCommand] that
 * designed to prepare data to display root menu items.
 */
class MediaItemRoot : MediaItemCommand {
    override fun execute(playbackStateListener: IUpdatePlaybackState?, dependencies: MediaItemCommandDependencies) {
        AppLogger.d("$LOG_TAG invoked")
        val context = dependencies.context
        dependencies.radioStationsStorage.clear()
        dependencies.result.detach()

        // TODO: Refactor these to factory.

        // Get lat known Radio Station.
        // If this feature disabled by Settings - return null, in this case all consecutive UI views will not be
        // exposed.
        val latestRadioStation: RadioStation?
        if (AppPreferencesManager.lastKnownRadioStationEnabled(context)) {
            latestRadioStation = LatestRadioStationStorage.get(dependencies.context)
            if (latestRadioStation != null) {
                dependencies.radioStationsStorage.add(latestRadioStation)
                // Add Radio Station to Menu
                val mediaItem = MediaBrowserCompat.MediaItem(
                        buildMediaDescriptionFromRadioStation(
                                context, latestRadioStation
                        ),
                        MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                )
                updateFavoriteField(
                        mediaItem, FavoritesStorage.isFavorite(latestRadioStation, dependencies.context)
                )
                updateLastPlayedField(mediaItem, true)
                // In case of Android Auto, display latest played Radio Station on top of Menu.
                if (dependencies.isAndroidAuto) {
                    dependencies.addMediaItem(mediaItem)
                }
            }
        }

        // Show Favorites if they are exists.
        if (!FavoritesStorage.isFavoritesEmpty(context)) {
            // Favorites list
            val builder = MediaDescriptionCompat.Builder()
                    .setMediaId(MediaIdHelper.MEDIA_ID_FAVORITES_LIST)
                    .setTitle(context.getString(R.string.favorites_list_title))
            val bundle = Bundle()
            setDrawableId(bundle, R.drawable.ic_stars_black_24dp)
            builder.setExtras(bundle)
            dependencies.addMediaItem(
                    MediaBrowserCompat.MediaItem(
                            builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    )
            )
        }

        // Recently added Radio Stations
        run {
            val builder = MediaDescriptionCompat.Builder()
                    .setMediaId(MediaIdHelper.MEDIA_ID_RECENT_ADDED_STATIONS)
                    .setTitle(context.getString(R.string.new_stations_title))
            val bundle = Bundle()
            setDrawableId(bundle, R.drawable.ic_fiber_new_black_24dp)
            builder.setExtras(bundle)
            dependencies.addMediaItem(
                    MediaBrowserCompat.MediaItem(
                            builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    )
            )
        }

        // Popular Radio Stations
        run {
            val builder = MediaDescriptionCompat.Builder()
                    .setMediaId(MediaIdHelper.MEDIA_ID_POPULAR_STATIONS)
                    .setTitle(context.getString(R.string.popular_stations_title))
            val bundle = Bundle()
            setDrawableId(bundle, R.drawable.ic_trending_up_black_24dp)
            builder.setExtras(bundle)
            dependencies.addMediaItem(
                    MediaBrowserCompat.MediaItem(
                            builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    )
            )
        }

        // Worldwide Stations
        val builder = MediaDescriptionCompat.Builder()
                .setMediaId(MediaIdHelper.MEDIA_ID_ALL_CATEGORIES)
                .setTitle(context.getString(R.string.all_categories_title))
        val bundle = Bundle()
        setDrawableId(bundle, R.drawable.ic_all_categories)
        builder.setExtras(bundle)
        dependencies.addMediaItem(
                MediaBrowserCompat.MediaItem(
                        builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                )
        )

        // All countries list
        val builderCounties = MediaDescriptionCompat.Builder()
                .setMediaId(MediaIdHelper.MEDIA_ID_COUNTRIES_LIST)
                .setTitle(context.getString(R.string.countries_list_title))
        val bundleCounties = Bundle()
        setDrawableId(bundleCounties, R.drawable.ic_public_black_24dp)
        builderCounties.setExtras(bundleCounties)
        dependencies.addMediaItem(
                MediaBrowserCompat.MediaItem(
                        builderCounties.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                )
        )

        // If the Country code is known:
        if (!TextUtils.isEmpty(dependencies.countryCode)) {
            val identifier = context.resources.getIdentifier(
                    "flag_" + dependencies.countryCode.toLowerCase(),
                    "drawable", context.packageName
            )
            val bundle1 = Bundle()
            setDrawableId(bundle1, identifier)
            dependencies.addMediaItem(
                    MediaBrowserCompat.MediaItem(
                            MediaDescriptionCompat.Builder()
                                    .setMediaId(MediaIdHelper.MEDIA_ID_COUNTRY_STATIONS)
                                    .setTitle(LocationService.COUNTRY_CODE_TO_NAME[dependencies.countryCode])
                                    .setExtras(bundle1)
                                    .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    )
            )
        }

        // Show Local Radio Stations if they are exists
        if (!LocalRadioStationsStorage.isLocalsEmpty(context)) {
            // Locals list
            val builder1 = MediaDescriptionCompat.Builder()
                    .setMediaId(MediaIdHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST)
                    .setTitle(context.getString(R.string.local_radio_stations_list_title))
            val bundle1 = Bundle()
            setDrawableId(bundle1, R.drawable.ic_locals)
            builder1.setExtras(bundle1)
            dependencies.addMediaItem(
                    MediaBrowserCompat.MediaItem(
                            builder1.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    )
            )
        }
        AppLogger.d("$LOG_TAG invocation completed")
        dependencies.result.sendResult(dependencies.mediaItems)
        dependencies.resultListener.onResult()
    }

    companion object {
        private val LOG_TAG = MediaItemRoot::class.java.simpleName
    }
}