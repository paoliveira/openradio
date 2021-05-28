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
package com.yuriy.openradio.tv.view.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.UiUtils
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.dialog.AboutDialog
import com.yuriy.openradio.shared.view.dialog.GeneralSettingsDialog
import com.yuriy.openradio.shared.view.dialog.GoogleDriveDialog
import com.yuriy.openradio.shared.view.dialog.LogsDialog
import com.yuriy.openradio.shared.view.dialog.NetworkDialog
import com.yuriy.openradio.shared.view.dialog.SleepTimerDialog
import com.yuriy.openradio.shared.view.dialog.StreamBufferingDialog
import com.yuriy.openradio.tv.R
import java.util.*
import java.util.concurrent.atomic.*

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class TvSettingsDialog : BaseDialogFragment() {

    private val mIsInstanceSaved: AtomicBoolean = AtomicBoolean(false)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mIsInstanceSaved.set(false)
        val view = inflater.inflate(
            R.layout.dialog_tv_settings,
            requireActivity().findViewById(R.id.dialog_tv_settings_root)
        )
        setWindowDimensions(view, 0.9f, 0.9f)
        val context = context
        val titleText = context!!.getString(R.string.app_settings_title)
        val title = view.findViewById<TextView>(R.id.dialog_tv_settings_title_view)
        title.text = titleText
        val listView = view.findViewById<ListView>(R.id.settings_tv_list_view)
        // TODO: Refactor this and the same from activity_main_drawer to string resources
        val values = arrayOf(
            getString(R.string.main_menu_general),
            getString(R.string.main_menu_network),
            getString(R.string.main_menu_buffering),
            getString(R.string.main_menu_sleep_timer),
            getString(R.string.main_menu_google_drive),
            getString(R.string.main_menu_logs),
            getString(R.string.main_menu_about)
        )
        val list: MutableList<String> = ArrayList()
        Collections.addAll(list, *values)
        val adapter = ArrayAdapterExt(
            getContext(),
            android.R.layout.simple_list_item_1, list
        )
        listView.adapter = adapter
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                AppLogger.d("$CLASS_NAME click:${values[position]}")
                if (mIsInstanceSaved.get()) {
                    return@OnItemClickListener
                }
                val ctx = activity ?: return@OnItemClickListener
                val transaction = ctx.supportFragmentManager.beginTransaction()
                UiUtils.clearDialogs(this@TvSettingsDialog.requireActivity(), transaction)
                when (position) {
                    0 -> {
                        // Show Search Dialog
                        val dialog = newInstance(GeneralSettingsDialog::class.java.name)
                        dialog!!.show(transaction, GeneralSettingsDialog.DIALOG_TAG)
                    }
                    1 -> {
                        // Show Network Dialog
                        val dialog = newInstance(NetworkDialog::class.java.name)
                        dialog!!.show(transaction, NetworkDialog.DIALOG_TAG)
                    }
                    2 -> {
                        // Show Stream Buffering Dialog
                        val dialog = newInstance(StreamBufferingDialog::class.java.name)
                        dialog!!.show(transaction, StreamBufferingDialog.DIALOG_TAG)
                    }
                    3 -> {
                        // Show Sleep Timer Dialog
                        val dialog = newInstance(SleepTimerDialog::class.java.name)
                        dialog!!.show(transaction, SleepTimerDialog.DIALOG_TAG)
                    }
                    4 -> {
                        // Show Google Drive Dialog
                        val dialog = newInstance(GoogleDriveDialog::class.java.name)
                        dialog!!.show(transaction, GoogleDriveDialog.DIALOG_TAG)
                    }
                    5 -> {
                        // Show Application Logs Dialog
                        val dialog = newInstance(LogsDialog::class.java.name)
                        dialog!!.show(transaction, LogsDialog.DIALOG_TAG)
                    }
                    6 -> {
                        // Show About Dialog
                        val dialog = newInstance(AboutDialog::class.java.name)
                        dialog!!.show(transaction, AboutDialog.DIALOG_TAG)
                    }
                    else -> {
                        // No dialog found.
                    }
                }
            }
        return createAlertDialog(view)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mIsInstanceSaved.set(true)
    }

    private class ArrayAdapterExt(
        context: Context?, textViewResourceId: Int,
        objects: List<String>
    ) :
        ArrayAdapter<String?>(context!!, textViewResourceId, objects) {

        private val mMap: MutableMap<String?, Long> = HashMap()

        override fun getItemId(position: Int): Long {
            return mMap[getItem(position)] ?: 0
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        init {
            for (i in objects.indices) {
                mMap[objects[i]] = i.toLong()
            }
        }
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = TvSettingsDialog::class.java.simpleName

        /**
         * Tag string to use in dialog transactions.
         */
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"
    }
}
