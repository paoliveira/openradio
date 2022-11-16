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

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommonUi
import com.yuriy.openradio.shared.dependencies.SleepTimerModelDependency
import com.yuriy.openradio.shared.model.timer.SleepTimerModel
import com.yuriy.openradio.shared.utils.*
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.view.SafeToast
import java.text.SimpleDateFormat
import java.util.*

/**
 * [SleepTimerDialog] is responsible for handling timeout timer view.
 * Once the user set a valid timer, it will communicate to the service to start the timer.
 */
class SleepTimerDialog : BaseDialogFragment(), SleepTimerModelDependency {

    private lateinit var mView: View
    /**
     * Formatter to represent the user friendly Date.
     */
    private val mDateFormatter = SimpleDateFormat(DATE_PATTERN, Locale.getDefault())

    /**
     * Formatter to represent a user friendly Time.
     */
    private val mTimeFormatter = SimpleDateFormat(TIME_PATTERN, Locale.getDefault())
    private lateinit var mSleepTimerModel: SleepTimerModel
    private var mIsEnabled = false

    override fun configureWith(sleepTimerModel: SleepTimerModel) {
        mSleepTimerModel = sleepTimerModel
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        DependencyRegistryCommonUi.inject(this)
        mView = inflater.inflate(
                R.layout.dialog_sleep_timer,
                requireActivity().findViewById(R.id.dialog_sleep_timer_root)
        )
        setWindowDimensions(mView, 0.8f, 0.2f)
        val toggle = mView.findToggleButton(R.id.sleep_timer_on_off_view)
        toggle.isChecked = mSleepTimerModel.isEnabled()
        handleEnabled(toggle.isChecked, mView)
        mSleepTimerModel.updateTime(toggle.isChecked)
        updatedDateTimeViews(mView)
        toggle.setOnCheckedChangeListener { _, isChecked ->
            handleEnabled(isChecked, mView)
        }
        val dateBtn = mView.findButton(R.id.sleep_timer_date_btn)
        dateBtn.setOnClickListener {
            val newFragment = DatePickerFragment()
            newFragment.show(parentFragmentManager, DatePickerFragment.TAG)
        }
        val timeBtn = mView.findButton(R.id.sleep_timer_time_btn)
        timeBtn.setOnClickListener {
            val newFragment = TimePickerFragment()
            newFragment.show(parentFragmentManager, TimePickerFragment.TAG)
        }
        return createAlertDialog(mView)
    }

    override fun onDetach() {
        super.onDetach()
        val timestamp = mSleepTimerModel.getTimestamp()
        if (mIsEnabled) {
            if (mSleepTimerModel.isTimestampNotValid(timestamp)) {
                val ctx = context ?: return
                SafeToast.showAnyThread(ctx, getString(R.string.can_not_set_time))
                return
            }
        }
        mSleepTimerModel.updateTimer(timestamp, mIsEnabled)
    }

    fun onDateSet(year: Int, month: Int, day: Int) {
        mSleepTimerModel.setDate(year, month, day)
        updatedDateTimeViews(mView)
    }

    fun onTimeSet(hourOfDay: Int, minute: Int) {
        mSleepTimerModel.setTime(hourOfDay, minute)
        updatedDateTimeViews(mView)
    }

    private fun updatedDateTimeViews(view: View?) {
        if (view == null) {
            AppLogger.e("$CLASS_NAME can't update date and time views")
            return
        }
        val date = view.findTextView(R.id.sleep_timer_date_view)
        val time = view.findTextView(R.id.sleep_timer_time_view)
        date.text = mDateFormatter.format(mSleepTimerModel.getTime())
        time.text = mTimeFormatter.format(mSleepTimerModel.getTime())
    }

    private fun handleEnabled(isChecked: Boolean, view: View) {
        mIsEnabled = isChecked
        val visibility = if (mIsEnabled) {
            // The toggle is enabled
            View.VISIBLE
        } else {
            // The toggle is disabled
            View.GONE
        }
        val dateTimeView = view.findLinearLayout(R.id.sleep_timer_date_time_view)
        val dateTimeLabelsView = view.findLinearLayout(R.id.sleep_timer_date_time_labels_view)
        dateTimeView.visibility = visibility
        dateTimeLabelsView.visibility = visibility
        mSleepTimerModel.setEnabled(mIsEnabled)
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = SleepTimerDialog::class.java.simpleName

        /**
         * The pattern describing the date format.
         */
        private const val DATE_PATTERN = "yyyy-MM-dd"

        /**
         * The pattern describing the time format.
         */
        private const val TIME_PATTERN = "hh:mm"

        /**
         * Tag string to use in dialog transactions.
         */
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"

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
