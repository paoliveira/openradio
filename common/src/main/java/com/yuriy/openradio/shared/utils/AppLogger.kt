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

package com.yuriy.openradio.shared.utils

import android.content.Context
import android.util.Log
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.PatternLayout
import org.apache.log4j.RollingFileAppender
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.zip.*

object AppLogger {

    private const val LOG_TAG = "OPENRADIO"
    private const val LOG_FILENAME = "OpenRadio.log"
    private const val MAX_BACKUP_INDEX = 3
    private const val MAX_FILE_SIZE = "750KB"
    private val logger = Logger.getLogger(AppLogger::class.java)
    private var sInitLogsDirectory = AppUtils.EMPTY_STRING
    private var sLoggingEnabled = false

    @JvmStatic
    fun initLogger(context: Context) {
        initLogsDirectories(context)
        val fileName = getCurrentLogsDirectory() + "/" + LOG_FILENAME
        logger.level = Level.DEBUG
        val layout = PatternLayout("%d [%t] %-5p %m%n")
        try {
            logger.removeAllAppenders()
        } catch (e: Exception) {
            e("Can't remove all logs", e)
        }
        try {
            val appender = RollingFileAppender(layout, fileName)
            appender.setMaxFileSize(MAX_FILE_SIZE)
            appender.maxBackupIndex = MAX_BACKUP_INDEX
            logger.addAppender(appender)
        } catch (e: IOException) {
            e("Can't append log", e)
        }
        d("Current log stored to $fileName")
    }

    @JvmStatic
    fun setLoggingEnabled(value: Boolean) {
        sLoggingEnabled = value
    }

    private fun initLogsDirectories(context: Context) {
        sInitLogsDirectory = FileUtils.getFilesDir(context).toString() + "/logs"
    }

    private fun getCurrentLogsDirectory(): String {
        return sInitLogsDirectory
    }

    private fun getLogsDirectories(): Array<File> {
        return arrayOf(File(sInitLogsDirectory))
    }

    fun deleteAllLogs(): Boolean {
        val resultDelZip = deleteZipFile()
        val resultDelLogcat = deleteLogcatFile()
        val files = getAllLogs()
        var result = true
        for (file in files) {
            if (!file.delete()) {
                result = false
            }
        }
        return result && resultDelZip && resultDelLogcat
    }

    fun deleteZipFile(): Boolean {
        val file = getLogsZipFile()
        return file.exists() && file.delete()
    }

    private fun deleteLogcatFile(): Boolean {
        val file = getLogcatFile()
        return file.exists() && file.delete()
    }

    private fun getAllLogs(): Array<File> {
        val logs = ArrayList<File>()
        val logDirs = getLogsDirectories()
        for (dir in logDirs) {
            if (dir.exists()) {
                logs.addAll(listOf(*getLogs(dir)))
            }
        }
        return logs.toTypedArray()
    }

    private fun getLogs(directory: File): Array<File> {
        require(!directory.isFile) {
            ("directory is not folder " + directory.absolutePath)
        }
        return directory.listFiles { dir: File, name: String ->
            if (name != null && name.lowercase(Locale.ROOT).endsWith(".log")) {
                return@listFiles true
            }
            for (i in 1..MAX_BACKUP_INDEX) {
                if (name != null && name.lowercase(Locale.ROOT).endsWith(".log.$i")) {
                    return@listFiles true
                }
            }
            false
        }
    }

    fun getLogsZipFile(): File {
        val path = getCurrentLogsDirectory()
        FileUtils.createDirIfNeeded(path)
        return FileUtils.createFileIfNeeded("$path/logs.zip")
    }

    private fun getLogcatFile(): File {
        val path = getCurrentLogsDirectory()
        FileUtils.createDirIfNeeded(path)
        return FileUtils.createFileIfNeeded("$path/logcat.txt")
    }

    @Throws(IOException::class)
    fun zip() {
        val logcatFile = getLogcatFile()
        try {
            Runtime.getRuntime().exec("logcat -f " + logcatFile.path)
        } catch (e: Exception) {
            e("Can't zip file", e)
        }
        val logs = getAllLogs()
        val fileOutputStream = FileOutputStream(getLogsZipFile())
        val zipOutputStream = ZipOutputStream(fileOutputStream)
        zipFile(logcatFile, zipOutputStream)
        for (file in logs) {
            if (!file.isDirectory) {
                zipFile(file, zipOutputStream)
            }
        }
        zipOutputStream.closeEntry()
        zipOutputStream.close()
    }

    @Throws(IOException::class)
    private fun zipFile(inputFile: File, zipOutputStream: ZipOutputStream) {

        // A ZipEntry represents a file entry in the zip archive
        // We name the ZipEntry after the original file's name
        val zipEntry = ZipEntry(inputFile.name)
        zipOutputStream.putNextEntry(zipEntry)
        val fileInputStream = FileInputStream(inputFile)
        val buf = ByteArray(1024)
        var bytesRead: Int

        // Read the input file by chucks of 1024 bytes
        // and write the read bytes to the zip stream
        while (fileInputStream.read(buf).also { bytesRead = it } > 0) {
            zipOutputStream.write(buf, 0, bytesRead)
        }

        // close ZipEntry to store the stream to the file
        zipOutputStream.closeEntry()
        d("Log file :" + inputFile.canonicalPath + " is zipped to archive")
    }

    fun e(logMsg: String) {
        if (sLoggingEnabled) {
            logger.error(logMsg)
        }
        Log.e(LOG_TAG, "[" + Thread.currentThread().name + "] " + logMsg)
    }

    fun e(logMsg: String, t: Throwable?) {
        if (sLoggingEnabled) {
            logger.error(logMsg)
        }
        Log.e(LOG_TAG, "[" + Thread.currentThread().name + "] " + logMsg + "\n${Log.getStackTraceString(t)}")
    }

    fun w(logMsg: String) {
        if (sLoggingEnabled) {
            logger.warn(logMsg)
        }
        Log.w(LOG_TAG, "[" + Thread.currentThread().name + "] " + logMsg)
    }

    fun i(logMsg: String) {
        if (sLoggingEnabled) {
            logger.info(logMsg)
        }
        Log.i(LOG_TAG, "[" + Thread.currentThread().name + "] " + logMsg)
    }

    fun d(logMsg: String) {
        if (sLoggingEnabled) {
            logger.debug(logMsg)
        }
        Log.d(LOG_TAG, "[" + Thread.currentThread().name + "] " + logMsg)
    }
}
