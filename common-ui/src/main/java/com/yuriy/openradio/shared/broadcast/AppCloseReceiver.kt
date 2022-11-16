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

package com.yuriy.openradio.shared.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommonUi
import com.yuriy.openradio.shared.dependencies.MediaPresenterDependency
import com.yuriy.openradio.shared.presenter.MediaPresenter
import com.yuriy.openradio.shared.utils.AppLogger

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class AppCloseReceiver : BroadcastReceiver(), MediaPresenterDependency {

    private lateinit var mMediaPresenter: MediaPresenter

    override fun configureWith(mediaPresenter: MediaPresenter) {
        mMediaPresenter = mediaPresenter
    }

    override fun onReceive(context: Context, intent: Intent) {
        AppLogger.d("$CLASS_NAME [" + this.hashCode() + "]->onReceive(" + intent + ")")
        DependencyRegistryCommonUi.inject(this)
        mMediaPresenter.handleClosePresenter()
    }

    companion object {
        private val CLASS_NAME = AppCloseReceiver::class.java.simpleName
    }
}
