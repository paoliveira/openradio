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

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.widget.EditText
import com.google.android.exoplayer2.DefaultLoadControl
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import com.yuriy.openradio.shared.utils.findButton
import com.yuriy.openradio.shared.utils.findEditText
import com.yuriy.openradio.shared.utils.findTextView
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.SafeToast.showAnyThread

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class StreamBufferingDialog : BaseDialogFragment() {

    private lateinit var mMinBuffer: EditText
    private lateinit var mMaxBuffer: EditText
    private lateinit var mPlayBuffer: EditText
    private lateinit var mPlayBufferRebuffer: EditText

    @SuppressLint("StringFormatInvalid")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflater.inflate(
            R.layout.settings_stream_buffering,
            requireActivity().findViewById(R.id.settings_stream_buffering_root)
        )
        setWindowDimensions(view, 0.9f, 0.9f)
        val context: Context? = activity
        val descView = view.findTextView(R.id.stream_buffering_desc_view)
        try {
            descView.text = String.format(
                resources.getString(R.string.stream_buffering_descr),
                resources.getInteger(R.integer.min_buffer_val),
                resources.getInteger(R.integer.max_buffer_val),
                resources.getInteger(R.integer.min_buffer_sec),
                resources.getInteger(R.integer.max_buffer_min)
            )
        } catch (e: Exception) {
            /* Ignore */
        }
        mMinBuffer = view.findEditText(R.id.min_buffer_edit_view)
        mMaxBuffer = view.findEditText(R.id.max_buffer_edit_view)
        mPlayBuffer = view.findEditText(R.id.play_buffer_edit_view)
        mPlayBufferRebuffer = view.findEditText(R.id.play_buffer_after_rebuffer_edit_view)
        val restoreBtn = view.findButton(R.id.buffering_restore_btn)
        restoreBtn.setOnClickListener {
            handleRestoreButton(mMinBuffer, mMaxBuffer, mPlayBuffer, mPlayBufferRebuffer)
        }

        handleOnCreate(context!!, mMinBuffer, mMaxBuffer, mPlayBuffer, mPlayBufferRebuffer)

        return createAlertDialog(view)
    }

    override fun onPause() {
        super.onPause()
        val context = activity ?: return
        handleOnPause(context, mMinBuffer, mMaxBuffer, mPlayBuffer, mPlayBufferRebuffer)
    }

    companion object {

        fun handleRestoreButton(
            minBuffer: EditText,
            maxBuffer: EditText,
            playBuffer: EditText,
            playBufferRebuffer: EditText
        ) {
            minBuffer.setText(DefaultLoadControl.DEFAULT_MIN_BUFFER_MS.toString())
            maxBuffer.setText(DefaultLoadControl.DEFAULT_MAX_BUFFER_MS.toString())
            playBuffer.setText(DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS.toString())
            playBufferRebuffer.setText(DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS.toString())
        }

        fun handleOnCreate(
            context: Context,
            minBuffer: EditText,
            maxBuffer: EditText,
            playBuffer: EditText,
            playBufferRebuffer: EditText
        ) {
            minBuffer.setText(AppPreferencesManager.getMinBuffer(context).toString())
            maxBuffer.setText(AppPreferencesManager.getMaxBuffer(context).toString())
            playBuffer.setText(AppPreferencesManager.getPlayBuffer(context).toString())
            playBufferRebuffer.setText(AppPreferencesManager.getPlayBufferRebuffer(context).toString())
        }

        fun handleOnPause(
            context: Context,
            minBufferEditText: EditText,
            maxBufferEditText: EditText,
            playBufferEditText: EditText,
            playBufferRebufferEdittext: EditText
        ) {
            val erroMsgBase = context.getString(R.string.invalid_buffer_desc)
            val minBufferStr = minBufferEditText.text.toString().trim { it <= ' ' }
            if (!validateInput(context, minBufferStr)) {
                showAnyThread(context, erroMsgBase + minBufferStr)
                return
            }
            val maxBufferStr = maxBufferEditText.text.toString().trim { it <= ' ' }
            if (!validateInput(context, maxBufferStr)) {
                showAnyThread(context, erroMsgBase + maxBufferStr)
                return
            }
            val playBufferStr = playBufferEditText.text.toString().trim { it <= ' ' }
            if (!validateInput(context, playBufferStr)) {
                showAnyThread(context, erroMsgBase + playBufferStr)
                return
            }
            val playBufferRebufferStr = playBufferRebufferEdittext.text.toString().trim { it <= ' ' }
            if (!validateInput(context, playBufferRebufferStr)) {
                showAnyThread(context, erroMsgBase + playBufferRebufferStr)
                return
            }
            val minBuffer = minBufferStr.toInt()
            val maxBuffer = maxBufferStr.toInt()
            val playBuffer = playBufferStr.toInt()
            val playBufferRebuffer = playBufferRebufferStr.toInt()
            if (maxBuffer < minBuffer) {
                showAnyThread(context, context.getString(R.string.min_buffer_greater_max_buffer))
                return
            }
            if (minBuffer < playBuffer) {
                showAnyThread(context, context.getString(R.string.play_buffer_greater_min_buffer))
                return
            }
            if (minBuffer < playBufferRebuffer) {
                showAnyThread(context, context.getString(R.string.play_re_buffer_greater_min_buffer))
                return
            }
            AppPreferencesManager.setMinBuffer(context, minBuffer)
            AppPreferencesManager.setMaxBuffer(context, maxBuffer)
            AppPreferencesManager.setPlayBuffer(context, playBuffer)
            AppPreferencesManager.setPlayBufferRebuffer(context, playBufferRebuffer)
        }

        private fun validateInput(context: Context, value: String): Boolean {
            if (value.isEmpty()) {
                return false
            }
            if (!TextUtils.isDigitsOnly(value)) {
                return false
            }
            val valueInt: Int = try {
                value.toInt()
            } catch (e: NumberFormatException) {
                return false
            }
            return if (valueInt > context.resources.getInteger(R.integer.max_buffer_val)) {
                false
            } else valueInt >= context.resources.getInteger(R.integer.min_buffer_val)
        }

        /**
         * Tag string mTo use in logging message.
         */
        private val CLASS_NAME = StreamBufferingDialog::class.java.simpleName

        /**
         * Tag string mTo use in dialog transactions.
         */
        @JvmField
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"
    }
}
