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

package com.yuriy.openradio.shared.model.media

import android.content.Context
import com.yuriy.openradio.shared.model.ModelLayer
import com.yuriy.openradio.shared.model.net.UrlLayer
import com.yuriy.openradio.shared.model.storage.DeviceLocalsStorage
import com.yuriy.openradio.shared.model.storage.FavoritesStorage
import com.yuriy.openradio.shared.model.storage.images.ImagesPersistenceLayer
import com.yuriy.openradio.shared.model.storage.images.ImagesStore
import com.yuriy.openradio.shared.service.OpenRadioStore
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.RadioStationValidator
import com.yuriy.openradio.shared.vo.MediaStream
import com.yuriy.openradio.shared.vo.RadioStation
import com.yuriy.openradio.shared.vo.RadioStationToAdd
import com.yuriy.openradio.shared.vo.isInvalid
import com.yuriy.openradio.shared.vo.setVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RadioStationManagerLayerImpl(
    provider: ModelLayer,
    urlLayer: UrlLayer,
    private val mDeviceLocalsStorage: DeviceLocalsStorage,
    private val mFavoritesStorage: FavoritesStorage,
    private val mImagesPersistenceLayer: ImagesPersistenceLayer
) : RadioStationManagerLayer {

    private var mUiScope = CoroutineScope(Dispatchers.Main)
    private var mScope = CoroutineScope(Dispatchers.IO)
    private var mRadioStationValidator = RadioStationValidator(provider, urlLayer, mUiScope, mScope)

    override fun addRadioStation(
        context: Context, rsToAdd: RadioStationToAdd,
        onSuccess: (msg: String) -> Unit,
        onFailure: (msg: String) -> Unit
    ) {
        mRadioStationValidator.validate(
            context, rsToAdd,
            { msg ->
                onSuccess(msg)
                run {
                    val radioStation = RadioStation.makeDefaultInstance(
                        mDeviceLocalsStorage.getId()
                    )
                    radioStation.name = rsToAdd.name
                    radioStation.setVariant(MediaStream.BIT_RATE_DEFAULT, rsToAdd.url)
                    radioStation.imageUrl = rsToAdd.imageLocalUrl
                    radioStation.genre = rsToAdd.genre
                    radioStation.country = rsToAdd.country
                    radioStation.isLocal = true
                    mDeviceLocalsStorage.add(radioStation)
                    if (rsToAdd.isAddToFav) {
                        mFavoritesStorage.add(radioStation)
                    }
                    context.startService(
                        OpenRadioStore.makeNotifyChildrenChangedIntent(
                            context,
                            MediaId.MEDIA_ID_ROOT
                        )
                    )
                    onSuccess("Radio Station added successfully")
                }
            },
            { msg -> onFailure(msg) },
            { msg -> onFailure(msg) }
        )
    }

    override fun editRadioStation(
        context: Context, mediaId: String, rsToAdd: RadioStationToAdd,
        onSuccess: (msg: String) -> Unit,
        onFailure: (msg: String) -> Unit
    ) {
        mScope.launch {
            mImagesPersistenceLayer.delete(mediaId)

            val result = mDeviceLocalsStorage.update(
                mediaId, rsToAdd.name, rsToAdd.url, rsToAdd.imageLocalUrl, rsToAdd.genre, rsToAdd.country,
                rsToAdd.isAddToFav
            )
            if (result) {
                context.startService(
                    OpenRadioStore.makeNotifyChildrenChangedIntent(
                        context,
                        MediaId.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST
                    )
                )
                mUiScope.launch { onSuccess("Radio Station updated successfully") }
            } else {
                mUiScope.launch { onFailure("Can not update Radio Station") }
            }
        }
    }

    override fun removeRadioStation(context: Context?, mediaId: String?) {
        if (context == null) {
            return
        }
        if (mediaId.isNullOrEmpty()) {
            return
        }
        mScope.launch {
            val radioStation = mDeviceLocalsStorage[mediaId]
            if (radioStation.isInvalid().not()) {
                context.contentResolver.delete(ImagesStore.getDeleteUri(mediaId), AppUtils.EMPTY_STRING, emptyArray())
                mDeviceLocalsStorage.remove(radioStation)
            }
            context.startService(
                OpenRadioStore.makeRemoveByMediaIdIntent(
                    context, mediaId
                )
            )
            context.startService(
                OpenRadioStore.makeNotifyChildrenChangedIntent(
                    context, MediaId.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST
                )
            )
        }
    }
}
