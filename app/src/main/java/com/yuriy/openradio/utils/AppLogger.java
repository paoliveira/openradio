/*
 * Copyright 2015 The "Driver Assistant" Project. Author: Chernyshov Yuriy [chernyshov.yuriy@gmail.com]
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

import com.yuriy.openradio.BuildConfig;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppLogger {

    public static final String LOG_TAG = "DRIVER_ASSISTANT";
    public static final String ERROR_LOG_PREFIX = "LOG_ERR: ";

    private static final String LOG_FILENAME = "DriverAssistant.log";
    private static final int MAX_BACKUP_INDEX = 3;
    private static final String MAX_FILE_SIZE = "750KB";
    private static final Logger logger = Logger.getLogger(AppLogger.class);

    private static String sInitLogsDirectory;

    public static void initLogger(final Context context) {
        initLogsDirectories(context);
        final String fileName = getCurrentLogsDirectory(context) + "/" + LOG_FILENAME;
        logger.setLevel(Level.DEBUG);
        final Layout layout = new PatternLayout("%d [%t] %-5p %m%n");
        try {
            logger.removeAllAppenders();
        } catch (Exception e) {
            AppLogger.e("Unable to remove logger appenders.");
        }
        try {
            final RollingFileAppender appender = new RollingFileAppender(layout, fileName);
            appender.setMaxFileSize(MAX_FILE_SIZE);
            appender.setMaxBackupIndex(MAX_BACKUP_INDEX);
            logger.addAppender(appender);
        } catch (final IOException ioe) {
            Log.e(LOG_TAG, "unable to create log file: " + fileName);
        }
        AppLogger.d("Current log stored to " + fileName);
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

    private static File[] getLogs(File directory) {
        if (directory.isFile()) {
            throw new IllegalArgumentException("directory is not folder "
                    + directory.getAbsolutePath());
        }
        return directory.listFiles(new FilenameFilter() {
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

    public static void e(String logMsg) {
        e(logMsg, (Throwable) null);
    }

    public static void w(String logMsg) {
        w(logMsg, (Throwable) null);
    }

    public static void i(String logMsg) {
        i(logMsg, (Throwable) null);
    }

    public static void d(String logMsg) {
        d(logMsg, (Throwable) null);
    }

    public static void e(String logPrefix, String logMsg) {
        e(logPrefix + logMsg);
    }

    public static void w(String logPrefix, String logMsg) {
        w(logPrefix + logMsg);
    }

    public static void i(String logPrefix, String logMsg) {
        i(logPrefix + logMsg);
    }

    public static void d(String logPrefix, String logMsg) {
        d(logPrefix + logMsg);
    }

    public static void e(String logMsg, Throwable t) {
        logMsg = ERROR_LOG_PREFIX + logMsg;
        if (t != null) {
            logger.error(logMsg, t);
            Log.e(LOG_TAG, logMsg, t);
        } else {
            logger.error(logMsg);
            Log.e(LOG_TAG, logMsg);
        }
    }

    public static void w(String logMsg, Throwable t) {
        if (t != null) {
            logger.warn(logMsg, t);
            Log.w(LOG_TAG, logMsg, t);
        } else {
            logger.warn(logMsg);
            Log.w(LOG_TAG, logMsg);
        }
    }

    public static void i(String logMsg, Throwable t) {
        if (t != null) {
            logger.info(logMsg, t);
            Log.i(LOG_TAG, logMsg, t);
        } else {
            logger.info(logMsg);
            Log.i(LOG_TAG, logMsg);
        }
    }

    public static void d(String logMsg, Throwable t) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        if (t != null) {
            logger.debug(logMsg, t);
            Log.d(LOG_TAG, logMsg, t);
        } else {
            logger.debug(logMsg);
            Log.d(LOG_TAG, logMsg);
        }
    }
}