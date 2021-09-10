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

package com.yuriy.openradio.shared.utils

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * Utility class to handle operations over files.
 */
object FileUtils {

    /**
     * This method creates a directory with given name is such does not exists
     *
     * @param path a path to the directory
     */
    fun createDirIfNeeded(path: String) {
        val file = File(path)
        deleteFile(file)
        if (!file.exists()) {
            file.mkdirs()
        }
    }

    /**
     *
     * @param file
     * @return
     */
    private fun deleteFile(file: File?): Boolean {
        if (file == null) {
            return false
        }
        return if (isFileExists(file)) {
            file.delete()
        } else false
    }

    /**
     *
     * @param context
     * @return
     */
    @JvmStatic
    fun getFilesDir(context: Context): File {
        return context.filesDir
    }

    /**
     *
     * @param path
     * @return
     */
    fun createFileIfNeeded(path: String): File {
        val file = File(path)
        try {
            file.createNewFile()
        } catch (e: IOException) {
            AppLogger.e(
                "File $path not created", e
            )
        }
        return file
    }

    private fun isFileExists(file: File?): Boolean {
        return if (file == null) {
            false
        } else file.exists() && !file.isDirectory
    }
}
