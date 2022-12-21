/*
 * Copyright 2020-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommonUi
import com.yuriy.openradio.shared.utils.findSpinner
import com.yuriy.openradio.shared.utils.findTextView
import com.yuriy.openradio.shared.utils.gone
import com.yuriy.openradio.shared.utils.visible
import com.yuriy.openradio.shared.view.BaseDialogFragment
import com.yuriy.openradio.shared.vo.EqualizerState

class EqualizerDialog : BaseDialogFragment() {

    private lateinit var mLinearLayout: LinearLayout
    private lateinit var mPresenter: EqualizerPresenter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflater.inflate(
                R.layout.dialog_eq,
                activity?.findViewById(R.id.dialog_eq_root)
        )
        DependencyRegistryCommonUi.inject(this)
        mLinearLayout = view.findViewById(R.id.eq_controls_view)
        setWindowDimensions(view, 0.8f, 0.6f)
        val notAvailableView = view.findTextView(R.id.eq_not_available_view)
        if (mPresenter.hasEqualizerState()) {
            notAvailableView.gone()
            handleEqualizer(view)
        } else {
            notAvailableView.visible()
        }
        return createAlertDialog(view)
    }

    fun configureWith(presenter: EqualizerPresenter) {
        mPresenter = presenter
    }

    private fun handleEqualizer(view: View) {
        val state = mPresenter.loadEqualizerState()
        updateEqualizerUi(state)
        val presets = state.presets
        val adapter = ArrayAdapter(
                requireActivity(),
                android.R.layout.simple_spinner_item,
                presets
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val spinner = view.findSpinner(R.id.eq_presets_spinner)
        spinner.adapter = adapter
        spinner.setSelection(state.currentPreset.toInt())
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                mPresenter.selectEqualizerState(state, position) {
                    updateEqualizerUi(it)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateEqualizerUi(state: EqualizerState) {
        state.printState()
        mLinearLayout.removeAllViews()
        val lowerEqualizerBandLevel = state.bandLevelRange[0]
        val upperEqualizerBandLevel = state.bandLevelRange[1]
        for (i in 0 until state.numOfBands) {
            val frequencyView = TextView(context)
            frequencyView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            frequencyView.gravity = Gravity.CENTER_HORIZONTAL
            val msg0 = (state.centerFrequencies[i] / 1000).toString() + " Hz"
            frequencyView.text = msg0
            mLinearLayout.addView(frequencyView)
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
            mLinearLayout.addView(seekBarRowLayout)
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
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"
    }
}
