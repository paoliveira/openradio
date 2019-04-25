package com.yuriy.openradio.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class FileUtils {

    private static final int FILE_BUFFER = 1024;

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

    public static boolean deleteFile(final String filePath) {
        return deleteFile(new File(filePath));
    }

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
    public static String copyExtFileToIntDir(final Context context, final String filePath) {
        final File directory = getFilesDir(context);
        final File file = new File(directory, AppUtils.generateRandomHexToken(16));
        boolean isException = false;
        try (final InputStream in = new FileInputStream(filePath)) {
            try (final OutputStream out = new FileOutputStream(file)) {
                // Transfer bytes from in to out
                final byte[] buf = new byte[FILE_BUFFER];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } catch (final IOException e) {
                isException = true;
                FabricUtils.logException(e);
            }
        } catch (final FileNotFoundException e) {
            isException = true;
            FabricUtils.logException(
                    new FileNotFoundException("File " + filePath + " not found:\n" + Log.getStackTraceString(e))
            );
        } catch (final IOException e) {
            isException = true;
            FabricUtils.logException(
                    new FileNotFoundException("File " + filePath + " I/O exception:\n" + Log.getStackTraceString(e))
            );
        }
        if (!isFileExists(file)) {
            return null;
        }
        if (isException) {
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
