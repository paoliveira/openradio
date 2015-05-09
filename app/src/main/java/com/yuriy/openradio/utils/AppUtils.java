/*
 * Copyright 2015 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 11/29/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * {@link AppUtils} is a helper class which holds various help-methods
 */
public final class AppUtils {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = AppUtils.class.getSimpleName();

    /**
     * Private constructor
     */
    private AppUtils() {}

    /**
     * Read resource file as bytes array.
     *
     * @param id      Identifier of the resource.
     * @param context Application context.
     * @return Bytes array associated with a resource
     */
    public static byte[] getResource(final int id, final Context context) {
        final Resources resources = context.getResources();
        final InputStream is = resources.openRawResource(id);
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final byte[] readBuffer = new byte[4 * 1024];

        try {
            int read;
            do {
                read = is.read(readBuffer, 0, readBuffer.length);
                if(read == -1) {
                    break;
                }
                bout.write(readBuffer, 0, read);
            } while(true);

            return bout.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                /* Ignore */
            }
        }
        return new byte[0];
    }

    /**
     * Get application's version
     *
     * @param context Application context.
     * @return Application Version
     */
    public static String getApplicationVersion(final Context context) {
        final PackageInfo packageInfo = getPackageInfo(context);
        if (packageInfo != null) {
            return packageInfo.versionName;
        } else {
            Log.w(CLASS_NAME, "Can't get application version");
            return "?";
        }
    }

    /**
     * @return PackageInfo for the current application or null if the PackageManager could not be
     * contacted.
     */
    private static PackageInfo getPackageInfo(final Context context) {
        final PackageManager packageManager = context.getPackageManager();
        if (packageManager == null) {
            Log.w(CLASS_NAME, "Package manager is NULL");
            return null;
        }
        String packageName = "";
        try {
            packageName = context.getPackageName();
            return packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(CLASS_NAME, "Failed to find PackageInfo : " + packageName);
            return null;
        } catch (RuntimeException e) {
            // To catch RuntimeException("Package manager has died") that can occur on some
            // version of Android,
            // when the remote PackageManager is unavailable. I suspect this sometimes occurs
            // when the App is being reinstalled.
            Log.e(CLASS_NAME, "Package manager has died : " + packageName);
            return null;
        } catch (Throwable e) {
            Log.e(CLASS_NAME, "Package manager has Throwable : " + e);
            return null;
        }
    }

    /**
     * This is a helper method with allows to prevent get a list of the predefined categories,
     * in order to do not show an empty category.
     *
     * @return Collection of the categories.
     */
    public static Set<String> predefinedCategories() {
        final Set<String> predefinedCategories = new HashSet<>();
        predefinedCategories.add("Classical");
        predefinedCategories.add("Country");
        predefinedCategories.add("Decades");
        predefinedCategories.add("Electronic");
        predefinedCategories.add("Folk");
        predefinedCategories.add("International");
        predefinedCategories.add("Jazz");
        predefinedCategories.add("Misc");
        predefinedCategories.add("Pop");
        predefinedCategories.add("R&B/Urban");
        predefinedCategories.add("Rap");
        predefinedCategories.add("Reggae");
        predefinedCategories.add("Rock");
        predefinedCategories.add("Talk & Speech");
        return predefinedCategories;
    }

    /**
     * This method save provided Bitmap to the specified file.
     *
     * @param bitmap   Bitmap data.
     * @param dirName  Path to the directory.
     * @param fileName Name of the file.
     */
    public static void saveBitmapToFile(final Bitmap bitmap, final String dirName,
                                        final String fileName) {

        if (bitmap == null) {
            Log.e(CLASS_NAME, "Save bitmap to file, bitmap is null");
            return;
        }
        // Create directory if needed
        createDirIfNeeded(dirName);

        //create a file to write bitmap data
        final File file = new File(dirName + "/" + fileName);

        // http://stackoverflow.com/questions/11539657/open-failed-ebusy-device-or-resource-busy
        file.renameTo(file);
        file.delete();

        //Convert bitmap to byte array
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        final byte[] byteArray = byteArrayOutputStream.toByteArray();

        //write the bytes in file
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(byteArray);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                byteArrayOutputStream.flush();
            } catch (IOException e) {
                /* Ignore */
            }
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.flush();
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                /* Ignore */
            }
        }
    }

    /**
     * Save data bytes to a file
     *
     * @param data     data as bytes array
     * @param filePath a path to file
     *
     * @return true in case of success, false - otherwise
     */
    public static boolean saveDataToFile(byte[] data, String filePath) {
        if (data == null) {
            Log.w(CLASS_NAME, "Save data to file -> data is null, path:" + filePath);
            return false;
        }
        File file = new File(filePath);
        Log.d(CLASS_NAME, "Saving data to file '" + filePath + "', exists:" + file.exists());
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream mFileOutputStream = null;
        boolean result = false;
        try {
            mFileOutputStream = new FileOutputStream(file.getPath());
            mFileOutputStream.write(data);

            result = true;
        } catch (IOException e) {
            Log.e(CLASS_NAME, "Save Data To File IOException", e);
        } finally {
            if (mFileOutputStream != null) {
                try {
                    mFileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     *
     * @param data
     * @param dirName
     * @param fileName
     * @return
     */
    public static boolean saveDataToFile(final byte[] data, final String dirName,
                                         final String fileName) {
        createDirIfNeeded(dirName);
        return saveDataToFile(data, dirName + "/" + fileName);
    }

    /**
     * This method creates a directory with given name is such does not exists
     *
     * @param path a path to the directory
     */
    public static void createDirIfNeeded(final String path) {
        final File file = new File(path);
        if (file.exists() && !file.isDirectory()) {
            file.delete();
        }
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static boolean isFileExist(final String path) {
        final File file = new File(path);
        return file.exists() && !file.isDirectory();
    }

    /**
     * Return {@link java.io.File} object legal to call on API 8.
     *
     * @param type The type of files directory to return. May be null for the root of the
     *             files directory or one of the following Environment constants for a subdirectory:
     *             DIRECTORY_MUSIC, DIRECTORY_PODCASTS, DIRECTORY_RINGTONES, DIRECTORY_ALARMS,
     *             DIRECTORY_NOTIFICATIONS, DIRECTORY_PICTURES, or DIRECTORY_MOVIES.
     * @param context Context of the callee.
     * @return {@link java.io.File} object.
     */
    @TargetApi(8)
    public static File getExternalFilesDirAPI8(final Context context, final String type) {
        return context.getExternalFilesDir(type);
    }

    public static String getExternalStorageDir(final Context context) {
        final File externalDir = getExternalFilesDirAPI8(context, null);
        return externalDir != null ? externalDir.getAbsolutePath() : null;
    }
}
