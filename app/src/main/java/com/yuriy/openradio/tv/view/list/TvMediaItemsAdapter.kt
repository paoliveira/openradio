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
package com.yuriy.openradio.tv.view.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.utils.MediaItemHelper.getBitrateField
import com.yuriy.openradio.shared.view.list.MediaItemViewHolder
import com.yuriy.openradio.shared.view.list.MediaItemsAdapter

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/18/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class TvMediaItemsAdapter (private var mContext: Context?) : MediaItemsAdapter() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemViewHolder {
        return MediaItemViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.tv_category_list_item, parent, false),
                R.id.tv_root_view, R.id.tv_name_view, R.id.tv_description_view, R.id.tv_img_view,
                R.id.tv_favorite_btn_view, R.id.tv_bitrate_view, -1, -1
        )
    }

    override fun onBindViewHolder(holder: MediaItemViewHolder, position: Int) {
        val mediaItem = getItem(position) ?: return
        if (mContext == null) {
            return
        }
        val description = mediaItem.description
        val isPlayable = mediaItem.isPlayable
        holder.mRoot.setOnClickListener { v: View? ->
            if (listener == null) {
                return@setOnClickListener
            }
            listener!!.onItemSelected(mediaItem, position)
        }
        handleNameAndDescriptionView(holder.mNameView, holder.mDescriptionView, description, parentId)
        updateImage(description, holder.mImageView)
        updateBitrateView(
                getBitrateField(mediaItem), holder.mBitrateView, isPlayable
        )
        holder.mFavoriteCheckView.buttonDrawable = AppCompatResources.getDrawable(mContext!!, R.drawable.src_favorite)
        if (isPlayable) {
            handleFavoriteAction(
                    holder.mFavoriteCheckView, description, mediaItem, mContext!!
            )
        } else {
            holder.mFavoriteCheckView.visibility = View.GONE
        }
        var selected = false
        if (position == activeItemId) {
            selected = true
            holder.mRoot.requestFocus()
        }
        holder.mRoot.isSelected = selected
    }

    override fun clear() {
        super.clear()
        mContext = null
    }
}