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

package com.yuriy.openradio.mobile.dependencies

import android.content.Context
import com.yuriy.openradio.mobile.view.activity.MainActivity
import com.yuriy.openradio.mobile.view.activity.MainActivityPresenter
import com.yuriy.openradio.mobile.view.activity.MainActivityPresenterImpl
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommon
import java.util.concurrent.atomic.AtomicBoolean

object DependencyRegistry {

    private lateinit var sMainActivityPresenter: MainActivityPresenter

    @Volatile
    private var sInit = AtomicBoolean(false)

    /**
     * Init with application's context only!
     */
    fun init(context: Context) {
        if (sInit.get()) {
            return
        }
        sMainActivityPresenter = MainActivityPresenterImpl(
            DependencyRegistryCommon.getFavoriteStorage(),
            DependencyRegistryCommon.getLatestRadioStationStorage()
        )

        sInit.set(true)
    }

    fun inject(dependency: MainActivity) {
        dependency.configureWith(sMainActivityPresenter)
    }
}
