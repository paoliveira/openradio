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
import android.os.Bundle
import android.view.View
import android.widget.*
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommonUi
import com.yuriy.openradio.shared.dependencies.MediaPresenterDependency
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import com.yuriy.openradio.shared.permission.PermissionChecker
import com.yuriy.openradio.shared.presenter.MediaPresenter
import com.yuriy.openradio.shared.service.LocationService
import com.yuriy.openradio.shared.service.OpenRadioService
import com.yuriy.openradio.shared.service.OpenRadioStore
import com.yuriy.openradio.shared.utils.*
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.SafeToast.showAnyThread
import com.yuriy.openradio.shared.view.list.CountriesArrayAdapter

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class GeneralSettingsDialog : BaseDialogFragment(), MediaPresenterDependency {

    private lateinit var mUserAgentEditView: EditText
    private lateinit var mMediaPresenter: MediaPresenter

    override fun configureWith(mediaPresenter: MediaPresenter) {
        mMediaPresenter = mediaPresenter
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        DependencyRegistryCommonUi.inject(this)
        val activity = requireActivity()
        val context = requireContext()
        val view = inflater.inflate(
            R.layout.dialog_general_settings,
            activity.findViewById(R.id.dialog_general_settings_root)
        )
        setWindowDimensions(view, 0.9f, 0.9f)
        val titleText = activity.getString(R.string.app_settings_title)
        val title = view.findTextView(R.id.dialog_settings_title_view)
        title.text = titleText
        val lastKnownRadioStationEnabled = AppPreferencesManager.lastKnownRadioStationEnabled(context)
        val lastKnownRadioStationEnableCheckView = view.findCheckBox(
            R.id.settings_dialog_enable_last_known_radio_station_check_view
        )
        lastKnownRadioStationEnableCheckView.isChecked = lastKnownRadioStationEnabled
        lastKnownRadioStationEnableCheckView.setOnClickListener { view1: View ->
            val checked = (view1 as CheckBox).isChecked
            AppPreferencesManager.lastKnownRadioStationEnabled(context, checked)
        }
        mUserAgentEditView = view.findEditText(R.id.user_agent_input_view)
        mUserAgentEditView.setText(AppUtils.getUserAgent(context))
        val userAgentCheckView = view.findCheckBox(R.id.user_agent_check_view)
        userAgentCheckView.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            AppPreferencesManager.isCustomUserAgent(context, isChecked)
            mUserAgentEditView.isEnabled = isChecked
        }
        val isCustomUserAgent = AppPreferencesManager.isCustomUserAgent(context)
        userAgentCheckView.isChecked = isCustomUserAgent
        mUserAgentEditView.isEnabled = isCustomUserAgent
        val masterVolumeSeekBar = view.findSeekBar(R.id.master_vol_seek_bar)
        masterVolumeSeekBar.progress =
            AppPreferencesManager.getMasterVolume(context, OpenRadioService.MASTER_VOLUME_DEFAULT)
        masterVolumeSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    context.startService(OpenRadioStore.makeMasterVolumeChangedIntent(context, seekBar.progress))
                }
            }
        )
        val btAutoRestart = view.findCheckBox(R.id.bt_auto_restart_check_view)
        btAutoRestart.isChecked = AppPreferencesManager.isBtAutoPlay(context)
        btAutoRestart.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (AppUtils.hasVersionS()) {
                if (!PermissionChecker.isBluetoothConnectGranted(context)) {
                    PermissionChecker.requestBluetoothPermission(activity, view)
                } else {
                    AppPreferencesManager.setBtAutoPlay(context, isChecked)
                }
            } else {
                AppPreferencesManager.setBtAutoPlay(context, isChecked)
            }
        }
        val clearCache = view.findButton(R.id.clear_cache_btn)
        clearCache.setOnClickListener {
            context.startService(OpenRadioStore.makeClearCacheIntent(context))
        }
        val array = LocationService.getCountriesWithLocation(context)
        val countryCode = mMediaPresenter.getCountryCode()
        var idx = 0
        for ((i, item) in array.withIndex()) {
            if (item.code == countryCode) {
                idx = i
                break
            }
        }

        val adapter = CountriesArrayAdapter(context, array)
        val spinner = view.findSpinner(R.id.default_country_spinner)
        spinner.adapter = adapter
        spinner.setSelection(idx)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val code = array[position].code
                mMediaPresenter.onLocationChanged(code)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        return createAlertDialog(view)
    }

    override fun onPause() {
        super.onPause()
        saveCustomUserAgent()
    }

    private fun saveCustomUserAgent() {
        if (this::mUserAgentEditView.isInitialized.not()) {
            return
        }
        val context = activity ?: return
        val userAgent = mUserAgentEditView.text.toString().trim { it <= ' ' }
        if (userAgent.isEmpty()) {
            showAnyThread(context, getString(R.string.user_agent_empty_warning))
            return
        }
        AppPreferencesManager.setCustomUserAgent(context, userAgent)
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = GeneralSettingsDialog::class.java.simpleName

        /**
         * Tag string to use in dialog transactions.
         */
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"
    }
}
