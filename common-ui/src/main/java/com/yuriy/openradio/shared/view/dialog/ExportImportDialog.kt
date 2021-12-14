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
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.model.storage.file.ExportImportManager
import com.yuriy.openradio.shared.utils.IntentUtils
import com.yuriy.openradio.shared.view.BaseDialogFragment

/**
 * Dialog for initiating the export and import of favorite and local radio stations to/from a file.
 *
 * @author Eran Leshem
 */
class ExportImportDialog : BaseDialogFragment() {

    private lateinit var mExportImportManager: ExportImportManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mExportImportManager = ExportImportManager(requireContext())
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflater.inflate(R.layout.dialog_export_import,
                requireActivity().findViewById(R.id.dialog_export_import_root)
        )
        setWindowDimensions(view, 0.9f, 0.5f)
        val exportLauncher = IntentUtils.registerForActivityResultIntrl(this, ::onExportFileSelected)
        view.findViewById<Button>(R.id.export_btn).setOnClickListener { exportRadioStations(exportLauncher) }
        val importLauncher = IntentUtils.registerForActivityResultIntrl(this, ::onImportFileSelected)
        view.findViewById<Button>(R.id.import_btn).setOnClickListener { importRadioStations(importLauncher) }
        return createAlertDialog(view)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun exportRadioStations(exportLauncher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.type = MIME_TYPE
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.suggested_export_file_name))
        val chooserIntent = Intent.createChooser(intent, getString(R.string.select_file))
        exportLauncher.launch(chooserIntent)
    }

    private fun onExportFileSelected(intent: Intent?) {
        dismiss()
        mExportImportManager.exportRadioStations(intent)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun importRadioStations(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = MIME_TYPE
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        val chooserIntent = Intent.createChooser(intent, getString(R.string.select_file))
        launcher.launch(chooserIntent)
    }

    private fun onImportFileSelected(intent: Intent?) {
        dismiss()
        mExportImportManager.importRadioStations(intent)
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
        private const val MIME_TYPE = "application/json"
    }
}
