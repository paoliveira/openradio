/*
 * Copyright 2017-2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.vo.RadioStationToAdd

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *
 * Dialog view to handle Add Radio Station functionality.
 */
class AddStationDialog : BaseAddEditStationDialog() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        dialog?.setTitle(R.string.add_station_dialog_title)
        return view
    }

    /**
     * Validate provided input in order to pass data farther to
     * generate [RadioStationToAdd].
     *
     * @param radioStationToAdd
     */
    override fun processInput(radioStationToAdd: RadioStationToAdd) {
        val context = activity?.applicationContext ?: return
        mPresenter.addRadioStation(
            context, radioStationToAdd,
            { msg -> onSuccess(msg) },
            { msg -> onFailure(msg) }
        )
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = AddStationDialog::class.java.simpleName

        /**
         * Tag string to use in dialog transactions.
         */
        val DIALOG_TAG = "${CLASS_NAME}_DIALOG_TAG"
    }
}
