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
package com.yuriy.openradio.shared.view.dialog

import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.media.MediaBrowserCompat
import android.view.View
import android.widget.TextView
import com.yuriy.openradio.R
import com.yuriy.openradio.mobile.view.activity.MainActivity
import com.yuriy.openradio.shared.view.BaseDialogFragment

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class RSSettingsDialog : BaseDialogFragment() {

    override fun onCreateDialog(savedInstance: Bundle?): Dialog {
        val activity = activity as MainActivity?
        val view = inflater.inflate(
                R.layout.dialog_rs_settings,
                activity!!.findViewById(R.id.dialog_rs_settings_root)
        )
        setWindowDimensions(view, 0.8f, 0.4f)
        val args = arguments
        val item = extractMediaItem(args)
        if (item == null) {
            view.findViewById<View>(R.id.dialog_rs_settings_coming_soon).visibility = View.VISIBLE
        } else {
            view.findViewById<View>(R.id.dialog_rs_settings_edit_remove).visibility = View.VISIBLE
            view.findViewById<View>(R.id.dialog_rs_settings_edit_btn).tag = item
            view.findViewById<View>(R.id.dialog_rs_settings_remove_btn).tag = item
            val name = view.findViewById<TextView>(R.id.dialog_rs_settings_rs_name)
            name.text = item.description.title
        }
        return createAlertDialog(view)
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = RSSettingsDialog::class.java.simpleName

        /**
         * Tag string to use in dialog transactions.
         */
        @JvmField
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"
        private val KEY_MEDIA_ITEM = CLASS_NAME + "_KEY_MEDIA_ITEM"
        @JvmStatic
        fun provideMediaItem(bundle: Bundle,
                             mediaItem: MediaBrowserCompat.MediaItem) {
            bundle.putParcelable(KEY_MEDIA_ITEM, mediaItem)
        }

        private fun extractMediaItem(bundle: Bundle?): MediaBrowserCompat.MediaItem? {
            if (bundle == null) {
                return null
            }
            return if (!bundle.containsKey(KEY_MEDIA_ITEM)) {
                null
            } else bundle.getParcelable<Parcelable>(KEY_MEDIA_ITEM) as MediaBrowserCompat.MediaItem?
        }
    }
}
