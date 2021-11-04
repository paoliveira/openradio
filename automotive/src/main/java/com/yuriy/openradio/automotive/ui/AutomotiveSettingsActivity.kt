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
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.yuriy.openradio.automotive.R
import com.yuriy.openradio.shared.broadcast.AppLocalBroadcast
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import com.yuriy.openradio.shared.model.storage.drive.GoogleDriveError
import com.yuriy.openradio.shared.model.storage.drive.GoogleDriveManager
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.IntentUtils
import com.yuriy.openradio.shared.view.SafeToast
import com.yuriy.openradio.shared.view.dialog.LogsDialog
import com.yuriy.openradio.shared.view.dialog.StreamBufferingDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutomotiveSettingsActivity : AppCompatActivity() {

    private lateinit var mMinBuffer: EditText
    private lateinit var mMaxBuffer: EditText
    private lateinit var mPlayBuffer: EditText
    private lateinit var mPlayBufferRebuffer: EditText
    private lateinit var mProgressBarUpload: ProgressBar
    private lateinit var mProgressBarDownload: ProgressBar
    private lateinit var mGoogleDriveManager: GoogleDriveManager
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.automotive_activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.automotive_settings_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val lastKnownRsEnabled = AppPreferencesManager.lastKnownRadioStationEnabled(applicationContext)
        val lastKnownRsEnableCheckView = findViewById<CheckBox>(
            R.id.automotive_settings_enable_last_known_radio_station_check_view
        )
        lastKnownRsEnableCheckView.isChecked = lastKnownRsEnabled
        lastKnownRsEnableCheckView.setOnClickListener { view1: View ->
            val checked = (view1 as CheckBox).isChecked
            AppPreferencesManager.lastKnownRadioStationEnabled(applicationContext, checked)
        }

        val clearCache = findViewById<Button>(R.id.automotive_settings_clear_cache_btn)
        clearCache.setOnClickListener {
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(
                AppLocalBroadcast.createIntentClearCache()
            )
        }

        val masterVolumeSeekBar = findViewById<SeekBar>(R.id.automotive_master_vol_seek_bar)
        masterVolumeSeekBar.progress = AppPreferencesManager.getMasterVolume(applicationContext)
        masterVolumeSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    AppPreferencesManager.setMasterVolume(applicationContext, seekBar.progress)
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(
                        AppLocalBroadcast.createIntentMasterVolumeChanged()
                    )
                }
            }
        )

        val descView = findViewById<TextView>(R.id.automotive_stream_buffering_desc_view)
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

        mMinBuffer = findViewById(R.id.automotive_min_buffer_edit_view)
        mMaxBuffer = findViewById(R.id.automotive_max_buffer_edit_view)
        mPlayBuffer = findViewById(R.id.automotive_play_buffer_edit_view)
        mPlayBufferRebuffer = findViewById(R.id.automotive_rebuffer_edit_view)
        val restoreBtn = findViewById<Button>(R.id.automotive_buffering_restore_btn)
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

        val uploadTo = findViewById<Button>(R.id.automotive_upload_to_google_drive_btn)
        val downloadFrom = findViewById<Button>(R.id.automotive_download_from_google_drive_btn)
        mProgressBarUpload = findViewById(R.id.automotive_upload_to_google_drive_progress)
        mProgressBarDownload = findViewById(R.id.automotive_download_to_google_drive_progress)

        uploadTo.setOnClickListener { uploadRadioStationsToGoogleDrive() }
        downloadFrom.setOnClickListener { downloadRadioStationsFromGoogleDrive() }

        val listener = GoogleDriveManagerListenerImpl()
        mGoogleDriveManager = GoogleDriveManager(applicationContext, listener)

        hideProgress(GoogleDriveManager.Command.UPLOAD)
        hideProgress(GoogleDriveManager.Command.DOWNLOAD)

        val logsEnabled = AppPreferencesManager.areLogsEnabled(applicationContext)
        val clearLogsBtn = findViewById<Button>(R.id.automotive_settings_clear_logs_btn_view)
        val sendLogsBtn = findViewById<Button>(R.id.automotive_settings_send_logs_btn_view)
        val logsEnableCheckView = findViewById<CheckBox>(R.id.automotive_settings_enable_logs_check_view)
        logsEnableCheckView.isChecked = logsEnabled
        LogsDialog.processEnableCheckView(applicationContext, sendLogsBtn, clearLogsBtn, logsEnabled)
        logsEnableCheckView.setOnClickListener { view1: View ->
            val checked = (view1 as CheckBox).isChecked
            LogsDialog.processEnableCheckView(applicationContext, sendLogsBtn, clearLogsBtn, checked)
        }
        clearLogsBtn.setOnClickListener {
            val result = AppLogger.deleteAllLogs()
            val message = if (result) "All logs deleted" else "Can not delete logs"
            SafeToast.showAnyThread(applicationContext, message)
            AppLogger.initLogger(applicationContext)
        }
        mProgressBar = findViewById(R.id.automotive_settings_dialog_logs_progress)
        mProgressBar.visibility = View.INVISIBLE
        sendLogsBtn.setOnClickListener {
            mProgressBar.visibility = View.VISIBLE
            LogsDialog.sendLogMailTask(
                applicationContext,
                {
                    SafeToast.showAnyThread(
                        applicationContext,
                        applicationContext.getString(com.yuriy.openradio.shared.R.string.logs_sent_msg)
                    )
                    hideLogsProgress()
                },
                {
                    SafeToast.showAnyThread(
                        applicationContext,
                        applicationContext.getString(com.yuriy.openradio.shared.R.string.logs_can_not_send)
                    )
                    hideLogsProgress()
                }
            )
        }

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
                AppLogger.d("Signed in as " + googleAccount.email)
                mGoogleDriveManager.connect(googleAccount.account)
            }
            .addOnFailureListener { exception: Exception? ->
                AppLogger.e("Can't do sign in", exception)
                SafeToast.showAnyThread(
                    applicationContext, getString(R.string.can_not_get_account_name)
                )
            }
    }

    private fun hideLogsProgress() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                mProgressBar.visibility = View.INVISIBLE
            } catch (e: Exception) {
                // Ignore for now but must be fixed.
                // Fatal Exception: java.lang.IllegalStateException
                // The current thread must have a looper!
                AppLogger.e("$CLASS_NAME can not hide progress bar", e)
            }
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
                mProgressBarUpload.visibility = View.VISIBLE
            }
            GoogleDriveManager.Command.DOWNLOAD -> runOnUiThread {
                mProgressBarDownload.visibility = View.VISIBLE
            }
        }
    }

    private fun hideProgress(command: GoogleDriveManager.Command) {
        when (command) {
            GoogleDriveManager.Command.UPLOAD -> runOnUiThread {
                mProgressBarUpload.visibility = View.GONE
            }
            GoogleDriveManager.Command.DOWNLOAD -> runOnUiThread {
                mProgressBarDownload.visibility = View.GONE
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
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                        AppLocalBroadcast.createIntentGoogleDriveDownloaded()
                    )
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

    companion object {

        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = AutomotiveSettingsActivity::class.java.simpleName
    }
}
