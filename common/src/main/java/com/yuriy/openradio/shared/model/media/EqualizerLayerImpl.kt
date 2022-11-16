/*
 * Copyright 2020-2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.media.audiofx.Equalizer
import com.yuriy.openradio.shared.model.storage.EqualizerStorage
import com.yuriy.openradio.shared.model.translation.EqualizerJsonStateSerializer
import com.yuriy.openradio.shared.model.translation.EqualizerStateJsonDeserializer
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.vo.EqualizerState

class EqualizerLayerImpl(
    private val mEqualizerStorage: EqualizerStorage
) : EqualizerLayer {

    private var mEqualizer: Equalizer? = null
    private val mSerializer = EqualizerJsonStateSerializer()

    override fun init(audioSessionId: Int) {
        if (mEqualizer != null) {
            AppLogger.e("$CLASS_NAME can not init with null equalizer")
            return
        }
        try {
            mEqualizer = Equalizer(10, audioSessionId)
        } catch (e: Exception) {
            AppLogger.e("$CLASS_NAME exception while init:${e.message}")
            mEqualizer = null
            mEqualizerStorage.saveEqualizerState(AppUtils.EMPTY_STRING)
            return
        }
        if (isInit()) {
            applyState(loadState()) {}
        }
    }

    override fun deinit() {
        if (mEqualizer == null) {
            AppLogger.e("$CLASS_NAME can not deinit with null equalizer")
            return
        }
        mEqualizer?.release()
        mEqualizer = null
    }

    override fun isInit(): Boolean {
        return mEqualizerStorage.isEmpty().not()
    }

    override fun saveState(state: EqualizerState) {
        mEqualizerStorage.saveEqualizerState(mSerializer.serialize(state))
    }

    override fun loadState(): EqualizerState {
        val deserializer = EqualizerStateJsonDeserializer()
        return deserializer.deserialize(mEqualizerStorage.loadEqualizerState())
    }

    override fun applyState(state: EqualizerState, onSuccess: (state: EqualizerState) -> Unit) {
        if (mEqualizer == null) {
            AppLogger.e("$CLASS_NAME can not load state for null equalizer")
            return
        }
        try {
            mEqualizer?.enabled = false
            mEqualizer?.enabled = true
            mEqualizer?.usePreset(state.currentPreset)
            val newState = EqualizerState(mEqualizer!!)
            saveState(newState)
            onSuccess(newState)
        } catch (e: Exception) {
            AppLogger.e("$CLASS_NAME apply state", e)
        }
        state.printState()
    }

    companion object {

        private val CLASS_NAME = EqualizerLayerImpl::class.java.simpleName
    }
}
