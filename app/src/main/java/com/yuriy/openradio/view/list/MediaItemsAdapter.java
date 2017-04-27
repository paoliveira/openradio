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

import android.content.Intent;
import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yuriy.openradio.R;
import com.yuriy.openradio.service.OpenRadioService;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.ImageFetcher;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.view.MainActivity;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/18/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class MediaItemsAdapter extends BaseAdapter {

    @SuppressWarnings("unused")
    private static final String CLASS_NAME = MediaItemsAdapter.class.getSimpleName();

    private ListAdapterViewHolder mViewHolder;
    private MainActivity mCurrentActivity;
    private ImageFetcher mImageFetcher;
    private final ListAdapterData<MediaBrowserCompat.MediaItem> mAdapterData;

    /**
     * The currently selected / active Item Id.
     */
    private long mActiveItemId = MediaSessionCompat.QueueItem.UNKNOWN_ID;

    /**
     * Main constructor.
     *
     * @param activity     current {@link android.app.Activity}
     * @param imageFetcher {@link ImageFetcher} instance
     */
    public MediaItemsAdapter(final MainActivity activity, final ImageFetcher imageFetcher) {
        mAdapterData = new ListAdapterData<>(
                (o1, o2) -> {
                    AppLogger.d("Item1:" + MediaItemHelper.getSortIdField(o1));
                    AppLogger.d("Item2:" + MediaItemHelper.getSortIdField(o2));
                    return 0;
                }
        );
        mCurrentActivity = activity;
        mImageFetcher = imageFetcher;
    }

    @Override
    public final int getCount() {
        return mAdapterData.getItemsCount();
    }

    @Override
    public final Object getItem(final int position) {
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
     *
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

    /**
     * Set active Id from the items list.
     * @param id Id of the Item.
     */
    public void setActiveItemId(final long id) {
        mActiveItemId = id;
    }

    /**
     * Returns the currently active Item Id.
     *
     * @return The currently active Item Id.
     */
    public long getActiveItemId() {
        return mActiveItemId;
    }

    @Override
    public final View getView(final int position, View convertView, final ViewGroup parent) {
        final MediaBrowserCompat.MediaItem mediaItem = (MediaBrowserCompat.MediaItem) getItem(position);
        final MediaDescriptionCompat description = mediaItem.getDescription();

        convertView = prepareViewAndHolder(convertView, R.layout.category_list_item);

        mViewHolder.mNameView.setText(description.getTitle());
        mViewHolder.mDescriptionView.setText(description.getSubtitle());
        if (description.getIconBitmap() != null) {
            mViewHolder.mImageView.setImageBitmap(description.getIconBitmap());
        } else {
            final Uri iconUri = description.getIconUri();
            if (mediaItem.isPlayable()) {

                if (iconUri != null && iconUri.toString().startsWith("android")) {
                    mViewHolder.mImageView.setImageURI(iconUri);
                } else {
                    // Load the image asynchronously into the ImageView, this also takes care of
                    // setting a placeholder image while the background thread runs
                    mImageFetcher.loadImage(iconUri, mViewHolder.mImageView);
                }
            } else {
                mViewHolder.mImageView.setImageURI(iconUri);
            }
        }

        mViewHolder.mImageView.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mCurrentActivity.startDrag(mediaItem);
                return true;
            }
            return false;
        });

        if (mediaItem.isPlayable() && !MediaItemHelper.isLocalRadioStationField(mediaItem)) {
            mViewHolder.mFavoriteCheckView.setChecked(MediaItemHelper.isFavoriteField(mediaItem));

            mViewHolder.mFavoriteCheckView.setVisibility(View.VISIBLE);

            mViewHolder.mFavoriteCheckView.setOnClickListener(

                    view -> {
                        boolean isChecked = ((CheckBox) view).isChecked();

                        MediaItemHelper.updateFavoriteField(mediaItem, isChecked);

                        // Make Intent to update Favorite RadioStation object associated with
                        // the Media Description
                        final Intent intent = OpenRadioService.makeUpdateFavoriteIntent(
                                mCurrentActivity,
                                description,
                                isChecked
                        );
                        // Send Intent to the OpenRadioService.
                        mCurrentActivity.startService(intent);
                    }
            );
        } else {
            mViewHolder.mFavoriteCheckView.setVisibility(View.GONE);
        }

        if (position == getActiveItemId()
                || (mCurrentActivity.mDragMediaItem != null && mCurrentActivity.mDragMediaItem == mediaItem)) {
            mViewHolder.mRootView.setBackgroundColor(
                    mCurrentActivity.getResources().getColor(R.color.queue_item_selected_bg_color)
            );
        } else {
            mViewHolder.mRootView.setBackgroundColor(
                    mCurrentActivity.getResources().getColor(R.color.transparent_color)
            );
        }

        return convertView;
    }

    /**
     * Add {@link android.support.v4.media.MediaBrowserCompat.MediaItem}s into the collection.
     * @param value {@link android.support.v4.media.MediaBrowserCompat.MediaItem}s.
     */
    public final void addAll(final List<MediaBrowserCompat.MediaItem> value) {
        mAdapterData.addAll(value);
    }

    public final void addAt(final int position, final MediaBrowserCompat.MediaItem mediaItem) {
        mAdapterData.addAt(position, mediaItem);
    }

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
            convertView = LayoutInflater.from(mCurrentActivity).inflate(inflateViewResId, null);
            mViewHolder = createViewHolder(convertView);
            convertView.setTag(mViewHolder);
        } else {
            // Get View by provided Tag
            mViewHolder = (ListAdapterViewHolder) convertView.getTag();
        }
        return convertView;
    }

    /**
     * Create View holder to keep reference to the layout items
     * @param view {@link android.view.View}
     * @return {@link ListAdapterViewHolder} object
     */
    private ListAdapterViewHolder createViewHolder(final View view) {
        final ListAdapterViewHolder viewHolder = new ListAdapterViewHolder();
        viewHolder.mNameView = (TextView) view.findViewById(R.id.name_view);
        viewHolder.mDescriptionView = (TextView) view.findViewById(R.id.description_view);
        viewHolder.mImageView = (ImageView) view.findViewById(R.id.img_view);
        viewHolder.mFavoriteCheckView = (CheckBox) view.findViewById(R.id.favorite_check_view);
        viewHolder.mRootView = (RelativeLayout) view.findViewById(R.id.category_list_root_view);
        return viewHolder;
    }
}
