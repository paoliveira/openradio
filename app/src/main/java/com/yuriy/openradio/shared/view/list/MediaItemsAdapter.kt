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
package com.yuriy.openradio.shared.view.list

import android.R
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.text.TextUtils
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.yuriy.openradio.shared.model.net.UrlBuilder
import com.yuriy.openradio.shared.service.OpenRadioService
import com.yuriy.openradio.shared.utils.MediaIdHelper
import com.yuriy.openradio.shared.utils.MediaItemHelper

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/18/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
abstract class MediaItemsAdapter : RecyclerView.Adapter<MediaItemViewHolder>() {
    interface Listener {
        fun onItemSettings(item: MediaBrowserCompat.MediaItem?, position: Int)
        fun onItemSelected(item: MediaBrowserCompat.MediaItem?, position: Int)
    }

    private val mAdapterData: ListAdapterData<MediaBrowserCompat.MediaItem> = ListAdapterData()
    var parentId: String = MediaIdHelper.MEDIA_ID_ROOT
    var listener: Listener? = null
    /**
     * Returns the currently active Item Id.
     *
     * @return The currently active Item Id.
     */
    /**
     * Set active Id from the items list.
     *
     * @param id Id of the Item.
     */
    /**
     * The currently selected / active Item Id.
     */
    var activeItemId = MediaSessionCompat.QueueItem.UNKNOWN_ID
    override fun getItemCount(): Int {
        return mAdapterData.itemsCount
    }

    fun getItem(position: Int): MediaBrowserCompat.MediaItem? {
        return mAdapterData.getItem(position)
    }

    override fun onViewRecycled(holder: MediaItemViewHolder) {
        super.onViewRecycled(holder)
        holder.mImageView.setImageResource(R.color.transparent)
    }

    fun removeListener() {
        listener = null
    }

    /**
     * get index of the Item by provided Media Id.
     *
     * @param mediaId Media Id of the Radio Station.
     * @return Index of the Radio Station in the adapter,
     * or [MediaSessionCompat.QueueItem.UNKNOWN_ID] if nothing founded.
     */
    fun getIndexForMediaId(mediaId: String?): Int {
        val count = mAdapterData.itemsCount
        var item: MediaBrowserCompat.MediaItem?
        for (i in 0 until count) {
            item = mAdapterData.getItem(i)
            if (item == null) {
                continue
            }
            if (TextUtils.equals(item.description.mediaId, mediaId)) {
                return i
            }
        }
        return MediaSessionCompat.QueueItem.UNKNOWN_ID
    }

    /**
     * Add [MediaBrowserCompat.MediaItem]s into the collection.
     *
     * @param value [MediaBrowserCompat.MediaItem]s.
     */
    fun addAll(value: List<MediaBrowserCompat.MediaItem>) {
        mAdapterData.addAll(value)
    }

    /**
     * Add [MediaBrowserCompat.MediaItem] into the collection
     * at specified position.
     *
     * @param position  Position to add.
     * @param mediaItem [MediaBrowserCompat.MediaItem] to add.
     */
    fun addAt(position: Int, mediaItem: MediaBrowserCompat.MediaItem) {
        mAdapterData.addAt(position, mediaItem)
    }

    /**
     * Removes Media Item from the adapter.
     *
     * @param mediaItem Media Item to remove.
     */
    fun remove(mediaItem: MediaBrowserCompat.MediaItem) {
        mAdapterData.remove(mediaItem)
    }

    /**
     * Clear adapter data.
     */
    fun clearData() {
        mAdapterData.clear()
    }

    open fun clear() {
        clearData()
    }

    companion object {
        @JvmStatic
        fun updateBitrateView(bitrate: Int,
                              view: TextView?,
                              isPlayable: Boolean) {
            if (view == null) {
                return
            }
            if (isPlayable) {
                if (bitrate != 0) {
                    view.visibility = View.VISIBLE
                    val bitrateStr = bitrate.toString() + "kb/s"
                    view.text = bitrateStr
                } else {
                    view.visibility = View.GONE
                }
            } else {
                view.visibility = View.GONE
            }
        }

        /**
         * Handle view of list item responsible to display Title and Description.
         *
         *
         * Different categories requires different handle approaches.
         *
         * @param nameView
         * @param descriptionView
         * @param description
         * @param parentId
         */
        @JvmStatic
        fun handleNameAndDescriptionView(nameView: TextView,
                                         descriptionView: TextView,
                                         description: MediaDescriptionCompat,
                                         parentId: String) {
            nameView.text = description.title
            descriptionView.text = description.subtitle
            val layoutParams = nameView.layoutParams as RelativeLayout.LayoutParams
            if (MediaIdHelper.MEDIA_ID_ROOT == parentId) {
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
                descriptionView.visibility = View.GONE
            } else {
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.LEFT_OF)
                descriptionView.visibility = View.VISIBLE
            }
            nameView.layoutParams = layoutParams
        }

        /**
         * Updates an image view of the Media Item.
         *
         * @param description Media Description of the Media Item.
         * @param view        Image View to apply image to.
         */
        @JvmStatic
        fun updateImage(description: MediaDescriptionCompat,
                        view: ImageView?) {
            if (view == null) {
                return
            }
            if (description.iconBitmap != null) {
                view.setImageBitmap(description.iconBitmap)
            } else {
                val iconUri = UrlBuilder.preProcessIconUri(description.iconUri)
                val iconId = MediaItemHelper.getDrawableId(description.extras)
                if (MediaItemHelper.isDrawableIdValid(iconId)) {
                    view.setImageResource(iconId)
                }
                if (iconUri != null) {
                    Picasso.get().load(iconUri).noPlaceholder().into(view)
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
        @JvmStatic
        fun handleFavoriteAction(checkBox: CheckBox, description: MediaDescriptionCompat?,
                                 mediaItem: MediaBrowserCompat.MediaItem?, context: Context) {
            checkBox.isChecked = MediaItemHelper.isFavoriteField(mediaItem)
            checkBox.visibility = View.VISIBLE
            checkBox.setOnClickListener { view: View ->
                val isChecked = (view as CheckBox).isChecked
                MediaItemHelper.updateFavoriteField(mediaItem, isChecked)

                // Make Intent to update Favorite RadioStation object associated with
                // the Media Description
                val intent = OpenRadioService.makeUpdateIsFavoriteIntent(
                        context,
                        description,
                        isChecked
                )
                // Send Intent to the OpenRadioService.
                context.startService(intent)
            }
        }
    }

}
