package com.yuriy.openradio.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
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
            handlingStartCommand(getUrl(intent));
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
    public static Intent getStartRetrievalIntent(final Context context, final String url) {
        final Intent intent = new Intent(context, MetadataRetrievalService.class);
        intent.putExtra(COMMAND, START_COMMAND);
        intent.putExtra(KEY_URL, url);
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
     * @param url
     */
    private void handlingStartCommand(final String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        // Create a Message that will be sent to ServiceHandler to
        // retrieve a data based on the URL.
        final Message message = mServiceHandler.makeStartMessage(url);

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
            if (handler != null) {
                final Metadata metadata = handler.mRetriever.getMetadata();

                Log.d(CLASS_NAME, "Metadata:" + metadata);
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

                handler.mHandler.postDelayed(this, ServiceHandler.DELAY);
            }
        }
    }

    /**
     *
     */
    public final class ServiceHandler extends Handler {

        private final Handler mHandler;

        private volatile Looper mLooper;

        private final Runnable mRetrievalRunnable = new Retrieval(this);

        private static final int DELAY = 2000;

        private final FFmpegMediaMetadataRetriever mRetriever = new FFmpegMediaMetadataRetriever();

        private static final int MSG_MAKE_START_REQUEST = 1;

        private static final int MSG_MAKE_STOP_REQUEST = 2;

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
            switch (message.what) {
                case MSG_MAKE_START_REQUEST:
                    try {
                        mRetriever.setDataSource(String.valueOf(message.obj));
                        mHandler.removeCallbacks(mRetrievalRunnable);
                        mHandler.post(mRetrievalRunnable);
                    } catch (final Throwable throwable) {
                        Log.e(CLASS_NAME, "Can not set data sources:" + throwable.getMessage());
                    }
                    break;
                case MSG_MAKE_STOP_REQUEST:
                    mRetriever.release();
                    mHandler.removeCallbacks(mRetrievalRunnable);
                    break;
                default:
                    Log.w(CLASS_NAME, "Unknown command:" + message.what);
                    break;
            }
        }

        /**
         *
         * @param url
         * @return
         */
        private Message makeStartMessage(final String url) {

            final Message message = Message.obtain();
            // Include Intent in Message to ...
            message.obj = url;
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
    }
}
