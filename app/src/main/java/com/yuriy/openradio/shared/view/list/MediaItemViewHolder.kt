/*
 * Copyright 2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xenione.libs.swipemaker.SwipeLayout

class MediaItemViewHolder(view: View, root_view_id: Int, name_view_id: Int,
                          description_view_id: Int, img_view_id: Int, favorite_view_id: Int,
                          bitrate_view_id: Int, settings_btn_view_id: Int,
                          foreground_view_id: Int) : RecyclerView.ViewHolder(view) {
    /**
     * Title text view.
     */
    @JvmField
    val mNameView: TextView = view.findViewById(name_view_id)

    @JvmField
    val mBitrateView: TextView = view.findViewById(bitrate_view_id)

    /**
     * Description text view.
     */
    @JvmField
    val mDescriptionView: TextView = view.findViewById(description_view_id)

    /**
     * Category image view.
     */
    @JvmField
    val mImageView: ImageView = view.findViewById(img_view_id)

    /**
     * Check box vew for the "Favorites" option.
     */
    @JvmField
    val mFavoriteCheckView: CheckBox = view.findViewById(favorite_view_id)

    @JvmField
    val mForegroundView: SwipeLayout? = view.findViewById(foreground_view_id)

    @JvmField
    val mRoot: ViewGroup = view.findViewById(root_view_id)

    @JvmField
    val mSettingsView: ImageButton? = view.findViewById(settings_btn_view_id)
}
