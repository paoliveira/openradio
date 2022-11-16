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
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommonUi
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.findButton
import com.yuriy.openradio.shared.utils.findTextView
import com.yuriy.openradio.shared.view.BaseDialogFragment

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class RemoveStationDialog : BaseDialogFragment() {

    private lateinit var mRemoveStationDialogPresenter: RemoveStationDialogPresenter

    fun configureWith(presenter: RemoveStationDialogPresenter) {
        mRemoveStationDialogPresenter = presenter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DependencyRegistryCommonUi.inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflater.inflate(
                R.layout.dialog_remove_station,
                requireActivity().findViewById(R.id.remove_station_dialog_root)
        )
        setWindowDimensions(view, 0.8f, 0.2f)
        val mediaId = getArgument(arguments, KEY_MEDIA_ID)
        val name = getArgument(arguments, KEY_NAME)
        val textView = view.findTextView(R.id.remove_station_text_view)
        textView.text = getString(R.string.remove_station_dialog_main_text, name)
        val removeBtn = view.findButton(R.id.remove_station_dialog_add_btn_view)
        removeBtn.setOnClickListener {
            mRemoveStationDialogPresenter.removeRadioStation(mediaId)
            dialog?.dismiss()
        }
        val cancelBtn = view.findButton(R.id.remove_station_dialog_cancel_btn_view)
        cancelBtn.setOnClickListener { dialog?.dismiss() }
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
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"

        /**
         * Key for the Media Id value.
         */
        private const val KEY_MEDIA_ID = "KEY_MEDIA_ID"

        /**
         * Key for the Name value.
         */
        private const val KEY_NAME = "KEY_NAME"

        fun makeBundle(mediaId: String?, name: String?): Bundle {
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
                return AppUtils.EMPTY_STRING
            }
            return if (bundle.containsKey(key)) {
                bundle.getString(key)
            } else AppUtils.EMPTY_STRING
        }
    }
}
