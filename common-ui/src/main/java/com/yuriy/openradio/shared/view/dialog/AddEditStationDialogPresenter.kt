package com.yuriy.openradio.shared.view.dialog

import android.content.Context
import com.yuriy.openradio.shared.vo.RadioStationToAdd

interface AddEditStationDialogPresenter {

    fun addRadioStation(
        context: Context, radioStation: RadioStationToAdd,
        onSuccess: (msg: String) -> Unit,
        onFailure: (msg: String) -> Unit
    )

    fun editRadioStation(
        context: Context, mediaId: String, radioStation: RadioStationToAdd,
        onSuccess: (msg: String) -> Unit,
        onFailure: (msg: String) -> Unit
    )
}
