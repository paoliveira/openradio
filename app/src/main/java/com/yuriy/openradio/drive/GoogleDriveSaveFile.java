package com.yuriy.openradio.drive;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.MetadataChangeSet;
import com.yuriy.openradio.utils.AppLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
final class GoogleDriveSaveFile extends GoogleDriveAPIChain {

    GoogleDriveSaveFile() {
        this(false);
    }

    GoogleDriveSaveFile(final boolean isTerminator) {
        super(isTerminator);
    }

    @Override
    protected void handleRequest(final GoogleDriveRequest request, final GoogleDriveResult result) {
        AppLogger.d("Save file '" + request.getFileName() + "'");

        // create new contents resource
        Drive.DriveApi.newDriveContents(request.getGoogleApiClient()).setResultCallback(
                driveContentsResult -> {
                    if (!driveContentsResult.getStatus().isSuccess()) {
                        return;
                    }
                    final DriveContents driveContents = driveContentsResult.getDriveContents();
                    final Thread thread = new Thread(
                            () -> {
                                // write content to DriveContents
                                final OutputStream outputStream = driveContents.getOutputStream();
                                final Writer writer = new OutputStreamWriter(outputStream);
                                try {
                                    writer.write(request.getData());
                                } catch (final IOException e) {
                                    AppLogger.e("Can not open writer:" + e.getMessage());
                                } finally {
                                    try {
                                        writer.close();
                                    } catch (IOException e) {
                                        /* Ignore */
                                    }
                                }

                                final MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                        .setTitle(request.getFileName())
                                        .setMimeType("text/plain")
                                        .setStarred(true)
                                        .build();

                                // create a file on root folder
                                result.getFolder()
                                        .createFile(request.getGoogleApiClient(), changeSet, driveContents)
                                        .setResultCallback(
                                                driveFileResult -> AppLogger.d("File '" + request.getFileName() + "' saved: " + driveFileResult.getStatus().isSuccess())
                                        );
                            }
                    );
                    thread.start();
                }
        );
    }
}
