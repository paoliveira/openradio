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
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.TextView
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.service.OpenRadioService
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.MediaIdHelper
import com.yuriy.openradio.shared.utils.MediaItemHelper
import com.yuriy.openradio.shared.view.BaseDialogFragment

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class RSSettingsDialog : BaseDialogFragment() {

    private var mParentCategoryId = AppUtils.EMPTY_STRING
    private var mSortMediaId = AppUtils.EMPTY_STRING
    private var mSortNewPosition = 0
    private var mSortOriginalPosition = 0

    override fun onCreateDialog(savedInstance: Bundle?): Dialog {
        val view = inflater.inflate(
                R.layout.dialog_rs_settings,
                requireActivity().findViewById(R.id.dialog_rs_settings_root)
        )
        setWindowDimensions(view, 0.8f, 0.3f)
        val item = extractMediaItem(arguments)
        if (item != null) {
            handleUi(view, item, arguments)
        } else {
            //TODO: Address null data
            AppLogger.e("Setting dialog provided with null data")
        }
        return createAlertDialog(view)
    }

    override fun onPause() {
        if (mSortNewPosition != mSortOriginalPosition) {
            val ctx = requireContext()
            ctx.startService(
                OpenRadioService.makeUpdateSortIdsIntent(ctx, mSortMediaId, mSortNewPosition, mParentCategoryId)
            )
        }
        super.onPause()
    }

    private fun handleUi(view: View, item: MediaBrowserCompat.MediaItem, args: Bundle?) {
        mParentCategoryId = extractParentId(args)
        mSortMediaId = item.mediaId.toString()
        val isLocal = MediaIdHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST == mParentCategoryId
        val isSortable = MediaIdHelper.isMediaIdSortable(mParentCategoryId)
        view.findViewById<View>(R.id.dialog_rs_settings_edit_remove).visibility =
                if (isLocal) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.dialog_rs_settings_sort).visibility =
                if (isSortable) View.VISIBLE else View.GONE

        view.findViewById<View>(R.id.dialog_rs_settings_edit_btn).tag = item
        view.findViewById<View>(R.id.dialog_rs_settings_remove_btn).tag = item
        val name = view.findViewById<TextView>(R.id.dialog_rs_settings_rs_name)
        name.text = item.description.title

        val imageView = view.findViewById<ImageView>(R.id.dialog_rs_settings_logo_view)
        // At this point Image should be fetched from the internet or local file system.
        imageView.setImageURI(item.description.iconUri)

        if (isSortable) {
            handleSortUi(view, item, args)
        }
    }

    private fun handleSortUi(view: View, item: MediaBrowserCompat.MediaItem, args: Bundle?) {
        val maxId = extractMaxId(args)
        val numPicker = view.findViewById<NumberPicker>(R.id.sort_id_picker)
        numPicker.minValue = 0
        numPicker.maxValue = maxId
        numPicker.value = MediaItemHelper.getSortIdField(item)
        mSortNewPosition = numPicker.value
        mSortOriginalPosition = numPicker.value
        numPicker.setOnValueChangedListener { _, _, newVal ->
            mSortNewPosition = newVal
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
        private val KEY_PARENT_ID = CLASS_NAME + "_KEY_PARENT_ID"
        private val KEY_MAX_SORT_ID = CLASS_NAME + "_KEY_MAX_SORT_ID"

        fun provideMediaItem(bundle: Bundle,
                             mediaItem: MediaBrowserCompat.MediaItem,
                             parentId: String, maxSortId: Int) {
            bundle.putParcelable(KEY_MEDIA_ITEM, mediaItem)
            bundle.putString(KEY_PARENT_ID, parentId)
            bundle.putInt(KEY_MAX_SORT_ID, maxSortId)
        }

        private fun extractParentId(bundle: Bundle?): String {
            if (bundle == null) {
                return AppUtils.EMPTY_STRING
            }
            return if (!bundle.containsKey(KEY_PARENT_ID)) {
                AppUtils.EMPTY_STRING
            } else bundle.getString(KEY_PARENT_ID, AppUtils.EMPTY_STRING)
        }

        private fun extractMaxId(bundle: Bundle?): Int {
            if (bundle == null) {
                return 1000
            }
            return if (!bundle.containsKey(KEY_MAX_SORT_ID)) {
                1000
            } else bundle.getInt(KEY_MAX_SORT_ID, 1000)
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
