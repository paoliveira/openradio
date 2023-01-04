/*
 * Copyright 2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.RadioButton
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommon
import com.yuriy.openradio.shared.dependencies.SourcesLayerDependency
import com.yuriy.openradio.shared.model.source.Source
import com.yuriy.openradio.shared.model.source.SourcesLayer
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.findImageButton
import com.yuriy.openradio.shared.utils.gone
import com.yuriy.openradio.shared.utils.visible
import com.yuriy.openradio.shared.view.BaseDialogFragment

/**
 *
 */
class SourceDialog : BaseDialogFragment(), SourcesLayerDependency {

    private lateinit var mSourcesLayer: SourcesLayer
    private var mInitSrc: Source? = null
    private var mNewSrc: Source? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        DependencyRegistryCommon.inject(this)
        val view = inflater.inflate(
            R.layout.dialog_source,
            requireActivity().findViewById(R.id.dialog_source_root)
        )
        setWindowDimensions(view, 0.8f, 0.3f)
        val ctx = requireContext()
        val restartBtn = view.findImageButton(R.id.sources_restart_btn)
        restartBtn.setOnClickListener {
            restart(ctx)
        }
        handleRadioBtns(ctx, view.findViewById(R.id.sources_radio_group)) { isSrcChanged ->
            run {
                if (isSrcChanged) {
                    restartBtn.visible()
                } else {
                    restartBtn.gone()
                }
            }
        }
        return createAlertDialog(view)
    }

    override fun configureWith(sourcesLayer: SourcesLayer) {
        mSourcesLayer = sourcesLayer
    }

    override fun onDestroy() {
        super.onDestroy()
        // In case a user selected a new source but did not restart.
        if (mInitSrc != null && mInitSrc != mNewSrc) {
            mSourcesLayer.setActiveSource(mInitSrc!!)
        }
    }

    @SuppressLint("InflateParams")
    private fun handleRadioBtns(
        context: Context,
        view: LinearLayout,
        onSelectChanged: (isSrcChanged: Boolean) -> Unit
    ) {
        mInitSrc = mSourcesLayer.getActiveSource()
        val sources = mSourcesLayer.getAllSources()
        for (source in sources) {
            val child = LayoutInflater.from(context).inflate(R.layout.source_view, null) as RadioButton
            child.text = source.name
            child.id = source.idx
            if (source == mSourcesLayer.getActiveSource()) {
                child.isChecked = true
            }
            child.setOnCheckedChangeListener { buttonView, isChecked ->
                run {
                    val src = sources.elementAt(buttonView.id)
                    if (isChecked) {
                        mNewSrc = src
                        mSourcesLayer.setActiveSource(src)
                    }
                    onSelectChanged(mInitSrc == src)
                }
            }
            view.addView(child)
        }
    }

    private fun restart(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        try {
            context.startActivity(mainIntent)
        } catch (exception: ActivityNotFoundException) {
            AppLogger.e("Can't restart after src changed", exception)
        }
        Runtime.getRuntime().exit(0)
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = SourceDialog::class.java.simpleName

        /**
         * Tag string to use in dialog transactions.
         */
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"
    }
}
