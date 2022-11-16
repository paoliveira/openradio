/*
 * Copyright 2020-2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.yuriy.openradio.shared.utils.findCheckBox
import com.yuriy.openradio.shared.utils.findImageView
import com.yuriy.openradio.shared.utils.findTextView

class MediaItemViewHolder(view: View, root_view_id: Int, name_view_id: Int,
                          description_view_id: Int, img_view_id: Int, favorite_view_id: Int,
                          bitrate_view_id: Int, settings_btn_view_id: Int,
                          foreground_view_id: Int) : RecyclerView.ViewHolder(view) {
    /**
     * Title text view.
     */
    val mNameView = view.findTextView(name_view_id)

    val mBitrateView = view.findTextView(bitrate_view_id)

    /**
     * Description text view.
     */
    val mDescriptionView = view.findTextView(description_view_id)

    /**
     * Category image view.
     */
    val mImageView = view.findImageView(img_view_id)

    /**
     * Check box vew for the "Favorites" option.
     */
    val mFavoriteCheckView = view.findCheckBox(favorite_view_id)

    val mForegroundView: RelativeLayout? = view.findViewById(foreground_view_id)

    val mRoot = view.findViewById<ViewGroup>(root_view_id)

    val mSettingsView: ImageButton? = view.findViewById(settings_btn_view_id)
}
