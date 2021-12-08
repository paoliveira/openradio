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
import android.os.Bundle
import android.widget.Button
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.model.storage.drive.ExportImportManager
import com.yuriy.openradio.shared.view.BaseDialogFragment

/**
 * Created by Eran Leshem
 * Using Android Studio
 * On 11/10/21
 */
class ExportImportDialog : BaseDialogFragment() {

    private lateinit var mExportImportManager: ExportImportManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mExportImportManager = ExportImportManager(requireContext())
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflater.inflate(R.layout.dialog_export_import,
                requireActivity().findViewById(R.id.dialog_export_import_root)
        )
        setWindowDimensions(view, 0.9f, 0.5f)
        view.findViewById<Button>(R.id.export_btn).setOnClickListener { exportRadioStations() }
        view.findViewById<Button>(R.id.import_btn).setOnClickListener { importRadioStations() }
        return createAlertDialog(view)
    }

    private fun exportRadioStations() {
        mExportImportManager.exportRadioStations()
        dismiss()
    }

    private fun importRadioStations() {
        mExportImportManager.importRadioStations()
        dismiss()
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = ExportImportDialog::class.java.simpleName

        /**
         * Tag string to use in dialog transactions.
         */
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"
    }
}
