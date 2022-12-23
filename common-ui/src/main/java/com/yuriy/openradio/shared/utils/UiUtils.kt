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

import android.app.Activity
import android.graphics.BitmapFactory
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yuriy.openradio.shared.view.dialog.AboutDialog
import com.yuriy.openradio.shared.view.dialog.AddStationDialog
import com.yuriy.openradio.shared.view.dialog.EditStationDialog
import com.yuriy.openradio.shared.view.dialog.EqualizerDialog
import com.yuriy.openradio.shared.view.dialog.GeneralSettingsDialog
import com.yuriy.openradio.shared.view.dialog.GoogleDriveDialog
import com.yuriy.openradio.shared.view.dialog.NetworkDialog
import com.yuriy.openradio.shared.view.dialog.RSSettingsDialog
import com.yuriy.openradio.shared.view.dialog.RemoveStationDialog
import com.yuriy.openradio.shared.view.dialog.SearchDialog
import com.yuriy.openradio.shared.view.dialog.SleepTimerDialog
import com.yuriy.openradio.shared.view.dialog.StreamBufferingDialog

fun Activity.findTextView(id: Int): TextView {
    return findViewById(id)
}

fun Activity.findButton(id: Int): Button {
    return findViewById(id)
}

fun Activity.findCheckBox(id: Int): CheckBox {
    return findViewById(id)
}

fun Activity.findImageView(id: Int): ImageView {
    return findViewById(id)
}

fun Activity.findView(id: Int): View {
    return findViewById(id)
}

fun Activity.findSpinner(id: Int): Spinner {
    return findViewById(id)
}

fun Activity.findSeekBar(id: Int): SeekBar {
    return findViewById(id)
}

fun Activity.findToolbar(id: Int): Toolbar {
    return findViewById(id)
}

fun Activity.findProgressBar(id: Int): ProgressBar {
    return findViewById(id)
}

fun Activity.findFloatingActionButton(id: Int): FloatingActionButton {
    return findViewById(id)
}

fun Activity.findEditText(id: Int): EditText {
    return findViewById(id)
}

fun View?.visible() {
    this?.visibility = View.VISIBLE
}

fun View?.invisible() {
    this?.visibility = View.INVISIBLE
}

fun View?.gone() {
    this?.visibility = View.GONE
}

fun View.findTextView(id: Int): TextView {
    return findViewById(id)
}

fun View.findButton(id: Int): Button {
    return findViewById(id)
}

fun View.findToggleButton(id: Int): ToggleButton {
    return findViewById(id)
}

fun View.findCheckBox(id: Int): CheckBox {
    return findViewById(id)
}

fun View.findLinearLayout(id: Int): LinearLayout {
    return findViewById(id)
}

fun View.findImageView(id: Int): ImageView {
    return findViewById(id)
}

fun View.findView(id: Int): View {
    return findViewById(id)
}

fun View.findEditText(id: Int): EditText {
    return findViewById(id)
}

fun View.findSpinner(id: Int): Spinner {
    return findViewById(id)
}

fun View.findSeekBar(id: Int): SeekBar {
    return findViewById(id)
}

fun ImageView.setImageBitmap(bytes: ByteArray) {
    if (bytes.isEmpty()) {
        return
    }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    this.setImageBitmap(bitmap)
}

object UiUtils {

    /**
     * Clears any active dialog.
     *
     * @param manager Reference to the fragment manager.
     * @param transaction Instance of Fragment transaction.
     */
    fun clearDialogs(manager: FragmentManager, transaction: FragmentTransaction) {
        removeFragment(transaction, manager.findFragmentByTag(AboutDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(AddStationDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(EditStationDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(EqualizerDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(GeneralSettingsDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(GoogleDriveDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(NetworkDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(RemoveStationDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(RSSettingsDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(SearchDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(SleepTimerDialog.DIALOG_TAG))
        removeFragment(transaction, manager.findFragmentByTag(StreamBufferingDialog.DIALOG_TAG))
        try {
            transaction.commitNow()
        } catch (exception: IllegalStateException) {
            AppLogger.e("Can't clear dialogs", exception)
        }
    }

    private fun removeFragment(transaction: FragmentTransaction, fragment: Fragment?) {
        if (fragment != null) {
            transaction.remove(fragment)
        }
    }
}
