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
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentManager
import com.yuriy.openradio.BuildConfig
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import com.yuriy.openradio.shared.utils.AnalyticsUtils
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.SafeToast.showAnyThread
import java.io.IOException
import java.lang.ref.WeakReference

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class LogsDialog : BaseDialogFragment() {

    private var mSendLogMailTask: SendLogEmailTask? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflater.inflate(
                R.layout.dialog_settings_logs,
                activity!!.findViewById(R.id.dialog_settings_logs_root)
        )
        setWindowDimensions(view, 0.8f, 0.8f)
        val titleText = getString(R.string.app_logs_label)
        val title = view.findViewById<TextView>(R.id.settings_logs_label_view)
        title.text = titleText
        val context: Context? = activity
        val areLogsEnabled = AppPreferencesManager.areLogsEnabled(context!!)
        val logsEnableCheckView = view.findViewById<CheckBox>(R.id.settings_dialog_enable_logs_check_view)
        logsEnableCheckView.isChecked = areLogsEnabled
        processEnableCheckView(context, view, areLogsEnabled)
        logsEnableCheckView.setOnClickListener { view1: View ->
            val checked = (view1 as CheckBox).isChecked
            processEnableCheckView(context, view, checked)
        }
        val clearLogsBtn = view.findViewById<Button>(R.id.settings_dialog_clear_logs_btn_view)
        clearLogsBtn.setOnClickListener { view12: View? ->
            val result = AppLogger.deleteAllLogs(context)
            val message = if (result) "All logs deleted" else "Can not delete logs"
            showAnyThread(context, message)
            AppLogger.initLogger(context)
        }
        val sendLogsBtn = view.findViewById<Button>(R.id.settings_dialog_send_logs_btn_view)
        sendLogsBtn.setOnClickListener { view13: View? -> sendLogMailTask() }
        return createAlertDialog(view)
    }

    private fun processEnableCheckView(context: Context?, view: View?, isEnable: Boolean) {
        if (view == null) {
            return
        }
        val sendLogsBtn = view.findViewById<Button>(R.id.settings_dialog_send_logs_btn_view)
        val clearLogsBtn = view.findViewById<Button>(R.id.settings_dialog_clear_logs_btn_view)
        sendLogsBtn.isEnabled = isEnable
        clearLogsBtn.isEnabled = isEnable
        AppPreferencesManager.setLogsEnabled(context!!, isEnable)
        AppLogger.setLoggingEnabled(isEnable)
    }

    @Synchronized
    private fun sendLogMailTask() {
        //attempt of run task one more time
        if (!checkRunningTasks()) {
            AppLogger.w("Send Logs task is running, return")
            return
        }
        AppLogger.deleteZipFile(activity)
        try {
            AppLogger.zip(activity)
        } catch (e: IOException) {
            showAnyThread(activity, getString(R.string.can_not_zip_logs))
            AnalyticsUtils.logException(e)
            return
        }
        mSendLogMailTask = SendLogEmailTask(this)
        val subj = ("Logs report from " + getString(R.string.app_name) + ", "
                + "v:" + AppUtils.getApplicationVersion(activity)
                + "." + AppUtils.getApplicationVersionCode(activity))
        val bodyHeader = "Archive with logs is in attachment."
        mSendLogMailTask!!.execute(MailInfo(SUPPORT_MAIL, subj, bodyHeader))
    }

    private fun checkRunningTasks(): Boolean {
        return !(mSendLogMailTask != null && mSendLogMailTask!!.status == AsyncTask.Status.RUNNING)
    }

    private class SendLogEmailTask(context: LogsDialog) : AsyncTask<MailInfo?, Void?, Intent?>() {

        private val mContext: WeakReference<LogsDialog> = WeakReference(context)

        override fun doInBackground(vararg params: MailInfo?): Intent? {
            val dialog = mContext.get() ?: return null
            val activity = dialog.activity ?: return null
            val mailInfo = params[0]

            // Prepare email intent
            val sendIntent = Intent(Intent.ACTION_SEND)
            sendIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(mailInfo?.mTo))
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, mailInfo?.mSubj)
            sendIntent.putExtra(Intent.EXTRA_TEXT, mailInfo?.mMailBody + "\r\n");
            sendIntent.type = "vnd.android.cursor.dir/email"
            try {
                val path = FileProvider.getUriForFile(
                        activity,
                        BuildConfig.APPLICATION_ID + ".provider",
                        AppLogger.getLogsZipFile(activity)
                )
                sendIntent.putExtra(Intent.EXTRA_STREAM, path)
            } catch (e: Exception) {
                AnalyticsUtils.logException(e)
                return null
            }
            return sendIntent
        }

        override fun onPostExecute(intent: Intent?) {
            super.onPostExecute(intent)
            if (intent == null) {
                return
            }
            val dialog = mContext.get() ?: return
            val activity = dialog.activity ?: return
            val intent1 = Intent.createChooser(
                    intent,
                    activity.getString(R.string.send_logs_chooser_title)
            )
            if (!AppUtils.startActivityForResultSafe(activity, intent1, LOGS_EMAIL_REQUEST_CODE)) {
                showAnyThread(
                        activity,
                        activity.getString(R.string.cant_start_activity)
                )
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        AppLogger.d(CLASS_NAME + "OnActivityResult: request:" + requestCode + " result:" + resultCode)
        val context = context ?: return
        if (requestCode == LOGS_EMAIL_REQUEST_CODE) {
            showAnyThread(context, context.getString(R.string.logs_sent_msg))
        }
    }

    private class MailInfo(val mTo: String, val mSubj: String, val mMailBody: String)
    companion object {
        /**
         * Tag string mTo use in logging message.
         */
        private val CLASS_NAME = LogsDialog::class.java.simpleName

        /**
         * Tag string mTo use in dialog transactions.
         */
        @JvmField
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"
        private const val LOGS_EMAIL_REQUEST_CODE = 4762
        private const val SUPPORT_MAIL = "chernyshov.yuriy@gmail.com"

        @JvmStatic
        fun findLogsDialog(fragmentManager: FragmentManager?): LogsDialog? {
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
