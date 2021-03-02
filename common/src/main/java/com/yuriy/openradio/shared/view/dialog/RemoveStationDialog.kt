/*
 * Copyright 2017 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.view.BaseDialogFragment

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class RemoveStationDialog : BaseDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        val activity = activity as MainActivity?
        val view = inflater.inflate(
                R.layout.dialog_remove_station,
                activity!!.findViewById(R.id.remove_station_dialog_root)
        )
        setWindowDimensions(view, 0.8f, 0.2f)
        val mediaId = getArgument(arguments, KEY_MEDIA_ID)
        val name = getArgument(arguments, KEY_NAME)
        val textView = view.findViewById<TextView>(R.id.remove_station_text_view)
        textView.text = getString(R.string.remove_station_dialog_main_text, name)
        val removeBtn = view.findViewById<Button>(R.id.remove_station_dialog_add_btn_view)
        removeBtn.setOnClickListener {
            //TODO: FIX ME
//            activity.processRemoveStationCallback(mediaId)
            dialog!!.dismiss()
        }
        val cancelBtn = view.findViewById<Button>(R.id.remove_station_dialog_cancel_btn_view)
        cancelBtn.setOnClickListener { viewBtn: View? -> dialog!!.dismiss() }
        return createAlertDialog(view)
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = RemoveStationDialog::class.java.simpleName

        /**
         * Tag string to use in dialog transactions.
         */
        @JvmField
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"

        /**
         * Key for the Media Id value.
         */
        private const val KEY_MEDIA_ID = "KEY_MEDIA_ID"

        /**
         * Key for the Name value.
         */
        private const val KEY_NAME = "KEY_NAME"
        @JvmStatic
        fun createBundle(mediaId: String?, name: String?): Bundle {
            val bundle = Bundle()
            bundle.putString(KEY_MEDIA_ID, mediaId)
            bundle.putString(KEY_NAME, name)
            return bundle
        }

        /**
         * Extract argument from the Bundle.
         *
         * @param bundle Arguments [Bundle].
         * @param key    Key of the argument.
         *
         * @return Value associated with the provided key, or an empty string.
         */
        private fun getArgument(bundle: Bundle?, key: String): String? {
            if (bundle == null) {
                return ""
            }
            return if (bundle.containsKey(key)) {
                bundle.getString(key)
            } else ""
        }
    }
}
