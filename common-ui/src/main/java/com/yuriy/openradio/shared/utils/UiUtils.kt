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

package com.yuriy.openradio.shared.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.yuriy.openradio.shared.view.dialog.AboutDialog
import com.yuriy.openradio.shared.view.dialog.EditStationDialog
import com.yuriy.openradio.shared.view.dialog.EqualizerDialog
import com.yuriy.openradio.shared.view.dialog.GeneralSettingsDialog
import com.yuriy.openradio.shared.view.dialog.GoogleDriveDialog
import com.yuriy.openradio.shared.view.dialog.NetworkDialog
import com.yuriy.openradio.shared.view.dialog.RSSettingsDialog
import com.yuriy.openradio.shared.view.dialog.SearchDialog
import com.yuriy.openradio.shared.view.dialog.SleepTimerDialog

object UiUtils {

    /**
     * Clears any active dialog.
     *
     * @param context [FragmentActivity]
     * @param transaction Instance of Fragment transaction.
     */
    fun clearDialogs(context: FragmentActivity, transaction: FragmentTransaction) {
        val manager = context.supportFragmentManager
        removeFragment(transaction, manager.findFragmentByTag(AboutDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(SearchDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(EqualizerDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(GoogleDriveDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(GeneralSettingsDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(RSSettingsDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(EditStationDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(SleepTimerDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(NetworkDialog.DIALOG_TAG))
        transaction.commitNow()
    }

    private fun removeFragment(transaction: FragmentTransaction, fragment: Fragment?) {
        if (fragment != null) {
            transaction.remove(fragment)
        }
    }
}
