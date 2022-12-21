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

package com.yuriy.openradio.automotive.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.SeekBar
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.yuriy.openradio.automotive.R
import com.yuriy.openradio.automotive.dependencies.DependencyRegistryAutomotive
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommonUi
import com.yuriy.openradio.shared.dependencies.MediaPresenterDependency
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import com.yuriy.openradio.shared.model.storage.drive.GoogleDriveError
import com.yuriy.openradio.shared.model.storage.drive.GoogleDriveManager
import com.yuriy.openradio.shared.presenter.MediaPresenter
import com.yuriy.openradio.shared.service.LocationService
import com.yuriy.openradio.shared.service.OpenRadioService
import com.yuriy.openradio.shared.service.OpenRadioStore
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.IntentUtils
import com.yuriy.openradio.shared.utils.findButton
import com.yuriy.openradio.shared.utils.findCheckBox
import com.yuriy.openradio.shared.utils.findEditText
import com.yuriy.openradio.shared.utils.findProgressBar
import com.yuriy.openradio.shared.utils.findSeekBar
import com.yuriy.openradio.shared.utils.findSpinner
import com.yuriy.openradio.shared.utils.findTextView
import com.yuriy.openradio.shared.utils.findToolbar
import com.yuriy.openradio.shared.utils.gone
import com.yuriy.openradio.shared.utils.visible
import com.yuriy.openradio.shared.view.SafeToast
import com.yuriy.openradio.shared.view.dialog.StreamBufferingDialog
import com.yuriy.openradio.shared.view.list.CountriesArrayAdapter

class AutomotiveSettingsActivity : AppCompatActivity(), MediaPresenterDependency {

    private lateinit var mMinBuffer: EditText
    private lateinit var mMaxBuffer: EditText
    private lateinit var mPlayBuffer: EditText
    private lateinit var mPlayBufferRebuffer: EditText
    private lateinit var mProgressBarUpload: ProgressBar
    private lateinit var mProgressBarDownload: ProgressBar
    private lateinit var mGoogleDriveManager: GoogleDriveManager
    private lateinit var mLauncher: ActivityResultLauncher<Intent>
    private lateinit var mMediaPresenter: MediaPresenter
    private lateinit var mPresenter: AutomotiveSettingsActivityPresenter

    override fun configureWith(mediaPresenter: MediaPresenter) {
        mMediaPresenter = mediaPresenter
    }

    fun configureWith(presenter: AutomotiveSettingsActivityPresenter) {
        mPresenter = presenter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.automotive_activity_settings)

        DependencyRegistryCommonUi.inject(this)
        DependencyRegistryAutomotive.inject(this)

        val toolbar = findToolbar(R.id.automotive_settings_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val lastKnownRsEnabled = AppPreferencesManager.lastKnownRadioStationEnabled(applicationContext)
        val lastKnownRsEnableCheckView = findCheckBox(
            R.id.automotive_settings_enable_last_known_radio_station_check_view
        )
        lastKnownRsEnableCheckView.isChecked = lastKnownRsEnabled
        lastKnownRsEnableCheckView.setOnClickListener { view1: View ->
            val checked = (view1 as CheckBox).isChecked
            AppPreferencesManager.lastKnownRadioStationEnabled(applicationContext, checked)
        }

        val clearCache = findButton(R.id.automotive_settings_clear_cache_btn)
        clearCache.setOnClickListener {
            startService(OpenRadioStore.makeClearCacheIntent(applicationContext))
        }

        val array = LocationService.getCountries()
        val countryCode = mPresenter.getCountryCode()
        var idx = 0
        for ((i, item) in array.withIndex()) {
            if (item.code == countryCode) {
                idx = i
                break
            }
        }
        val adapter = CountriesArrayAdapter(applicationContext, array)
        val spinner = findSpinner(R.id.automotive_settings_default_country_spinner)
        spinner.adapter = adapter
        spinner.setSelection(idx)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val code = array[position].code
                mMediaPresenter.onLocationChanged(code)
                startService(OpenRadioStore.makeUpdateTreeIntent(applicationContext))
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val masterVolumeSeekBar = findSeekBar(R.id.automotive_master_vol_seek_bar)
        masterVolumeSeekBar.progress =
            AppPreferencesManager.getMasterVolume(applicationContext, OpenRadioService.MASTER_VOLUME_DEFAULT)
        masterVolumeSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    startService(OpenRadioStore.makeMasterVolumeChangedIntent(applicationContext, seekBar.progress))
                }
            }
        )

        val descView = findTextView(R.id.automotive_stream_buffering_desc_view)
        try {
            descView.text = String.format(
                resources.getString(R.string.stream_buffering_descr_automotive),
                resources.getInteger(R.integer.min_buffer_val),
                resources.getInteger(R.integer.max_buffer_val),
                resources.getInteger(R.integer.min_buffer_sec),
                resources.getInteger(R.integer.max_buffer_min)
            )
        } catch (e: Exception) {
            /* Ignore */
        }

        mMinBuffer = findEditText(R.id.automotive_min_buffer_edit_view)
        mMaxBuffer = findEditText(R.id.automotive_max_buffer_edit_view)
        mPlayBuffer = findEditText(R.id.automotive_play_buffer_edit_view)
        mPlayBufferRebuffer = findEditText(R.id.automotive_rebuffer_edit_view)
        val restoreBtn = findButton(R.id.automotive_buffering_restore_btn)
        restoreBtn.setOnClickListener {
            StreamBufferingDialog.handleRestoreButton(mMinBuffer, mMaxBuffer, mPlayBuffer, mPlayBufferRebuffer)
        }

        StreamBufferingDialog.handleOnCreate(
            applicationContext,
            mMinBuffer,
            mMaxBuffer,
            mPlayBuffer,
            mPlayBufferRebuffer
        )

        // TODO: Refactor GDrive to handle in single place:

        val uploadTo = findButton(R.id.automotive_upload_to_google_drive_btn)
        val downloadFrom = findButton(R.id.automotive_download_from_google_drive_btn)
        mProgressBarUpload = findProgressBar(R.id.automotive_upload_to_google_drive_progress)
        mProgressBarDownload = findProgressBar(R.id.automotive_download_to_google_drive_progress)

        uploadTo.setOnClickListener { uploadRadioStationsToGoogleDrive() }
        downloadFrom.setOnClickListener { downloadRadioStationsFromGoogleDrive() }

        val listener = GoogleDriveManagerListenerImpl()
        mGoogleDriveManager = GoogleDriveManager(applicationContext, listener)

        hideProgress(GoogleDriveManager.Command.UPLOAD)
        hideProgress(GoogleDriveManager.Command.DOWNLOAD)

        mLauncher = IntentUtils.registerForActivityResultIntrl(
            this, ::onActivityResultCallback
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        hideProgress(GoogleDriveManager.Command.UPLOAD)
        hideProgress(GoogleDriveManager.Command.DOWNLOAD)
        mGoogleDriveManager.disconnect()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onPause() {
        super.onPause()
        StreamBufferingDialog.handleOnPause(
            applicationContext,
            mMinBuffer,
            mMaxBuffer,
            mPlayBuffer,
            mPlayBufferRebuffer
        )
    }

    private fun onActivityResultCallback(data: Intent?) {
        GoogleSignIn
            .getSignedInAccountFromIntent(data)
            .addOnSuccessListener { googleAccount: GoogleSignInAccount ->
                mGoogleDriveManager.connect(googleAccount.account)
            }
            .addOnFailureListener { exception: Exception? ->
                AppLogger.e("Can't do sign in", exception)
                SafeToast.showAnyThread(
                    applicationContext, getString(R.string.can_not_get_account_name)
                )
            }
    }

    private fun uploadRadioStationsToGoogleDrive() {
        mGoogleDriveManager.uploadRadioStations()
    }

    private fun downloadRadioStationsFromGoogleDrive() {
        mGoogleDriveManager.downloadRadioStations()
    }

    private fun showProgress(command: GoogleDriveManager.Command) {
        when (command) {
            GoogleDriveManager.Command.UPLOAD -> runOnUiThread {
                mProgressBarUpload.visible()
            }
            GoogleDriveManager.Command.DOWNLOAD -> runOnUiThread {
                mProgressBarDownload.visible()
            }
        }
    }

    private fun hideProgress(command: GoogleDriveManager.Command) {
        when (command) {
            GoogleDriveManager.Command.UPLOAD -> runOnUiThread {
                mProgressBarUpload.gone()
            }
            GoogleDriveManager.Command.DOWNLOAD -> runOnUiThread {
                mProgressBarDownload.gone()
            }
        }
    }

    private inner class GoogleDriveManagerListenerImpl : GoogleDriveManager.Listener {

        override fun onAccountRequested(client: GoogleSignInClient) {
            mLauncher.launch(client.signInIntent)
        }

        override fun onStart(command: GoogleDriveManager.Command) {
            showProgress(command)
        }

        override fun onSuccess(command: GoogleDriveManager.Command) {
            val context = this@AutomotiveSettingsActivity.applicationContext
            if (context == null) {
                AppLogger.e("Can not handle Google Drive success, context is null")
                return
            }
            val message = when (command) {
                GoogleDriveManager.Command.UPLOAD -> context.getString(R.string.google_drive_data_saved)
                GoogleDriveManager.Command.DOWNLOAD -> {
                    mMediaPresenter.updateRootView()
                    context.getString(R.string.google_drive_data_read)
                }
            }
            SafeToast.showAnyThread(context, message)
            hideProgress(command)
        }

        override fun onError(command: GoogleDriveManager.Command, error: GoogleDriveError?) {
            val context = this@AutomotiveSettingsActivity.applicationContext
            if (context == null) {
                AppLogger.e("Can not handle Google Drive error, context is null, error:$error")
                return
            }
            val message = when (command) {
                GoogleDriveManager.Command.UPLOAD -> context.getString(R.string.google_drive_error_when_save)
                GoogleDriveManager.Command.DOWNLOAD -> context.getString(R.string.google_drive_error_when_read)
            }
            SafeToast.showAnyThread(context, message)
            hideProgress(command)
        }
    }
}
