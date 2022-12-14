/*
 * Copyright 2017-2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.mobile.view.list

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.xenione.libs.swipemaker.SwipeLayout
import com.yuriy.openradio.mobile.R
import com.yuriy.openradio.shared.utils.MediaItemHelper
import com.yuriy.openradio.shared.utils.gone
import com.yuriy.openradio.shared.view.list.MediaItemViewHolder
import com.yuriy.openradio.shared.view.list.MediaItemsAdapter

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/18/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class MobileMediaItemsAdapter(private var mContext: Context) : MediaItemsAdapter() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemViewHolder {
        return MediaItemViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_both_side_swipe, parent, false),
            R.id.item_root, R.id.name_view, R.id.description_view, R.id.img_view, R.id.favorite_btn_view,
            R.id.bitrate_view, R.id.settings_btn_view, R.id.foreground_view
        )
    }

    override fun onBindViewHolder(holder: MediaItemViewHolder, position: Int) {
        val mediaItem = getItem(position) ?: return
        val description = mediaItem.description
        val isPlayable = mediaItem.isPlayable
        handleNameAndDescriptionView(holder.mNameView, holder.mDescriptionView, description, parentId)
        updateImage(mContext, description, holder.mImageView)
        updateBitrateView(
            MediaItemHelper.getBitrateField(mediaItem), holder.mBitrateView, isPlayable
        )
        holder.mFavoriteCheckView.buttonDrawable = AppCompatResources.getDrawable(mContext, R.drawable.src_favorite)
        if (isPlayable) {
            handleFavoriteAction(
                holder.mFavoriteCheckView, description, mediaItem, mContext
            )
        } else {
            holder.mFavoriteCheckView.gone()
        }
        holder.mSettingsView?.setOnClickListener(OnSettingsListener(mediaItem))
        (holder.mForegroundView as SwipeLayout).isDragDisabled(!isPlayable)
        holder.mForegroundView?.setOnClickListener(OnItemTapListener(mediaItem, position))
        var color = R.color.or_color_primary
        if (position == activeItemId) {
            color = R.color.or_color_primary_dark
        }
        holder.mForegroundView?.setBackgroundColor(ContextCompat.getColor(mContext, color))
    }

    override fun onViewRecycled(holder: MediaItemViewHolder) {
        super.onViewRecycled(holder)
        (holder.mRoot as BothSideCoordinatorLayout).sync()
    }

    private inner class OnItemTapListener(private val mItem: MediaBrowserCompat.MediaItem, private val mPosition: Int) :
        View.OnClickListener {

        override fun onClick(view: View) {
            listener?.onItemSelected(mItem, mPosition)
        }
    }
}
