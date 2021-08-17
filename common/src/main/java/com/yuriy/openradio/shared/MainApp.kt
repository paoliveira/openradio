/*
 * Copyright 2017-2021 The "Open Radio" Project. Author: Chernyshov Yuriy [chernyshov.yuriy@gmail.com]
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

package com.yuriy.openradio.shared

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.dependencies.DependencyRegistry
import com.yuriy.openradio.shared.dependencies.NetworkMonitorDependency
import com.yuriy.openradio.shared.model.net.NetworkMonitor
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import com.yuriy.openradio.shared.utils.AnalyticsUtils
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.net.ssl.SSLContext

/**
 * Created with Android Studio.
 * User: Yuriy Chernyshov
 * Date: 12/21/13
 * Time: 6:29 PM
 */
class MainApp : MultiDexApplication(), NetworkMonitorDependency {

    private lateinit var mNetworkMonitor: NetworkMonitor

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(applicationContext)
    }

    override fun onCreate() {
        AppLogger.d(CLASS_NAME + "OnCreate")
        AnalyticsUtils.init()
        val context = applicationContext
        DependencyRegistry.init(context)
        DependencyRegistry.injectNetworkMonitor(this)

        super.onCreate()

        // Address devices API 19 and lower.
        try {
            ProviderInstaller.installIfNeeded(applicationContext)
            val sslContext = SSLContext.getInstance("TLSv1.2")
            sslContext.init(null, null, null)
            sslContext.createSSLEngine()
        } catch (e: Throwable) {
            AppLogger.e("$CLASS_NAME can't install the provider", e)
        }

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        GlobalScope.launch(Dispatchers.IO) {
            val isLoggingEnabled = AppPreferencesManager.areLogsEnabled(
                    context
            )
            AppLogger.initLogger(context)
            AppLogger.setLoggingEnabled(isLoggingEnabled)
            printFirstLogMessage(context)
            correctBufferSettings(context)
        }
    }

    override fun configureWith(networkMonitor: NetworkMonitor) {
        mNetworkMonitor = networkMonitor
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
            val densities = AppUtils.getDensity(context)
            val firstLogMessage = StringBuilder()
            firstLogMessage.append("\n")
            firstLogMessage.append("########### Create '")
            firstLogMessage.append(context.getString(R.string.app_name))
            firstLogMessage.append("' Application ###########\n")
            firstLogMessage.append("- version: ")
            firstLogMessage.append(AppUtils.getApplicationVersionName(context))
            firstLogMessage.append(".")
            firstLogMessage.append(AppUtils.getApplicationVersionCode(context))
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
            firstLogMessage.append(AppUtils.getUserCountry(context))
            AppLogger.i(firstLogMessage.toString())
        }

        /**
         * Correct mal formatted values entered by user.
         *
         * @param context Context of a callee.
         */
        private fun correctBufferSettings(context: Context) {
            val maxBufferMs = AppPreferencesManager.getMaxBuffer(context)
            val minBufferMs = AppPreferencesManager.getMinBuffer(context)
            val playBufferMs = AppPreferencesManager.getPlayBuffer(context)
            val playBufferRebufferMs = AppPreferencesManager.getPlayBufferRebuffer(context)
            if (maxBufferMs < minBufferMs) {
                AppPreferencesManager.setMaxBuffer(context, DefaultLoadControl.DEFAULT_MAX_BUFFER_MS)
                AppPreferencesManager.setMinBuffer(context, DefaultLoadControl.DEFAULT_MIN_BUFFER_MS)
            }
            if (minBufferMs < playBufferMs) {
                AppPreferencesManager.setPlayBuffer(
                        context, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS
                )
                AppPreferencesManager.setMinBuffer(context, DefaultLoadControl.DEFAULT_MIN_BUFFER_MS)
            }
            if (minBufferMs < playBufferRebufferMs) {
                AppPreferencesManager.setPlayBufferRebuffer(
                        context, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                )
                AppPreferencesManager.setMinBuffer(context, DefaultLoadControl.DEFAULT_MIN_BUFFER_MS)
            }
        }
    }
}
