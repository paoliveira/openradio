/*
 * Copyright 2020-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.media.audiofx.Equalizer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.yuriy.openradio.shared.broadcast.AppLocalBroadcast
import com.yuriy.openradio.shared.model.storage.EqualizerStorage
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.vo.EqualizerState

class IEqualizerImpl private constructor(private val mContext: Context): IEqualizer {

    private var mEqualizer: Equalizer? = null

    override fun init(audioSessionId: Int) {
        AppLogger.d("$CLASS_NAME init:$mEqualizer")
        if (mEqualizer != null) {
            return
        }
        try {
            mEqualizer = Equalizer(10, audioSessionId)
        } catch (e: Exception) {
            AppLogger.e("$CLASS_NAME exception while init:$e")
            mEqualizer = null
            EqualizerStorage.saveEqualizerState(mContext, "")
            return
        }
        //TODO: Do state operations in separate thread.
        if (EqualizerStorage.isEmpty(mContext)) {
            saveState()
        } else {
            loadState()
        }
    }

    override fun deinit() {
        AppLogger.d("$CLASS_NAME deinit:$mEqualizer")
        if (mEqualizer == null) {
            return
        }
        mEqualizer!!.release()
        mEqualizer = null
    }

    override fun saveState() {
        AppLogger.d("$CLASS_NAME save state:$mEqualizer")
        if (mEqualizer == null) {
            return
        }
        var state: EqualizerState? = null
        try {
            state = EqualizerState.createState(mEqualizer!!)
        } catch (e: IllegalArgumentException) {
            AppLogger.e("Can not create state from $mEqualizer, $e")
        } catch (e: IllegalStateException) {
            AppLogger.e("Can not create state from $mEqualizer, $e")
        } catch (e: UnsupportedOperationException) {
            AppLogger.e("Can not create state from $mEqualizer, $e")
        } catch (e: RuntimeException) {
            // Some times this happen with "AudioEffect: set/get parameter error"
            AppLogger.e("$CLASS_NAME can not create state from $mEqualizer, $e")
        }
        if (state != null) {
            saveState(mContext, state)
        }
    }

    override fun loadState() {
        AppLogger.d("$CLASS_NAME load state:$mEqualizer")
        if (mEqualizer == null) {
            return
        }
        val state = loadState(mContext)
        try {
            AppLogger.d("$CLASS_NAME try to apply preset N:${state.currentPreset} on $mEqualizer")
            var result = (mEqualizer!!.setEnabled(false))
            AppLogger.d("$CLASS_NAME set disabled:$result")
            result = (mEqualizer!!.setEnabled(true))
            AppLogger.d("$CLASS_NAME set enabled:$result")
            mEqualizer!!.usePreset(state.currentPreset)
            AppLogger.d("$CLASS_NAME applied preset N:${state.currentPreset} on $mEqualizer")
            saveState(mContext, EqualizerState(mEqualizer!!))
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(
                    AppLocalBroadcast.createIntentEqualizerApplied()
            )
        } catch (e: Exception) {
            AppLogger.e("$CLASS_NAME apply state exception:$e")
        }
        state.printState()
    }

    companion object {

        private val CLASS_NAME = IEqualizerImpl::class.java.simpleName

        fun makeInstance(context: Context): IEqualizer {
            return IEqualizerImpl(context)
        }

        fun loadState(context: Context): EqualizerState {
            val deserializer: com.yuriy.openradio.shared.model.translation.EqualizerStateDeserializer = com.yuriy.openradio.shared.model.translation.EqualizerStateJsonDeserializer()
            return deserializer.deserialize(context, EqualizerStorage.loadEqualizerState(context))
        }

        fun saveState(context: Context, state: EqualizerState) {
            val serializer: com.yuriy.openradio.shared.model.translation.EqualizerStateSerializer = com.yuriy.openradio.shared.model.translation.EqualizerJsonStateSerializer()
            EqualizerStorage.saveEqualizerState(context, serializer.serialize(state))
        }
    }
}