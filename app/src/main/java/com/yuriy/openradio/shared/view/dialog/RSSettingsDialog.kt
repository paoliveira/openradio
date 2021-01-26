/*
 * Copyright 2020-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.widget.NumberPicker
import android.widget.TextView
import com.yuriy.openradio.R
import com.yuriy.openradio.mobile.view.activity.MainActivity
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.MediaItemHelper
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
        setWindowDimensions(view, 0.8f, 0.3f)
        val item = extractMediaItem(arguments)
        if (item != null) {
            handleUi(view, item, arguments)
        } else {
            //TODO: Address null data
        }
        return createAlertDialog(view)
    }

    private fun handleUi(view: View, item: MediaBrowserCompat.MediaItem, args: Bundle?) {
        val isLocal = extractIsLocal(args)
        val isSortable = extractIsSortable(args)
        view.findViewById<View>(R.id.dialog_rs_settings_edit_remove).visibility =
                if (isLocal) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.dialog_rs_settings_sort).visibility =
                if (isSortable) View.VISIBLE else View.GONE

        view.findViewById<View>(R.id.dialog_rs_settings_edit_btn).tag = item
        view.findViewById<View>(R.id.dialog_rs_settings_remove_btn).tag = item
        val name = view.findViewById<TextView>(R.id.dialog_rs_settings_rs_name)
        name.text = item.description.title

        if (isSortable) {
            handleSortUi(view, item)
        }
    }

    private fun handleSortUi(view: View, item: MediaBrowserCompat.MediaItem) {
        val numPicker: NumberPicker = view.findViewById(R.id.sort_id_picker)
        numPicker.minValue = 0
        numPicker.maxValue = 1000
        numPicker.value = MediaItemHelper.getSortIdField(item)
        numPicker.setOnValueChangedListener { _, _, newVal ->
            AppLogger.d("$DIALOG_TAG id set to:$newVal")
            MediaItemHelper.updateSortIdField(item, newVal)
        }
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = RSSettingsDialog::class.java.simpleName

        /**
         * Tag string to use in dialog transactions.
         */
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"
        private val KEY_MEDIA_ITEM = CLASS_NAME + "_KEY_MEDIA_ITEM"
        private val KEY_IS_LOCAL = CLASS_NAME + "_KEY_IS_LOCAL"
        private val KEY_IS_SORTABLE = CLASS_NAME + "_KEY_IS_SORTABLE"

        fun provideMediaItem(bundle: Bundle, mediaItem: MediaBrowserCompat.MediaItem) {
            bundle.putParcelable(KEY_MEDIA_ITEM, mediaItem)
        }

        fun provideIsLocal(bundle: Bundle, value: Boolean) {
            bundle.putBoolean(KEY_IS_LOCAL, value)
        }

        fun provideIsSortable(bundle: Bundle, value: Boolean) {
            bundle.putBoolean(KEY_IS_SORTABLE, value)
        }

        private fun extractIsLocal(bundle: Bundle?): Boolean {
            if (bundle == null) {
                return false
            }
            return if (!bundle.containsKey(KEY_IS_LOCAL)) {
                false
            } else bundle.getBoolean(KEY_IS_LOCAL, false)
        }

        private fun extractIsSortable(bundle: Bundle?): Boolean {
            if (bundle == null) {
                return false
            }
            return if (!bundle.containsKey(KEY_IS_SORTABLE)) {
                false
            } else bundle.getBoolean(KEY_IS_SORTABLE, false)
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
