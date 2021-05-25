/*
 * Copyright 2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import com.google.android.exoplayer2.ExoPlayerLibraryInfo
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.model.storage.NetworkSettingsStorage
import com.yuriy.openradio.shared.service.OpenRadioService
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.IntentUtils
import com.yuriy.openradio.shared.view.BaseDialogFragment

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class NetworkDialog : BaseDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflater.inflate(
            R.layout.dialog_network,
            activity!!.findViewById(R.id.dialog_network_root)
        )
        setWindowDimensions(view, 0.8f, 0.3f)
        val useMobileCheckBox = view.findViewById<CheckBox>(R.id.use_mobile_network_check_box)
        useMobileCheckBox.isChecked = NetworkSettingsStorage.getUseMobile(context!!)
        useMobileCheckBox.setOnCheckedChangeListener { _, isChecked ->
            NetworkSettingsStorage.setUseMobile(
                context!!,
                isChecked
            )
            context!!.startService(OpenRadioService.makeNetworkSettingsChangedIntent(context))
        }
        return createAlertDialog(view)
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = NetworkDialog::class.java.simpleName

        /**
         * Tag string to use in dialog transactions.
         */
        @JvmField
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"
    }
}
