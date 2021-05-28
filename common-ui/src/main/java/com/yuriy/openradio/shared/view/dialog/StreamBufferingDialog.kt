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
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.android.exoplayer2.DefaultLoadControl
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.SafeToast.showAnyThread

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class StreamBufferingDialog : BaseDialogFragment() {

    private var mMinBuffer: EditText? = null
    private var mMaxBuffer: EditText? = null
    private var mPlayBuffer: EditText? = null
    private var mPlayBufferRebuffer: EditText? = null

    @SuppressLint("StringFormatInvalid")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflater.inflate(
                R.layout.settings_stream_buffering,
                activity!!.findViewById(R.id.settings_stream_buffering_root)
        )
        setWindowDimensions(view, 0.9f, 0.9f)
        val titleText = getString(R.string.stream_buffering_label)
        val title = view.findViewById<TextView>(R.id.stream_buffering_label_view)
        title.text = titleText
        val context: Context? = activity
        val descView = view.findViewById<TextView>(R.id.stream_buffering_desc_view)
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
        mMinBuffer = view.findViewById(R.id.min_buffer_edit_view)
        mMinBuffer?.setText(AppPreferencesManager.getMinBuffer(context!!).toString())
        mMaxBuffer = view.findViewById(R.id.max_buffer_edit_view)
        mMaxBuffer?.setText(AppPreferencesManager.getMaxBuffer(context!!).toString())
        mPlayBuffer = view.findViewById(R.id.play_buffer_edit_view)
        mPlayBuffer?.setText(AppPreferencesManager.getPlayBuffer(context!!).toString())
        mPlayBufferRebuffer = view.findViewById(R.id.play_buffer_after_rebuffer_edit_view)
        mPlayBufferRebuffer?.setText(AppPreferencesManager.getPlayBufferRebuffer(context!!).toString())
        val restoreBtn = view.findViewById<Button>(R.id.buffering_restore_btn)
        restoreBtn.setOnClickListener {
            mMinBuffer?.setText(DefaultLoadControl.DEFAULT_MIN_BUFFER_MS.toString())
            mMaxBuffer?.setText(DefaultLoadControl.DEFAULT_MAX_BUFFER_MS.toString())
            mPlayBuffer?.setText(DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS.toString())
            mPlayBufferRebuffer?.setText(DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS.toString())
        }
        return createAlertDialog(view)
    }

    override fun onPause() {
        super.onPause()
        val context = activity ?: return
        val erroMsgBase = getString(R.string.invalid_buffer_desc)
        var minBufferStr = DefaultLoadControl.DEFAULT_MIN_BUFFER_MS.toString()
        var maxBufferStr = DefaultLoadControl.DEFAULT_MAX_BUFFER_MS.toString()
        var playBufferStr = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS.toString()
        var playBufferRebufferStr =
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                        .toString()
        if (mMinBuffer != null) {
            minBufferStr = mMinBuffer!!.text.toString().trim { it <= ' ' }
            if (!validateInput(minBufferStr)) {
                showAnyThread(context, erroMsgBase + minBufferStr)
                return
            }
        }
        if (mMaxBuffer != null) {
            maxBufferStr = mMaxBuffer!!.text.toString().trim { it <= ' ' }
            if (!validateInput(maxBufferStr)) {
                showAnyThread(context, erroMsgBase + maxBufferStr)
                return
            }
        }
        if (mPlayBuffer != null) {
            playBufferStr = mPlayBuffer!!.text.toString().trim { it <= ' ' }
            if (!validateInput(playBufferStr)) {
                showAnyThread(context, erroMsgBase + playBufferStr)
                return
            }
        }
        if (mPlayBufferRebuffer != null) {
            playBufferRebufferStr = mPlayBufferRebuffer!!.text.toString().trim { it <= ' ' }
            if (!validateInput(playBufferRebufferStr)) {
                showAnyThread(context, erroMsgBase + playBufferRebufferStr)
                return
            }
        }
        val minBuffer = minBufferStr.toInt()
        val maxBuffer = maxBufferStr.toInt()
        val playBuffer = playBufferStr.toInt()
        val playBufferRebuffer = playBufferRebufferStr.toInt()
        if (maxBuffer < minBuffer) {
            showAnyThread(context, getString(R.string.min_buffer_greater_max_buffer))
            return
        }
        if (minBuffer < playBuffer) {
            showAnyThread(context, getString(R.string.play_buffer_greater_min_buffer))
            return
        }
        if (minBuffer < playBufferRebuffer) {
            showAnyThread(context, getString(R.string.play_re_buffer_greater_min_buffer))
            return
        }
        AppPreferencesManager.setMinBuffer(context, minBuffer)
        AppPreferencesManager.setMaxBuffer(context, maxBuffer)
        AppPreferencesManager.setPlayBuffer(context, playBuffer)
        AppPreferencesManager.setPlayBufferRebuffer(context, playBufferRebuffer)
    }

    private fun validateInput(value: String): Boolean {
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
        return if (valueInt > resources.getInteger(R.integer.max_buffer_val)) {
            false
        } else valueInt >= resources.getInteger(R.integer.min_buffer_val)
    }

    companion object {
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
