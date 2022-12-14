/*
 * Copyright 2017-2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.model.storage.drive

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommonUi
import com.yuriy.openradio.shared.model.storage.StorageManagerLayer
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * @param mContext  Context of the application.
 * @param mListener Listener for the Google Drive client events.
 */
class GoogleDriveManager(private val mContext: Context, private val mListener: Listener) {

    /**
     * Listener for the Google Drive client events.
     */
    interface Listener {
        /**
         * Google Drive requested information about account to use.
         */
        fun onAccountRequested(client: GoogleSignInClient)

        /**
         * Google Drive client start to perform command, such as [Command.UPLOAD] or [Command.DOWNLOAD].
         *
         * @param command Command which is started.
         */
        fun onStart(command: Command)

        /**
         * Google Drive successfully completed to perform command,
         * such as [Command.UPLOAD] or [Command.DOWNLOAD].
         *
         * @param command Command which is completed.
         */
        fun onSuccess(command: Command)

        /**
         * Google Drive experiencing an error while perform command,
         * such as [Command.UPLOAD] or [Command.DOWNLOAD].
         *
         * @param command Command which experiencing an error.
         * @param error   Error message describes a reason.
         */
        fun onError(command: Command, error: GoogleDriveError?)
    }

    /**
     * Google Drive API helper.
     */
    private var mGoogleDriveApiHelper: GoogleDriveHelper? = null

    /**
     *
     */
    private val mCommands = ConcurrentLinkedQueue<Command>()
    private lateinit var mStorageManagerLayer: StorageManagerLayer

    /**
     * Command to perform.
     */
    enum class Command {
        UPLOAD, DOWNLOAD
    }

    init {
        DependencyRegistryCommonUi.inject(this)
    }

    fun configureWith(storagePresenter: StorageManagerLayer) {
        mStorageManagerLayer = storagePresenter
    }

    fun connect(account: Account?) {
        if (mGoogleDriveApiHelper != null) {
            return
        }
        mGoogleDriveApiHelper = getGoogleApiClient(account!!)
        handleNextCommand()
    }

    fun disconnect() {
        mCommands.clear()
    }

    /**
     * Upload Radio Stations to Google Drive.
     */
    fun uploadRadioStations() {
        queueCommand(Command.UPLOAD)
    }

    /**
     * Download Radio Stations from Google Drive.
     */
    fun downloadRadioStations() {
        queueCommand(Command.DOWNLOAD)
    }

    /**
     * Put a command to query.
     *
     * @param command Command to put in queue.
     */
    private fun queueCommand(command: Command) {
        addCommand(command)
        if (mGoogleDriveApiHelper == null) {
            // Check if the user is already signed in and all required scopes are granted
            val account = GoogleSignIn.getLastSignedInAccount(mContext)
            if (account != null && GoogleSignIn.hasPermissions(account, Scope(Scopes.DRIVE_FILE))) {
                connect(account.account)
            } else {
                val client = buildGoogleSignInClient()
                mListener.onAccountRequested(client)
            }
        } else {
            handleNextCommand()
        }
    }

    private fun buildGoogleSignInClient(): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        return GoogleSignIn.getClient(mContext, options)
    }

    /**
     * Get data of all Radio Stations which are intended to upload and upload it.
     */
    private fun getRadioStationsAndUpload() {
        val favorites = mStorageManagerLayer.getAllFavoritesAsString()
        val locals = mStorageManagerLayer.getAllDeviceLocalsAsString()
        val data = mergeRadioStationCategories(favorites, locals)
        val listener = GoogleDriveRequestListenerImpl(this, Command.UPLOAD)
        GlobalScope.launch(Dispatchers.IO) {
            withTimeoutOrNull(GoogleDriveHelper.CMD_TIMEOUT_MS) {
                uploadInternal(FOLDER_NAME, FILE_NAME_RADIO_STATIONS, data, listener)
            } ?: listener.onError(GoogleDriveError("Upload radio stations time out"))
        }
    }

    /**
     * Initiate download and provide a listener.
     */
    private fun downloadRadioStationsAndApply() {
        val listener = GoogleDriveRequestListenerImpl(this, Command.DOWNLOAD)
        GlobalScope.launch(Dispatchers.IO) {
            withTimeoutOrNull(GoogleDriveHelper.CMD_TIMEOUT_MS) {
                downloadInternal(FOLDER_NAME, FILE_NAME_RADIO_STATIONS, listener)
            } ?: listener.onError(GoogleDriveError("Download radio stations time out"))
        }
    }

    /**
     * Do actual upload of a single Radio Stations category.
     *
     * @param folderName Folder to upload to.
     * @param fileName   File name to associate with Radio Stations data.
     * @param data       Marshalled Radio Stations.
     * @param listener   Listener.
     */
    private fun uploadInternal(
        folderName: String, fileName: String, data: String,
        listener: GoogleDriveRequest.Listener
    ) {
        val request = GoogleDriveRequest(
            mGoogleDriveApiHelper!!, folderName, fileName, data, listener
        )
        val result = GoogleDriveResult()
        val queryFolder = GoogleDriveQueryFolder()
        val createFolder = GoogleDriveCreateFolder()
        val queryFile = GoogleDriveQueryFile()
        val deleteFile = GoogleDriveDeleteFile()
        val saveFile = GoogleDriveSaveFile(true)
        queryFolder.setNext(createFolder)
        createFolder.setNext(queryFile)
        queryFile.setNext(deleteFile)
        deleteFile.setNext(saveFile)
        queryFolder.handleRequest(request, result)
    }

    /**
     * Do actual downloading of the data stored on Google Drive.
     *
     * @param folderName Name of the folder to download from.
     * @param fileName   File name associated with Radio Stations data.
     * @param listener   Listener of the download related events.
     */
    private fun downloadInternal(
        folderName: String, fileName: String,
        listener: GoogleDriveRequest.Listener
    ) {
        val request = GoogleDriveRequest(
            mGoogleDriveApiHelper!!, folderName, fileName, null, listener
        )
        val result = GoogleDriveResult()
        val queryFolder = GoogleDriveQueryFolder()
        val queryFile = GoogleDriveQueryFile()
        val readFile = GoogleDriveReadFile(true)
        queryFolder.setNext(queryFile)
        queryFile.setNext(readFile)
        queryFolder.handleRequest(request, result)
    }

    /**
     * Add command to queue.
     *
     * @param command Command to add.
     */
    private fun addCommand(command: Command) {
        if (mCommands.contains(command)) {
            return
        }
        mCommands.add(command)
    }

    /**
     * Remove command from the queue.
     *
     * @return Removed command.
     */
    private fun removeCommand(): Command {
        return mCommands.remove()
    }

    /**
     * Returns instance to Google Drive API helper.
     *
     * @return Instance of the [GoogleDriveHelper].
     */
    private fun getGoogleApiClient(account: Account): GoogleDriveHelper {
        // Use the authenticated account to sign in to the Drive service.
        val credential = GoogleAccountCredential.usingOAuth2(
            mContext, setOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account
        val drive = Drive.Builder(AndroidHttp.newCompatibleTransport(), GsonFactory(), credential)
            .setApplicationName(mContext.getString(R.string.app_name))
            .build()

        // The DriveServiceHelper encapsulates all REST API and SAF functionality.
        // Its instantiation is required before handling any onClick actions.
        return GoogleDriveHelper(drive)
    }

    /**
     * Handles next available command.
     */
    private fun handleNextCommand() {
        if (mCommands.isEmpty()) {
            return
        }
        when (removeCommand()) {
            Command.UPLOAD -> getRadioStationsAndUpload()
            Command.DOWNLOAD -> downloadRadioStationsAndApply()
        }
    }

    /**
     * Demarshall String into List of Radio Stations and update storage of the application.
     *
     * @param data     String representing list of Radio Stations.
     * @param fileName Name of the file
     */
    private fun handleDownloadCompleted(data: String, fileName: String) {
        AppLogger.d("OnDownloadCompleted file:$fileName data:$data")
        if (FILE_NAME_RADIO_STATIONS == fileName) {
            val favoritesRx = splitRadioStationCategories(data)[0]
            mStorageManagerLayer.mergeFavorites(favoritesRx)
            val localsRx = splitRadioStationCategories(data)[1]
            mStorageManagerLayer.mergeDeviceLocals(localsRx)
        }
    }

    /**
     * Merge provided categories into the single data string.
     *
     * @param favorites Favorites Radio Stations as one single string.
     * @param locals    Locals Radio Stations as one single string.
     * @return Data sting.
     */
    private fun mergeRadioStationCategories(favorites: String, locals: String): String {
        val jsonObject = JSONObject()
        try {
            jsonObject.put(RADIO_STATION_CATEGORY_FAVORITES, favorites)
            jsonObject.put(RADIO_STATION_CATEGORY_LOCALS, locals)
        } catch (e: JSONException) {
            AppLogger.e("MergeRadioStationCategories", e)
        }
        return jsonObject.toString()
    }

    /**
     * Split provided data string in to the Radio Station categories.
     *
     * @param data String data represent merged Radio Stations.
     * @return Array of string each of which represent Radio Stations in category.
     */
    private fun splitRadioStationCategories(data: String): Array<String> {
        val categories = arrayOf(AppUtils.EMPTY_STRING, AppUtils.EMPTY_STRING)
        var jsonObject: JSONObject? = null
        try {
            jsonObject = JSONObject(data)
        } catch (e: JSONException) {
            AppLogger.e("SplitRadioStationCategories", e)
        }
        if (jsonObject != null) {
            categories[0] = jsonObject.optString(RADIO_STATION_CATEGORY_FAVORITES, AppUtils.EMPTY_STRING)
            categories[1] = jsonObject.optString(RADIO_STATION_CATEGORY_LOCALS, AppUtils.EMPTY_STRING)
        }
        return categories
    }

    private class GoogleDriveRequestListenerImpl(
        reference: GoogleDriveManager,
        private val mCommand: Command
    ) : GoogleDriveRequest.Listener {

        private val mReference = WeakReference(reference)

        override fun onStart() {
            AppLogger.d("On Google Drive started")
            val manager = mReference.get() ?: return
            manager.mListener.onStart(mCommand)
        }

        override fun onUploadComplete() {
            AppLogger.d("On Google Drive upload completed")
            val manager = mReference.get() ?: return
            manager.handleNextCommand()
            manager.mListener.onSuccess(mCommand)
        }

        override fun onDownloadComplete(data: String, fileName: String) {
            val manager = mReference.get()
            AppLogger.d("On Google Drive download completed, manager:$manager")
            if (manager == null) {
                return
            }
            manager.handleNextCommand()
            manager.handleDownloadCompleted(data, fileName)
            AppLogger.d("On Google Drive download completed, listener:" + manager.mListener)
            manager.mListener.onSuccess(mCommand)
        }

        override fun onError(error: GoogleDriveError?) {
            val manager = mReference.get() ?: return
            manager.handleNextCommand()
            manager.mListener.onError(mCommand, error)
        }

    }

    companion object {
        private const val RADIO_STATION_CATEGORY_FAVORITES = "favorites"
        private const val RADIO_STATION_CATEGORY_LOCALS = "locals"
        private const val FOLDER_NAME = "OPEN_RADIO"
        private const val FILE_NAME_RADIO_STATIONS = "RadioStations.txt"
    }
}
