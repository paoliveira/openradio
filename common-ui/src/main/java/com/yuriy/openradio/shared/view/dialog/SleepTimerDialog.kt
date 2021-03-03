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

package com.yuriy.openradio.shared.view.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ToggleButton
import androidx.fragment.app.FragmentManager
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.model.storage.SleepTimerStorage
import com.yuriy.openradio.shared.service.OpenRadioService
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.SafeToast
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class SleepTimerDialog : BaseDialogFragment() {

    private var mView: View? = null
    private val mDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val mTimeFormatter = SimpleDateFormat("hh:mm", Locale.getDefault())
    private val mCalendar = Calendar.getInstance()
    private var mIsEnabled = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity: Activity? = activity
        mView = inflater.inflate(
                R.layout.dialog_sleep_timer,
                activity!!.findViewById(R.id.dialog_sleep_timer_root)
        )
        setWindowDimensions(mView!!, 0.8f, 0.2f)
        val ctx = context
        val toggle: ToggleButton = mView!!.findViewById(R.id.sleep_timer_on_off_view)
        toggle.isChecked = SleepTimerStorage.loadEnabled(ctx!!)
        handleEnabled(toggle.isChecked, mView!!)
        updateCalendar(ctx, toggle.isChecked)
        updatedDateTimeViews(mView)
        toggle.setOnCheckedChangeListener { _, isChecked ->
            handleEnabled(isChecked, mView!!)
        }
        val dateBtn: Button = mView!!.findViewById(R.id.sleep_timer_date_btn)
        dateBtn.setOnClickListener {
            val newFragment = DatePickerFragment()
            newFragment.show(fragmentManager!!, DatePickerFragment.TAG)
        }
        val timeBtn: Button = mView!!.findViewById(R.id.sleep_timer_time_btn)
        timeBtn.setOnClickListener {
            val newFragment = TimePickerFragment()
            newFragment.show(fragmentManager!!, TimePickerFragment.TAG)
        }
        return createAlertDialog(mView)
    }

    override fun onDetach() {
        super.onDetach()
        val ctx = context ?: return
        SleepTimerStorage.saveDate(ctx, mCalendar.time)
        if (mIsEnabled) {
            if (mCalendar.time.time <= System.currentTimeMillis()) {
                SafeToast.showAnyThread(ctx, getString(R.string.can_not_set_time))
                return
            }
        }
        ctx.startService(OpenRadioService.makeSleepTimerIntent(ctx, mIsEnabled, mCalendar.time.time))
    }

    fun onDateSet(year: Int, month: Int, day: Int) {
        mCalendar.set(Calendar.YEAR, year)
        mCalendar.set(Calendar.MONTH, month)
        mCalendar.set(Calendar.DAY_OF_MONTH, day)
        updatedDateTimeViews(mView)
    }

    fun onTimeSet(hourOfDay: Int, minute: Int) {
        mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        mCalendar.set(Calendar.MINUTE, minute)
        updatedDateTimeViews(mView)
    }

    private fun updateCalendar(context: Context, enabled: Boolean) {
        if (enabled) {
            mCalendar.time = SleepTimerStorage.loadDate(context)
        } else {
            mCalendar.time = Date(System.currentTimeMillis())
        }
    }

    private fun updatedDateTimeViews(view: View?) {
        if (view == null) {
            AppLogger.e("$CLASS_NAME can't update date and time views")
            return
        }
        val date: TextView = view.findViewById(R.id.sleep_timer_date_view)
        val time: TextView = view.findViewById(R.id.sleep_timer_time_view)
        date.text = mDateFormatter.format(mCalendar.time)
        time.text = mTimeFormatter.format(mCalendar.time)
    }

    private fun handleEnabled(isChecked: Boolean, view: View) {
        mIsEnabled = isChecked
        val visibility: Int = if (mIsEnabled) {
            // The toggle is enabled
            View.VISIBLE
        } else {
            // The toggle is disabled
            View.GONE
        }
        val dateTimeView: LinearLayout = view.findViewById(R.id.sleep_timer_date_time_view)
        val dateTimeLabelsView: LinearLayout = view.findViewById(R.id.sleep_timer_date_time_labels_view)
        dateTimeView.visibility = visibility
        dateTimeLabelsView.visibility = visibility
        SleepTimerStorage.saveEnabled(context!!, mIsEnabled)
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = SleepTimerDialog::class.java.simpleName

        /**
         * Tag string to use in dialog transactions.
         */
        @JvmField
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"

        @JvmStatic
        fun findDialog(fragmentManager: FragmentManager?): SleepTimerDialog? {
            if (fragmentManager == null) {
                return null
            }
            val fragment = fragmentManager.findFragmentByTag(DIALOG_TAG)
            return if (fragment is SleepTimerDialog) {
                fragment
            } else null
        }
    }
}
