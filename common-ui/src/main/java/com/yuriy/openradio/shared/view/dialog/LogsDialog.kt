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
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import androidx.fragment.app.FragmentManager
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.email.Email
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.SafeToast.showAnyThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class LogsDialog : BaseDialogFragment() {

    private var mProgressBar: ProgressBar ?= null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflater.inflate(
                R.layout.dialog_settings_logs,
                activity!!.findViewById(R.id.dialog_settings_logs_root)
        )
        setWindowDimensions(view, 0.8f, 0.8f)
        val context: Context? = activity
        val logsEnabled = AppPreferencesManager.areLogsEnabled(context!!)
        val logsEnableCheckView = view.findViewById<CheckBox>(R.id.settings_dialog_enable_logs_check_view)
        val sendLogsBtn = view.findViewById<Button>(R.id.settings_dialog_send_logs_btn_view)
        val clearLogsBtn = view.findViewById<Button>(R.id.settings_dialog_clear_logs_btn_view)
        logsEnableCheckView.isChecked = logsEnabled
        processEnableCheckView(context, sendLogsBtn, clearLogsBtn, logsEnabled)
        logsEnableCheckView.setOnClickListener { view1: View ->
            val checked = (view1 as CheckBox).isChecked
            processEnableCheckView(context, sendLogsBtn, clearLogsBtn, checked)
        }
        clearLogsBtn.setOnClickListener {
            val result = AppLogger.deleteAllLogs()
            val message = if (result) "All logs deleted" else "Can not delete logs"
            showAnyThread(context, message)
            AppLogger.initLogger(context)
        }

        mProgressBar = view.findViewById(R.id.settings_dialog_logs_progress)
        mProgressBar?.visibility = View.INVISIBLE
        sendLogsBtn.setOnClickListener {
            mProgressBar?.visibility = View.VISIBLE
            sendLogMailTask(
                context,
                {
                    showAnyThread(context, context.getString(R.string.logs_sent_msg))
                    hideProgressBar()
                },
                {
                    showAnyThread(context, context.getString(R.string.logs_can_not_send))
                    hideProgressBar()
                }
            )
        }
        return createAlertDialog(view)
    }

    private fun hideProgressBar() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                mProgressBar?.visibility = View.INVISIBLE
            } catch (e: Exception) {
                // Ignore for now but must be fixed.
                // Fatal Exception: java.lang.IllegalStateException
                // The current thread must have a looper!
                AppLogger.e("$CLASS_NAME can not hide progress bar", e)
            }
        }
    }

    companion object {
        /**
         * Tag string mTo use in logging message.
         */
        private val CLASS_NAME = LogsDialog::class.java.simpleName

        /**
         * Tag string mTo use in dialog transactions.
         */
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"

        fun processEnableCheckView(context: Context, sendLogsBtn: Button, clearLogsBtn: Button, isEnable: Boolean) {
            sendLogsBtn.isEnabled = isEnable
            clearLogsBtn.isEnabled = isEnable
            AppPreferencesManager.setLogsEnabled(context, isEnable)
            AppLogger.setLoggingEnabled(isEnable)
        }

        fun sendLogMailTask(context: Context, success: () -> Unit, failure: () -> Unit) {
            AppLogger.deleteZipFile()
            try {
                AppLogger.zip()
            } catch (e: IOException) {
                showAnyThread(context, context.getString(R.string.logs_can_not_zip))
                return
            }
            GlobalScope.launch(Dispatchers.IO) {
                val subj = ("Logs report from " + context.getString(R.string.app_name) + ", "
                    + "v:" + AppUtils.getApplicationVersion(context)
                    + "." + AppUtils.getApplicationVersionCode(context))
                try {
                    val email = Email(context)
                    email.setBody(context.getString(R.string.logs_mail_body_header))
                    email.setSubject(subj)
                    email.addAttachment(AppLogger.getLogsZipFile().absolutePath)
                    email.send()
                    success()
                } catch (e: Throwable) {
                    AppLogger.e("Email exception:$e")
                    failure()
                }
            }
        }

        fun findDialog(fragmentManager: FragmentManager?): LogsDialog? {
            if (fragmentManager == null) {
                return null
            }
            val fragment = fragmentManager.findFragmentByTag(DIALOG_TAG)
            return if (fragment is LogsDialog) {
                fragment
            } else null
        }
    }
}
