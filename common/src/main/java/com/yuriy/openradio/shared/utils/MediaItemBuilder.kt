/*
 * Copyright 2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.model.media.MediaId
import com.yuriy.openradio.shared.service.LocationService
import com.yuriy.openradio.shared.vo.Country
import java.util.*

object MediaItemBuilder {

    fun buildFavoritesMenuItem(context: Context): MediaBrowserCompat.MediaItem {
        val builder = MediaDescriptionCompat.Builder()
            .setMediaId(MediaId.MEDIA_ID_FAVORITES_LIST)
            .setTitle(context.getString(R.string.favorites_list_title))
        val bundle = Bundle()
        MediaItemHelper.setDrawableId(bundle, R.drawable.ic_stars_black_24dp)
        builder.setExtras(bundle)
        return MediaBrowserCompat.MediaItem(builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    fun buildRecentMenuItem(context: Context): MediaBrowserCompat.MediaItem {
        val builder = MediaDescriptionCompat.Builder()
            .setMediaId(MediaId.MEDIA_ID_RECENT_ADDED_STATIONS)
            .setTitle(context.getString(R.string.new_stations_title))
        val bundleRecent = Bundle()
        MediaItemHelper.setDrawableId(bundleRecent, R.drawable.ic_fiber_new_black_24dp)
        builder.setExtras(bundleRecent)
        return MediaBrowserCompat.MediaItem(builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    fun buildBrowseMenuItem(context: Context): MediaBrowserCompat.MediaItem {
        val builder = MediaDescriptionCompat.Builder()
            .setMediaId(MediaId.MEDIA_ID_BROWSE)
            .setTitle(context.getString(R.string.browse_title))
        return MediaBrowserCompat.MediaItem(builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    fun buildPopularMenuItem(context: Context): MediaBrowserCompat.MediaItem {
        val builder = MediaDescriptionCompat.Builder()
            .setMediaId(MediaId.MEDIA_ID_POPULAR_STATIONS)
            .setTitle(context.getString(R.string.popular_stations_title))
        val bundlePop = Bundle()
        MediaItemHelper.setDrawableId(bundlePop, R.drawable.ic_trending_up_black_24dp)
        builder.setExtras(bundlePop)
        return MediaBrowserCompat.MediaItem(builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    fun buildCategoriesMenuItem(context: Context): MediaBrowserCompat.MediaItem {
        val builder = MediaDescriptionCompat.Builder()
            .setMediaId(MediaId.MEDIA_ID_ALL_CATEGORIES)
            .setTitle(context.getString(R.string.all_categories_title))
        val bundle = Bundle()
        MediaItemHelper.setDrawableId(bundle, R.drawable.ic_all_categories)
        builder.setExtras(bundle)
        return MediaBrowserCompat.MediaItem(builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    fun buildCountriesMenuItem(context: Context): MediaBrowserCompat.MediaItem {
        val builder = MediaDescriptionCompat.Builder()
            .setMediaId(MediaId.MEDIA_ID_COUNTRIES_LIST)
            .setTitle(context.getString(R.string.countries_list_title))
        val bundleCounties = Bundle()
        MediaItemHelper.setDrawableId(bundleCounties, R.drawable.ic_public_black_24dp)
        builder.setExtras(bundleCounties)
        return MediaBrowserCompat.MediaItem(builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    fun buildCountryMenuItem(context: Context, countryCode: String): MediaBrowserCompat.MediaItem {
        val identifier = context.resources.getIdentifier(
            "flag_" + countryCode.lowercase(Locale.ROOT),
            "drawable", context.packageName
        )
        val bundle = Bundle()
        MediaItemHelper.setDrawableId(bundle, identifier)
        val builder = MediaDescriptionCompat.Builder()
            .setMediaId(MediaId.MEDIA_ID_COUNTRY_STATIONS)
            .setTitle(LocationService.COUNTRY_CODE_TO_NAME[countryCode])
            .setExtras(bundle)
        return MediaBrowserCompat.MediaItem(builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    fun buildCountryMenuItem(context: Context, country: Country): MediaBrowserCompat.MediaItem {
        val builder = MediaDescriptionCompat.Builder()
            .setMediaId(
                MediaId.MEDIA_ID_COUNTRIES_LIST + country.code
            )
            .setTitle(country.name)
            .setSubtitle(country.code)
        val identifier = context.resources.getIdentifier(
            "flag_" + country.code.lowercase(Locale.ROOT),
            "drawable", context.packageName
        )
        val bundle = Bundle()
        MediaItemHelper.setDrawableId(bundle, identifier)
        builder.setExtras(bundle)
        return MediaBrowserCompat.MediaItem(builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    fun buildDeviceLocalsMenuItem(context: Context): MediaBrowserCompat.MediaItem {
        val builder = MediaDescriptionCompat.Builder()
            .setMediaId(MediaId.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST)
            .setTitle(context.getString(R.string.local_radio_stations_list_title))
        val bundle = Bundle()
        MediaItemHelper.setDrawableId(bundle, R.drawable.ic_locals)
        builder.setExtras(bundle)
        return MediaBrowserCompat.MediaItem(builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }
}
