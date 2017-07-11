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

    private enum Command {
        UPLOAD_FILE,
        DOWNLOAD_FILE,
        NONE
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
     *
     */
    public void uploadRadioStationsToGoogleDrive() {
        //mListener.showProgress();
        queueCommand(mContext, Command.UPLOAD_FILE);
    }

    /**
     *
     */
    public void downloadRadioStationsFromGoogleDrive() {
        queueCommand(mContext, Command.DOWNLOAD_FILE);
    }

    /**
     *
     * @param context
     * @param command
     */
    private void queueCommand(final Context context, final Command command) {
        final GoogleApiClient client = getGoogleApiClient(context);
        if (client.isConnecting()) {
            addCommand(Command.UPLOAD_FILE);
        } else if (!client.isConnected()) {
            addCommand(Command.UPLOAD_FILE);
            client.connect();
        } else {
            onConnected();
        }
    }

    private void getRadioStationsAndUpload() {
        final String favorites = FavoritesStorage.getAllFavoritesAsString(mContext);
        final String locals = LocalRadioStationsStorage.getAllLocalAsString(mContext);

        final GoogleDriveRequest request = new GoogleDriveRequest(
                mGoogleApiClient, FOLDER_NAME, FILE_NAME_FAVORITES, favorites
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

    private void downloadRadioStationsAndApply() {
        // TODO:
    }

    private synchronized void addCommand(final Command command) {
        mCommands.add(command);
    }

    private synchronized void removeCommand(final Command command) {
        mCommands.remove(command);
    }

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

    private void onConnected() {
        final Iterator<Command> iterator = mCommands.iterator();
        Command command;
        while (iterator.hasNext()) {
            command = iterator.next();
            iterator.remove();
            removeCommand(command);
            switch (command) {
                case UPLOAD_FILE:
                    getRadioStationsAndUpload();
                    break;
                case DOWNLOAD_FILE:
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
}
