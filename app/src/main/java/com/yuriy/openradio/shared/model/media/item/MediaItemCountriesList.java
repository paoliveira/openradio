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

import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;

import androidx.annotation.NonNull;

import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.model.net.UrlBuilder;
import com.yuriy.openradio.shared.service.LocationService;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.ConcurrentUtils;
import com.yuriy.openradio.shared.utils.MediaIdHelper;
import com.yuriy.openradio.shared.utils.MediaItemHelper;
import com.yuriy.openradio.shared.vo.Country;

import java.util.Collections;
import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * {@link MediaItemCountriesList} is concrete implementation of the {@link MediaItemCommand} that
 * designed to prepare data to display list of all Countries.
 */
public final class MediaItemCountriesList implements MediaItemCommand {

    private static final String LOG_TAG = MediaItemCountriesList.class.getSimpleName();

    /**
     * String tag to use in the log message.
     */
    private static final String CLASS_NAME = MediaItemCountriesList.class.getSimpleName();

    @Override
    public void execute(final IUpdatePlaybackState playbackStateListener,
                        @NonNull final MediaItemCommandDependencies dependencies) {
        AppLogger.d(LOG_TAG + " invoked");
        // Use result.detach to allow calling result.sendResult from another thread:
        dependencies.getResult().detach();

        ConcurrentUtils.API_CALL_EXECUTOR.submit(
                () -> {
                    // Load all countries into menu
                    loadAllCountries(playbackStateListener, dependencies);
                }
        );
    }

    /**
     * Load All Countries into Menu.
     *
     * @param playbackStateListener Listener of the Playback State changes.
     * @param dependencies           Instance of the {@link MediaItemCommandDependencies} which holds various
     *                              references needed to execute command.
     */
    private void loadAllCountries(final IUpdatePlaybackState playbackStateListener,
                                  @NonNull final MediaItemCommandDependencies dependencies) {

        final List<Country> list = dependencies.getServiceProvider().getCountries(
                dependencies.getDownloader(),
                UrlBuilder.getAllCountriesUrl(),
                MediaItemCommandImpl.getCacheType(dependencies)
        );

        if (list.isEmpty() && playbackStateListener != null) {
            playbackStateListener.updatePlaybackState(
                    dependencies.getContext().getString(R.string.no_data_message)
            );
            return;
        }

        Collections.sort(list, (c1, c2) -> c1.getName().compareTo(c2.getName()));

        int identifier;
        MediaDescriptionCompat.Builder builder;

        for (final Country country : list) {

            if (!LocationService.COUNTRY_CODE_TO_NAME.containsKey(country.getCode())) {
                // Add missing country to the Map of the existing ones.
                AppLogger.w(CLASS_NAME + " Missing country:" + country);
                continue;
            }

            builder = new MediaDescriptionCompat.Builder()
                    .setMediaId(
                            MediaIdHelper.MEDIA_ID_COUNTRIES_LIST + country.getCode()
                    )
                    .setTitle(country.getName())
                    .setSubtitle(country.getCode());

            identifier = dependencies.getContext().getResources().getIdentifier(
                    "flag_" + country.getCode().toLowerCase(),
                    "drawable", dependencies.getContext().getPackageName()
            );
            final Bundle bundle1 = new Bundle();
            MediaItemHelper.setDrawableId(bundle1, identifier);
            builder.setExtras(bundle1);
            dependencies.addMediaItem(
                    new MediaBrowserCompat.MediaItem(
                            builder.build(),
                            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    )
            );
        }

        dependencies.getResult().sendResult(dependencies.getMediaItems());
        dependencies.getResultListener().onResult();
    }
}
