/*
 * Copyright 2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.model.storage.EqualizerStorage
import com.yuriy.openradio.shared.model.translation.EqualizerJsonStateSerializer
import com.yuriy.openradio.shared.model.translation.EqualizerStateDeserializer
import com.yuriy.openradio.shared.model.translation.EqualizerStateJsonDeserializer
import com.yuriy.openradio.shared.model.translation.EqualizerStateSerializer
import com.yuriy.openradio.shared.service.OpenRadioService
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.vo.EqualizerState

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class EqualizerDialog : BaseDialogFragment() {

    private var mLinearLayout: LinearLayout? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity: Activity? = activity
        val view = inflater.inflate(
                R.layout.dialog_eq,
                activity!!.findViewById(R.id.dialog_eq_root)
        )
        mLinearLayout = view.findViewById(R.id.eq_controls_view)
        setWindowDimensions(view, 0.8f, 0.6f)
        val context = context
        val notAvailableView = view.findViewById<TextView>(R.id.eq_not_available_view)
        if (EqualizerStorage.isEmpty(context!!)) {
            notAvailableView.visibility = View.VISIBLE
        } else {
            notAvailableView.visibility = View.GONE
            handleEqualizer(context, view)
        }
        return createAlertDialog(view)
    }

    private fun handleEqualizer(context: Context?, view: View) {
        if (context == null) {
            return
        }
        val deserializer: EqualizerStateDeserializer = EqualizerStateJsonDeserializer()
        val state = deserializer.deserialize(
                context, EqualizerStorage.loadEqualizerState(context)
        )
        val presets = state.presets
        val adapter = ArrayAdapter(
                activity!!,
                android.R.layout.simple_spinner_item,
                presets
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val spinner = view.findViewById<Spinner>(R.id.eq_presets_spinner)
        spinner.adapter = adapter
        spinner.setSelection(state.currentPreset.toInt())
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                AppLogger.d(CLASS_NAME + " use preset: " + presets[position])
                state.currentPreset = position.toShort()
                val serializer: EqualizerStateSerializer = EqualizerJsonStateSerializer()
                EqualizerStorage.saveEqualizerState(context, serializer.serialize(state))
                val activity: Activity? = activity
                if (activity == null) {
                    AppLogger.e("Can not call equalizer update, activity is null")
                    return
                }
                activity.startService(OpenRadioService.makeUpdateEqualizerIntent(context))
                Handler(Looper.getMainLooper()).postDelayed(
                        {
                            val state1 = deserializer.deserialize(
                                    context, EqualizerStorage.loadEqualizerState(context)
                            )
                            updateEqualizer(context, state1)
                        },
                        500
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        updateEqualizer(context, state)
    }

    private fun updateEqualizer(context: Context,
                                state: EqualizerState) {
        mLinearLayout!!.removeAllViews()
        val lowerEqualizerBandLevel = state.bandLevelRange[0]
        val upperEqualizerBandLevel = state.bandLevelRange[1]
        for (i in 0 until state.numOfBands) {
            val frequencyView = TextView(context)
            frequencyView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            frequencyView.gravity = Gravity.CENTER_HORIZONTAL
            val msg0 = (state.centerFrequencies[i] / 1000).toString() + " Hz"
            frequencyView.text = msg0
            mLinearLayout!!.addView(frequencyView)
            val lowerBandLevelView = TextView(context)
            lowerBandLevelView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val msg1 = (lowerEqualizerBandLevel / 100).toString() + " dB"
            lowerBandLevelView.text = msg1
            val upperBandLevelView = TextView(context)
            upperBandLevelView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val msg2 = (upperEqualizerBandLevel / 100).toString() + " dB"
            upperBandLevelView.text = msg2
            val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT - 60, 120
            )
            params.weight = 1f
            val seekBar = SeekBar(context)
            seekBar.isEnabled = false
            seekBar.id = i
            seekBar.setPadding(35, 15, 35, 15)
            seekBar.layoutParams = params
            seekBar.max = upperEqualizerBandLevel - lowerEqualizerBandLevel
            seekBar.progress = (upperEqualizerBandLevel - lowerEqualizerBandLevel) / 2 + state.bandLevels[i]
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//                    mEqualizer.setBandLevel(equalizerBandIndex, (short) (progress + lowerEqualizerBandLevel));
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    //not used
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
//                    properties.edit_preferences.putInt("seek_" + seek_id, seekBar.getProgress()).commit();
//                    properties.edit_preferences.putInt("position", 0).commit();
                }
            })
            seekBar.progressDrawable = ColorDrawable(Color.rgb(56, 60, 62))
            val seekBarLayout = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            seekBarLayout.weight = 1f
            seekBarLayout.setMargins(5, 0, 5, 0)
            val seekBarRowLayout = LinearLayout(context)
            seekBarRowLayout.orientation = LinearLayout.HORIZONTAL
            seekBarRowLayout.layoutParams = seekBarLayout
            seekBarRowLayout.addView(lowerBandLevelView)
            seekBarRowLayout.addView(seekBar)
            seekBarRowLayout.addView(upperBandLevelView)
            mLinearLayout!!.addView(seekBarRowLayout)
        }
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = EqualizerDialog::class.java.simpleName

        /**
         * Tag string to use in dialog transactions.
         */
        @JvmField
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"
    }
}
