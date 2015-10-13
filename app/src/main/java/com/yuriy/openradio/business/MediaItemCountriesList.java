/*
 * Copyright 2015 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.business;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;

import com.yuriy.openradio.R;
import com.yuriy.openradio.api.APIServiceProvider;
import com.yuriy.openradio.net.Downloader;
import com.yuriy.openradio.net.UrlBuilder;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.utils.MediaIDHelper;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class MediaItemCountriesList implements MediaItemCommand {

    @Override
    public void create(final Context context, final String countryCode,
                       final Downloader downloader, final APIServiceProvider serviceProvider,
                       @NonNull final MediaBrowserService.Result<List<MediaBrowser.MediaItem>> result,
                       final List<MediaBrowser.MediaItem> mediaItems,
                       final IUpdatePlaybackState playbackStateListener) {

        // Use result.detach to allow calling result.sendResult from another thread:
        result.detach();

        AppUtils.API_CALL_EXECUTOR.submit(
                new Runnable() {

                    @Override
                    public void run() {

                        // Load all countries into menu
                        loadAllCountries(
                                context,
                                serviceProvider,
                                downloader,
                                mediaItems,
                                result,
                                playbackStateListener
                        );
                    }
                }
        );
    }

    /**
     * Load All Countries into Menu.
     *
     * @param serviceProvider {@link com.yuriy.openradio.api.APIServiceProvider}
     * @param downloader      {@link com.yuriy.openradio.net.Downloader}
     * @param mediaItems      Collections of {@link android.media.browse.MediaBrowser.MediaItem}s
     * @param result          Result of the loading.
     */
    private void loadAllCountries(final Context context,
                                  final APIServiceProvider serviceProvider,
                                  final Downloader downloader,
                                  final List<MediaBrowser.MediaItem> mediaItems,
                                  final MediaBrowserService.Result<List<MediaBrowser.MediaItem>> result,
                                  final IUpdatePlaybackState playbackStateListener) {
        final List<String> list = serviceProvider.getCounties(downloader,
                UrlBuilder.getAllCountriesUrl(context));

        if (list.isEmpty() && playbackStateListener != null) {
            playbackStateListener.updatePlaybackState(context.getString(R.string.no_data_message));
            return;
        }

        Collections.sort(list, new Comparator<String>() {

            @Override
            public int compare(String lhs, String rhs) {
                return lhs.compareTo(rhs);
            }
        });

        String countryName;
        // Overlay base image with the appropriate flag
        final BitmapsOverlay flagLoader = BitmapsOverlay.getInstance();
        Bitmap bitmap;

        for (final String countryCode : list) {

            if (AppUtils.COUNTRY_CODE_TO_NAME.containsKey(countryCode)) {
                countryName = AppUtils.COUNTRY_CODE_TO_NAME.get(countryCode);
            } else {
                countryName = "";
            }

            final int identifier = context.getResources().getIdentifier(
                    "flag_" + countryCode.toLowerCase(),
                    "drawable", context.getPackageName()
            );

            bitmap = flagLoader.execute(context, identifier,
                    BitmapFactory.decodeResource(
                            context.getResources(),
                            R.drawable.ic_child_categories
                    )
            );

            mediaItems.add(new MediaBrowser.MediaItem(
                    new MediaDescription.Builder()
                            .setMediaId(
                                    MediaIDHelper.MEDIA_ID_COUNTRIES_LIST + countryCode
                            )
                            .setTitle(countryName)
                            .setIconBitmap(bitmap)
                            .setSubtitle(countryCode)
                            .build(), MediaBrowser.MediaItem.FLAG_BROWSABLE
            ));
        }

        result.sendResult(mediaItems);
    }
}
