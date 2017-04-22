/*
 * Copyright 2017 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.business.mediaitem;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;

import com.yuriy.openradio.R;
import com.yuriy.openradio.business.BitmapsOverlay;
import com.yuriy.openradio.service.FavoritesStorage;
import com.yuriy.openradio.service.LocalRadioStationsStorage;
import com.yuriy.openradio.utils.MediaIDHelper;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * {@link MediaItemRoot} is concrete implementation of the {@link MediaItemCommand} that
 * designed to prepare data to display root menu items.
 */
public final class MediaItemRoot implements MediaItemCommand {

    @Override
    public void create(final IUpdatePlaybackState playbackStateListener,
                       @NonNull final MediaItemShareObject shareObject) {

        final Context context = shareObject.getContext();

        final String iconUrl = "android.resource://" +
                context.getPackageName() + "/drawable/ic_all_categories";
        final List<MediaBrowserCompat.MediaItem> mediaItems = shareObject.getMediaItems();

        // Recently added Radio Stations
        mediaItems.add(new MediaBrowserCompat.MediaItem(
                new MediaDescriptionCompat.Builder()
                        .setMediaId(MediaIDHelper.MEDIA_ID_RECENT_ADDED_STATIONS)
                        .setTitle(context.getString(R.string.recent_added_stations_title))
                        .setIconUri(Uri.parse(iconUrl))
                        .setSubtitle(context.getString(R.string.recent_added_stations_sub_title))
                        .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        ));

        // Popular Radio Stations
        mediaItems.add(new MediaBrowserCompat.MediaItem(
                new MediaDescriptionCompat.Builder()
                        .setMediaId(MediaIDHelper.MEDIA_ID_POPULAR_STATIONS)
                        .setTitle(context.getString(R.string.popular_stations_title))
                        .setIconUri(Uri.parse(iconUrl))
                        .setSubtitle(context.getString(R.string.popular_stations_sub_title))
                        .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        ));

        // Do not show list of Worldwide Stations and all Countries for the Auto version
        if (!shareObject.isAndroidAuto()) {
            // Worldwide Stations
            mediaItems.add(new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(MediaIDHelper.MEDIA_ID_ALL_CATEGORIES)
                            .setTitle(context.getString(R.string.all_categories_title))
                            .setIconUri(Uri.parse(iconUrl))
                            .setSubtitle(context.getString(R.string.all_categories_sub_title))
                            .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            ));

            // All countries list
            mediaItems.add(new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(MediaIDHelper.MEDIA_ID_COUNTRIES_LIST)
                            .setTitle(context.getString(R.string.countries_list_title))
                            .setIconUri(Uri.parse(iconUrl))
                            .setSubtitle(context.getString(R.string.country_stations_sub_title))
                            .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            ));
        }

        // If the Country code is known but not in Auto mode
        if (!shareObject.isAndroidAuto() && !shareObject.getCountryCode().isEmpty()) {

            final int identifier = context.getResources().getIdentifier(
                    "flag_" + shareObject.getCountryCode().toLowerCase(),
                    "drawable", context.getPackageName()
            );
            // Overlay base image with the appropriate flag
            final BitmapsOverlay overlay = BitmapsOverlay.getInstance();
            final Bitmap bitmap = overlay.execute(context, identifier,
                    BitmapFactory.decodeResource(
                            context.getResources(),
                            R.drawable.ic_all_categories
                    ));
            mediaItems.add(new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(MediaIDHelper.MEDIA_ID_COUNTRY_STATIONS)
                            .setTitle(context.getString(R.string.country_stations_title))
                            .setIconBitmap(bitmap)
                            .setSubtitle(context.getString(
                                    R.string.country_stations_sub_title
                            ))
                            .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            ));
        }

        // Show Favorites if they are exists.
        if (!FavoritesStorage.isFavoritesEmpty(context)) {
            // Favorites list

            final int identifier = context.getResources().getIdentifier(
                    "ic_favorites_on",
                    "drawable", context.getPackageName()
            );
            // Overlay base image with the appropriate flag
            final BitmapsOverlay overlay = BitmapsOverlay.getInstance();
            final Bitmap bitmap = overlay.execute(context, identifier,
                    BitmapFactory.decodeResource(
                            context.getResources(),
                            R.drawable.ic_all_categories
                    ));

            mediaItems.add(new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(MediaIDHelper.MEDIA_ID_FAVORITES_LIST)
                            .setTitle(context.getString(R.string.favorites_list_title))
                            .setIconBitmap(bitmap)
                            .setSubtitle(context.getString(R.string.favorites_list_sub_title))
                            .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            ));
        }

        // Show Local Radio Stations if they are exists
        if (!LocalRadioStationsStorage.isLocalsEmpty(context)) {
            // Locals list

            final int identifier = context.getResources().getIdentifier(
                    "ic_local_stations",
                    "drawable", context.getPackageName()
            );
            // Overlay base image with the appropriate flag
            final BitmapsOverlay overlay = BitmapsOverlay.getInstance();
            final Bitmap bitmap = overlay.execute(context, identifier,
                    BitmapFactory.decodeResource(
                            context.getResources(),
                            R.drawable.ic_all_categories
                    ));

            mediaItems.add(new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(MediaIDHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST)
                            .setTitle(context.getString(R.string.local_radio_stations_list_title))
                            .setIconBitmap(bitmap)
                            .setSubtitle(context.getString(R.string.local_radio_stations_list_sub_title))
                            .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            ));
        }

        shareObject.getResult().sendResult(shareObject.getMediaItems());
    }
}
