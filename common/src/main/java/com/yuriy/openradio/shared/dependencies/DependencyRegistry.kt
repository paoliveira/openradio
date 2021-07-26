/*
 * Copyright 2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.dependencies

import android.content.Context
import android.net.ConnectivityManager
import com.yuriy.openradio.shared.model.net.Downloader
import com.yuriy.openradio.shared.model.net.HTTPDownloaderImpl
import com.yuriy.openradio.shared.model.net.NetworkMonitor
import com.yuriy.openradio.shared.model.parser.DataParser
import com.yuriy.openradio.shared.model.parser.JsonDataParserImpl

object DependencyRegistry {

    private lateinit var mNetMonitor: NetworkMonitor
    private lateinit var mDownloader: Downloader
    private lateinit var mParser: DataParser

    fun init(context: Context) {
        mNetMonitor = NetworkMonitor(context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
        mNetMonitor.init()
        mDownloader = HTTPDownloaderImpl()
        mParser = JsonDataParserImpl(context)
    }

    fun injectNetworkMonitor(networkMonitorDependency: NetworkMonitorDependency) {
        networkMonitorDependency.configureWith(getNetMonitor())
    }

    fun injectDownloader(downloaderDependency: DownloaderDependency) {
        downloaderDependency.configureWith(getDownloader())
    }

    fun injectParser(parserDependency: ParserDependency) {
        parserDependency.configureWith(getParser())
    }

    fun getNetMonitor(): NetworkMonitor {
        return mNetMonitor
    }

    fun getDownloader(): Downloader {
        return mDownloader
    }

    fun getParser(): DataParser {
        return mParser
    }
}
