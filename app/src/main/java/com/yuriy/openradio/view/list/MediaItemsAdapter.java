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

package com.yuriy.openradio.view.list;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.yuriy.openradio.R;
import com.yuriy.openradio.service.OpenRadioService;
import com.yuriy.openradio.utils.ImageFetcher;
import com.yuriy.openradio.utils.MediaItemHelper;

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
    private Activity mCurrentActivity;
    private ImageFetcher mImageFetcher;
    private final ListAdapterData<MediaBrowser.MediaItem> mAdapterData = new ListAdapterData<>(null);

    /**
     * Stores an instance of {@link com.yuriy.openradio.view.list.MediaItemsAdapter.MessagesHandler}.
     */
    //private Handler mMessagesHandler = null;

    /**
     * Constructor.
     *
     * @param activity     current {@link android.app.Activity}
     * @param imageFetcher {@link ImageFetcher} instance
     */
    public MediaItemsAdapter(final FragmentActivity activity, final ImageFetcher imageFetcher) {
        mCurrentActivity = activity;
        mImageFetcher = imageFetcher;

        // Initialize the Messages Handler.
        //mMessagesHandler = new MessagesHandler(mCurrentActivity, this);
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

    @Override
    public final View getView(final int position, View convertView, final ViewGroup parent) {
        final MediaBrowser.MediaItem mediaItem = (MediaBrowser.MediaItem) getItem(position);
        final MediaDescription description = mediaItem.getDescription();

        convertView = prepareViewAndHolder(convertView, R.layout.category_list_item);

        mViewHolder.mNameView.setText(description.getTitle());
        mViewHolder.mDescriptionView.setText(description.getSubtitle());
        if (description.getIconBitmap() != null) {
            mViewHolder.mImageView.setImageBitmap(description.getIconBitmap());
        } else {
            final Uri iconUri = description.getIconUri();
            if (mediaItem.isPlayable()) {

                if (iconUri != null
                        && iconUri.toString().startsWith("android")) {
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

        if (mediaItem.isPlayable() && !MediaItemHelper.isLocalRadioStationField(mediaItem)) {
            mViewHolder.mFavoriteCheckView.setChecked(MediaItemHelper.isFavoriteField(mediaItem));

            mViewHolder.mFavoriteCheckView.setVisibility(View.VISIBLE);

            mViewHolder.mFavoriteCheckView.setOnClickListener(

                    new View.OnClickListener() {

                        @Override
                        public void onClick(final View view) {
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
                    }
            );
        } else {
            mViewHolder.mFavoriteCheckView.setVisibility(View.GONE);
        }

        return convertView;
    }

    /**
     * Add {@link com.yuriy.openradio.api.CategoryVO} into the collection.
     * @param value {@link com.yuriy.openradio.api.CategoryVO}
     */
    public final void addItem(final MediaBrowser.MediaItem value) {
        mAdapterData.addItem(value);
    }

    /**
     * Add {@link com.yuriy.openradio.api.CategoryVO}s into the collection.
     * @param items Collection of the {@link com.yuriy.openradio.api.CategoryVO}
     */
    public final void addItems(final List<MediaBrowser.MediaItem> items) {
        for (MediaBrowser.MediaItem item : items) {
            addItem(item);
        }
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
        return viewHolder;
    }

    private void updateFavorite(final int position, final boolean isFavorite) {
        if (mAdapterData == null) {
            return;
        }
        MediaItemHelper.updateFavoriteField(mAdapterData.getItem(position), isFavorite);
        notifyDataSetChanged();
    }

    // Template for the future use

    /**
     * An inner class that inherits from {@link android.os.Handler}
     * and uses its {@link android.os.Handler#handleMessage(android.os.Message)}
     * hook method to process Messages
     * sent to it from the {@link OpenRadioService}.
     */
    /*public static class MessagesHandler extends Handler {

        private static final String CLASS_NAME = MessagesHandler.class.getSimpleName();

        *//**
         * Allows Activity to be garbage collected properly.
         *//*
        private final Activity mActivity;

        *//**
         * Allows Media Items Adapter to be garbage collected properly.
         *//*
        private final MediaItemsAdapter mMediaItemsAdapter;

        *//**
         * Class constructor constructs {@link #mActivity} as weak reference
         * to the activity.
         *
         * @param activity The corresponding activity.
         *//*
        public MessagesHandler(final Activity activity, final MediaItemsAdapter mediaItemsAdapter) {
            mActivity = activity;
            mMediaItemsAdapter = mediaItemsAdapter;
        }

        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);

            final int what = msg.what;
            //final Intent incomeIntent = (Intent) msg.obj;

            switch (what) {
                default:
                    Log.w(CLASS_NAME, "Unknown message:" + what);
                    break;
            }
        }
    }*/
}
