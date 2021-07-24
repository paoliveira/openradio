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

package com.yuriy.openradio.shared.model.storage

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import java.io.File

class ImagesStore {

    companion object {

        private val TAG = ImagesStore::class.simpleName

        /**
         * The authority for the this provider.
         */
        const val AUTHORITY = "openradio.images"

        /**
         * Uri to the authority for the this provider.
         */
        private val AUTHORITY_URI: Uri = Uri.parse("content://$AUTHORITY")

        /**
         * Base value for MIME type of the content provided.
         */
        private const val MIME_TYPE_BASE = "com.yuriy.provider"

        private const val IMG_URL_KEY = "IMG_URL_KEY"

        private const val RS_ID_KEY = "RS_ID_KEY"

        /**
         * The MIME type for this table.
         */
        const val CONTENT_TYPE_IMAGE = "${ContentResolver.CURSOR_ITEM_BASE_TYPE}/$MIME_TYPE_BASE.image"

        const val CONTENT_TYPE_UNKNOWN = "${ContentResolver.CURSOR_ITEM_BASE_TYPE}/$MIME_TYPE_BASE.unknown"

        fun getInsertUri(): Uri {
            return AUTHORITY_URI.buildUpon()
                    .appendPath("insert")
                    .build()
        }

        fun getDeleteUri(): Uri {
            return AUTHORITY_URI.buildUpon()
                    .appendPath("delete")
                    .build()
        }

        fun getContentValues(rsId: String, imageUrl: String?): ContentValues {
            val values = ContentValues()
            values.put(RS_ID_KEY, rsId)
            values.put(IMG_URL_KEY, imageUrl)
            return values
        }

        fun getImageUrl(contentValues: ContentValues): String {
            if (!contentValues.containsKey(IMG_URL_KEY)) {
                return AppUtils.EMPTY_STRING
            }
            return contentValues.getAsString(IMG_URL_KEY)
        }

        fun getRsId(contentValues: ContentValues): String {
            if (!contentValues.containsKey(RS_ID_KEY)) {
                return AppUtils.EMPTY_STRING
            }
            return contentValues.getAsString(RS_ID_KEY)
        }

        fun buildImageUri(rsId: String): Uri {
            return Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(AUTHORITY)
                    .appendPath(rsId)
                    .build()
        }
    }
}
