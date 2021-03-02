/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy [chernyshov.yuriy@gmail.com]
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

package com.yuriy.openradio.mobile

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.google.android.exoplayer2.DefaultLoadControl
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager.areLogsEnabled
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager.getMaxBuffer
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager.getMinBuffer
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager.getPlayBuffer
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager.getPlayBufferRebuffer
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager.setMaxBuffer
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager.setMinBuffer
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager.setPlayBuffer
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager.setPlayBufferRebuffer
import com.yuriy.openradio.shared.model.storage.LocalRadioStationsStorage.add
import com.yuriy.openradio.shared.model.storage.LocalRadioStationsStorage.getAllLocals
import com.yuriy.openradio.shared.utils.AnalyticsUtils
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppLogger.initLogger
import com.yuriy.openradio.shared.utils.AppLogger.setLoggingEnabled
import com.yuriy.openradio.shared.utils.AppUtils.getApplicationVersionCode
import com.yuriy.openradio.shared.utils.AppUtils.getApplicationVersionName
import com.yuriy.openradio.shared.utils.AppUtils.getDensity
import com.yuriy.openradio.shared.utils.AppUtils.getUserCountry
import com.yuriy.openradio.shared.utils.FileUtils.copyExtFileToIntDir
import com.yuriy.openradio.shared.utils.FileUtils.getFilesDir
import com.yuriy.openradio.shared.vo.RadioStation
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Created with Android Studio.
 * User: Yuriy Chernyshov
 * Date: 12/21/13
 * Time: 6:29 PM
 */
@HiltAndroidApp
class MainApp : MultiDexApplication() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        AnalyticsUtils.init()
        AppLogger.d(CLASS_NAME + "OnCreate")
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        val context = applicationContext
        GlobalScope.launch(Dispatchers.IO) {
            val isLoggingEnabled = areLogsEnabled(
                    context
            )
            initLogger(context)
            setLoggingEnabled(isLoggingEnabled)
            printFirstLogMessage(context)
            correctBufferSettings(context)
            migrateImagesToIntStorage(context)
        }
    }

    companion object {
        /**
         * Tag string to use in logging message.
         */
        private val CLASS_NAME = MainApp::class.java.simpleName + " "

        /**
         * Print first log message with summary information about device and application.
         */
        private fun printFirstLogMessage(context: Context) {
            val densities = getDensity(context)
            val firstLogMessage = StringBuilder()
            firstLogMessage.append("\n")
            firstLogMessage.append("########### Create '")
            firstLogMessage.append(context.getString(R.string.app_name))
            firstLogMessage.append("' Application ###########\n")
            firstLogMessage.append("- version: ")
            firstLogMessage.append(getApplicationVersionName(context))
            firstLogMessage.append(".")
            firstLogMessage.append(getApplicationVersionCode(context))
            firstLogMessage.append("\n")
            firstLogMessage.append("- processors: ")
            firstLogMessage.append(Runtime.getRuntime().availableProcessors())
            firstLogMessage.append("\n")
            firstLogMessage.append("- OS ver: ")
            firstLogMessage.append(Build.VERSION.RELEASE)
            firstLogMessage.append("\n")
            firstLogMessage.append("- API level: ")
            firstLogMessage.append(Build.VERSION.SDK_INT)
            firstLogMessage.append("\n")
            firstLogMessage.append("- Density: ")
            firstLogMessage.append(densities[0]).append(" ").append(densities[1])
            firstLogMessage.append("\n")
            firstLogMessage.append("- Country: ")
            firstLogMessage.append(getUserCountry(context))
            AppLogger.i(firstLogMessage.toString())
        }

        /**
         * Correct mal formatted values entered by user.
         *
         * @param context Context of a callee.
         */
        private fun correctBufferSettings(context: Context) {
            val maxBufferMs = getMaxBuffer(context)
            val minBufferMs = getMinBuffer(context)
            val playBufferMs = getPlayBuffer(context)
            val playBufferRebufferMs = getPlayBufferRebuffer(context)
            if (maxBufferMs < minBufferMs) {
                setMaxBuffer(context, DefaultLoadControl.DEFAULT_MAX_BUFFER_MS)
                setMinBuffer(context, DefaultLoadControl.DEFAULT_MIN_BUFFER_MS)
            }
            if (minBufferMs < playBufferMs) {
                setPlayBuffer(
                        context, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS
                )
                setMinBuffer(context, DefaultLoadControl.DEFAULT_MIN_BUFFER_MS)
            }
            if (minBufferMs < playBufferRebufferMs) {
                setPlayBufferRebuffer(
                        context, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                )
                setMinBuffer(context, DefaultLoadControl.DEFAULT_MIN_BUFFER_MS)
            }
        }

        /**
         * @param context Context of a callee.
         */
        private fun migrateImagesToIntStorage(context: Context) {
            AppLogger.d(CLASS_NAME + "Migrate image to int. storage started")
            val list: List<RadioStation> = getAllLocals(context)
            var imageUrl: String
            var imageUrlLocal: String?
            val filesDir = getFilesDir(context).absolutePath
            for (radioStation in list) {
                imageUrl = radioStation.imageUrl
                if (imageUrl.isEmpty()) {
                    continue
                }
                if (imageUrl.contains(filesDir)) {
                    continue
                }
                imageUrlLocal = copyExtFileToIntDir(context, imageUrl)
                if (imageUrlLocal == null) {
                    imageUrlLocal = imageUrl
                }
                radioStation.imageUrl = imageUrlLocal
                radioStation.thumbUrl = imageUrlLocal
                add(radioStation, context)
            }
            AppLogger.d(CLASS_NAME + "Migrate image to int. storage completed")
        }
    }
}
