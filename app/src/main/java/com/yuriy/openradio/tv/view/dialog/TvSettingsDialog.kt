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
package com.yuriy.openradio.tv.view.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.utils.UiUtils.clearDialogs
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.dialog.AboutDialog
import com.yuriy.openradio.shared.view.dialog.GeneralSettingsDialog
import com.yuriy.openradio.shared.view.dialog.GoogleDriveDialog
import com.yuriy.openradio.shared.view.dialog.LogsDialog
import com.yuriy.openradio.shared.view.dialog.StreamBufferingDialog
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
                activity!!.findViewById(R.id.dialog_tv_settings_root)
        )
        setWindowDimensions(view, 0.9f, 0.9f)
        val context = context
        val titleText = context!!.getString(R.string.app_settings_title)
        val title = view.findViewById<TextView>(R.id.dialog_tv_settings_title_view)
        title.text = titleText
        val listview = view.findViewById<ListView>(R.id.settings_tv_list_view)
        // TODO: Refactor this and the same from activity_main_drawer to string resources
        val values = arrayOf(
                getString(R.string.main_menu_general),
                getString(R.string.main_menu_buffering),
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
        listview.adapter = adapter
        listview.onItemClickListener = AdapterView.OnItemClickListener {
            parent: AdapterView<*>?, view1: View?, position: Int, id: Long ->
            d(CLASS_NAME + " click:" + values[position])
            if (mIsInstanceSaved.get()) {
                return@OnItemClickListener
            }
            val ctx = activity ?: return@OnItemClickListener
            val transaction = ctx.supportFragmentManager.beginTransaction()
            clearDialogs(this@TvSettingsDialog.activity!!, transaction)
            when (position) {
                0 -> {
                    // Show Search Dialog
                    val settingsDialog = newInstance(
                            GeneralSettingsDialog::class.java.name
                    )
                    settingsDialog!!.show(transaction, GeneralSettingsDialog.DIALOG_TAG)
                }
                1 -> {
                    // Show Stream Buffering Dialog
                    val streamBufferingDialog = newInstance(
                            StreamBufferingDialog::class.java.name
                    )
                    streamBufferingDialog!!.show(transaction, StreamBufferingDialog.DIALOG_TAG)
                }
                2 -> {
                    // Show Google Drive Dialog
                    val googleDriveDialog = newInstance(
                            GoogleDriveDialog::class.java.name
                    )
                    googleDriveDialog!!.show(transaction, GoogleDriveDialog.DIALOG_TAG)
                }
                3 -> {
                    // Show Application Logs Dialog
                    val applicationLogsDialog = newInstance(
                            LogsDialog::class.java.name
                    )
                    applicationLogsDialog!!.show(transaction, LogsDialog.DIALOG_TAG)
                }
                4 -> {
                    // Show About Dialog
                    val aboutDialog = newInstance(
                            AboutDialog::class.java.name
                    )
                    aboutDialog!!.show(transaction, AboutDialog.DIALOG_TAG)
                }
                else -> {
                }
            }
        }
        return createAlertDialog(view)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mIsInstanceSaved.set(true)
    }

    private class ArrayAdapterExt(context: Context?, textViewResourceId: Int,
                                  objects: List<String>) :
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