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

package com.yuriy.openradio.view.list;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yuriy.openradio.R;
import com.yuriy.openradio.model.net.UrlBuilder;
import com.yuriy.openradio.service.OpenRadioService;
import com.yuriy.openradio.utils.ImageFetcher;
import com.yuriy.openradio.utils.ImageWorker;
import com.yuriy.openradio.utils.MediaIdHelper;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.utils.MediaItemsComparator;
import com.yuriy.openradio.view.activity.MainActivity;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/18/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class MediaItemsAdapter extends BaseAdapter {

    @SuppressWarnings("unused")
    private static final String CLASS_NAME = MediaItemsAdapter.class.getSimpleName() + " ";

    private ListAdapterViewHolder mViewHolder;
    private MainActivity mActivity;
    private ImageWorker mImageFetcher;
    private final ListAdapterData<MediaBrowserCompat.MediaItem> mAdapterData;
    private String mParentId;

    /**
     * The currently selected / active Item Id.
     */
    private int mActiveItemId = MediaSessionCompat.QueueItem.UNKNOWN_ID;

    /**
     * Main constructor.
     *
     * @param activity     current {@link android.app.Activity}
     * @param imageFetcher {@link ImageFetcher} instance
     */
    public MediaItemsAdapter(final MainActivity activity, final ImageWorker imageFetcher) {
        super();
        mParentId = MediaIdHelper.MEDIA_ID_ROOT;
        mAdapterData = new ListAdapterData<>(new MediaItemsComparator());
        mActivity = activity;
        mImageFetcher = imageFetcher;
    }

    @Override
    public final int getCount() {
        return mAdapterData.getItemsCount();
    }

    @Override
    @Nullable
    public final MediaBrowserCompat.MediaItem getItem(final int position) {
        return mAdapterData.getItem(position);
    }

    @Override
    public final long getItemId(final int position) {
        return position;
    }

    /**
     * get index of the Item by provided Media Id.
     *
     * @param mediaId Media Id of the Radio Station.
     * @return Index of the Radio Station in the adapter, or -1 if nothing founded.
     */
    public int getIndexForMediaId(final String mediaId) {
        final int count = mAdapterData.getItemsCount();
        MediaBrowserCompat.MediaItem item;
        for (int i = 0; i < count; i++) {
            item = mAdapterData.getItem(i);
            if (item == null) {
                continue;
            }
            if (TextUtils.equals(item.getDescription().getMediaId(), mediaId)) {
                return i;
            }
        }
        return -1;
    }

    private String getParentId() {
        return mParentId;
    }

    public void setParentId(final String value) {
        mParentId = value;
    }

    /**
     * Set active Id from the items list.
     *
     * @param id Id of the Item.
     */
    public void setActiveItemId(final int id) {
        mActiveItemId = id;
    }

    /**
     * Returns the currently active Item Id.
     *
     * @return The currently active Item Id.
     */
    public int getActiveItemId() {
        return mActiveItemId;
    }

    @Override
    public final View getView(final int position, View convertView, final ViewGroup parent) {
        final MediaBrowserCompat.MediaItem mediaItem = getItem(position);
        convertView = prepareViewAndHolder(convertView, R.layout.category_list_item);

        if (mediaItem == null) {
            return convertView;
        }

        final MediaDescriptionCompat description = mediaItem.getDescription();

        handleNameAndDescriptionView(mViewHolder.mNameView, mViewHolder.mDescriptionView, description, getParentId());

        updateImage(description, mediaItem.isPlayable(), mViewHolder.mImageView, mImageFetcher);

        if (mediaItem.isPlayable()) {
            handleFavoriteAction(
                    mViewHolder.mFavoriteCheckView, description, mediaItem, mActivity.getApplicationContext()
            );
        } else {
            mViewHolder.mFavoriteCheckView.setVisibility(View.GONE);
        }

        int color;
        if (position == getActiveItemId()
                || (mActivity.mDragMediaItem != null && mActivity.mDragMediaItem == mediaItem)) {
            color = R.color.list_item_selected_bg_color;
            if (mActivity.mIsSortMode) {
                color = R.color.item_bg_color_selected_sort_mode;
            }
        } else {
            color = R.color.transparent_color;
            if (mActivity.mIsSortMode) {
                color = R.color.item_bg_color_sort_mode;
            }
        }

        mViewHolder.mRootView.setBackgroundColor(
                mActivity.getResources().getColor(color)
        );

        return convertView;
    }

    /**
     * Add {@link MediaBrowserCompat.MediaItem}s into the collection.
     *
     * @param value {@link MediaBrowserCompat.MediaItem}s.
     */
    public final void addAll(final List<MediaBrowserCompat.MediaItem> value) {
        mAdapterData.addAll(value);
    }

    /**
     * Add {@link MediaBrowserCompat.MediaItem} into the collection
     * at specified position.
     *
     * @param position  Position to add.
     * @param mediaItem {@link MediaBrowserCompat.MediaItem} to add.
     */
    public final void addAt(final int position, final MediaBrowserCompat.MediaItem mediaItem) {
        mAdapterData.addAt(position, mediaItem);
    }

    /**
     * Removes Media Item from the adapter.
     *
     * @param mediaItem Media Item to remove.
     */
    public final void remove(final MediaBrowserCompat.MediaItem mediaItem) {
        mAdapterData.remove(mediaItem);
    }

    /**
     * Clear adapter data.
     */
    public final void clear() {
        mAdapterData.clear();
    }

    /**
     * Prepare view holder for the item rendering.
     *
     * @param convertView      {@link android.view.View} associated with List Item
     * @param inflateViewResId Id of the View layout
     * @return View
     */
    private View prepareViewAndHolder(View convertView, final int inflateViewResId) {
        // If there is no View created - create it here and set it's Tag
        if (convertView == null) {
            convertView = LayoutInflater.from(mActivity).inflate(inflateViewResId, null);
            mViewHolder = createViewHolder(convertView);
            convertView.setTag(mViewHolder);
        } else {
            // Get View by provided Tag
            mViewHolder = (ListAdapterViewHolder) convertView.getTag();
        }
        return convertView;
    }

    /**
     * Handle view of list item responsible to display Title and Description.<p>
     * Different categories requires different handle approaches.
     *
     * @param nameView
     * @param descriptionView
     * @param description
     * @param parentId
     */
    private static void handleNameAndDescriptionView(@NonNull final TextView nameView,
                                                     @NonNull final TextView descriptionView,
                                                     final MediaDescriptionCompat description,
                                                     @NonNull final String parentId) {
        nameView.setText(description.getTitle());
        descriptionView.setText(description.getSubtitle());

        final RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) nameView.getLayoutParams();

        if (MediaIdHelper.MEDIA_ID_ROOT.equals(parentId)) {
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            descriptionView.setVisibility(View.GONE);
        } else {
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.LEFT_OF);
            descriptionView.setVisibility(View.VISIBLE);
        }
        nameView.setLayoutParams(layoutParams);
    }

    /**
     * Create View holder to keep reference to the layout items
     *
     * @param view {@link android.view.View}
     * @return {@link ListAdapterViewHolder} object
     */
    private ListAdapterViewHolder createViewHolder(final View view) {
        final ListAdapterViewHolder viewHolder = new ListAdapterViewHolder();
        viewHolder.mNameView = view.findViewById(R.id.name_view);
        viewHolder.mDescriptionView = view.findViewById(R.id.description_view);
        viewHolder.mImageView = view.findViewById(R.id.img_view);
        viewHolder.mFavoriteCheckView = view.findViewById(R.id.favorite_check_view);
        viewHolder.mRootView = view.findViewById(R.id.category_list_root_view);
        return viewHolder;
    }

    /**
     * Updates an image of the Media Item.
     *
     * @param description  Media Description of the Media Item.
     * @param isPlayable   Is Media Item playable (whether it is Radio Station or Folder).
     * @param imageView    Image View to apply image to.
     * @param imageWorker Fetcher object to download image in background thread.
     */
    public static void updateImage(final MediaDescriptionCompat description, final boolean isPlayable,
                                   final ImageView imageView, final ImageWorker imageWorker) {
        if (description.getIconBitmap() != null) {
            imageView.setImageBitmap(description.getIconBitmap());
        } else {
            final Uri iconUri = UrlBuilder.preProcessIconUri(description.getIconUri());
            if (isPlayable) {
                if (iconUri != null && iconUri.toString().startsWith("android")) {
                    imageView.setImageURI(iconUri);
                } else {
                    // Load the image asynchronously into the ImageView, this also takes care of
                    // setting a placeholder image while the background thread runs
                    imageWorker.loadImage(iconUri, imageView);
                }
            } else {
                imageView.setImageURI(iconUri);
            }
        }
    }

    /**
     * Handle "Add | Remove to | from Favorites".
     *
     * @param checkBox    Favorite check box view.
     * @param description Media aItem description.
     * @param mediaItem   Media Item.
     * @param context     Current context.
     */
    public static void handleFavoriteAction(final CheckBox checkBox, final MediaDescriptionCompat description,
                                            final MediaBrowserCompat.MediaItem mediaItem, final Context context) {
        checkBox.setChecked(MediaItemHelper.isFavoriteField(mediaItem));
        checkBox.setVisibility(View.VISIBLE);
        checkBox.setOnClickListener(

                view -> {
                    final boolean isChecked = ((CheckBox) view).isChecked();

                    MediaItemHelper.updateFavoriteField(mediaItem, isChecked);

                    // Make Intent to update Favorite RadioStation object associated with
                    // the Media Description
                    final Intent intent = OpenRadioService.makeUpdateIsFavoriteIntent(
                            context,
                            description,
                            isChecked
                    );
                    // Send Intent to the OpenRadioService.
                    context.startService(intent);
                }
        );
    }
}
