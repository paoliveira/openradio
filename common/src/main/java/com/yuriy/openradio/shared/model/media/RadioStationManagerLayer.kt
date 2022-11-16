package com.yuriy.openradio.shared.model.media

import android.content.Context
import com.yuriy.openradio.shared.vo.RadioStationToAdd

interface RadioStationManagerLayer {

    fun addRadioStation(
        context: Context, rsToAdd: RadioStationToAdd,
        onSuccess: (msg: String) -> Unit,
        onFailure: (msg: String) -> Unit
    )

    fun editRadioStation(
        context: Context, mediaId: String, rsToAdd: RadioStationToAdd,
        onSuccess: (msg: String) -> Unit,
        onFailure: (msg: String) -> Unit
    )

    fun removeRadioStation(context: Context?, mediaId: String?)
}
