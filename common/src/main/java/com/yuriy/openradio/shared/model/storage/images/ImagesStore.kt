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

package com.yuriy.openradio.shared.model.storage.images

import android.content.ContentResolver
import android.net.Uri
import com.yuriy.openradio.shared.utils.AppUtils

object ImagesStore {

    /**
     * Base value for MIME type of the content provided.
     */
    private const val MIME_TYPE_BASE = "com.yuriy.provider"

    /**
     * The MIME type for images.
     */
    const val CONTENT_TYPE_IMAGE = "${ContentResolver.CURSOR_ITEM_BASE_TYPE}/$MIME_TYPE_BASE.image"

    /**
     * The unknown MIME type.
     */
    const val CONTENT_TYPE_UNKNOWN = "${ContentResolver.CURSOR_ITEM_BASE_TYPE}/$MIME_TYPE_BASE.unknown"

    /**
     * The authority for the this provider.
     */
    private const val AUTHORITY = "openradio.images"

    /**
     * Uri to the authority for the this provider.
     */
    private val AUTHORITY_URI: Uri = Uri.parse("content://$AUTHORITY")

    private const val PATH_DELETE = "delete"

    private const val PATH_LOADED = "loaded"

    private const val QUERY_PARAM_ID = "id"

    private const val QUERY_PARAM_URL = "url"

    fun getDeleteUri(id: String): Uri {
        return AUTHORITY_URI.buildUpon()
            .appendPath(PATH_DELETE)
            .appendQueryParameter(QUERY_PARAM_ID, id)
            .build()
    }

    fun buildImageUri(id: String, url: String): Uri {
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AUTHORITY)
            .appendQueryParameter(QUERY_PARAM_ID, id)
            .appendQueryParameter(QUERY_PARAM_URL, Uri.encode(url))
            .build()
    }

    fun buildImageLoadedBaseUri(): Uri {
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AUTHORITY)
            .appendPath(PATH_LOADED)
            .build()
    }

    fun buildImageLoadedUri(id: String): Uri {
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AUTHORITY)
            .appendPath(PATH_LOADED)
            .appendQueryParameter(QUERY_PARAM_ID, id)
            .build()
    }

    fun getId(uri: Uri): String {
        return uri.getQueryParameter(QUERY_PARAM_ID) ?: AppUtils.EMPTY_STRING
    }

    fun getImageUrl(uri: Uri?): String {
        if (uri == null) {
            return AppUtils.EMPTY_STRING
        }
        return Uri.decode(uri.getQueryParameter(QUERY_PARAM_URL)) ?: AppUtils.EMPTY_STRING
    }

    fun isAuthorised(uri: Uri): Boolean {
        return uri.authority == AUTHORITY
    }
}
