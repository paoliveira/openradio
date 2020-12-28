/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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
package com.yuriy.openradio.shared.model.storage.drive

import androidx.core.util.Pair
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.yuriy.openradio.shared.utils.AppLogger.e
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.*

/**
 * A utility for performing read/write operations on Drive files via the REST API.
 */
class GoogleDriveHelper(private val mDriveService: Drive) {
    /**
     * Creates a text file in the user's My Drive folder and returns its file ID.
     */
    fun createFile(executorService: ExecutorService,
                   folderId: String, name: String,
                   content: String): Task<String> {
        if (executorService.isShutdown) {
            e("Executor is terminated, can't handle create file requests")
            return Tasks.forCanceled()
        }
        return Tasks.call(executorService, {
            val metadata = File()
                    .setParents(listOf(folderId))
                    .setMimeType(MIME_TYPE_TEXT)
                    .setName(name)
            // Convert content to an AbstractInputStreamContent instance.
            val contentStream = ByteArrayContent.fromString(MIME_TYPE_TEXT, content)
            val file = mDriveService.files().create(metadata, contentStream).execute()
                    ?: throw IOException("Null result when requesting file creation.")
            file.id
        })
    }

    /**
     *
     * @param name
     * @return
     */
    fun createFolder(executorService: ExecutorService,
                     name: String): Task<String> {
        if (executorService.isShutdown) {
            e("Executor is terminated, can't handle create folder requests")
            return Tasks.forCanceled()
        }
        return Tasks.call(executorService, {
            val metadata = File()
                    .setMimeType(MIME_TYPE_FOLDER)
                    .setName(name)
            val folder = mDriveService.files().create(metadata)
                    .setFields("id")
                    .execute()
            folder.id
        }
        )
    }

    /**
     * Opens the file identified by `fileId` and returns a [Pair] of its name and
     * contents.
     */
    fun readFile(executorService: ExecutorService,
                 fileId: String): Task<Pair<String, String>> {
        if (executorService.isShutdown) {
            e("Executor is terminated, can't handle read file requests")
            return Tasks.forCanceled()
        }
        return Tasks.call(executorService, {

            // Retrieve the metadata as a File object.
            val metadata = mDriveService.files()[fileId].execute()
            val name = metadata.name
            mDriveService.files()[fileId].executeMediaAsInputStream().use { `is` ->
                BufferedReader(InputStreamReader(`is`)).use { reader ->
                    val stringBuilder = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                    return@call Pair.create(name, stringBuilder.toString())
                }
            }
        }
        )
    }

    /**
     * Updates the file identified by `fileId` with the given `name` and `content`.
     */
    fun saveFile(executorService: ExecutorService,
                 fileId: String,
                 name: String,
                 content: String): Task<Void?> {
        if (executorService.isShutdown) {
            e("Executor is terminated, can't handle save file requests")
            return Tasks.forCanceled()
        }
        return Tasks.call(executorService, {

            // Create a File containing any metadata changes.
            val metadata = File().setName(name)
            // Convert content to an AbstractInputStreamContent instance.
            val contentStream = ByteArrayContent.fromString("text/plain", content)
            // Update the metadata and contents.
            mDriveService.files().update(fileId, metadata, contentStream).execute()
            null
        })
    }

    fun deleteFile(executorService: ExecutorService,
                   fileId: String): Task<Void?> {
        if (executorService.isShutdown) {
            e("Executor is terminated, can't handle delete file requests")
            return Tasks.forCanceled()
        }
        return Tasks.call(executorService, {

            // Delete file with specified id..
            mDriveService.files().delete(fileId).execute()
            null
        }
        )
    }

    /**
     * Returns a [FileList] containing folder with given name in the user's My Drive.
     *
     *
     * The returned list will only contain folder visible to this app, i.e. those which were
     * created by this app.
     */
    fun queryFolder(executorService: ExecutorService, name: String): Task<FileList> {
        if (executorService.isShutdown) {
            e("Executor is terminated, can't handle query folder requests")
            return Tasks.forCanceled()
        }
        return Tasks.call(executorService, {
            mDriveService.files().list()
                    .setSpaces("drive")
                    .setQ("mimeType='$MIME_TYPE_FOLDER' and trashed=false and name='$name'")
                    .execute()
        }
        )
    }

    /**
     * Returns a [FileList] containing file with given name in the user's My Drive.
     *
     *
     * The returned list will only contain folder visible to this app, i.e. those which were
     * created by this app.
     */
    fun queryFile(executorService: ExecutorService, fileName: String): Task<FileList> {
        if (executorService.isShutdown) {
            e("Executor is terminated, can't handle query file requests")
            return Tasks.forCanceled()
        }
        return Tasks.call(executorService, {
            mDriveService.files().list()
                    .setSpaces("drive")
                    .setQ("mimeType='$MIME_TYPE_TEXT' and trashed=false and name='$fileName'")
                    .execute()
        }
        )
    }

    companion object {
        private const val MIME_TYPE_TEXT = "text/plain"
        private const val MIME_TYPE_FOLDER = "application/vnd.google-apps.folder"
    }
}
