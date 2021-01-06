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
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.broadcast.AppLocalBroadcast
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.SafeToast.showAnyThread

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class GeneralSettingsDialog : BaseDialogFragment() {

    private var mUserAgentEditView: EditText? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflater.inflate(
                R.layout.dialog_general_settings,
                activity!!.findViewById(R.id.dialog_general_settings_root)
        )
        setWindowDimensions(view, 0.9f, 0.9f)
        val titleText = activity!!.getString(R.string.app_settings_title)
        val title = view.findViewById<TextView>(R.id.dialog_settings_title_view)
        title.text = titleText
        val context: Context? = activity
        val lastKnownRadioStationEnabled = AppPreferencesManager.lastKnownRadioStationEnabled(context!!)
        val lastKnownRadioStationEnableCheckView = view.findViewById<CheckBox>(
                R.id.settings_dialog_enable_last_known_radio_station_check_view
        )
        lastKnownRadioStationEnableCheckView.isChecked = lastKnownRadioStationEnabled
        lastKnownRadioStationEnableCheckView.setOnClickListener { view1: View ->
            val checked = (view1 as CheckBox).isChecked
            AppPreferencesManager.lastKnownRadioStationEnabled(context, checked)
        }
        mUserAgentEditView = view.findViewById(R.id.user_agent_input_view)
        mUserAgentEditView?.setText(AppUtils.getUserAgent(context))
        val userAgentCheckView = view.findViewById<CheckBox>(R.id.user_agent_check_view)
        userAgentCheckView.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            AppPreferencesManager.isCustomUserAgent(context, isChecked)
            mUserAgentEditView?.isEnabled = isChecked
        }
        val isCustomUserAgent = AppPreferencesManager.isCustomUserAgent(context)
        userAgentCheckView.isChecked = isCustomUserAgent
        mUserAgentEditView?.isEnabled = isCustomUserAgent
        val masterVolumeSeekBar = view.findViewById<SeekBar>(R.id.master_vol_seek_bar)
        masterVolumeSeekBar.progress = AppPreferencesManager.getMasterVolume(context)
        masterVolumeSeekBar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar,
                                                   progress: Int,
                                                   fromUser: Boolean) {
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        AppPreferencesManager.setMasterVolume(context, seekBar.progress)
                        LocalBroadcastManager.getInstance(context).sendBroadcast(
                                AppLocalBroadcast.createIntentMasterVolumeChanged()
                        )
                    }
                }
        )
        val btAutoRestart = view.findViewById<CheckBox>(R.id.bt_auto_restart_check_view)
        btAutoRestart.isChecked = AppPreferencesManager.isBtAutoPlay(context)
        btAutoRestart.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            AppPreferencesManager.setBtAutoPlay(context, isChecked)
        }
        val clearCache = view.findViewById<Button>(R.id.clear_cache_btn)
        clearCache.setOnClickListener { v: View? ->
            LocalBroadcastManager.getInstance(context).sendBroadcast(
                    AppLocalBroadcast.createIntentClearCache()
            )
        }
        return createAlertDialog(view)
    }

    override fun onPause() {
        super.onPause()
        saveCustomUserAgent()
    }

    private fun saveCustomUserAgent() {
        if (mUserAgentEditView == null) {
            return
        }
        val context = activity ?: return
        val userAgent = mUserAgentEditView!!.text.toString().trim { it <= ' ' }
        if (userAgent.isEmpty()) {
            showAnyThread(context, getString(R.string.user_agent_empty_warning))
            return
        }
        AppPreferencesManager.setCustomUserAgent(context, userAgent)
    }

    companion object {
        /**
         * Tag string mTo use in logging message.
         */
        private val CLASS_NAME = GeneralSettingsDialog::class.java.simpleName

        /**
         * Tag string mTo use in dialog transactions.
         */
        @JvmField
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"
    }
}
