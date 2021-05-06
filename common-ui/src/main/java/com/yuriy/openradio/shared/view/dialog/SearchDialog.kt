/*
 * Copyright 2017-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.os.Parcel
import android.os.Parcelable
import android.widget.Button
import android.widget.EditText
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.view.BaseDialogFragment

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class SearchDialog : BaseDialogFragment() {

    interface Listener : Parcelable {

        fun onSuccess(queryBundle: Bundle)

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel?, flags: Int) {

        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflater.inflate(
            R.layout.dialog_search,
            activity!!.findViewById(R.id.dialog_search_root)
        )
        setWindowDimensions(view, 0.8f, 0.4f)
        val searchEditView = view.findViewById<EditText>(R.id.search_dialog_edit_txt_view)
        val searchBtn = view.findViewById<Button>(R.id.search_dialog_btn_view)
        searchBtn.setOnClickListener {
            getListener(arguments)?.onSuccess(AppUtils.makeSearchQueryBundle(searchEditView.text.toString().trim()))
            dialog!!.dismiss()
        }
        return createAlertDialog(view)
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = SearchDialog::class.java.simpleName

        /**
         * Tag string to use in dialog transactions.
         */
        @JvmField
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"

        private const val KEY_LISTENER = "KEY_LISTENER"

        @JvmStatic
        fun makeNewInstanceBundle(listener: Listener): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(KEY_LISTENER, listener)
            return bundle
        }

        private fun getListener(bundle: Bundle?): Listener? {
            if (bundle == null) {
                return null
            }
            return if (bundle.containsKey(KEY_LISTENER)) {
                bundle.get(KEY_LISTENER) as Listener
            } else null
        }
    }
}
