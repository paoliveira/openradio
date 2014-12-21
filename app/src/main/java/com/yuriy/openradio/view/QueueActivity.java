/*
 * Copyright 2014 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.yuriy.openradio.R;
import com.yuriy.openradio.service.OpenRadioService;
import com.yuriy.openradio.view.list.QueueAdapter;

import java.util.List;

/**
 * Created with Android Studio.
 * Author: Chernyshov Yuriy - Mobile Development
 * Date: 19.12.14
 * Time: 15:13
 */

/**
 * {@link com.yuriy.openradio.view.QueueActivity} is the activity which represents
 * UI for the playing a queue of the radio stations
 */
public class QueueActivity extends FragmentActivity {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = QueueActivity.class.getSimpleName();

    /**
     * Skip Next button
     */
    private ImageButton mSkipNext;

    /**
     * Skip Previous button
     */
    private ImageButton mSkipPrevious;

    /**
     * Play - Pause button
     */
    private ImageButton mPlayPause;

    /**
     * Media Browser
     */
    private MediaBrowser mMediaBrowser;

    /**
     * Transport controls of the Media Controller
     */
    private MediaController.TransportControls mTransportControls;

    /**
     * Media Controller
     */
    private MediaController mMediaController;

    /**
     * Playback state
     */
    private PlaybackState mPlaybackState;

    /**
     * adapter to manage list items in the queue
     */
    private QueueAdapter mQueueAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set content view
        setContentView(R.layout.activity_queue);

        // Assign listeners to the buttons

        mSkipPrevious = (ImageButton) findViewById(R.id.skip_previous);
        mSkipPrevious.setEnabled(false);
        mSkipPrevious.setOnClickListener(buttonListener);

        mSkipNext = (ImageButton) findViewById(R.id.skip_next);
        mSkipNext.setEnabled(false);
        mSkipNext.setOnClickListener(buttonListener);

        mPlayPause = (ImageButton) findViewById(R.id.play_pause);
        mPlayPause.setEnabled(true);
        mPlayPause.setOnClickListener(buttonListener);

        // Initialize adapter
        mQueueAdapter = new QueueAdapter(this);

        // Get list view reference from the inflated xml
        final ListView mListView = (ListView) findViewById(R.id.queue_list_view);
        // Set adapter
        mListView.setAdapter(mQueueAdapter);
        // Set focusable
        mListView.setFocusable(true);
        // Set listener
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(final AdapterView<?> parent, final View view,
                                    final int position, final long id) {

                final MediaSession.QueueItem item = mQueueAdapter.getItem(position);
                mTransportControls.skipToQueueItem(item.getQueueId());
            }
        });

        // Initialize Media Browser
        mMediaBrowser = new MediaBrowser(
                this, new ComponentName(this, OpenRadioService.class), connectionCallback, null
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mMediaBrowser != null) {
            mMediaBrowser.connect();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mMediaController != null) {
            mMediaController.unregisterCallback(sessionCallback);
        }

        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }
    }

    /**
     * Factory method to create an Intent for the {@link com.yuriy.openradio.view.QueueActivity}
     * launching.
     *
     * @param context Context of the callee.
     *
     * @return {@link android.content.Intent}
     */
    public static Intent makeIntent(final Context context) {
        return new Intent(context, QueueActivity.class);
    }

    /**
     * Callback object for the Media Browser connection events
     */
    private final MediaBrowser.ConnectionCallback connectionCallback
            = new MediaBrowser.ConnectionCallback() {

        @Override
        public void onConnected() {
            Log.d(CLASS_NAME, "On Connected: session token " + mMediaBrowser.getSessionToken());

            // If session token is null - throw exception
            if (mMediaBrowser.getSessionToken() == null) {
                throw new IllegalArgumentException("No Session token");
            }

            // Initialize Media Controller
            mMediaController = new MediaController(
                    QueueActivity.this,
                    mMediaBrowser.getSessionToken()
            );

            // Initialize Transport Controlls
            mTransportControls = mMediaController.getTransportControls();
            // Register callbacks
            mMediaController.registerCallback(sessionCallback);

            // Set actual media controller
            setMediaController(mMediaController);

            // Get actual playback state
            mPlaybackState = mMediaController.getPlaybackState();

            // Update queue
            final List<MediaSession.QueueItem> queue = mMediaController.getQueue();
            if (queue != null) {
                mQueueAdapter.clear();
                mQueueAdapter.notifyDataSetInvalidated();
                mQueueAdapter.addAll(queue);
                mQueueAdapter.notifyDataSetChanged();
            }

            // Change play state
            onPlaybackStateChanged(mPlaybackState);
        }

        @Override
        public void onConnectionFailed() {
            Log.w(CLASS_NAME, "On Connection Failed");
        }

        @Override
        public void onConnectionSuspended() {
            Log.w(CLASS_NAME, "On Connection Suspended");
            mMediaController.unregisterCallback(sessionCallback);
            mTransportControls = null;
            mMediaController = null;
            setMediaController(null);
        }
    };

    /**
     * Receive callbacks from the MediaController.
     * Here we update our state such as which queue is being shown,
     * the current title and description and the PlaybackState.
     */
    private final MediaController.Callback sessionCallback = new MediaController.Callback() {

        @Override
        public void onSessionDestroyed() {
            Log.d(CLASS_NAME, "Session destroyed. Need to fetch a new Media Session");
        }

        @Override
        public void onPlaybackStateChanged(final PlaybackState state) {
            if (state == null) {
                return;
            }
            Log.d(CLASS_NAME, "Received playback state change to state " + state.getState());
            mPlaybackState = state;
            QueueActivity.this.onPlaybackStateChanged(state);
        }

        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            Log.d(CLASS_NAME, "On Queue Changed: " + queue);
            if (queue == null) {
                return;
            }
            mQueueAdapter.clear();
            mQueueAdapter.notifyDataSetInvalidated();
            mQueueAdapter.addAll(queue);
            mQueueAdapter.notifyDataSetChanged();
        }
    };

    /**
     * Process Playback state changed
     *
     * @param state Actual {@link android.media.session.PlaybackState}
     */
    private void onPlaybackStateChanged(final PlaybackState state) {
        Log.d(CLASS_NAME, "On Playback State Changed " + state);
        if (state == null) {
            return;
        }

        mQueueAdapter.setActiveQueueItemId(state.getActiveQueueItemId());
        mQueueAdapter.notifyDataSetChanged();

        boolean enablePlay = false;
        final StringBuilder statusBuilder = new StringBuilder();

        switch (state.getState()) {
            case PlaybackState.STATE_PLAYING:
                statusBuilder.append("playing");
                enablePlay = false;
                break;
            case PlaybackState.STATE_PAUSED:
                statusBuilder.append("paused");
                enablePlay = true;
                break;
            case PlaybackState.STATE_STOPPED:
                statusBuilder.append("ended");
                enablePlay = true;
                break;
            case PlaybackState.STATE_ERROR:
                statusBuilder.append("error: ").append(state.getErrorMessage());
                break;
            case PlaybackState.STATE_BUFFERING:
                statusBuilder.append("buffering");
                break;
            case PlaybackState.STATE_NONE:
                statusBuilder.append("none");
                enablePlay = false;
                break;
            case PlaybackState.STATE_CONNECTING:
                statusBuilder.append("connecting");
                break;
            default:
                statusBuilder.append(mPlaybackState);
        }
        statusBuilder.append(" -- At position: ").append(state.getPosition());
        Log.d(CLASS_NAME, statusBuilder.toString());

        if (enablePlay) {
            mPlayPause.setImageDrawable(getDrawable(R.drawable.ic_play_arrow_white_24dp));
        } else {
            mPlayPause.setImageDrawable(getDrawable(R.drawable.ic_pause_white_24dp));
        }

        mSkipPrevious.setEnabled((state.getActions() & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0);
        mSkipNext.setEnabled((state.getActions() & PlaybackState.ACTION_SKIP_TO_NEXT) != 0);

        Log.d(CLASS_NAME, "Queue From MediaController *** Title " +
                mMediaController.getQueueTitle() + "\n: Queue: " + mMediaController.getQueue() +
                "\n Metadata " + mMediaController.getMetadata());
    }

    /**
     * Control Buttons listeners
     */
    private final View.OnClickListener buttonListener = new View.OnClickListener() {

        @Override
        public void onClick(final View view) {

            final int state
                    = mPlaybackState == null ? PlaybackState.STATE_NONE : mPlaybackState.getState();

            switch (view.getId()) {
                case R.id.play_pause:
                    Log.d(CLASS_NAME, "Play button pressed, in state " + state);
                    if (state == PlaybackState.STATE_PAUSED
                            || state == PlaybackState.STATE_STOPPED
                            || state == PlaybackState.STATE_NONE) {
                        playMedia();
                    } else if (state == PlaybackState.STATE_PLAYING) {
                        pauseMedia();
                    }
                    break;
                case R.id.skip_previous:
                    Log.d(CLASS_NAME, "Start button pressed, in state " + state);
                    skipToPrevious();
                    break;
                case R.id.skip_next:
                    skipToNext();
                    break;
            }
        }
    };

    /**
     * Play media handler
     */
    private void playMedia() {
        if (mTransportControls == null) {
            return;
        }
        mTransportControls.play();
    }

    /**
     * Pause media handler
     */
    private void pauseMedia() {
        if (mTransportControls == null) {
            return;
        }
        mTransportControls.pause();
    }

    /**
     * Skip to previous handler
     */
    private void skipToPrevious() {
        if (mTransportControls != null) {
            mTransportControls.skipToPrevious();
        }
    }

    /**
     * Skip to next handler
     */
    private void skipToNext() {
        if (mTransportControls == null) {
            return;
        }
        mTransportControls.skipToNext();
    }
}
