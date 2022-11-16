/*
 * Copyright 2017-2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.model.net

import android.content.Context
import android.net.Uri
import androidx.core.util.Pair
import com.yuriy.openradio.shared.utils.AppUtils

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
/**
 * [DownloaderLayer] is an interface provides method which allows to
 * perform download operations. Different implementations will allows to perform downloading via
 * different protocols: HTTP, FTP, etc ...
 */
interface DownloaderLayer {

    /**
     * Method to download data from provided [Uri].
     *
     * @param context Context of the callee.
     * @param uri Provided [Uri].
     * @param parameters List of parameters to attach to connection.
     * @param contentTypeFilter Content type to download. An empty string - for any type.
     * @return Downloaded data.
     */
    fun downloadDataFromUri(
        context: Context,
        uri: Uri,
        parameters: List<Pair<String, String>> = ArrayList(),
        contentTypeFilter: String = AppUtils.EMPTY_STRING
    ): ByteArray
}
