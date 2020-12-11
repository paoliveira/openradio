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

package com.yuriy.openradio.shared.model.storage.drive;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.yuriy.openradio.shared.utils.AppLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

/**
 * A utility for performing read/write operations on Drive files via the REST API.
 */
public final class GoogleDriveHelper {

    private static final String MIME_TYPE_TEXT = "text/plain";
    private static final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";
    private final Drive mDriveService;

    public GoogleDriveHelper(@NonNull final Drive driveService) {
        super();
        mDriveService = driveService;
    }

    /**
     * Creates a text file in the user's My Drive folder and returns its file ID.
     */
    public final Task<String> createFile(@NonNull final ExecutorService executorService,
                                         final String folderId, @NonNull final String name,
                                         @NonNull final String content) {
        if (executorService.isShutdown()) {
            AppLogger.e("Executor is terminated, can't handle create file requests");
            return Tasks.forCanceled();
        }
        return Tasks.call(executorService, () -> {
            final File metadata = new File()
                    .setParents(Collections.singletonList(folderId))
                    .setMimeType(MIME_TYPE_TEXT)
                    .setName(name);
            // Convert content to an AbstractInputStreamContent instance.
            final ByteArrayContent contentStream = ByteArrayContent.fromString(MIME_TYPE_TEXT, content);
            final File file = mDriveService.files().create(metadata, contentStream).execute();
            if (file == null) {
                throw new IOException("Null result when requesting file creation.");
            }
            return file.getId();
        });
    }

    /**
     *
     * @param name
     * @return
     */
    public final Task<String> createFolder(@NonNull final ExecutorService executorService,
                                           @NonNull final String name) {
        if (executorService.isShutdown()) {
            AppLogger.e("Executor is terminated, can't handle create folder requests");
            return Tasks.forCanceled();
        }
        return Tasks.call(executorService, () -> {
                    final File metadata = new File()
                            .setMimeType(MIME_TYPE_FOLDER)
                            .setName(name);
                    final File folder = mDriveService.files().create(metadata)
                            .setFields("id")
                            .execute();
                    return folder.getId();
                }
        );
    }

    /**
     * Opens the file identified by {@code fileId} and returns a {@link Pair} of its name and
     * contents.
     */
    public final Task<Pair<String, String>> readFile(@NonNull final ExecutorService executorService,
                                                     @NonNull final String fileId) {
        if (executorService.isShutdown()) {
            AppLogger.e("Executor is terminated, can't handle read file requests");
            return Tasks.forCanceled();
        }
        return Tasks.call(executorService, () -> {
                    // Retrieve the metadata as a File object.
                    final File metadata = mDriveService.files().get(fileId).execute();
                    final String name = metadata.getName();
                    // Stream the file contents to a String.
                    try (final InputStream is = mDriveService.files().get(fileId).executeMediaAsInputStream();
                         final BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                        final StringBuilder stringBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line);
                        }
                        return Pair.create(name, stringBuilder.toString());
                    }
                }
        );
    }

    /**
     * Updates the file identified by {@code fileId} with the given {@code name} and {@code
     * content}.
     */
    public final Task<Void> saveFile(@NonNull final ExecutorService executorService,
                                     @NonNull final String fileId,
                                     @NonNull final String name,
                                     @NonNull final String content) {
        if (executorService.isShutdown()) {
            AppLogger.e("Executor is terminated, can't handle save file requests");
            return Tasks.forCanceled();
        }
        return Tasks.call(executorService, () -> {
            // Create a File containing any metadata changes.
            File metadata = new File().setName(name);
            // Convert content to an AbstractInputStreamContent instance.
            ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content);
            // Update the metadata and contents.
            mDriveService.files().update(fileId, metadata, contentStream).execute();
            return null;
        });
    }

    public final Task<Void> deleteFile(@NonNull final ExecutorService executorService,
                                       @NonNull final String fileId) {
        if (executorService.isShutdown()) {
            AppLogger.e("Executor is terminated, can't handle delete file requests");
            return Tasks.forCanceled();
        }
        return Tasks.call(executorService, () -> {
                    // Delete file with specified id..
                    mDriveService.files().delete(fileId).execute();
                    return null;
                }
        );
    }

    /**
     * Returns a {@link FileList} containing folder with given name in the user's My Drive.
     *
     * <p>The returned list will only contain folder visible to this app, i.e. those which were
     * created by this app.</p>
     */
    public final Task<FileList> queryFolder(@NonNull final ExecutorService executorService, final String name) {
        if (executorService.isShutdown()) {
            AppLogger.e("Executor is terminated, can't handle query folder requests");
            return Tasks.forCanceled();
        }
        return Tasks.call(executorService, () -> mDriveService.files().list()
                        .setSpaces("drive")
                        .setQ("mimeType='" + MIME_TYPE_FOLDER + "' and trashed=false and name='" + name + "'")
                        .execute()
        );
    }

    /**
     * Returns a {@link FileList} containing file with given name in the user's My Drive.
     *
     * <p>The returned list will only contain folder visible to this app, i.e. those which were
     * created by this app.</p>
     */
    public final Task<FileList> queryFile(@NonNull final ExecutorService executorService, final String fileName) {
        if (executorService.isShutdown()) {
            AppLogger.e("Executor is terminated, can't handle query file requests");
            return Tasks.forCanceled();
        }
        return Tasks.call(executorService, () -> mDriveService.files().list()
                        .setSpaces("drive")
                        .setQ("mimeType='" + MIME_TYPE_TEXT + "' and trashed=false and name='" + fileName + "'")
                        .execute()
        );
    }
}
