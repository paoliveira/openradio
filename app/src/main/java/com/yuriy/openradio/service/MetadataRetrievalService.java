package com.yuriy.openradio.service;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;

import wseemann.media.FFmpegMediaMetadataRetriever;
import wseemann.media.Metadata;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 11/18/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class MetadataRetrievalService extends Service {

    private static final String CLASS_NAME = MetadataRetrievalService.class.getSimpleName();

    private static final String COMMAND = "COMMAND";

    private static final String START_COMMAND = "START_COMMAND";

    private static final String STOP_COMMAND = "STOP_COMMAND";

    private static final String KEY_URL = "URL";

    private static final String KEY_MESSENGER = "MESSENGER";

    /**
     * Processes Messages sent to it from onStartCommand().
     */
    private volatile ServiceHandler mServiceHandler;

    /**
     * Looper associated with the ServiceHandler.
     */
    private volatile Looper mServiceLooper;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create and start a background HandlerThread since by
        // default a Service runs in the UI Thread, which we don't
        // want to block.
        final HandlerThread thread = new HandlerThread(CLASS_NAME + "-Thread");
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler.
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(CLASS_NAME, "Destroy");

        mServiceHandler.removeCallbacksAndMessages(null);
        mServiceLooper.quitSafely();
    }

    @Nullable
    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {

        Log.d(CLASS_NAME, "OnStartCommand Intent:" + intent);
        final String command = getCommandName(intent);
        Log.d(CLASS_NAME, "Command:" + command);
        if (TextUtils.equals(START_COMMAND, command)) {
            handlingStartCommand(intent);
        }
        if (TextUtils.equals(STOP_COMMAND, command)) {
            handlingStopCommand();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     *
     * @param context
     * @param url
     * @return
     */
    public static Intent getStartRetrievalIntent(final Context context, final Handler handler,
                                                 final String url) {
        final Intent intent = new Intent(context, MetadataRetrievalService.class);
        intent.putExtra(COMMAND, START_COMMAND);
        intent.putExtra(KEY_URL, url);
        if (handler != null) {
            intent.putExtra(KEY_MESSENGER, new Messenger(handler));
        }
        return intent;
    }

    /**
     *
     * @param context
     * @return
     */
    public static Intent getStopRetrievalIntent(final Context context) {
        final Intent intent = new Intent(context, MetadataRetrievalService.class);
        intent.putExtra(COMMAND, STOP_COMMAND);
        return intent;
    }

    /**
     * Determines whether there is Metadata in the response Message.
     *
     * @param message Response Message.
     *
     * @return {@code true} if there is Metadata, {@code false} otherwise.
     */
    public static boolean isMetadataResponse(final Message message) {
        return message != null && message.what == ServiceHandler.MSG_MAKE_METADATA_RESPONSE;
    }

    /**
     * Extracts Stream Title from the response message.
     *
     * @param message Response Message.
     *
     * @return Stream Title.
     */
    public static String getStreamTitle(final Message message) {
        if (message == null) {
            return null;
        }
        final Bundle bundle = message.getData();
        if (bundle == null) {
            return null;
        }
        return bundle.getString(ServiceHandler.BUNDLE_KEY_STREAM_TITLE);
    }

    /**
     *
     * @param intent
     * @return
     */
    private static String getCommandName(final Intent intent) {
        if (intent == null) {
            return null;
        }
        return intent.getStringExtra(COMMAND);
    }

    /**
     *
     * @param intent
     * @return
     */
    private static String getUrl(final Intent intent) {
        if (intent == null) {
            return null;
        }
        return intent.getStringExtra(KEY_URL);
    }

    /**
     *
     * @param intent
     */
    private void handlingStartCommand(final Intent intent) {
        // Create a Message that will be sent to ServiceHandler to
        // retrieve a data based on the URL.
        final Message message = mServiceHandler.makeStartMessage(intent);

        // Send the Message to ServiceHandler to ....
        mServiceHandler.sendMessage(message);
    }

    /**
     *
     */
    private void handlingStopCommand() {
        // Create a Message that will be sent to ServiceHandler to stop retrieve a data.
        final Message message = mServiceHandler.makeStopMessage();

        // Send the Message to ServiceHandler to ....
        mServiceHandler.sendMessage(message);

        // service is no longer necessary. Will be started again if needed.
        stopSelf();
    }

    /**
     *
     */
    private static final class Retrieval implements Runnable {

        /**
         *
         */
        private final WeakReference<ServiceHandler> mService;

        /**
         *
         * @param service
         */
        public Retrieval(final ServiceHandler service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void run() {
            final ServiceHandler handler = mService.get();
            if (handler == null) {
                return;
            }
            Metadata metadata = null;
            final FFmpegMediaMetadataRetriever retriever = new FFmpegMediaMetadataRetriever();
            try {
                retriever.setDataSource(handler.mUrl);
                metadata = retriever.getMetadata();
            } catch (final Exception e) {
                Log.e(CLASS_NAME, "Can not get Metadata:" + e.getMessage());
            } finally {
                retriever.release();
            }

            handler.obtainAndSendMetadata(metadata);

            handler.mHandler.postDelayed(this, ServiceHandler.PERIOD);
        }
    }

    /**
     *
     */
    public final class ServiceHandler extends Handler {

        private final Handler mHandler;

        private volatile Looper mLooper;

        private final Runnable mRetrievalRunnable = new Retrieval(this);

        private Messenger mMessenger;

        /**
         * Period between metadata retrievals, in milliseconds.
         */
        private static final int PERIOD = 10000;

        private static final int MSG_MAKE_START_REQUEST = 1;

        private static final int MSG_MAKE_STOP_REQUEST = 2;

        private static final int MSG_MAKE_METADATA_RESPONSE = 3;

        private static final String BUNDLE_KEY_STREAM_TITLE = "STREAM_TITLE";

        private static final int MAX_COUNT = 3;

        private String mCurrentStreamTitle;

        private String mUrl;

        private int mCounter = 0;

        /**
         * Class constructor initializes the Looper.
         *
         * @param looper The Looper that we borrow from HandlerThread.
         */
        public ServiceHandler(final Looper looper) {
            super(looper);

            final HandlerThread thread = new HandlerThread(
                    ServiceHandler.class.getSimpleName() + "-Thread"
            );
            thread.start();

            // Get the HandlerThread's Looper and use it for our Handler.
            mLooper = thread.getLooper();
            mHandler = new Handler(mLooper);
        }

        /**
         * Hook method
         */
        @Override
        public void handleMessage(final Message message) {
            Log.d(CLASS_NAME, "Message:" + message);
            if (message == null) {
                return;
            }
            final Intent intent = (Intent) message.obj;
            // Extract the Messenger.
            if (mMessenger == null && intent != null) {
                mMessenger = (Messenger) intent.getExtras().get(KEY_MESSENGER);
            }
            switch (message.what) {
                case MSG_MAKE_START_REQUEST:
                    mCounter = 0;
                    processStartCommand(intent);
                    break;
                case MSG_MAKE_STOP_REQUEST:
                    //mRetriever.release();
                    mHandler.removeCallbacks(mRetrievalRunnable);
                    mHandler.removeCallbacksAndMessages(null);
                    mLooper.quitSafely();
                    break;
                default:
                    Log.w(CLASS_NAME, "Unknown command:" + message.what);
                    break;
            }
        }

        private void processStartCommand(final Intent intent) {
            if (intent == null) {
                Log.w(CLASS_NAME, "Can not start, Intent is null");
            }
            final String url = getUrl(intent);
            if (TextUtils.isEmpty(url)) {
                Log.w(CLASS_NAME, "Can not start, URL is null");
                return;
            }
            mUrl = url;
            mHandler.removeCallbacks(mRetrievalRunnable);
            mHandler.post(mRetrievalRunnable);
        }

        /**
         *
         * @param intent
         * @return
         */
        private Message makeStartMessage(final Intent intent) {
            final Message message = Message.obtain();
            // Include Intent in Message to ...
            message.obj = intent;
            message.what = MSG_MAKE_START_REQUEST;
            return message;
        }

        /**
         *
         * @return
         */
        private Message makeStopMessage() {
            final Message message = Message.obtain();
            message.what = MSG_MAKE_STOP_REQUEST;
            return message;
        }

        /**
         * Obtain a new Message instance from the global pool and add Metadata field to it.
         *
         */
        private void obtainAndSendMetadata(final Metadata metadata) {

            Log.d(CLASS_NAME, "Metadata:" + metadata);

            // TODO : refactor this condition to the separate method
            final String streamTitle = getStreamTitle(metadata);
            Log.d(CLASS_NAME, "Stream Title:" + streamTitle);
            if (TextUtils.equals(mCurrentStreamTitle, streamTitle)) {
                Log.d(CLASS_NAME, "Metadata didn't changed, counter:" + mCounter);
                if (mCounter++ > MAX_COUNT) {
                    return;
                }
            }
            mCurrentStreamTitle = streamTitle;

            if (mMessenger == null) {
                return;
            }

            //0  = {java.util.HashMap$HashMapEntry@4499} "StreamTitle" -> "ALKILADOS - ME IGNORAS"
            //1  = {java.util.HashMap$HashMapEntry@4500} "audio_codec" -> "mp3"
            //2  = {java.util.HashMap$HashMapEntry@4501} "chapter_count" -> "0"
            //3  = {java.util.HashMap$HashMapEntry@4502} "duration" -> "0"
            //4  = {java.util.HashMap$HashMapEntry@4503} "icy-url" -> "http://104radioactiva.com/"
            //5  = {java.util.HashMap$HashMapEntry@4504} "icy-genre" -> "Alternative,Electronic,Latin,Pop,R&B/Urban,Reggae,Rock"
            //6  = {java.util.HashMap$HashMapEntry@4505} "icy-name" -> "104RadioActiva"
            //7  = {java.util.HashMap$HashMapEntry@4506} "icy_metadata" -> "StreamTitle='ALKILADOS - ME IGNORAS';StreamUrl='';"
            //8  = {java.util.HashMap$HashMapEntry@4507} "icy-notice2" -> "SHOUTcast Distributed Network Audio Server/Linux v1.9.7<BR>"
            //9  = {java.util.HashMap$HashMapEntry@4508} "StreamUrl" ->
            //10 = {java.util.HashMap$HashMapEntry@4509} "filesize" -> "-38"
            //11 = {java.util.HashMap$HashMapEntry@4510} "icy-notice1" -> "<BR>This stream requires <a href="http://www.winamp.com/">Winamp</a><BR>"
            //12 = {java.util.HashMap$HashMapEntry@4511} "icy-br" -> "128"
            //13 = {java.util.HashMap$HashMapEntry@4512} "icy-pub" -> "1"

            // Call factory method to create Message.
            final Message message = Message.obtain();

            // Return the result to indicate whether the download
            // succeeded or failed.
            message.arg1 = Activity.RESULT_OK;

            final Bundle data = new Bundle();

            // Put Metadata in the bundle.
            data.putString(BUNDLE_KEY_STREAM_TITLE, mCurrentStreamTitle);
            message.setData(data);

            try {
                // Send Radio to back to the DownloadActivity.
                message.what = MSG_MAKE_METADATA_RESPONSE;
                mMessenger.send(message);
            } catch (final RemoteException e) {
                Log.e(CLASS_NAME, "Exception while sending response:" + e.getMessage());
            }
        }

        /**
         * Extract Stream Title from the metadata.
         *
         * @param metadata Metadata obtained from the stream.
         *
         * @return The Stream Title or an empty string in case of it is impossible to get it.
         */
        private String getStreamTitle(final Metadata metadata) {

            // key:StreamTitle, val:FONSECA - ENTRE MI VIDA Y LA TUYA
            // key:audio_codec, val:mp3
            // key:chapter_count, val:0
            // key:duration, val:0
            // key:icy-url, val:http://104radioactiva.com/
            // key:icy-genre, val:Alternative,Electronic,Latin,Pop,R&B/Urban,Reggae,Rock
            // key:icy-name, val:104RadioActiva
            // key:icy_metadata, val:StreamTitle='FONSECA - ENTRE MI VIDA Y LA TUYA';StreamUrl='';
            // key:icy-notice2, val:SHOUTcast Distributed Network Audio Server/Linux v1.9.7<BR>
            // key:StreamUrl, val:
            // key:filesize, val:-38
            // key:icy-notice1, val:<BR>This stream requires <a href="http://www.winamp.com/">Winamp</a><BR>
            // key:icy-br, val:128
            // key:icy-pub, val:1

            //Log.d(CLASS_NAME, "Metadata:");
            //for (final String key : metadata.getAll().keySet()) {
            //    Log.d(CLASS_NAME, "  key:" + key + ", val:" + metadata.getAll().get(key));
            //}
            if (metadata == null) {
                return "";
            }
            try {
                return metadata.getString("StreamTitle");
            } catch (final Throwable throwable) {
                Log.e(CLASS_NAME, "Can not extract title:" + throwable.getMessage());
                return "";
            }
        }
    }
}
