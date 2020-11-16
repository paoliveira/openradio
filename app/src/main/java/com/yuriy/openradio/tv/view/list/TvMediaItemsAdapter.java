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

package com.yuriy.openradio.tv.view.list;

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
public final class TvMediaItemsAdapter extends MediaItemsAdapter {

    private Context mContext;

    /**
     * Main constructor.
     *
     * @param context current {@link android.app.Activity}
     */
    public TvMediaItemsAdapter(final Context context) {
        super();
        mContext = context;
    }

    @NonNull
    @Override
    public MediaItemViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        return new MediaItemViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.tv_category_list_item, parent, false),
                R.id.tv_root_view, R.id.tv_name_view, R.id.tv_description_view, R.id.tv_img_view,
                R.id.tv_favorite_btn_view, R.id.tv_bitrate_view, -1, -1
        );
    }

    @Override
    public void onBindViewHolder(@NonNull MediaItemViewHolder holder, int position) {
        final MediaBrowserCompat.MediaItem mediaItem = getItem(position);
        if (mediaItem == null) {
            return;
        }
        if (mContext == null) {
            return;
        }

        final MediaDescriptionCompat description = mediaItem.getDescription();
        final boolean isPlayable = mediaItem.isPlayable();

        holder.mRoot.setOnClickListener(
                v -> {
                    if (getListener() == null) {
                        return;
                    }
                    getListener().onItemSelected(mediaItem, position);
                }
        );

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

        boolean selected = false;
        if (position == getActiveItemId()) {
            selected = true;
        }
        holder.mRoot.setSelected(selected);
    }

    @Override
    public void clear() {
        super.clear();
        mContext = null;
    }
}
