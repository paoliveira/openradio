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
import android.content.Intent
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
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.SafeToast.showAnyThread
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflater.inflate(
                R.layout.dialog_settings_logs,
                activity!!.findViewById(R.id.dialog_settings_logs_root)
        )
        setWindowDimensions(view, 0.8f, 0.8f)
        val titleText = getString(R.string.logs_label_app)
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
        clearLogsBtn.setOnClickListener {
            val result = AppLogger.deleteAllLogs(context)
            val message = if (result) "All logs deleted" else "Can not delete logs"
            showAnyThread(context, message)
            AppLogger.initLogger(context)
        }
        val sendLogsBtn = view.findViewById<Button>(R.id.settings_dialog_send_logs_btn_view)
        sendLogsBtn.setOnClickListener { sendLogMailTask(context) }
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

    private fun sendLogMailTask(context: Context) {
        AppLogger.deleteZipFile(context)
        try {
            AppLogger.zip(context)
        } catch (e: IOException) {
            showAnyThread(activity, getString(R.string.logs_can_not_zip))
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            val subj = ("Logs report from " + getString(R.string.app_name) + ", "
                    + "v:" + AppUtils.getApplicationVersion(context)
                    + "." + AppUtils.getApplicationVersionCode(context))
            sendLogs(context, MailInfo(SUPPORT_MAIL, subj, getString(R.string.logs_mail_body_header)))
        }
    }

    private fun sendLogs(context: Context, mailInfo: MailInfo) {
        // Prepare email intent
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(mailInfo.mTo))
        intent.putExtra(Intent.EXTRA_SUBJECT, mailInfo.mSubj)
        intent.putExtra(Intent.EXTRA_TEXT, mailInfo.mMailBody + "\r\n")
        intent.type = "vnd.android.cursor.dir/email"
        try {
            val path = FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".provider",
                    AppLogger.getLogsZipFile(context)
            )
            intent.putExtra(Intent.EXTRA_STREAM, path)
        } catch (e: Exception) {
            AppLogger.e("Send Logs exception while get uri for file:$e")
            showAnyThread(activity, getString(R.string.logs_can_not_send))
            return
        }
        GlobalScope.launch(Dispatchers.Main) {
            val intent1 = Intent.createChooser(intent, context.getString(R.string.logs_title_send_logs_chooser))
            if (!AppUtils.startActivityForResultSafe(activity, intent1, LOGS_EMAIL_REQUEST_CODE)) {
                showAnyThread(activity, context.getString(R.string.logs_can_not_send))
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
