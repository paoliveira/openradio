package com.yuriy.openradio.business;

import android.content.Context;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;

import com.yuriy.openradio.R;
import com.yuriy.openradio.api.APIServiceProvider;
import com.yuriy.openradio.api.CategoryVO;
import com.yuriy.openradio.net.Downloader;
import com.yuriy.openradio.net.UrlBuilder;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.utils.MediaIDHelper;
import com.yuriy.openradio.utils.QueueHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class MediaItemAllCategories implements MediaItemCommand {

    /**
     * Collection of All Categories.
     */
    private final List<CategoryVO> mAllCategories = new ArrayList<>();

    @Override
    public void create(final Context context, final String countryCode,
                       final Downloader downloader, final APIServiceProvider serviceProvider,
                       final @NonNull MediaBrowserService.Result<List<MediaBrowser.MediaItem>> result,
                       final List<MediaBrowser.MediaItem> mediaItems) {

        // Use result.detach to allow calling result.sendResult from another thread:
        result.detach();

        AppUtils.API_CALL_EXECUTOR.submit(
                new Runnable() {

                    @Override
                    public void run() {

                        // Load all categories into menu
                        loadAllCategories(
                                context,
                                serviceProvider,
                                downloader,
                                mediaItems,
                                result
                        );
                    }
                }
        );
    }

    /**
     * Load All Categories into Menu.
     *
     * @param serviceProvider {@link com.yuriy.openradio.api.APIServiceProvider}
     * @param downloader      {@link com.yuriy.openradio.net.Downloader}
     * @param mediaItems      Collections of {@link android.media.browse.MediaBrowser.MediaItem}s
     * @param result          Result of the loading.
     */
    private void loadAllCategories(final Context context,
                                   final APIServiceProvider serviceProvider,
                                   final Downloader downloader,
                                   final List<MediaBrowser.MediaItem> mediaItems,
                                   final MediaBrowserService.Result<List<MediaBrowser.MediaItem>> result) {
        final List<CategoryVO> list = serviceProvider.getCategories(downloader,
                UrlBuilder.getAllCategoriesUrl(context));

        if (list.isEmpty()) {
            updatePlaybackState(context.getString(R.string.no_data_message));
            return;
        }

        Collections.sort(list, new Comparator<CategoryVO>() {

            @Override
            public int compare(CategoryVO lhs, CategoryVO rhs) {
                return lhs.getTitle().compareTo(rhs.getTitle());
            }
        });

        QueueHelper.copyCollection(mAllCategories, list);

        final String iconUrl = "android.resource://" +
                context.getPackageName() + "/drawable/ic_child_categories";

        final Set<String> predefinedCategories = AppUtils.predefinedCategories();
        for (CategoryVO category : mAllCategories) {

            if (!predefinedCategories.contains(category.getTitle())) {
                continue;
            }

            mediaItems.add(new MediaBrowser.MediaItem(
                    new MediaDescription.Builder()
                            .setMediaId(
                                    MediaIDHelper.MEDIA_ID_PARENT_CATEGORIES
                                            + String.valueOf(category.getId())
                            )
                            .setTitle(category.getTitle())
                            .setIconUri(Uri.parse(iconUrl))
                            .setSubtitle(category.getDescription())
                            .build(), MediaBrowser.MediaItem.FLAG_BROWSABLE
            ));
        }

        result.sendResult(mediaItems);
    }
}
