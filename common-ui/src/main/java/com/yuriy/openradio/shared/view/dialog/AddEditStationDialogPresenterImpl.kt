package com.yuriy.openradio.shared.view.dialog

import android.content.Context
import com.yuriy.openradio.shared.model.media.RadioStationManagerLayer
import com.yuriy.openradio.shared.vo.RadioStationToAdd

class AddEditStationDialogPresenterImpl(
    private val mContext: Context,
    private val mRadioStationManagerLayer: RadioStationManagerLayer
) : AddEditStationDialogPresenter {

    override fun addRadioStation(
        context: Context,
        radioStation: RadioStationToAdd,
        onSuccess: (msg: String) -> Unit,
        onFailure: (msg: String) -> Unit
    ) {
        mRadioStationManagerLayer.addRadioStation(mContext, radioStation, onSuccess, onFailure)
    }

    override fun editRadioStation(
        context: Context,
        mediaId: String,
        radioStation: RadioStationToAdd,
        onSuccess: (msg: String) -> Unit,
        onFailure: (msg: String) -> Unit
    ) {
        mRadioStationManagerLayer.editRadioStation(mContext, mediaId, radioStation, onSuccess, onFailure)
    }
}
