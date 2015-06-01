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

package com.yuriy.openradio.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaDescription;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.yuriy.openradio.api.RadioStationVO;
import com.yuriy.openradio.db.DBHelper;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/23/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class DBService extends IntentService {

    /**
     * Tag for the logging messages.
     */
    private static final String CLASS_NAME = DBService.class.getSimpleName();

    /**
     * Key for the {@link android.os.Bundle} store to hold a {@link android.os.Messenger}
     */
    private static final String BUNDLE_KEY_MESSENGER = "MESSENGER";

    /**
     * Key for the {@link android.os.Bundle} store to hold a {@link MediaDescription}
     */
    private static final String BUNDLE_KEY_RADIO_STATION = "MEDIA_DESCRIPTION";

    private static final String BUNDLE_KEY_IS_FAVORITE = "IS_FAVORITE";

    private static final String BUNDLE_KEY_COMMAND = "COMMAND";

    private static final String COMMAND_SET_FAVORITE = "SET_FAVORITE";

    /**
     * Processes Messages sent to it from onStartCommnand() that
     * indicate which actions perform with the Database.
     */
    private volatile ServiceHandler mServiceHandler;

    /**
     * Looper associated with the HandlerThread.
     */
    private volatile Looper mServiceLooper;

    public DBService() {
        super(DBService.class.getSimpleName());
    }

    @Override
    public final void onCreate() {
        super.onCreate();

        Log.i(CLASS_NAME, "OnCreate");

        // Create and start a background HandlerThread since by
        // default a Service runs in the UI Thread,
        // which we don't want to block.
        final HandlerThread thread = new HandlerThread(DBService.class.getSimpleName() + "Thread");
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler.
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(CLASS_NAME, "OnDestroy");
    }

    @Override
    protected final void onHandleIntent(final Intent intent) {

        Log.i(CLASS_NAME, "OnHandleIntent:" + intent);

        // This method is call in NON UI Thread so there is no need to perform
        // further operation in separate thread.

        // Create a Message that will be sent to ServiceHandler to
        // perform the actions over Database based on the data in the Intent.

        if (!intent.hasExtra(BUNDLE_KEY_COMMAND)) {
            return;
        }

        final String command = intent.getStringExtra(BUNDLE_KEY_COMMAND);

        if (command == null || command.isEmpty()) {
            return;
        }

        Message message = null;
        if (command.equals(COMMAND_SET_FAVORITE)) {
            message = mServiceHandler.makeSaveToFavoritesMessage(intent);
        }

        if (message == null) {
            return;
        }

        // Send the Message to ServiceHandler to retrieve an image
        // based on contents of the Intent.
        mServiceHandler.sendMessage(message);
    }

    /**
     * Factory method to make the {@link android.content.Intent} to save {@link RadioStationVO}
     * as Favorite.
     *
     * @param context          Context of the callee.
     * @param radioStation     {@link RadioStationVO}.
     * @param isFavorite       Is Radio Station Favorite.
     * @param processHandler   Handler to handle response.
     *
     * @return {@link Intent}.
     */
    public static Intent makeUpdateFavoriteIntent(final Context context,
                                                  final RadioStationVO radioStation,
                                                  final boolean isFavorite,
                                                  final Handler processHandler) {
        // Create the Intent that's associated to the DownloadingService class.
        final Intent intent = new Intent(context, DBService.class);

        intent.putExtra(BUNDLE_KEY_COMMAND, COMMAND_SET_FAVORITE);

        // Create and pass a Messenger as an "extra" so the
        // DBService can send back the result.
        if (processHandler != null) {
            intent.putExtra(BUNDLE_KEY_MESSENGER, new Messenger(processHandler));
        }

        intent.putExtra(BUNDLE_KEY_RADIO_STATION, radioStation);

        intent.putExtra(BUNDLE_KEY_IS_FAVORITE, isFavorite);

        return intent;
    }

    private void updateFavoriteItem(final Intent intent) {
        if (intent == null) {
            Log.w(CLASS_NAME, "Update Favorite failed, intent is null");
            return;
        }
        if (!intent.hasExtra(BUNDLE_KEY_RADIO_STATION)) {
            Log.w(CLASS_NAME, "Update Favorite failed, intent has no data");
            return;
        }
        final RadioStationVO radioStation
                = (RadioStationVO) intent.getSerializableExtra(BUNDLE_KEY_RADIO_STATION);
        if (radioStation == null) {
            Log.w(CLASS_NAME, "Update Favorite failed, MediaDescription is null");
            return;
        }

        final boolean isFavorite = intent.hasExtra(BUNDLE_KEY_IS_FAVORITE)
                && intent.getBooleanExtra(BUNDLE_KEY_IS_FAVORITE, false);

        Log.d(CLASS_NAME, "Update Item:" + radioStation + " to Favorite:" + isFavorite);

        final DBHelper helper = new DBHelper(getApplicationContext());
        final SQLiteDatabase database = helper.getWritableDatabase();

        if (isFavorite) {
            addFavorite(database, radioStation);
        } else {
            removeFavorite(database, radioStation);
        }

        closeDatabase(database);
    }

    private void closeDatabase(final SQLiteDatabase database) {
        database.close();
        Log.d(CLASS_NAME, "DB successfully closed");
    }

    private void addFavorite(final SQLiteDatabase database,
                             final RadioStationVO radioStation) {
        Log.d(CLASS_NAME, "Add Favorite:" + radioStation);

        final ContentValues contentValues = new ContentValues();
        contentValues.put("id", String.valueOf(radioStation.getId()));
        contentValues.put("name", radioStation.getName());
        contentValues.put("streamUrl", radioStation.getStreamURL());
        contentValues.put("webSite", radioStation.getWebSite());
        contentValues.put("country", radioStation.getCountry());
        contentValues.put("genre", radioStation.getGenre());
        contentValues.put("imageUrl", radioStation.getImageUrl());
        contentValues.put("thumbUrl", radioStation.getThumbUrl());

        long result = -1;

        try {
            result = database.insertOrThrow(DBHelper.FAVORITES_TABLE_NAME, null, contentValues);
        } catch (SQLiteConstraintException exception) {
            Log.e(CLASS_NAME, "Favorite add error:" + exception.getMessage());
        }

        Log.d(CLASS_NAME, "Favorite add result:" + result);
    }

    private void removeFavorite(final SQLiteDatabase database, final RadioStationVO radioStation) {
        Log.d(CLASS_NAME, "Remove Favorite:" + radioStation);

        final long result = database.delete(
                DBHelper.FAVORITES_TABLE_NAME,
                "id=" + String.valueOf(radioStation.getId()),
                null
        );
        Log.d(CLASS_NAME, "Favorite remove result:" + result);
    }

    /**
     * An inner class that inherits from {@link android.os.Handler} and uses its
     * {@link #handleMessage(android.os.Message)} hook method to process Messages sent to
     * it from {@link #onHandleIntent(android.content.Intent)} that indicate actions to perform
     * with the Database.
     */
    private final class ServiceHandler extends Handler {

        private final String CLASS_NAME = ServiceHandler.class.getSimpleName();

        /**
         *
         */
        private static final int MSG_MAKE_SAVE_TO_FAVORITES = 1;

        /**
         * Class constructor initializes the Looper.
         *
         * @param looper The Looper that we borrow from HandlerThread.
         */
        public ServiceHandler(final Looper looper) {
            super(looper);
        }

        /**
         * Hook method that retrieves an image from a remote server.
         */
        @Override
        public final void handleMessage(final Message message) {
            // Perform actions over Database and reply (if necessary) to the
            // callee via the Messenger sent with the Intent.

            final Intent intent = (Intent) message.obj;
            final int messageId = message.what;
            Log.d(CLASS_NAME, "Handle message:" + messageId);
            switch (messageId) {

                case MSG_MAKE_SAVE_TO_FAVORITES:
                    updateFavoriteItem(intent);
                    break;
                default:
                    Log.w(CLASS_NAME, "Unknown message:" + messageId);
            }
        }

        /**
         * A factory method that creates a {@link android.os.Message} that contains
         * information on the {@link com.yuriy.openradio.api.RadioStationVO} that is necessary
         * to save in the Favorites.
         */
        private Message makeSaveToFavoritesMessage(final Intent intent) {

            final Message message = Message.obtain();
            // Include Intent in Message to indicate which Radio Station to save.
            message.obj = intent;
            message.what = MSG_MAKE_SAVE_TO_FAVORITES;
            return message;
        }
    }
}
