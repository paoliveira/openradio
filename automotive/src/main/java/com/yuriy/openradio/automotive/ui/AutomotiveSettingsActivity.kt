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
package com.yuriy.openradio.automotive.ui

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.yuriy.openradio.shared.utils.UiUtils
import com.yuriy.openradio.shared.view.BaseDialogFragment

class AutomotiveSettingsActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showSettings()
    }

    private fun showSettings() {
        val transaction = supportFragmentManager.beginTransaction()
        UiUtils.clearDialogs(this, transaction)
        // Show Settings Dialog
        val fragment = BaseDialogFragment.newInstance(
            AutomotiveSettingsDialog::class.java.name
        )
        fragment.show(transaction, AutomotiveSettingsDialog.DIALOG_TAG)
    }
}