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
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommonUi
import com.yuriy.openradio.shared.model.storage.NetworkSettingsStorage
import com.yuriy.openradio.shared.service.OpenRadioStore
import com.yuriy.openradio.shared.utils.findCheckBox
import com.yuriy.openradio.shared.view.BaseDialogFragment

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class NetworkDialog : BaseDialogFragment() {

    private lateinit var mNetworkSettingsStorage: NetworkSettingsStorage

    fun configureWith(locationStorage: NetworkSettingsStorage) {
        mNetworkSettingsStorage = locationStorage
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        DependencyRegistryCommonUi.inject(this)
        val view = inflater.inflate(
            R.layout.dialog_network,
            requireActivity().findViewById(R.id.dialog_network_root)
        )
        setWindowDimensions(view, 0.8f, 0.3f)
        val ctx = requireContext()
        val useMobileCheckBox = view.findCheckBox(R.id.use_mobile_network_check_box)
        useMobileCheckBox.isChecked = mNetworkSettingsStorage.getUseMobile()
        useMobileCheckBox.setOnCheckedChangeListener { _, isChecked ->
            mNetworkSettingsStorage.setUseMobile(isChecked)
            ctx.startService(OpenRadioStore.makeNetworkSettingsChangedIntent(ctx))
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
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"
    }
}
