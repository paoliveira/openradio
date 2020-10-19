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

package com.yuriy.openradio.shared.model.media.item;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager;
import com.yuriy.openradio.shared.model.storage.FavoritesStorage;
import com.yuriy.openradio.shared.model.storage.LatestRadioStationStorage;
import com.yuriy.openradio.shared.model.storage.LocalRadioStationsStorage;
import com.yuriy.openradio.shared.service.LocationService;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.MediaIdHelper;
import com.yuriy.openradio.shared.utils.MediaItemHelper;
import com.yuriy.openradio.shared.vo.RadioStation;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * {@link MediaItemRoot} is concrete implementation of the {@link MediaItemCommand} that
 * designed to prepare data to display root menu items.
 */
public final class MediaItemRoot implements MediaItemCommand {

    private static final String LOG_TAG = MediaItemRoot.class.getSimpleName();

    /**
     * Main constructor.
     */
    public MediaItemRoot() {
        super();
    }

    @Override
    public void execute(final IUpdatePlaybackState playbackStateListener,
                        @NonNull final MediaItemCommandDependencies dependencies) {
        AppLogger.d(LOG_TAG + " invoked");
        final Context context = dependencies.getContext();
        dependencies.getRadioStationsStorage().clear();
        dependencies.getResult().detach();

        // TODO: Refactor these to factory.

        // Get lat known Radio Station.
        // If this feature disabled by Settings - return null, in this case all consecutive UI views will not be
        // exposed.
        RadioStation latestRadioStation;
        if (AppPreferencesManager.lastKnownRadioStationEnabled(context)) {
            latestRadioStation = LatestRadioStationStorage.get(dependencies.getContext());
            if (latestRadioStation != null) {
                dependencies.getRadioStationsStorage().add(latestRadioStation);
                // Add Radio Station to Menu
                final MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                        MediaItemHelper.buildMediaDescriptionFromRadioStation(
                                context, latestRadioStation
                        ),
                        MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                );
                MediaItemHelper.updateFavoriteField(
                        mediaItem, FavoritesStorage.isFavorite(latestRadioStation, dependencies.getContext())
                );
                MediaItemHelper.updateLastPlayedField(mediaItem, true);
                // In case of Android Auto, display latest played Radio Station on top of Menu.
                if (dependencies.isAndroidAuto()) {
                    dependencies.addMediaItem(mediaItem);
                }
            }
        }

        // Show Favorites if they are exists.
        if (!FavoritesStorage.isFavoritesEmpty(context)) {
            // Favorites list
            final MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                    .setMediaId(MediaIdHelper.MEDIA_ID_FAVORITES_LIST)
                    .setTitle(context.getString(R.string.favorites_list_title));
            final Bundle bundle = new Bundle();
            MediaItemHelper.setDrawableId(bundle, R.drawable.ic_stars_black_24dp);
            builder.setExtras(bundle);
            dependencies.addMediaItem(
                    new MediaBrowserCompat.MediaItem(
                            builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    )
            );
        }

        // Recently added Radio Stations
        {
            final MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                    .setMediaId(MediaIdHelper.MEDIA_ID_RECENT_ADDED_STATIONS)
                    .setTitle(context.getString(R.string.new_stations_title));
            final Bundle bundle = new Bundle();
            MediaItemHelper.setDrawableId(bundle, R.drawable.ic_fiber_new_black_24dp);
            builder.setExtras(bundle);
            dependencies.addMediaItem(
                    new MediaBrowserCompat.MediaItem(
                            builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    )
            );
        }

        // Popular Radio Stations
        {
            final MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                    .setMediaId(MediaIdHelper.MEDIA_ID_POPULAR_STATIONS)
                    .setTitle(context.getString(R.string.popular_stations_title));
            final Bundle bundle = new Bundle();
            MediaItemHelper.setDrawableId(bundle, R.drawable.ic_trending_up_black_24dp);
            builder.setExtras(bundle);
            dependencies.addMediaItem(
                    new MediaBrowserCompat.MediaItem(
                            builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    )
            );
        }

        // Do not show list of Worldwide Stations for the Auto version
        if (!dependencies.isAndroidAuto()) {
            // Worldwide Stations
            final Bundle bundle = new Bundle();
            MediaItemHelper.setDrawableId(bundle, R.drawable.ic_all_categories);
            dependencies.addMediaItem(
                    new MediaBrowserCompat.MediaItem(
                            new MediaDescriptionCompat.Builder()
                                    .setMediaId(MediaIdHelper.MEDIA_ID_ALL_CATEGORIES)
                                    .setTitle(context.getString(R.string.all_categories_title))
                                    .setExtras(bundle)
                                    .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    )
            );
        }

        // All countries list
        final MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                .setMediaId(MediaIdHelper.MEDIA_ID_COUNTRIES_LIST)
                .setTitle(context.getString(R.string.countries_list_title));
        final Bundle bundle = new Bundle();
        MediaItemHelper.setDrawableId(bundle, R.drawable.ic_public_black_24dp);
        builder.setExtras(bundle);
        dependencies.addMediaItem(
                new MediaBrowserCompat.MediaItem(
                        builder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                )
        );

        // If the Country code is known:
        if (!TextUtils.isEmpty(dependencies.getCountryCode())) {

            final int identifier = context.getResources().getIdentifier(
                    "flag_" + dependencies.getCountryCode().toLowerCase(),
                    "drawable", context.getPackageName()
            );
            final Bundle bundle1 = new Bundle();
            MediaItemHelper.setDrawableId(bundle1, identifier);
            dependencies.addMediaItem(
                    new MediaBrowserCompat.MediaItem(
                            new MediaDescriptionCompat.Builder()
                                    .setMediaId(MediaIdHelper.MEDIA_ID_COUNTRY_STATIONS)
                                    .setTitle(LocationService.COUNTRY_CODE_TO_NAME.get(dependencies.getCountryCode()))
                                    .setExtras(bundle1)
                                    .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    )
            );
        }

        // Show Local Radio Stations if they are exists
        if (!LocalRadioStationsStorage.isLocalsEmpty(context)) {
            // Locals list
            final MediaDescriptionCompat.Builder builder1 = new MediaDescriptionCompat.Builder()
                    .setMediaId(MediaIdHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST)
                    .setTitle(context.getString(R.string.local_radio_stations_list_title));
            final Bundle bundle1 = new Bundle();
            MediaItemHelper.setDrawableId(bundle1, R.drawable.ic_locals);
            builder1.setExtras(bundle1);
            dependencies.addMediaItem(
                    new MediaBrowserCompat.MediaItem(
                            builder1.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    )
            );
        }

        AppLogger.d(LOG_TAG + " invocation completed");
        dependencies.getResult().sendResult(dependencies.getMediaItems());
        dependencies.getResultListener().onResult();
    }
}
