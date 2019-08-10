/*
 * Copyright 2019 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Utility class to handle operations over files.
 */
public final class FileUtils {

    /**
     * Private constructor to prevent this class instantiation.
     */
    private FileUtils() {
        super();
    }

    /**
     * This method creates a directory with given name is such does not exists
     *
     * @param path a path to the directory
     */
    public static void createDirIfNeeded(final String path) {
        final File file = new File(path);
        deleteFile(file);
        if (!file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.mkdirs();
        }
    }

    /**
     *
     * @param filePath
     * @return
     */
    public static boolean deleteFile(final String filePath) {
        return deleteFile(new File(filePath));
    }

    /**
     *
     * @param file
     * @return
     */
    public static boolean deleteFile(final File file) {
        if (file == null) {
            return false;
        }
        if (isFileExists(file)) {
            //noinspection ResultOfMethodCallIgnored
            return file.delete();
        }
        return false;
    }

    /**
     *
     * @param context
     * @return
     */
    public static File getFilesDir(final Context context) {
        return context.getFilesDir();
    }

    /**
     *
     * @param context
     * @param filePath
     * @return
     */
    @Nullable
    public static String copyExtFileToIntDir(final Context context, final String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return filePath;
        }
        final File directory = getFilesDir(context);
        final File file = new File(directory, AppUtils.generateRandomHexToken(16));

        OutputStream out;
        try {
            out = new FileOutputStream(file);
        } catch (final FileNotFoundException e) {
            FabricUtils.logException(e);
            return null;
        }

        ImageFetcher.downloadUrlToStream(context, filePath, out);

        try {
            out.close();
        } catch (IOException e) {
            /* Ignore */
        }

        if (!isFileExists(file)) {
            return null;
        }
        return file.getAbsolutePath();
    }

    /**
     *
     * @param path
     * @return
     */
    public static File createFileIfNeeded(final String path) {
        final File file = new File(path);
        try {
            final boolean result = file.createNewFile();
        } catch (final IOException e) {
            FabricUtils.logException(
                    new FileNotFoundException("File " + path + " not created:\n" + Log.getStackTraceString(e))
            );
        }
        return file;
    }

    public static boolean isFileExists(final File file) {
        if (file == null) {
            return false;
        }
        return file.exists() && !file.isDirectory();
    }
}
