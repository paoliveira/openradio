package com.yuriy.openradio.business;

import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.yuriy.openradio.R;
import com.yuriy.openradio.api.CategoryVO;
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

/**
 * {@link MediaItemAllCategories} is concrete implementation of the {@link MediaItemCommand} that
 * designed to prepare data to display radio stations of All Categories.
 */
public class MediaItemAllCategories implements MediaItemCommand {

    @Override
    public void create(final IUpdatePlaybackState playbackStateListener,
                       @NonNull final MediaItemShareObject shareObject) {

        // Use result.detach to allow calling result.sendResult from another thread:
        shareObject.getResult().detach();

        AppUtils.API_CALL_EXECUTOR.submit(
                new Runnable() {

                    @Override
                    public void run() {

                        // Load all categories into menu
                        loadAllCategories(playbackStateListener, shareObject);
                    }
                }
        );
    }

    /**
     * Load All Categories into Menu.
     *
     * @param playbackStateListener Listener of the Playback State changes.
     * @param shareObject           Instance of the {@link MediaItemShareObject} which holds various
     *                              references needed to execute command.
     */
    private void loadAllCategories(final IUpdatePlaybackState playbackStateListener,
                                   @NonNull final MediaItemShareObject shareObject) {
        final List<CategoryVO> list = shareObject.getServiceProvider().getCategories(
                shareObject.getDownloader(),
                UrlBuilder.getAllCategoriesUrl(shareObject.getContext()));

        if (list.isEmpty() && playbackStateListener != null) {
            playbackStateListener.updatePlaybackState(
                    shareObject.getContext().getString(R.string.no_data_message)
            );
            return;
        }

        Collections.sort(list, new Comparator<CategoryVO>() {

            @Override
            public int compare(CategoryVO lhs, CategoryVO rhs) {
                return lhs.getTitle().compareTo(rhs.getTitle());
            }
        });

        // Collection of All Categories.
        // TODO : Probably this collection is redundant.
        final List<CategoryVO> allCategories = new ArrayList<>();
        QueueHelper.copyCollection(allCategories, list);

        final String iconUrl = "android.resource://" +
                shareObject.getContext().getPackageName() + "/drawable/ic_child_categories";

        final Set<String> predefinedCategories = AppUtils.predefinedCategories();
        for (CategoryVO category : allCategories) {

            if (!predefinedCategories.contains(category.getTitle())) {
                continue;
            }

            shareObject.getMediaItems().add(new MediaBrowser.MediaItem(
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

        shareObject.getResult().sendResult(shareObject.getMediaItems());
    }
}
