/*
 * Copyright 2017 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.text.TextUtils
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.utils.AppLogger.e
import java.util.concurrent.*

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
internal abstract class GoogleDriveQueryDrive(isTerminator: Boolean, executorService: ExecutorService) :
        GoogleDriveAPIChain(isTerminator, executorService) {

    protected abstract fun getQueryTask(request: GoogleDriveRequest): Task<FileList>
    protected abstract fun getName(request: GoogleDriveRequest): String
    protected abstract fun setResult(result: GoogleDriveResult, driveFile: File?)

    override fun handleRequest(request: GoogleDriveRequest, result: GoogleDriveResult) {
        d("Query resource '" + getName(request) + "'")
        request.listener.onStart()
        val task = getQueryTask(request)
        val latch = CountDownLatch(1)
        val queryResult = arrayOf<FileList?>(null)
        task.addOnSuccessListener { fileList: FileList ->
            queryResult[0] = fileList.clone()
            latch.countDown()
        }
        task.addOnFailureListener { e: Exception? ->
            request.listener.onError(
                    GoogleDriveError(
                            "Can not query resource '" + getName(request) + "' " + Log.getStackTraceString(e)
                    )
            )
            latch.countDown()
        }
        try {
            latch.await(10, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            e("Can not query google drive folder, await exception:$e")
        }
        if (queryResult[0] == null) {
            request.listener.onError(
                    GoogleDriveError(
                            "Can not query resource '" + getName(request) + "', drive folder is null"
                    )
            )
            return
        }
        handleResult(queryResult[0], request, result)
    }

    private fun handleResult(fileList: FileList?,
                             request: GoogleDriveRequest,
                             result: GoogleDriveResult) {
        if (fileList == null) {
            handleNext(request, result)
            return
        }
        val list = fileList.files
        if (list == null) {
            handleNext(request, result)
            return
        }
        val driveFile = getDriveFile(list, getName(request))
        if (driveFile == null) {
            d("Resource '" + getName(request) + "' queried, does not exists, pass execution farther")
        } else {
            d("Resource '" + getName(request) + "' queried, exists, getting DriveResource reference")
            setResult(result, driveFile)
        }
        handleNext(request, result)
    }

    private fun getDriveFile(list: List<File>, name: String): File? {
        d("Check resource '" + name + "', list of " + list.size)
        var result: File? = null
        for (file in list) {
            d(" - file:$file")
            // All other fields are null, except name type and id.
            // Get the first record.
            if (TextUtils.equals(name, file.name)) {
                result = file.clone()
                break
            }
        }
        return result
    }
}
