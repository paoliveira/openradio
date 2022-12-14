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

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.model.media.MediaId
import com.yuriy.openradio.shared.model.storage.images.ImagesStore
import com.yuriy.openradio.shared.service.OpenRadioStore
import com.yuriy.openradio.shared.utils.MediaItemHelper
import com.yuriy.openradio.shared.utils.gone
import com.yuriy.openradio.shared.utils.setImageBitmap
import com.yuriy.openradio.shared.utils.visible

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/18/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
abstract class MediaItemsAdapter : RecyclerView.Adapter<MediaItemViewHolder>() {

    interface Listener {
        fun onItemSettings(item: MediaBrowserCompat.MediaItem)
        fun onItemSelected(item: MediaBrowserCompat.MediaItem, position: Int)
    }

    private val mAdapterData = ListAdapterData<MediaBrowserCompat.MediaItem>()
    private var mActiveItemId = MediaSessionCompat.QueueItem.UNKNOWN_ID
    var parentId = MediaId.MEDIA_ID_ROOT
    var listener: Listener? = null

    /**
     * The currently selected / active Item Id.
     */
    var activeItemId: Int
        get() {
            return mActiveItemId
        }
        set(value) {
            mActiveItemId = value
        }

    override fun getItemCount(): Int {
        return mAdapterData.itemsCount
    }

    fun getItem(position: Int): MediaBrowserCompat.MediaItem? {
        return mAdapterData.getItem(position)
    }

    override fun onViewRecycled(holder: MediaItemViewHolder) {
        super.onViewRecycled(holder)
        holder.mImageView.setImageResource(R.color.or_color_transparent)
    }

    fun removeListener() {
        listener = null
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
     * Clear adapter data.
     */
    fun clearData() {
        mAdapterData.clear()
    }

    open fun clear() {
        clearData()
    }

    inner class OnSettingsListener(item: MediaBrowserCompat.MediaItem) :
        View.OnClickListener {

        private val mItem = MediaBrowserCompat.MediaItem(item.description, item.flags)

        override fun onClick(view: View) {
            listener?.onItemSettings(
                MediaBrowserCompat.MediaItem(mItem.description, mItem.flags)
            )
        }
    }

    companion object {

        fun updateBitrateView(
            bitrate: Int,
            view: TextView?,
            isPlayable: Boolean
        ) {
            if (view == null) {
                return
            }
            if (isPlayable) {
                if (bitrate != 0) {
                    view.visible()
                    val bitrateStr = bitrate.toString() + "kb/s"
                    view.text = bitrateStr
                } else {
                    view.gone()
                }
            } else {
                view.gone()
            }
        }

        /**
         * Handle view of list item responsible to display Title and Description.
         * Different categories requires different handle approaches.
         *
         * @param nameView
         * @param descriptionView
         * @param description
         * @param parentId
         */
        fun handleNameAndDescriptionView(
            nameView: TextView,
            descriptionView: TextView,
            description: MediaDescriptionCompat,
            parentId: String
        ) {
            nameView.text = description.title
            descriptionView.text = description.subtitle
            val layoutParams = nameView.layoutParams as RelativeLayout.LayoutParams
            if (MediaId.MEDIA_ID_ROOT == parentId) {
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
                descriptionView.gone()
            } else {
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.LEFT_OF)
                descriptionView.visible()
            }
            nameView.layoutParams = layoutParams
        }

        /**
         * Updates an image view of the Media Item.
         *
         * @param description Media Description of the Media Item.
         * @param view        Image View to apply image to.
         */
        fun updateImage(context: Context, description: MediaDescriptionCompat, view: ImageView) {
            view.visible()
            // Show placeholder before load an image.
            view.setImageResource(R.drawable.ic_radio_station)
            if (description.iconBitmap != null) {
                view.setImageBitmap(description.iconBitmap)
            } else {
                val iconId = MediaItemHelper.getDrawableId(description.extras)
                if (MediaItemHelper.isDrawableIdValid(iconId)) {
                    view.setImageResource(iconId)
                }
                val imageUrl = ImagesStore.getImageUrl(description.iconUri)
                if (imageUrl.isEmpty()) {
                    return
                }
                val imageUri = description.iconUri ?: return
                val id = ImagesStore.getId(imageUri)
                // Mark view by tag, later on, when image downloaded and callback invoked in presenter
                // it will be possible to re-open stream and apply bytes to correct image view.
                view.tag = id
                context.contentResolver.openInputStream(imageUri)?.use {
                    // Get bytes if available. If not, the callback in presenter will hook downloaded bytes
                    // later on.
                    view.setImageBitmap(it.readBytes())
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
        fun handleFavoriteAction(
            checkBox: CheckBox, description: MediaDescriptionCompat?,
            mediaItem: MediaBrowserCompat.MediaItem?, context: Context
        ) {
            checkBox.isChecked = MediaItemHelper.isFavoriteField(mediaItem)
            checkBox.visible()
            checkBox.setOnClickListener { view: View ->
                val isChecked = (view as CheckBox).isChecked
                MediaItemHelper.updateFavoriteField(mediaItem, isChecked)

                // Make Intent to update Favorite RadioStation object associated with
                // the Media Description
                val intent = OpenRadioStore.makeUpdateIsFavoriteIntent(
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
