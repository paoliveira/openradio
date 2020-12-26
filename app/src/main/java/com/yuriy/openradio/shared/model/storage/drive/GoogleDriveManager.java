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

import android.accounts.Account;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.model.storage.FavoritesStorage;
import com.yuriy.openradio.shared.model.storage.LocalRadioStationsStorage;
import com.yuriy.openradio.shared.model.storage.RadioStationsStorage;
import com.yuriy.openradio.shared.utils.AnalyticsUtils;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.ConcurrentFactory;
import com.yuriy.openradio.shared.vo.RadioStation;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class GoogleDriveManager {

    /**
     * Listener for the Google Drive client events.
     */
    public interface Listener {

        /**
         * Google Drive requested information about account to use.
         */
        void onAccountRequested(final GoogleSignInClient client);

        /**
         * Google Drive client start to perform command, such as {@link Command#UPLOAD} or {@link Command#DOWNLOAD}.
         *
         * @param command Command which is started.
         */
        void onStart(final Command command);

        /**
         * Google Drive successfully completed to perform command,
         * such as {@link Command#UPLOAD} or {@link Command#DOWNLOAD}.
         *
         * @param command Command which is completed.
         */
        void onSuccess(final Command command);

        /**
         * Google Drive experiencing an error while perform command,
         * such as {@link Command#UPLOAD} or {@link Command#DOWNLOAD}.
         *
         * @param command Command which experiencing an error.
         * @param error   Error message describes a reason.
         */
        void onError(final Command command, final GoogleDriveError error);
    }

    private static final String RADIO_STATION_CATEGORY_FAVORITES = "favorites";
    private static final String RADIO_STATION_CATEGORY_LOCALS = "locals";
    private static final String FOLDER_NAME = "OPEN_RADIO";
    private static final String FILE_NAME_RADIO_STATIONS = "RadioStations.txt";
    /**
     * Google Drive API helper.
     */
    private GoogleDriveHelper mGoogleDriveApiHelper;
    /**
     *
     */
    private final Queue<Command> mCommands;
    private final Listener mListener;
    private final Context mContext;
    private final ExecutorService mExecutorService;

    /**
     * Command to perform.
     */
    public enum Command {
        UPLOAD,
        DOWNLOAD
    }

    /**
     * Main constructor.
     *
     * @param context  Context of the application.
     * @param listener Listener for the Google Drive client events.
     */
    public GoogleDriveManager(@NonNull final Context context, @NonNull final Listener listener) {
        super();

        mContext = context;
        mCommands = new ConcurrentLinkedQueue<>();
        mListener = listener;
        mExecutorService = ConcurrentFactory.makeGoogleDriveExecutor();
    }

    public void connect(final Account account) {
        if (mGoogleDriveApiHelper != null) {
            return;
        }
        mGoogleDriveApiHelper = getGoogleApiClient(account);
        handleNextCommand();
    }

    public void disconnect() {
        mCommands.clear();
        mExecutorService.shutdownNow();
    }

    /**
     * Upload Radio Stations to Google Drive.
     */
    public void uploadRadioStations() {
        queueCommand(Command.UPLOAD);
    }

    /**
     * Download Radio Stations from Google Drive.
     */
    public void downloadRadioStations() {
        queueCommand(Command.DOWNLOAD);
    }

    /**
     * Put a command to query.
     *
     * @param command Command to put in queue.
     */
    private void queueCommand(final Command command) {
        addCommand(command);
        if (mGoogleDriveApiHelper == null) {
            // Check if the user is already signed in and all required scopes are granted
            final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mContext);
            if (account != null && GoogleSignIn.hasPermissions(account, new Scope(Scopes.DRIVE_FILE))) {
                connect(account.getAccount());
            } else {
                final GoogleSignInClient client = buildGoogleSignInClient();
                mListener.onAccountRequested(client);
            }
        } else {
            handleNextCommand();
        }
    }

    private GoogleSignInClient buildGoogleSignInClient() {
        final GoogleSignInOptions options =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .build();
        return GoogleSignIn.getClient(mContext, options);
    }

    /**
     * Get data of all Radio Stations which are intended to upload and upload it.
     */
    private void getRadioStationsAndUpload() {
        final String favorites = FavoritesStorage.getAllFavoritesAsString(mContext);
        final String locals = LocalRadioStationsStorage.getAllLocalAsString(mContext);
        final String data = mergeRadioStationCategories(favorites, locals);
        final GoogleDriveRequest.Listener listener = new GoogleDriveRequestListenerImpl(this, Command.UPLOAD);

        if (mExecutorService.isShutdown()) {
            AppLogger.e("Executor is terminated, can't handle upload radio stations requests");
            return;
        }
        mExecutorService.submit(
                () -> uploadInternal(FOLDER_NAME, FILE_NAME_RADIO_STATIONS, data, listener)
        );
    }

    /**
     * Initiate download and provide a listener.
     */
    private void downloadRadioStationsAndApply() {
        final GoogleDriveRequest.Listener listener = new GoogleDriveRequestListenerImpl(this, Command.DOWNLOAD);

        if (mExecutorService.isShutdown()) {
            AppLogger.e("Executor is terminated, can't handle download radio stations requests");
            return;
        }
        mExecutorService.submit(
                () -> downloadInternal(FOLDER_NAME, FILE_NAME_RADIO_STATIONS, listener)
        );
    }

    /**
     * Do actual upload of a single Radio Stations category.
     *
     * @param folderName Folder to upload to.
     * @param fileName   File name to associate with Radio Stations data.
     * @param data       Marshalled Radio Stations.
     * @param listener   Listener.
     */
    private void uploadInternal(final String folderName, final String fileName, final String data,
                                final GoogleDriveRequest.Listener listener) {
        final GoogleDriveRequest request = new GoogleDriveRequest(
                mGoogleDriveApiHelper, folderName, fileName, data, listener
        );
        final GoogleDriveResult result = new GoogleDriveResult();

        final GoogleDriveAPIChain queryFolder = new GoogleDriveQueryFolder(mExecutorService);
        final GoogleDriveAPIChain createFolder = new GoogleDriveCreateFolder(mExecutorService);
        final GoogleDriveAPIChain queryFile = new GoogleDriveQueryFile(mExecutorService);
        final GoogleDriveAPIChain deleteFile = new GoogleDriveDeleteFile(mExecutorService);
        final GoogleDriveAPIChain saveFile = new GoogleDriveSaveFile(true, mExecutorService);

        queryFolder.setNext(createFolder);
        createFolder.setNext(queryFile);
        queryFile.setNext(deleteFile);
        deleteFile.setNext(saveFile);

        queryFolder.handleRequest(request, result);
    }

    /**
     * Do actual downloading of the data stored on Google Drive.
     *
     * @param folderName Name of the folder to download from.
     * @param fileName   File name associated with Radio Stations data.
     * @param listener   Listener of the download related events.
     */
    private void downloadInternal(final String folderName, final String fileName,
                                  final GoogleDriveRequest.Listener listener) {
        final GoogleDriveRequest request = new GoogleDriveRequest(
                mGoogleDriveApiHelper, folderName, fileName, null, listener
        );

        final GoogleDriveResult result = new GoogleDriveResult();

        final GoogleDriveAPIChain queryFolder = new GoogleDriveQueryFolder(mExecutorService);
        final GoogleDriveAPIChain queryFile = new GoogleDriveQueryFile(mExecutorService);
        final GoogleDriveAPIChain readFile = new GoogleDriveReadFile(true, mExecutorService);

        queryFolder.setNext(queryFile);
        queryFile.setNext(readFile);

        queryFolder.handleRequest(request, result);
    }

    /**
     * Add command to queue.
     *
     * @param command Command to add.
     */
    private void addCommand(final Command command) {
        if (mCommands.contains(command)) {
            return;
        }
        mCommands.add(command);
    }

    /**
     * Remove command from the queue.
     *
     * @return Removed command.
     */
    private Command removeCommand() {
        return mCommands.remove();
    }

    /**
     * Returns instance to Google Drive API helper.
     *
     * @return Instance of the {@link GoogleDriveHelper}.
     */
    private GoogleDriveHelper getGoogleApiClient(@NonNull final Account account) {
        // Use the authenticated account to sign in to the Drive service.
        final GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        mContext, Collections.singleton(DriveScopes.DRIVE_FILE)
                );
        credential.setSelectedAccount(account);
        final Drive drive = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                .setApplicationName(mContext.getString(R.string.app_name))
                .build();

        // The DriveServiceHelper encapsulates all REST API and SAF functionality.
        // Its instantiation is required before handling any onClick actions.
        return new GoogleDriveHelper(drive);
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

        if (FILE_NAME_RADIO_STATIONS.equals(fileName)) {
            final String favoritesRx = splitRadioStationCategories(data)[0];
            final String localsRx = splitRadioStationCategories(data)[1];
            final List<RadioStation> favoritesList = FavoritesStorage.getAll(mContext);
            final List<RadioStation> favoritesRxList = FavoritesStorage.getAllFavoritesFromString(mContext, favoritesRx);
            RadioStationsStorage.merge(favoritesList, favoritesRxList);
            for (final RadioStation radioStation : favoritesList) {
                FavoritesStorage.add(radioStation, mContext);
            }
            final List<RadioStation> localsList = LocalRadioStationsStorage.getAllLocals(mContext);
            final List<RadioStation> localsRxList = LocalRadioStationsStorage.getAllLocalsFromString(mContext, localsRx);
            RadioStationsStorage.merge(localsList, localsRxList);
            for (final RadioStation radioStation : localsList) {
                LocalRadioStationsStorage.add(radioStation, mContext);
            }
        }
    }

    /**
     * Merge provided categories into the single data string.
     *
     * @param favorites Favorites Radio Stations as one single string.
     * @param locals    Locals Radio Stations as one single string.
     * @return Data sting.
     */
    private String mergeRadioStationCategories(@NonNull final String favorites, @NonNull final String locals) {
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(RADIO_STATION_CATEGORY_FAVORITES, favorites);
            jsonObject.put(RADIO_STATION_CATEGORY_LOCALS, locals);
        } catch (final JSONException e) {
            AnalyticsUtils.logException(e);
        }
        return jsonObject.toString();
    }

    /**
     * Split provided data string in to the Radio Station categories.
     *
     * @param data String data represent merged Radio Stations.
     * @return Array of string each of which represent Radio Stations in category.
     */
    private String[] splitRadioStationCategories(@NonNull final String data) {
        final String[] categories = new String[]{"", ""};
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(data);
        } catch (final JSONException e) {
            AnalyticsUtils.logException(e);
        }
        if (jsonObject != null) {
            categories[0] = jsonObject.optString(RADIO_STATION_CATEGORY_FAVORITES, "");
            categories[1] = jsonObject.optString(RADIO_STATION_CATEGORY_LOCALS, "");
        }
        return categories;
    }

    private static final class GoogleDriveRequestListenerImpl implements GoogleDriveRequest.Listener {

        private final WeakReference<GoogleDriveManager> mReference;
        private final Command mCommand;

        private GoogleDriveRequestListenerImpl(final GoogleDriveManager reference,
                                               final Command command) {
            super();
            mReference = new WeakReference<>(reference);
            mCommand = command;
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
            manager.mListener.onSuccess(mCommand);
        }

        @Override
        public void onDownloadComplete(final String data, final String fileName) {
            final GoogleDriveManager manager = mReference.get();
            AppLogger.d("On Google Drive download completed, manager:" + manager);
            if (manager == null) {
                return;
            }

            manager.handleNextCommand();

            if (data != null) {
                manager.handleDownloadCompleted(data, fileName);
            }
            AppLogger.d("On Google Drive download completed, listener:" + manager.mListener);

            manager.mListener.onSuccess(mCommand);
        }

        @Override
        public void onError(final GoogleDriveError error) {
            final GoogleDriveManager manager = mReference.get();
            if (manager == null) {
                return;
            }

            manager.handleNextCommand();
            manager.mListener.onError(mCommand, error);
        }
    }
}

