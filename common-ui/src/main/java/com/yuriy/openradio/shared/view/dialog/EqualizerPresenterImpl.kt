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

package com.yuriy.openradio.shared.view.dialog

import com.yuriy.openradio.shared.model.media.EqualizerLayer
import com.yuriy.openradio.shared.vo.EqualizerState

class EqualizerPresenterImpl(
    private val mEqualizer: EqualizerLayer
) : EqualizerPresenter {

    override fun selectEqualizerState(
        state: EqualizerState,
        position: Int,
        onSuccess: (state: EqualizerState) -> Unit
    ) {
        state.currentPreset = position.toShort()
        mEqualizer.saveState(state)
        mEqualizer.applyState(state) {
            onSuccess(it)
        }
    }

    override fun loadEqualizerState(): EqualizerState {
        return mEqualizer.loadState()
    }

    override fun hasEqualizerState(): Boolean {
        return mEqualizer.isInit()
    }
}
