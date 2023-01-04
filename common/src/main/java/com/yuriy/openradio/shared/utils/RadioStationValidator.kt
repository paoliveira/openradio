/*
 * Copyright 2017-2023 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.utils

import android.content.Context
import com.yuriy.openradio.shared.model.ModelLayer
import com.yuriy.openradio.shared.model.net.UrlLayer
import com.yuriy.openradio.shared.vo.RadioStationToAdd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Validator object to validate candidate of Radio Station to add to the system.
 */
class RadioStationValidator(
    private val mProvider: ModelLayer,
    private val mUrlLayer: UrlLayer,
    private var mUiScope: CoroutineScope,
    private var mScope: CoroutineScope
) {

    fun validate(
        context: Context, rsToAdd: RadioStationToAdd,
        onSuccess: (msg: String) -> Unit,
        onWarning: (msg: String) -> Unit,
        onFailure: (msg: String) -> Unit
    ) {
        if (rsToAdd.name.isEmpty()) {
            onFailure("Radio Station's name is invalid")
            return
        }
        val url = rsToAdd.url
        if (url.isEmpty()) {
            onFailure("Radio Station's url is invalid")
            return
        }

        mScope.launch {
            if (!NetUtils.checkResource(context, url)) {
                mUiScope.launch { onFailure("Radio Station's stream is invalid") }
                return@launch
            }
            val imageWebUrl = rsToAdd.imageWebUrl
            if (!NetUtils.checkResource(context, imageWebUrl)) {
                mUiScope.launch { onWarning("Radio Station's web image is invalid") }
            }
            val homePage = rsToAdd.homePage
            if (!NetUtils.checkResource(context, homePage)) {
                mUiScope.launch { onWarning("Radio Station's home page is invalid") }
            }
            if (rsToAdd.isAddToServer) {
                val urlData = mUrlLayer.getAddStationUrl(rsToAdd)
                val uri = urlData.first
                if (uri == null) {
                    mUiScope.launch { onFailure("Radio Station's stream is invalid") }
                    return@launch
                }
                val pairs = urlData.second
                if (pairs == null) {
                    mUiScope.launch { onFailure("Radio Station's stream is invalid") }
                    return@launch
                }
                if (!mProvider.addStation(uri, pairs)) {
                    mUiScope.launch { onFailure("Radio Station can not be added to server") }
                }
            }
            mUiScope.launch { onSuccess("Radio Station validated successfully") }
        }
    }
}