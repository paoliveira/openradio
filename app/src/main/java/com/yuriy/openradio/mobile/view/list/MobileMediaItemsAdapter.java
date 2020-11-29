/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.mobile.view.list;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.utils.MediaItemHelper;
import com.yuriy.openradio.shared.view.list.MediaItemViewHolder;
import com.yuriy.openradio.shared.view.list.MediaItemsAdapter;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/18/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class MobileMediaItemsAdapter extends MediaItemsAdapter {

    private Context mContext;

    /**
     * Main constructor.
     *
     * @param context Current {@link android.app.Activity}. This has to be activity in order to use
     *                {@link AppCompatResources#getDrawable(Context, int)} on Android 4.
     */
    public MobileMediaItemsAdapter(final Context context) {
        super();
        mContext = context;
    }

    @NonNull
    @Override
    public MediaItemViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        return new MediaItemViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_both_side_swipe, parent, false),
                R.id.item_root, R.id.name_view, R.id.description_view, R.id.img_view, R.id.favorite_btn_view,
                R.id.bitrate_view, R.id.settings_btn_view, R.id.foreground_view
        );
    }

    @Override
    public void onBindViewHolder(@NonNull final MediaItemViewHolder holder, final int position) {
        final MediaBrowserCompat.MediaItem mediaItem = getItem(position);
        if (mediaItem == null) {
            return;
        }
        if (mContext == null) {
            return;
        }

        final MediaDescriptionCompat description = mediaItem.getDescription();
        final boolean isPlayable = mediaItem.isPlayable();

        handleNameAndDescriptionView(holder.mNameView, holder.mDescriptionView, description, getParentId());
        updateImage(description, holder.mImageView);
        updateBitrateView(
                MediaItemHelper.getBitrateField(mediaItem), holder.mBitrateView, isPlayable
        );

        holder.mFavoriteCheckView.setButtonDrawable(
                AppCompatResources.getDrawable(mContext, R.drawable.src_favorite)
        );

        if (isPlayable) {
            handleFavoriteAction(
                    holder.mFavoriteCheckView, description, mediaItem, mContext
            );
        } else {
            holder.mFavoriteCheckView.setVisibility(View.GONE);
        }

        holder.mSettingsView.setOnClickListener(new OnSettingsListener(mediaItem, position));

        holder.mForegroundView.isDragDisabled(!isPlayable);
        holder.mForegroundView.setOnClickListener(new OnItemTapListener(mediaItem, position));
        int color = R.color.or_color_primary;
        if (position == getActiveItemId()) {
            color = R.color.or_color_primary_dark;
        }
        holder.mForegroundView.setBackgroundColor(mContext.getResources().getColor(color));
    }

    @Override
    public void clear() {
        super.clear();
        mContext = null;
    }

    @Override
    public void onViewRecycled(@NonNull final MediaItemViewHolder holder) {
        super.onViewRecycled(holder);
        ((BothSideCoordinatorLayout) holder.mRoot).sync();
    }

    private final class OnSettingsListener implements View.OnClickListener {

        private final int mPosition;
        @NonNull
        private final MediaBrowserCompat.MediaItem mItem;

        public OnSettingsListener(@NonNull final MediaBrowserCompat.MediaItem item, final int position) {
            super();
            mPosition = position;
            mItem = new MediaBrowserCompat.MediaItem(item.getDescription(), item.getFlags());
        }

        @Override
        public void onClick(final View view) {
            if (getListener() == null) {
                return;
            }
            getListener().onItemSettings(
                    new MediaBrowserCompat.MediaItem(mItem.getDescription(), mItem.getFlags()), mPosition
            );
        }
    }

    private final class OnItemTapListener implements View.OnClickListener {

        private final int mPosition;
        private final MediaBrowserCompat.MediaItem mItem;

        public OnItemTapListener(final MediaBrowserCompat.MediaItem item, final int position) {
            super();
            mPosition = position;
            mItem = item;
        }

        @Override
        public void onClick(final View view) {
            if (getListener() == null) {
                return;
            }
            getListener().onItemSelected(mItem, mPosition);
        }
    }
}
