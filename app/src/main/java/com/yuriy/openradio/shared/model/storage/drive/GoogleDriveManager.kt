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
import com.yuriy.openradio.shared.model.storage.FavoritesStorage
import com.yuriy.openradio.shared.model.storage.FavoritesStorage.getAll
import com.yuriy.openradio.shared.model.storage.FavoritesStorage.getAllFavoritesAsString
import com.yuriy.openradio.shared.model.storage.FavoritesStorage.getAllFavoritesFromString
import com.yuriy.openradio.shared.model.storage.LocalRadioStationsStorage
import com.yuriy.openradio.shared.model.storage.LocalRadioStationsStorage.getAllLocalAsString
import com.yuriy.openradio.shared.model.storage.LocalRadioStationsStorage.getAllLocals
import com.yuriy.openradio.shared.model.storage.LocalRadioStationsStorage.getAllLocalsFromString
import com.yuriy.openradio.shared.model.storage.RadioStationsStorage.Companion.merge
import com.yuriy.openradio.shared.utils.AnalyticsUtils.logException
import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.vo.RadioStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.*

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * @param mContext  Context of the application.
 * @param listener Listener for the Google Drive client events.
*/
class GoogleDriveManager(private val mContext: Context, listener: Listener) {
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
    private val mCommands: Queue<Command>
    private val mListener: Listener

    /**
     * Command to perform.
     */
    enum class Command {
        UPLOAD, DOWNLOAD
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
    private val radioStationsAndUpload: Unit
        get() {
            val favorites = getAllFavoritesAsString(mContext)
            val locals = getAllLocalAsString(mContext)
            val data = mergeRadioStationCategories(favorites, locals)
            val listener: GoogleDriveRequest.Listener = GoogleDriveRequestListenerImpl(this, Command.UPLOAD)
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
        val listener: GoogleDriveRequest.Listener = GoogleDriveRequestListenerImpl(this, Command.DOWNLOAD)
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
    private fun uploadInternal(folderName: String, fileName: String, data: String,
                               listener: GoogleDriveRequest.Listener) {
        val request = GoogleDriveRequest(
                mGoogleDriveApiHelper!!, folderName, fileName, data, listener
        )
        val result = GoogleDriveResult()
        val queryFolder: GoogleDriveAPIChain = GoogleDriveQueryFolder()
        val createFolder: GoogleDriveAPIChain = GoogleDriveCreateFolder()
        val queryFile: GoogleDriveAPIChain = GoogleDriveQueryFile()
        val deleteFile: GoogleDriveAPIChain = GoogleDriveDeleteFile()
        val saveFile: GoogleDriveAPIChain = GoogleDriveSaveFile(true)
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
    private fun downloadInternal(folderName: String, fileName: String,
                                 listener: GoogleDriveRequest.Listener) {
        val request = GoogleDriveRequest(
                mGoogleDriveApiHelper!!, folderName, fileName, null, listener
        )
        val result = GoogleDriveResult()
        val queryFolder: GoogleDriveAPIChain = GoogleDriveQueryFolder()
        val queryFile: GoogleDriveAPIChain = GoogleDriveQueryFile()
        val readFile: GoogleDriveAPIChain = GoogleDriveReadFile(true)
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
                mContext, setOf(DriveScopes.DRIVE_FILE))
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
        val command = removeCommand()
        when (command) {
            Command.UPLOAD -> radioStationsAndUpload
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
        d("OnDownloadCompleted file:$fileName data:$data")
        if (FILE_NAME_RADIO_STATIONS == fileName) {
            val favoritesRx = splitRadioStationCategories(data)[0]
            val localsRx = splitRadioStationCategories(data)[1]
            val favoritesList: MutableList<RadioStation> = getAll(mContext)
            val favoritesRxList: List<RadioStation> = getAllFavoritesFromString(mContext, favoritesRx)
            merge(favoritesList, favoritesRxList)
            for (radioStation in favoritesList) {
                FavoritesStorage.add(radioStation, mContext)
            }
            val localsList: MutableList<RadioStation> = getAllLocals(mContext)
            val localsRxList: List<RadioStation> = getAllLocalsFromString(mContext, localsRx)
            merge(localsList, localsRxList)
            for (radioStation in localsList) {
                LocalRadioStationsStorage.add(radioStation, mContext)
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
    private fun mergeRadioStationCategories(favorites: String, locals: String): String {
        val jsonObject = JSONObject()
        try {
            jsonObject.put(RADIO_STATION_CATEGORY_FAVORITES, favorites)
            jsonObject.put(RADIO_STATION_CATEGORY_LOCALS, locals)
        } catch (e: JSONException) {
            logException(e)
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
        val categories = arrayOf("", "")
        var jsonObject: JSONObject? = null
        try {
            jsonObject = JSONObject(data)
        } catch (e: JSONException) {
            logException(e)
        }
        if (jsonObject != null) {
            categories[0] = jsonObject.optString(RADIO_STATION_CATEGORY_FAVORITES, "")
            categories[1] = jsonObject.optString(RADIO_STATION_CATEGORY_LOCALS, "")
        }
        return categories
    }

    private class GoogleDriveRequestListenerImpl(reference: GoogleDriveManager,
                                                 command: Command) : GoogleDriveRequest.Listener {
        private val mReference: WeakReference<GoogleDriveManager> = WeakReference(reference)
        private val mCommand: Command = command
        override fun onStart() {
            d("On Google Drive started")
            val manager = mReference.get() ?: return
            manager.mListener.onStart(mCommand)
        }

        override fun onUploadComplete() {
            d("On Google Drive upload completed")
            val manager = mReference.get() ?: return
            manager.handleNextCommand()
            manager.mListener.onSuccess(mCommand)
        }

        override fun onDownloadComplete(data: String, fileName: String) {
            val manager = mReference.get()
            d("On Google Drive download completed, manager:$manager")
            if (manager == null) {
                return
            }
            manager.handleNextCommand()
            manager.handleDownloadCompleted(data, fileName)
            d("On Google Drive download completed, listener:" + manager.mListener)
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

    init {
        mCommands = ConcurrentLinkedQueue()
        mListener = listener
    }
}
