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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommonUi
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.findButton
import com.yuriy.openradio.shared.utils.findCheckBox
import com.yuriy.openradio.shared.utils.gone
import com.yuriy.openradio.shared.view.SafeToast.showAnyThread
import com.yuriy.openradio.shared.vo.RadioStation
import com.yuriy.openradio.shared.vo.RadioStationToAdd
import com.yuriy.openradio.shared.vo.getStreamUrl
import com.yuriy.openradio.shared.vo.isInvalid

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *
 * Dialog to provide components to Edit Radio Station.
 */
class EditStationDialog : BaseAddEditStationDialog() {

    /**
     * Media Id associated with current Radio Station.
     */
    private var mMediaId = AppUtils.EMPTY_STRING

    private lateinit var mEditStationPresenter: EditStationPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DependencyRegistryCommonUi.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        dialog?.setTitle(R.string.edit_station_dialog_title)
        val addOrEditBtn = view!!.findButton(R.id.add_edit_station_dialog_add_btn_view)
        addOrEditBtn.setText(R.string.edit_station_dialog_button_label)
        val addToSrvChkBox = view.findCheckBox(R.id.add_to_srvr_check_view)
        addToSrvChkBox.gone()
        mMediaId = getMediaId(arguments)
        val ctx = requireContext()
        if (mMediaId.isNotEmpty()) {
            val radioStation = mEditStationPresenter.getDeviceLocalRadioStation(mMediaId)
            if (radioStation.isInvalid()) {
                handleInvalidRadioStation(ctx, addOrEditBtn)
            } else {
                handleUI(radioStation)
            }
        } else {
            handleInvalidRadioStation(ctx, addOrEditBtn)
        }
        return view
    }

    /**
     * Validate provided input in order to pass data farther to generate [RadioStation].
     */
    override fun processInput(radioStationToAdd: RadioStationToAdd) {
        val context = activity?.applicationContext ?: return
        mPresenter.editRadioStation(
            context, mMediaId, radioStationToAdd,
            { msg -> onSuccess(msg) },
            { msg -> onFailure(msg) }
        )
    }

    fun configureWith(presenter: EditStationPresenter) {
        mEditStationPresenter = presenter
    }

    /**
     * Handles UI in case of error while trying to edit Radio Station.
     *
     * @param context      Context of a callee.
     * @param addOrEditBtn Edit button.
     */
    private fun handleInvalidRadioStation(context: Context, addOrEditBtn: Button) {
        showAnyThread(context, context.getString(R.string.can_not_edit_station_label))
        addOrEditBtn.isEnabled = false
    }

    /**
     * Update UI with Radio Station loaded from storage.
     *
     * @param radioStation Radio Station.
     */
    private fun handleUI(radioStation: RadioStation) {
        mNameEdit.setText(radioStation.name)
        mUrlEdit.setText(radioStation.getStreamUrl())
        mCountriesSpinner.setSelection(getCountryPosition(radioStation.country))
        mGenresSpinner.setSelection(getGenrePosition(radioStation.genre))
        mAddToFavCheckView.isChecked = mEditStationPresenter.isRadioStationFavorite(radioStation)
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = EditStationDialog::class.java.simpleName

        /**
         * Tag string to use in dialog transactions.
         */
        val DIALOG_TAG = CLASS_NAME + "_DIALOG_TAG"

        /**
         * Key to keep Media Id's value in Bundle.
         */
        private const val MEDIA_ID_KEY = "MEDIA_ID_KEY"

        fun makeBundle(mediaId: String?): Bundle {
            val bundle = Bundle()
            bundle.putString(MEDIA_ID_KEY, mediaId)
            return bundle
        }

        /**
         * Extract media id from provided Bundle.
         *
         * @param bundle Bundle to handle.
         * @return Media Id or empty string if there is nothing to extract.
         */
        private fun getMediaId(bundle: Bundle?): String {
            if (bundle == null) {
                return AppUtils.EMPTY_STRING
            }
            return if (!bundle.containsKey(MEDIA_ID_KEY)) {
                AppUtils.EMPTY_STRING
            } else bundle.getString(MEDIA_ID_KEY) ?: AppUtils.EMPTY_STRING
        }
    }
}
