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

package com.yuriy.openradio.drive;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.yuriy.openradio.api.RadioStationVO;
import com.yuriy.openradio.service.FavoritesStorage;
import com.yuriy.openradio.service.LatestRadioStationStorage;
import com.yuriy.openradio.service.LocalRadioStationsStorage;
import com.yuriy.openradio.utils.AppLogger;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class GoogleDriveManager {

    public interface Listener {

        void onConnect();

        void onConnected();

        void onConnectionFailed();

        void handleConnectionFailed(@NonNull final ConnectionResult connectionResult);

        void onStart(final GoogleDriveManager.Command command);

        void onSuccess(final GoogleDriveManager.Command command);

        void onError(final GoogleDriveManager.Command command, final GoogleDriveError error);
    }

    private static final String FOLDER_NAME = "OPEN_RADIO";

    private static final String FILE_NAME_FAVORITES = "RadioStationsFavorites.txt";

    private static final String FILE_NAME_LOCALS = "RadioStationsLocals.txt";

    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     *
     */
    private final Queue<Command> mCommands;

    private final Listener mListener;

    private final Context mContext;

    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    public enum Command {
        UPLOAD,
        DOWNLOAD
    }

    /**
     *
     */
    public GoogleDriveManager(final Context context, final Listener listener) {
        super();

        mContext = context;
        mCommands = new ConcurrentLinkedQueue<>();
        mListener = listener;
    }

    public void release() {
        mExecutorService.shutdown();
    }

    public void disconnect() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    public void connect() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();

            mListener.onConnect();
        }
    }

    /**
     * Upload Radio Stations to Google Drive.
     */
    public void uploadRadioStations() {
        queueCommand(mContext, Command.UPLOAD);
    }

    /**
     *
     */
    public void downloadRadioStations() {
        queueCommand(mContext, Command.DOWNLOAD);
    }

    /**
     * Put a command to query.
     *
     * @param context Context of the callee.
     * @param command Command to put in queue.
     */
    private void queueCommand(final Context context, final Command command) {
        final GoogleApiClient client = getGoogleApiClient(context);
        addCommand(command);
        if (!client.isConnected()) {
            client.connect();
        } else {
            if (!client.isConnecting()) {
                handleNextCommand();
            }
        }
    }

    /**
     * Get data of all Radio Stations which are intended to upload and upload it.
     */
    private void getRadioStationsAndUpload() {
        final String favorites = FavoritesStorage.getAllFavoritesAsString(mContext);
        final String locals = LocalRadioStationsStorage.getAllLocalAsString(mContext);
        final int numOfCompleteCallbacks = 2;
        final GoogleDriveRequest.Listener listener = new GoogleDriveRequestListenerImpl(
                this, Command.UPLOAD, numOfCompleteCallbacks
        );

        mExecutorService.submit(
                () -> {
                    uploadInternal(FOLDER_NAME, FILE_NAME_FAVORITES, favorites, listener);
                    uploadInternal(FOLDER_NAME, FILE_NAME_LOCALS, locals, listener);
                }
        );
    }

    /**
     *
     */
    private void downloadRadioStationsAndApply() {
        final int numOfCompleteCallbacks = 2;
        final GoogleDriveRequest.Listener listener = new GoogleDriveRequestListenerImpl(
                this, Command.DOWNLOAD, numOfCompleteCallbacks
        );

        mExecutorService.submit(
                () -> {
                    downloadInternal(FOLDER_NAME, FILE_NAME_FAVORITES, listener);
                    downloadInternal(FOLDER_NAME, FILE_NAME_LOCALS, listener);
                }
        );
    }

    /**
     * Do actual upload of a single Radio Stations category.
     *
     * @param folderName Folder to upload to.
     * @param fileName   File name to associated with Radio Stations data.
     * @param data       Marshalled Radio Stations.
     * @param listener   Listener.
     */
    private void uploadInternal(final String folderName, final String fileName, final String data,
                                final GoogleDriveRequest.Listener listener) {
        final GoogleDriveRequest request = new GoogleDriveRequest(
                mGoogleApiClient, folderName, fileName, data, listener
        );
        final GoogleDriveResult result = new GoogleDriveResult();

        request.setExecutorService(mExecutorService);

        final GoogleDriveAPIChain queryFolder = new GoogleDriveQueryFolder();
        final GoogleDriveAPIChain createFolder = new GoogleDriveCreateFolder();
        final GoogleDriveAPIChain queryFile = new GoogleDriveQueryFile();
        final GoogleDriveAPIChain deleteFile = new GoogleDriveDeleteFile();
        final GoogleDriveAPIChain saveFile = new GoogleDriveSaveFile(true);

        queryFolder.setNext(createFolder);
        createFolder.setNext(queryFile);
        queryFile.setNext(deleteFile);
        deleteFile.setNext(saveFile);

        queryFolder.handleRequest(request, result);
    }

    /**
     *
     * @param folderName
     * @param fileName
     * @param listener
     */
    private void downloadInternal(final String folderName, final String fileName,
                                  final GoogleDriveRequest.Listener listener) {
        final GoogleDriveRequest request = new GoogleDriveRequest(
                mGoogleApiClient, folderName, fileName, null, listener
        );

        request.setExecutorService(mExecutorService);

        final GoogleDriveResult result = new GoogleDriveResult();

        final GoogleDriveAPIChain queryFolder = new GoogleDriveQueryFolder();
        final GoogleDriveAPIChain queryFile = new GoogleDriveQueryFile();
        final GoogleDriveAPIChain readFile = new GoogleDriveReadFile(true);

        queryFolder.setNext(queryFile);
        queryFile.setNext(readFile);

        queryFolder.handleRequest(request, result);
    }

    private void addCommand(final Command command) {
        if (mCommands.contains(command)) {
            return;
        }

        AppLogger.d("Add Command: " + command);
        mCommands.add(command);
    }

    private void removeCommand(final Command command) {
        mCommands.remove(command);
    }

    private Command removeCommand() {
        return mCommands.remove();
    }

    /**
     *
     * @param context
     * @return
     */
    private synchronized GoogleApiClient getGoogleApiClient(final Context context) {
        if (mGoogleApiClient != null) {
            return mGoogleApiClient;
        }

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(new ConnectionCallbackImpl(this))
                .addOnConnectionFailedListener(new ConnectionFailedListenerImpl(this))
                .build();
        return mGoogleApiClient;
    }

    /**
     * Handles next available command.
     */
    private void handleNextCommand() {
        if (mCommands.isEmpty()) {
            return;
        }

        final Command command = removeCommand();
        switch (command) {
            case UPLOAD:
                getRadioStationsAndUpload();
                break;
            case DOWNLOAD:
                downloadRadioStationsAndApply();
                break;
        }
    }

    /**
     * Demarshall String into List of Radio Stations and update storage of the application.
     *
     * @param data     String representing list of Radio Stations.
     * @param fileName Name of the file
     */
    private void handleDownloadCompleted(@NonNull final String data, @NonNull final String fileName) {
        AppLogger.d("OnDownloadCompleted file:" + fileName + " data:" + data);

        if (FILE_NAME_FAVORITES.equals(fileName)) {
            final List<RadioStationVO> list = FavoritesStorage.getAllFavoritesFromString(data);
            for (final RadioStationVO radioStation : list) {
                FavoritesStorage.addToFavorites(radioStation, mContext);
            }
        }

        if (FILE_NAME_LOCALS.equals(fileName)) {
            final List<RadioStationVO> list = LocalRadioStationsStorage.getAllLocalsFromString(data);
            for (final RadioStationVO radioStation : list) {
                LatestRadioStationStorage.addToLocals(radioStation, mContext);
            }
        }
    }

    private static final class ConnectionCallbackImpl implements GoogleApiClient.ConnectionCallbacks {

        private final WeakReference<GoogleDriveManager> mReference;

        private ConnectionCallbackImpl(final GoogleDriveManager reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onConnected(@Nullable final Bundle bundle) {
            AppLogger.d("On Connected:" + bundle);
            final GoogleDriveManager manager = mReference.get();
            if (manager == null) {
                return;
            }
            manager.handleNextCommand();

            manager.mListener.onConnected();
        }

        @Override
        public void onConnectionSuspended(final int i) {
            AppLogger.d("On Connection suspended:" + i);

            final GoogleDriveManager manager = mReference.get();
            if (manager == null) {
                return;
            }
            // TODO:
        }
    }

    private static final class ConnectionFailedListenerImpl implements GoogleApiClient.OnConnectionFailedListener {

        private final WeakReference<GoogleDriveManager> mReference;

        private ConnectionFailedListenerImpl(final GoogleDriveManager reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
            AppLogger.e("On Connection failed:" + connectionResult);
            final GoogleDriveManager manager = mReference.get();
            if (manager == null) {
                return;
            }
            manager.mListener.handleConnectionFailed(connectionResult);
        }
    }

    private static final class GoogleDriveRequestListenerImpl implements GoogleDriveRequest.Listener {

        private final WeakReference<GoogleDriveManager> mReference;
        private int mCompleteCounter = 0;
        private final int mNumOfCallbacks;
        private final Command mCommand;

        private GoogleDriveRequestListenerImpl(final GoogleDriveManager reference,
                                               final Command command,
                                               final int numOfCallbacks) {
            super();
            mReference = new WeakReference<>(reference);
            mCommand = command;
            mNumOfCallbacks = numOfCallbacks;
        }

        @Override
        public void onStart() {
            AppLogger.d("On Google Drive started");
            final GoogleDriveManager manager = mReference.get();
            if (manager == null) {
                return;
            }

            manager.mListener.onStart(mCommand);
        }

        @Override
        public void onUploadComplete() {
            AppLogger.d("On Google Drive upload completed");
            final GoogleDriveManager manager = mReference.get();
            if (manager == null) {
                return;
            }

            manager.handleNextCommand();
            if (++mCompleteCounter == mNumOfCallbacks) {
                manager.mListener.onSuccess(mCommand);
            }
        }

        @Override
        public void onDownloadComplete(final String data, final String fileName) {
            AppLogger.d("On Google Drive download completed");
            final GoogleDriveManager manager = mReference.get();
            if (manager == null) {
                return;
            }

            manager.handleNextCommand();
            manager.mListener.onSuccess(mCommand);

            if (data != null) {
                manager.handleDownloadCompleted(data, fileName);
            }
        }

        @Override
        public void onError(final GoogleDriveError error) {
            AppLogger.e("On Google Drive error : " + error.toString());
            final GoogleDriveManager manager = mReference.get();
            if (manager == null) {
                return;
            }

            manager.handleNextCommand();
            manager.mListener.onError(mCommand, error);
        }
    }
}
