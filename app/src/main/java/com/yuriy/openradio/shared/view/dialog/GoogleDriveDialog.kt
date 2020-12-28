/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.fragment.app.FragmentManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.model.storage.drive.GoogleDriveError
import com.yuriy.openradio.shared.model.storage.drive.GoogleDriveManager
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.SafeToast.showAnyThread
import com.yuriy.openradio.shared.view.dialog.GoogleDriveDialog

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class GoogleDriveDialog : BaseDialogFragment() {
    private var mProgressBarUpload: ProgressBar? = null
    private var mProgressBarDownload: ProgressBar? = null
    private var mProgressBarTitle: ProgressBar? = null

    /**
     *
     */
    private var mGoogleDriveManager: GoogleDriveManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = context
        val listener: GoogleDriveManager.Listener = GoogleDriveManagerListenerImpl()
        mGoogleDriveManager = GoogleDriveManager(context!!, listener)
    }

    override fun onDestroy() {
        super.onDestroy()
        hideProgress(GoogleDriveManager.Command.UPLOAD)
        hideProgress(GoogleDriveManager.Command.DOWNLOAD)
        mGoogleDriveManager!!.disconnect()
        mGoogleDriveManager = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflater.inflate(
                R.layout.dialog_google_drive,
                activity!!.findViewById(R.id.dialog_google_drive_root)
        )
        setWindowDimensions(view, 0.9f, 0.9f)
        val uploadTo = view.findViewById<Button>(R.id.upload_to_google_drive_btn)
        uploadTo.setOnClickListener { v: View? -> uploadRadioStationsToGoogleDrive() }
        val downloadFrom = view.findViewById<Button>(R.id.download_from_google_drive_btn)
        downloadFrom.setOnClickListener { v: View? -> downloadRadioStationsFromGoogleDrive() }
        mProgressBarUpload = view.findViewById(R.id.upload_to_google_drive_progress)
        mProgressBarDownload = view.findViewById(R.id.download_to_google_drive_progress)
        mProgressBarTitle = view.findViewById(R.id.google_drive_title_progress)
        hideProgress(GoogleDriveManager.Command.UPLOAD)
        hideProgress(GoogleDriveManager.Command.DOWNLOAD)
        hideTitleProgress()
        return createAlertDialog(view)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // TODO: Save state and continue with Google Drive if procedure was interrupted by device rotation.
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        AppLogger.d(CLASS_NAME + " OnActivityResult: request:" + requestCode + " result:" + resultCode)
        if (requestCode != ACCOUNT_REQUEST_CODE) {
            return
        }
        GoogleSignIn
                .getSignedInAccountFromIntent(data)
                .addOnSuccessListener { googleAccount: GoogleSignInAccount ->
                    if (mGoogleDriveManager == null) {
                        showErrorToast(getString(R.string.google_drive_error_msg_2))
                        return@addOnSuccessListener
                    }
                    AppLogger.d("Signed in as " + googleAccount.email)
                    mGoogleDriveManager!!.connect(googleAccount.account)
                }
                .addOnFailureListener { exception: Exception? ->
                    AppLogger.e("Can't do sign in:" + Log.getStackTraceString(exception))
                    showAnyThread(
                            context, getString(R.string.can_not_get_account_name)
                    )
                }
    }

    private fun uploadRadioStationsToGoogleDrive() {
        if (mGoogleDriveManager == null) {
            showErrorToast(getString(R.string.google_drive_error_msg_3))
            return
        }
        mGoogleDriveManager!!.uploadRadioStations()
    }

    private fun downloadRadioStationsFromGoogleDrive() {
        if (mGoogleDriveManager == null) {
            showErrorToast(getString(R.string.google_drive_error_msg_4))
            return
        }
        mGoogleDriveManager!!.downloadRadioStations()
    }

    private fun showProgress(command: GoogleDriveManager.Command) {
        val activity = activity ?: return
        when (command) {
            GoogleDriveManager.Command.UPLOAD -> if (mProgressBarUpload != null) {
                activity.runOnUiThread { mProgressBarUpload!!.visibility = View.VISIBLE }
            }
            GoogleDriveManager.Command.DOWNLOAD -> if (mProgressBarDownload != null) {
                activity.runOnUiThread { mProgressBarDownload!!.visibility = View.VISIBLE }
            }
        }
    }

    private fun hideProgress(command: GoogleDriveManager.Command) {
        val activity = activity ?: return
        when (command) {
            GoogleDriveManager.Command.UPLOAD -> if (mProgressBarUpload != null) {
                activity.runOnUiThread { mProgressBarUpload!!.visibility = View.GONE }
            }
            GoogleDriveManager.Command.DOWNLOAD -> if (mProgressBarDownload != null) {
                activity.runOnUiThread { mProgressBarDownload!!.visibility = View.GONE }
            }
        }
    }

    private fun showTitleProgress() {
        val activity = activity ?: return
        activity.runOnUiThread { mProgressBarTitle!!.visibility = View.VISIBLE }
    }

    private fun hideTitleProgress() {
        val activity = activity ?: return
        activity.runOnUiThread { mProgressBarTitle!!.visibility = View.GONE }
    }

    private fun showErrorToast(message: String) {
        showAnyThread(context, message)
    }

    private inner class GoogleDriveManagerListenerImpl() : GoogleDriveManager.Listener {
        override fun onAccountRequested(client: GoogleSignInClient) {
            if (mGoogleDriveManager == null) {
                showErrorToast(getString(R.string.google_drive_error_msg_1))
                return
            }
            if (!AppUtils.startActivityForResultSafe(
                            this@GoogleDriveDialog.activity,
                            client.signInIntent,
                            ACCOUNT_REQUEST_CODE)) {
                mGoogleDriveManager!!.connect(null)
            }
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
            val message: String
            message = when (command) {
                GoogleDriveManager.Command.UPLOAD -> context.getString(R.string.google_drive_data_saved)
                GoogleDriveManager.Command.DOWNLOAD -> context.getString(R.string.google_drive_data_read)
            }
            if (!TextUtils.isEmpty(message)) {
                showAnyThread(context, message)
            }
            hideProgress(command)
        }

        override fun onError(command: GoogleDriveManager.Command, error: GoogleDriveError) {
            val context = this@GoogleDriveDialog.context
            if (context == null) {
                AppLogger.e("Can not handle Google Drive error, context is null, error:$error")
                return
            }
            val message: String
            message = when (command) {
                GoogleDriveManager.Command.UPLOAD -> context.getString(R.string.google_drive_error_when_save)
                GoogleDriveManager.Command.DOWNLOAD -> context.getString(R.string.google_drive_error_when_read)
            }
            if (!TextUtils.isEmpty(message)) {
                showAnyThread(context, message)
            }
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
        @JvmField
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"
        private const val ACCOUNT_REQUEST_CODE = 400
        @JvmStatic
        fun findGoogleDriveDialog(fragmentManager: FragmentManager?): GoogleDriveDialog? {
            if (fragmentManager == null) {
                return null
            }
            val fragment = fragmentManager.findFragmentByTag(DIALOG_TAG)
            return if (fragment is GoogleDriveDialog) {
                fragment
            } else null
        }
    }
}
