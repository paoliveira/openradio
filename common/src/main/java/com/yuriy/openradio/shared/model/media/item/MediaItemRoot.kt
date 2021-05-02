/*
 * Copyright 2015-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.model.media.item.MediaItemCommand.IUpdatePlaybackState
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import com.yuriy.openradio.shared.model.storage.FavoritesStorage
import com.yuriy.openradio.shared.model.storage.LatestRadioStationStorage
import com.yuriy.openradio.shared.model.storage.LocalRadioStationsStorage
import com.yuriy.openradio.shared.service.LocationService
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.MediaIdHelper
import com.yuriy.openradio.shared.utils.MediaItemHelper
import com.yuriy.openradio.shared.vo.RadioStation
import java.util.*

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

        var latestRadioStation: RadioStation? = null
        if (AppPreferencesManager.lastKnownRadioStationEnabled(context)) {
            latestRadioStation = LatestRadioStationStorage[dependencies.context]
            if (latestRadioStation != null) {
                dependencies.radioStationsStorage.add(latestRadioStation)
            }
        }

        // Show Favorites if they are exists.
        if (!FavoritesStorage.isFavoritesEmpty(context)) {

            // In case of Android Auto, fill storage with Favorites. It will allow to browse stations from steering
            // wheel while UI is in Home and root items will contain only directories.
            if (dependencies.isAndroidAuto) {
                val list = FavoritesStorage.getAll(context)
                if (latestRadioStation != null) {
                    for (item in list) {
                        // If latest known station is in favorites list, remove it (it is in the list already).
                        // Else, just append favorites to the last known.
                        if (item.id == latestRadioStation.id) {
                            dependencies.radioStationsStorage.clear()
                            break
                        }
                    }
                }
                Collections.sort(list, dependencies.mRadioStationsComparator)
                dependencies.radioStationsStorage.addAll(list)
            }

            // Favorites list
            val builder = MediaDescriptionCompat.Builder()
                .setMediaId(MediaIdHelper.MEDIA_ID_FAVORITES_LIST)
                .setTitle(context.getString(R.string.favorites_list_title))
            val bundle = Bundle()
            MediaItemHelper.setDrawableId(bundle, R.drawable.ic_stars_black_24dp)
            builder.setExtras(bundle)
            dependencies.addMediaItem(
                MediaBrowserCompat.MediaItem(builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
            )
        }

        // Recently added Radio Stations
        val builderRecent = MediaDescriptionCompat.Builder()
            .setMediaId(MediaIdHelper.MEDIA_ID_RECENT_ADDED_STATIONS)
            .setTitle(context.getString(R.string.new_stations_title))
        val bundleRecent = Bundle()
        MediaItemHelper.setDrawableId(bundleRecent, R.drawable.ic_fiber_new_black_24dp)
        builderRecent.setExtras(bundleRecent)
        dependencies.addMediaItem(
            MediaBrowserCompat.MediaItem(builderRecent.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
        )

        // Popular Radio Stations
        val builderPop = MediaDescriptionCompat.Builder()
            .setMediaId(MediaIdHelper.MEDIA_ID_POPULAR_STATIONS)
            .setTitle(context.getString(R.string.popular_stations_title))
        val bundlePop = Bundle()
        MediaItemHelper.setDrawableId(bundlePop, R.drawable.ic_trending_up_black_24dp)
        builderPop.setExtras(bundlePop)
        dependencies.addMediaItem(
            MediaBrowserCompat.MediaItem(builderPop.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
        )

        // Worldwide Stations
        val builder = MediaDescriptionCompat.Builder()
            .setMediaId(MediaIdHelper.MEDIA_ID_ALL_CATEGORIES)
            .setTitle(context.getString(R.string.all_categories_title))
        val bundle = Bundle()
        MediaItemHelper.setDrawableId(bundle, R.drawable.ic_all_categories)
        builder.setExtras(bundle)
        dependencies.addMediaItem(
            MediaBrowserCompat.MediaItem(builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
        )

        // All countries list
        val builderCounties = MediaDescriptionCompat.Builder()
            .setMediaId(MediaIdHelper.MEDIA_ID_COUNTRIES_LIST)
            .setTitle(context.getString(R.string.countries_list_title))
        val bundleCounties = Bundle()
        MediaItemHelper.setDrawableId(bundleCounties, R.drawable.ic_public_black_24dp)
        builderCounties.setExtras(bundleCounties)
        dependencies.addMediaItem(
            MediaBrowserCompat.MediaItem(builderCounties.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
        )

        // If the Country code is known:
        val locationStr = context.getString(R.string.default_country_use_location)
        if (dependencies.countryCode.isNotEmpty() && dependencies.countryCode != locationStr) {
            val identifier = context.resources.getIdentifier(
                "flag_" + dependencies.countryCode.toLowerCase(Locale.ROOT),
                "drawable", context.packageName
            )
            val bundle1 = Bundle()
            MediaItemHelper.setDrawableId(bundle1, identifier)
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
            MediaItemHelper.setDrawableId(bundle1, R.drawable.ic_locals)
            builder1.setExtras(bundle1)
            dependencies.addMediaItem(
                MediaBrowserCompat.MediaItem(builder1.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
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
