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
import com.yuriy.openradio.service.FavoritesStorage;
import com.yuriy.openradio.service.LocalRadioStationsStorage;
import com.yuriy.openradio.utils.AppLogger;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class GoogleDriveManager {

    public interface Listener {

        void handleConnectionFailed(@NonNull final ConnectionResult connectionResult);

        void showProgress();

        void hideProgress();
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

    private final GoogleDriveRequest.Listener mGoogleRequestListener = new GoogleDriveRequestListenerImpl(this);

    private enum Command {
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

    public void disconnect() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    public void connect() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
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
                onConnected();
            }
        }
    }

    /**
     * Get data of all Radio Stations which are intended to upload and upload it.
     */
    private void getRadioStationsAndUpload() {
        final String favorites = FavoritesStorage.getAllFavoritesAsString(mContext);
        final String locals = LocalRadioStationsStorage.getAllLocalAsString(mContext);

        uploadInternal(FOLDER_NAME, FILE_NAME_FAVORITES, favorites);
        uploadInternal(FOLDER_NAME, FILE_NAME_LOCALS, locals);
    }

    private void downloadRadioStationsAndApply() {
        // TODO:
    }

    /**
     * Do actual upload of a single Radio Stations category.
     *
     * @param folderName Folder to upload to.
     * @param fileName   File name to associated with Radio Stations data.
     * @param data       Marshalled Radio Stations.
     */
    private void uploadInternal(final String folderName, final String fileName, final String data) {
        final GoogleDriveRequest request = new GoogleDriveRequest(
                mGoogleApiClient, folderName, fileName, data, mGoogleRequestListener
        );
        final GoogleDriveResult result = new GoogleDriveResult();

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

    private synchronized void addCommand(final Command command) {
        mCommands.add(command);
    }

    private synchronized void removeCommand(final Command command) {
        mCommands.remove(command);
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
     *
     */
    private void onConnected() {
        final Iterator<Command> iterator = mCommands.iterator();
        Command command;
        while (iterator.hasNext()) {
            command = iterator.next();
            iterator.remove();
            removeCommand(command);
            switch (command) {
                case UPLOAD:
                    getRadioStationsAndUpload();
                    break;
                case DOWNLOAD:
                    downloadRadioStationsAndApply();
                    break;
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
            manager.onConnected();
        }

        @Override
        public void onConnectionSuspended(final int i) {
            AppLogger.d("On Connection suspended:" + i);
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

        private GoogleDriveRequestListenerImpl(final GoogleDriveManager reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onStart() {
            AppLogger.e("On Google Drive started");
            final GoogleDriveManager manager = mReference.get();
            if (manager == null) {
                return;
            }

            manager.mListener.showProgress();
        }

        @Override
        public void onComplete() {
            AppLogger.e("On Google Drive completed");
            final GoogleDriveManager manager = mReference.get();
            if (manager == null) {
                return;
            }

            manager.mListener.hideProgress();
        }

        @Override
        public void onError() {
            AppLogger.e("On Google Drive error");
            final GoogleDriveManager manager = mReference.get();
            if (manager == null) {
                return;
            }

            manager.mListener.hideProgress();
        }
    }
}
