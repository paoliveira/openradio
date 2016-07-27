/*
 * Copyright 2016 The "Open Radio" Project. Author: Chernyshov Yuriy [chernyshov.yuriy@gmail.com]
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

package com.yuriy.openradio.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class AppLogger {

    private static final String LOG_TAG = "OPEN_RADIO";
    private static final String LOG_FILENAME = "OpenRadio.log";
    private static final int MAX_BACKUP_INDEX = 3;
    private static final String MAX_FILE_SIZE = "750KB";
    private static final Logger logger = Logger.getLogger(AppLogger.class);

    private static String sInitLogsDirectory;
    private static boolean sIsLoggingEnabled;

    private AppLogger() {
        super();
    }

    public static void initLogger(final Context context) {
        initLogsDirectories(context);
        final String fileName = getCurrentLogsDirectory(context) + "/" + LOG_FILENAME;
        logger.setLevel(Level.DEBUG);
        final Layout layout = new PatternLayout("%d [%t] %-5p %m%n");
        try {
            logger.removeAllAppenders();
        } catch (final Exception e) {
            AppLogger.e("Unable to remove logger appenders.");
            CrashlyticsUtils.logException(e);
        }
        try {
            final RollingFileAppender appender = new RollingFileAppender(layout, fileName);
            appender.setMaxFileSize(MAX_FILE_SIZE);
            appender.setMaxBackupIndex(MAX_BACKUP_INDEX);
            logger.addAppender(appender);
        } catch (final IOException e) {
            Log.e(LOG_TAG, "unable to create log file: " + fileName);
            CrashlyticsUtils.logException(e);
        }
        AppLogger.d("Current log stored to " + fileName);
    }

    public static void setIsLoggingEnabled(final boolean value) {
        sIsLoggingEnabled = value;
    }

    private static void initLogsDirectories(final Context context) {
        sInitLogsDirectory = context.getFilesDir() + "/logs";
    }

    public static String getCurrentLogsDirectory(final Context context) {
        if (AppUtils.externalStorageAvailable()) {
            final String extLogsDirectory = AppUtils.getExternalStorageDir(context);
            if (!TextUtils.isEmpty(extLogsDirectory)) {
                return extLogsDirectory + "/logs";
            }
        }
        return sInitLogsDirectory;
    }

    public static File[] getLogsDirectories(final Context context) {
        if (AppUtils.externalStorageAvailable()) {
            final String extLogsDirectory = AppUtils.getExternalStorageDir(context);
            if (!TextUtils.isEmpty(extLogsDirectory)) {
                return new File[]{
                        new File(sInitLogsDirectory), new File(extLogsDirectory + "/logs")
                };
            }
        }
        return new File[]{
                new File(sInitLogsDirectory)
        };
    }

    public static boolean deleteAllLogs(final Context context) {
        final File[] files = getAllLogs(context);
        boolean result = true;
        for (final File file : files) {
            if (!file.delete()) {
                result = false;
            }
        }
        return result;
    }

    public static boolean deleteZipFile(final Context context) {
        final File file = getLogsZipFile(context);
        return file.exists() && file.delete();
    }

    public static boolean deleteLogcatFile(final Context context) {
        final File file = getLogcatFile(context);
        return file.exists() && file.delete();
    }

    public static File[] getAllLogs(final Context context) {
        final List<File> logs = new ArrayList<>();
        final File[] logDirs = getLogsDirectories(context);
        for (final File dir : logDirs) {
            if (dir.exists()) {
                logs.addAll(Arrays.asList(getLogs(dir)));
            }
        }
        return logs.toArray(new File[logs.size()]);
    }

    public static File[] getInternalLogs() {
        return getLogs(new File(sInitLogsDirectory));
    }

    private static File[] getLogs(final File directory) {
        if (directory.isFile()) {
            throw new IllegalArgumentException("directory is not folder "
                    + directory.getAbsolutePath());
        }
        return directory.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                if (name != null && name.toLowerCase().endsWith(".log")) {
                    return true;
                }
                for (int i = 1; i <= MAX_BACKUP_INDEX; i++) {
                    if (name != null && name.toLowerCase().endsWith(".log." + i)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public static File getLogsZipFile(final Context context) {
        final String path = getCurrentLogsDirectory(context);
        AppUtils.createDirIfNeeded(path);
        return AppUtils.createFileIfNeeded(path + "/logs.zip");
    }

    public static File getLogcatFile(final Context context) {
        final String path = getCurrentLogsDirectory(context);
        AppUtils.createDirIfNeeded(path);
        return AppUtils.createFileIfNeeded(path + "/logcat.txt");
    }

    public static void zip(final Context context) throws IOException {

        final File logcatFile = getLogcatFile(context);
        try {
            Runtime.getRuntime().exec("logcat -f " + logcatFile.getPath());
        } catch (final Exception e) {
            AppLogger.e("Can not create Logcat file:\n" + Log.getStackTraceString(e));
            CrashlyticsUtils.logException(e);
        }

        final File[] logs = getAllLogs(context);
        final FileOutputStream fileOutputStream = new FileOutputStream(getLogsZipFile(context));
        final ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
        zipFile(logcatFile, zipOutputStream);
        for (final File file : logs) {
            if (!file.isDirectory()) {
                zipFile(file, zipOutputStream);
            }
        }
        zipOutputStream.closeEntry();
        zipOutputStream.close();
    }

    private static void zipFile(final File inputFile,
                                final ZipOutputStream zipOutputStream) throws IOException {

        // A ZipEntry represents a file entry in the zip archive
        // We name the ZipEntry after the original file's name
        final ZipEntry zipEntry = new ZipEntry(inputFile.getName());
        zipOutputStream.putNextEntry(zipEntry);

        final FileInputStream fileInputStream = new FileInputStream(inputFile);
        byte[] buf = new byte[1024];
        int bytesRead;

        // Read the input file by chucks of 1024 bytes
        // and write the read bytes to the zip stream
        while ((bytesRead = fileInputStream.read(buf)) > 0) {
            zipOutputStream.write(buf, 0, bytesRead);
        }

        // close ZipEntry to store the stream to the file
        zipOutputStream.closeEntry();

        AppLogger.d("Log file :" + inputFile.getCanonicalPath() + " is zipped to archive");
    }

    public static void e(final String logMsg) {
        if (sIsLoggingEnabled) {
            logger.error(logMsg);
        }
        Log.e(LOG_TAG, logMsg);
    }

    public static void w(final String logMsg) {
        if (sIsLoggingEnabled) {
            logger.warn(logMsg);
        }
        Log.w(LOG_TAG, logMsg);
    }

    public static void i(final String logMsg) {
        if (sIsLoggingEnabled) {
            logger.info(logMsg);
        }
        Log.i(LOG_TAG, logMsg);
    }

    public static void d(final String logMsg) {
        if (sIsLoggingEnabled) {
            logger.debug(logMsg);
        }
        Log.d(LOG_TAG, logMsg);
    }
}