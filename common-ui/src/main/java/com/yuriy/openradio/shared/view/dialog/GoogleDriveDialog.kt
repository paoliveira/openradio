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

package com.yuriy.openradio.shared.view.dialog

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommonUi
import com.yuriy.openradio.shared.dependencies.MediaPresenterDependency
import com.yuriy.openradio.shared.model.storage.drive.GoogleDriveError
import com.yuriy.openradio.shared.model.storage.drive.GoogleDriveManager
import com.yuriy.openradio.shared.presenter.MediaPresenter
import com.yuriy.openradio.shared.utils.*
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.SafeToast.showAnyThread

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class GoogleDriveDialog : BaseDialogFragment(), MediaPresenterDependency {

    private lateinit var mProgressBarUpload: ProgressBar
    private lateinit var mProgressBarDownload: ProgressBar
    private lateinit var mGoogleDriveManager: GoogleDriveManager
    private lateinit var mLauncher: ActivityResultLauncher<Intent>
    private lateinit var mMediaPresenter: MediaPresenter

    override fun configureWith(mediaPresenter: MediaPresenter) {
        mMediaPresenter = mediaPresenter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = requireContext()

        DependencyRegistryCommonUi.inject(this)

        val listener = GoogleDriveManagerListenerImpl()
        mGoogleDriveManager = GoogleDriveManager(context, listener)
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflater.inflate(
            R.layout.dialog_google_drive,
            requireActivity().findViewById(R.id.dialog_google_drive_root)
        )
        setWindowDimensions(view, 0.9f, 0.5f)
        val uploadTo = view.findButton(R.id.upload_to_google_drive_btn)
        uploadTo.setOnClickListener { uploadRadioStationsToGoogleDrive() }
        val downloadFrom = view.findButton(R.id.download_from_google_drive_btn)
        downloadFrom.setOnClickListener { downloadRadioStationsFromGoogleDrive() }
        mProgressBarUpload = view.findViewById(R.id.upload_to_google_drive_progress)
        mProgressBarDownload = view.findViewById(R.id.download_to_google_drive_progress)
        hideProgress(GoogleDriveManager.Command.UPLOAD)
        hideProgress(GoogleDriveManager.Command.DOWNLOAD)
        return createAlertDialog(view)
    }

    private fun onActivityResultCallback(data: Intent?) {
        GoogleSignIn
            .getSignedInAccountFromIntent(data)
            .addOnSuccessListener { googleAccount: GoogleSignInAccount ->
                mGoogleDriveManager.connect(googleAccount.account)
            }
            .addOnFailureListener { exception: Exception? ->
                AppLogger.e("Can't do sign in", exception)
                showAnyThread(
                    context, getString(R.string.can_not_get_account_name)
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
        val activity = activity ?: return
        when (command) {
            GoogleDriveManager.Command.UPLOAD -> activity.runOnUiThread {
                mProgressBarUpload.visible()
            }
            GoogleDriveManager.Command.DOWNLOAD -> activity.runOnUiThread {
                mProgressBarDownload.gone()
            }
        }
    }

    private fun hideProgress(command: GoogleDriveManager.Command) {
        val activity = activity ?: return
        when (command) {
            GoogleDriveManager.Command.UPLOAD -> activity.runOnUiThread {
                mProgressBarUpload.gone()
            }
            GoogleDriveManager.Command.DOWNLOAD -> activity.runOnUiThread {
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
            val context = this@GoogleDriveDialog.context
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
            showAnyThread(context, message)
            hideProgress(command)
        }

        override fun onError(command: GoogleDriveManager.Command, error: GoogleDriveError?) {
            val context = this@GoogleDriveDialog.context
            if (context == null) {
                AppLogger.e("Can not handle Google Drive error, context is null, error:$error")
                return
            }
            val message = when (command) {
                GoogleDriveManager.Command.UPLOAD -> context.getString(R.string.google_drive_error_when_save)
                GoogleDriveManager.Command.DOWNLOAD -> context.getString(R.string.google_drive_error_when_read)
            }
            showAnyThread(context, message)
            hideProgress(command)
        }
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = GoogleDriveDialog::class.java.simpleName

        /**
         * Tag string to use in dialog transactions.
         */
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"
    }
}
