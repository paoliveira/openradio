package com.yuriy.openradio.shared.utils

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.yuriy.openradio.shared.utils.AnalyticsUtils.logException
import com.yuriy.openradio.shared.utils.AppUtils.hasVersionKitKat

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 02/11/16
 * E-Mail: chernyshov.yuriy@gmail.com
 */
object ImageFilePath {
    private const val DATA_COLUMN = MediaStore.Images.Media.DATA

    /**
     * Method for return file path of Gallery image
     *
     * @param context Application context.
     * @param uri     Uri of the File.
     * @return path of the selected image file from gallery
     */
    @SuppressLint("NewApi")
    fun getPath(context: Context, uri: Uri): String? {

        // DocumentProvider
        if (hasVersionKitKat() && DocumentsContract.isDocumentUri(context, uri)) {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {
                // TODO: Implement file selection according to changes in Android P
                // using paths is deprecated for a long time, it was finally disabled in android 10
                // so this workaround doesnt work t all ut it does prevent crash
                val id = DocumentsContract.getDocumentId(uri)
                var contentUri = Uri.parse("")
                try {
                    contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), id.toLong())
                } catch (e: Exception) {
                    // Ignore
                }
                return getDataColumn(context, contentUri, uri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(
                        split[1]
                )
                return getDataColumn(context, contentUri, uri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {

            // Return the remote address
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context, uri, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param originalUri   The original Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    fun getDataColumn(context: Context,
                      uri: Uri?,
                      originalUri: Uri?,
                      selection: String?,
                      selectionArgs: Array<String>?): String? {
        val projection = arrayOf(DATA_COLUMN)
        try {
            context.contentResolver
                    .query(uri!!, projection, selection, selectionArgs, null).use { cursor ->
                        if (cursor != null && cursor.moveToFirst()) {
                            val index = cursor.getColumnIndexOrThrow(DATA_COLUMN)
                            return cursor.getString(index)
                        }
                    }
        } catch (e: Exception) {
            val msg = "Can not get data column for " + (originalUri?.toString() ?: "null.")
            logException(Exception(msg, e))
        }
        return null
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }
}
