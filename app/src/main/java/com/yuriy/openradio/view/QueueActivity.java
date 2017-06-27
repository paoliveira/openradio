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

package com.yuriy.openradio.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yuriy.openradio.R;
import com.yuriy.openradio.service.OpenRadioService;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.CrashlyticsUtils;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.view.list.QueueAdapter;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

/**
 * Created with Android Studio.
 * Author: Chernyshov Yuriy - Mobile Development
 * Date: 19.12.14
 * Time: 15:13
 *
 * {@link QueueActivity} is a view which represents
 * UI for the playing a queue of the radio stations.
 */
public final class QueueActivity extends FragmentActivity {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = QueueActivity.class.getSimpleName();

    /**
     * Play - Pause button
     */
    private ImageButton mPlayPause;

    /**
     * Media Browser
     */
    private MediaBrowserCompat mMediaBrowser;

    /**
     * Transport controls of the Media Controller
     */
    private MediaControllerCompat.TransportControls mTransportControls;

    /**
     * Media Controller
     */
    private MediaControllerCompat mMediaController;

    /**
     * Playback state
     */
    private PlaybackStateCompat mPlaybackState;

    /**
     * adapter to manage list items in the queue
     */
    private QueueAdapter mQueueAdapter;

    /**
     * Position of the first visible element in the List, usually using when restore position
     * when List re-creating.
     */
    private int mListFirstVisiblePosition = 0;

    /**
     * Key value for the first visible ID in the List for the store Bundle
     */
    private static final String BUNDLE_ARG_LIST_1_VISIBLE_ID = "BUNDLE_ARG_LIST_1_VISIBLE_ID";

    /**
     * Key value for the selected media id (from {@link MainActivity}).
     */
    private static final String BUNDLE_ARG_SELECTED_MEDIA_ID = "BUNDLE_ARG_SELECTED_MEDIA_ID";

    /**
     * Progress Bar to indicate that Radio Station is going to play.
     */
    private ProgressBar mProgressBar;

    /**
     * Listener of the media Controllers callbacks.
     */
    private final MediaControllerCompat.Callback mMediaSessionCallback = new MediaSessionCallback(this);

    /**
     * Control Buttons listener.
     */
    private final View.OnClickListener mButtonListener = new ControlsClickListener(this);

    /**
     *
     */
    private ListView mListView;

    /**
     *
     */
    private TextView mBufferedTextView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set content view
        setContentView(R.layout.activity_queue);

        // Assign listeners to the buttons

        final ImageButton skipPrevious = findViewById(R.id.skip_previous);
        skipPrevious.setOnClickListener(mButtonListener);

        final ImageButton skipNext = findViewById(R.id.skip_next);
        skipNext.setOnClickListener(mButtonListener);

        mPlayPause = findViewById(R.id.play_pause);
        mPlayPause.setOnClickListener(mButtonListener);

        mProgressBar = findViewById(R.id.queue_progress_bar_view);
        mBufferedTextView = findViewById(R.id.buffered_text_view);

        // Initialize adapter
        mQueueAdapter = new QueueAdapter(
                getApplicationContext(),
                (o1, o2) -> {
                    final int sortId1 = MediaItemHelper.getSortIdField(o1);
                    final int sortId2 = MediaItemHelper.getSortIdField(o2);
                    if (sortId2 > sortId1) {
                        return -1;
                    }
                    if (sortId2 < sortId1) {
                        return 1;
                    }
                    return 0;
                }
        );

        // Get list view reference from the inflated xml
        mListView = findViewById(R.id.queue_list_view);
        // Set List's choice mode
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        // Set adapter
        mListView.setAdapter(mQueueAdapter);
        // Set focusable
        mListView.setFocusable(true);
        // Set listeners
        mListView.setOnItemClickListener((parent, view, position, id) -> {
            skipToQueueItem(position);
            view.setSelected(true);
        });
        mListView.setOnScrollListener(
                new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(final AbsListView view, final int scrollState) {
                        if (scrollState == SCROLL_STATE_IDLE
                                || scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                            return;
                        }
                        mListFirstVisiblePosition = mListView.getFirstVisiblePosition();
                        AppLogger.d(
                                CLASS_NAME
                                        + " 1st visible pos (after scroll) is "
                                        + mListFirstVisiblePosition
                        );
                    }

                    @Override
                    public void onScroll(final AbsListView view, final int firstVisibleItem,
                                         final int visibleItemCount, final int totalItemCount) {

                    }
                }
        );

        // Initialize Media Browser
        mMediaBrowser = new MediaBrowserCompat(
                getApplicationContext(),
                new ComponentName(getApplicationContext(), OpenRadioService.class),
                new MediaBrowserConnectionCallback(this), null
        );

        updateBufferedTime(0);

        restoreState(savedInstanceState);
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
            mMediaController.unregisterCallback(mMediaSessionCallback);
        }

        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }

        hideProgressBar();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        // Save first visible ID of the List
        if (mListView != null) {
            outState.putInt(BUNDLE_ARG_LIST_1_VISIBLE_ID, mListView.getFirstVisiblePosition());
        }

        super.onSaveInstanceState(outState);
    }

    /**
     * Factory method to create an Intent for the {@link QueueActivity}
     * launching.
     *
     * @param context Context of the callee.
     * @param mediaId Selected Media Id.
     *
     * @return {@link android.content.Intent}
     */
    public static Intent makeIntent(final Context context, final String mediaId) {
        final Intent intent = new Intent(context, QueueActivity.class);
        intent.putExtra(BUNDLE_ARG_SELECTED_MEDIA_ID, mediaId);
        return intent;
    }

    private void skipToQueueItem(final int position) {
        final MediaSessionCompat.QueueItem item = mQueueAdapter.getItem(position);
        if (item == null) {
            AppLogger.w(CLASS_NAME + " clicked item is null");
            return;
        }

        mListFirstVisiblePosition = mListView.getFirstVisiblePosition();
        AppLogger.d(
                CLASS_NAME
                        + " 1st visible pos (after click) is "
                        + mListFirstVisiblePosition
        );

        mTransportControls.skipToQueueItem(item.getQueueId());
    }

    /**
     * Extract the value of the Selected Media Id (selected in the {@link MainActivity}).
     *
     * @param intent Intent associated with the start of the {@link QueueActivity}.
     * @return Value of the Selected Media Id.
     */
    private static String getSelectedMediaId(final Intent intent) {
        if (intent == null) {
            return "";
        }
        if (!intent.hasExtra(BUNDLE_ARG_SELECTED_MEDIA_ID)) {
            return "";
        }
        return intent.getStringExtra(BUNDLE_ARG_SELECTED_MEDIA_ID);
    }

    /**
     * Restore state of the UI as it was before destroying.
     *
     * @param savedInstanceState {@link android.os.Bundle} with stored values.
     */
    private void restoreState(final Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            // Nothing to restore
            return;
        }
        // Restore List's position
        mListFirstVisiblePosition = savedInstanceState.getInt(BUNDLE_ARG_LIST_1_VISIBLE_ID);
        AppLogger.d(CLASS_NAME + " 1st visible pos (after restore) is " + mListFirstVisiblePosition);
    }

    /**
     * Show progress when state of the station is set to "buffering"
     */
    private void showProgressBar() {
        if (mProgressBar == null) {
            // Skip further action
            return;
        }
        mProgressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Hide progress whether Station has been loaded or error has occured
     */
    private void hideProgressBar() {
        if (mProgressBar == null) {
            return;
        }
        mProgressBar.setVisibility(View.GONE);
    }

    /**
     * Process Playback state changed
     *
     * @param state Actual {@link android.media.session.PlaybackState}
     */
    private void onPlaybackStateChanged(final PlaybackStateCompat state) {
        AppLogger.d(CLASS_NAME + " On Playback State Changed " + state);
        if (state == null) {
            hideProgressBar();
            stop();
            return;
        }

        mQueueAdapter.setActiveQueueItemId(state.getActiveQueueItemId());
        mQueueAdapter.notifyDataSetChanged();

        boolean enablePlay = false;
        final StringBuilder statusBuilder = new StringBuilder("Status ");

        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                hideProgressBar();
                statusBuilder.append("playing");
                enablePlay = false;
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                statusBuilder.append("paused");
                enablePlay = true;
                break;
            case PlaybackStateCompat.STATE_STOPPED:
                statusBuilder.append("ended");
                enablePlay = true;
                break;
            case PlaybackStateCompat.STATE_ERROR:
                hideProgressBar();
                statusBuilder.append("error: ").append(state.getErrorMessage());
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
                showProgressBar();
                statusBuilder.append("buffering");
                break;
            case PlaybackStateCompat.STATE_NONE:
                hideProgressBar();
                statusBuilder.append("none");
                enablePlay = false;
                break;
            case PlaybackStateCompat.STATE_CONNECTING:
                statusBuilder.append("connecting");
                break;
            default:
                hideProgressBar();
                statusBuilder.append(mPlaybackState);
        }

        mListView.setSelection(mListFirstVisiblePosition);

        statusBuilder.append(" -- At position: ").append(state.getPosition());
        AppLogger.d(CLASS_NAME + " " + statusBuilder.toString());

        if (enablePlay) {
            mPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
        } else {
            mPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_white_24dp));
        }

        AppLogger.d(CLASS_NAME + " Queue From MediaController *** Title " +
                mMediaController.getQueueTitle() + "\n: Queue: " + mMediaController.getQueue() +
                "\n Metadata " + mMediaController.getMetadata());
    }

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
        final int position = mQueueAdapter.getActivePosition();
        if (position > 0) {
            skipToQueueItem(position - 1);
        }

        mListFirstVisiblePosition--;
        if (mListFirstVisiblePosition < 0) {
            mListFirstVisiblePosition = 0;
        }
    }

    /**
     * Skip to next handler
     */
    private void skipToNext() {
        final int count = mQueueAdapter.getCount();
        final int position = mQueueAdapter.getActivePosition();
        if (position < count - 1) {
            skipToQueueItem(position + 1);
        }

        mListFirstVisiblePosition++;
        if (mListFirstVisiblePosition > count) {
            mListFirstVisiblePosition = count;
        }
    }

    /**
     * Stop media handler
     */
    private void stop() {
        if (mTransportControls == null) {
            return;
        }
        mTransportControls.stop();
    }

    /**
     * Updates buffered value of the currently playing radio station.
     *
     * @param value Buffered time in seconds.
     */
    private void updateBufferedTime(double value) {
        if (mBufferedTextView == null) {
            return;
        }
        if (value < 0) {
            value = 0;
        }

        final double finalValue = value;
        runOnUiThread(
                () -> {
                    mBufferedTextView.setVisibility(finalValue > 0 ? View.VISIBLE : View.GONE);
                    mBufferedTextView.setText(String.format(Locale.getDefault(), "Buffered %.2f sec", finalValue));
                }
        );
    }

    /**
     * Receive callbacks from the MediaController.
     * Here we update our state such as which queue is being shown,
     * the current title and description and the PlaybackState.
     */
    private static final class MediaSessionCallback extends MediaControllerCompat.Callback {

        /**
         * Tag string to use in logging message.
         */
        private static final String CLASS_NAME = MediaSessionCallback.class.getSimpleName();

        /**
         * Weak reference to the outer activity.
         */
        private final WeakReference<QueueActivity> mReference;

        /**
         * Constructor
         *
         * @param reference Reference to the Activity.
         */
        private MediaSessionCallback(final QueueActivity reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onSessionDestroyed() {
            AppLogger.d(CLASS_NAME + " Session destroyed. Need to fetch a new Media Session");
        }

        @Override
        public void onPlaybackStateChanged(@NonNull final PlaybackStateCompat state) {
            AppLogger.d(CLASS_NAME + " Received playback state change to state " + state.getState());

            final QueueActivity activity = mReference.get();
            if (activity == null) {
                return;
            }
            activity.mPlaybackState = state;
            activity.onPlaybackStateChanged(state);

            final double bufferedDuration = (state.getBufferedPosition() - state.getPosition()) / 1000.0;
            activity.updateBufferedTime(bufferedDuration);
        }

        @Override
        public void onQueueChanged(final List<MediaSessionCompat.QueueItem> queue) {
            AppLogger.d(CLASS_NAME + " On Queue Changed: " + queue);
            final QueueActivity activity = mReference.get();
            if (activity == null) {
                return;
            }
            if (queue == null) {
                return;
            }
            activity.mQueueAdapter.clear();
            activity.mQueueAdapter.notifyDataSetInvalidated();
            activity.mQueueAdapter.addAll(queue);
            activity.mQueueAdapter.notifyDataSetChanged();
        }

        @Override
        public void onMetadataChanged(final MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);

            final QueueActivity activity = mReference.get();
            if (activity == null) {
                return;
            }

            // Update queue
            final List<MediaSessionCompat.QueueItem> queue = activity.mMediaController.getQueue();
            if (queue == null) {
                return;
            }

            final long activeQueueItemId = activity.mQueueAdapter.getActiveQueueItemId();
            AppLogger.d(CLASS_NAME + " Metadata changed:" + metadata + ", active id:" + activeQueueItemId);

            activity.mQueueAdapter.notifyDataSetInvalidated();
            activity.mQueueAdapter.clear();
            final int queueSize = queue.size();
            MediaSessionCompat.QueueItem item;
            for (int i = 0; i < queueSize; i++) {
                item = queue.get(i);
                if (item == null) {
                    continue;
                }
                // Get currently selected Radio Station
                if (item.getQueueId() == activeQueueItemId) {
                    final MediaSessionCompat.QueueItem newItem = new MediaSessionCompat.QueueItem(
                            metadata.getDescription(), item.getQueueId()
                    );

                    final MediaDescriptionCompat description = newItem.getDescription();
                    Bundle extras = description.getExtras();
                    if (extras == null) {
                        extras = item.getDescription().getExtras();
                        MediaItemHelper.updateExtras(description, extras);
                    }

                    queue.remove(item);
                    queue.add(i, newItem);
                    break;
                }
            }
            activity.mQueueAdapter.addAll(queue);
            activity.mQueueAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Callback object for the Media Browser connection events.
     */
    private static final class MediaBrowserConnectionCallback extends MediaBrowserCompat.ConnectionCallback {

        /**
         * Weak reference to the outer activity.
         */
        private final WeakReference<QueueActivity> mReference;

        /**
         * Constructor.
         *
         * @param reference Reference to the Activity.
         */
        private MediaBrowserConnectionCallback(final QueueActivity reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onConnected() {
            AppLogger.d(CLASS_NAME + " On Connected");

            final QueueActivity activity = mReference.get();
            if (activity == null) {
                return;
            }

            AppLogger.d(CLASS_NAME + " Session token " + activity.mMediaBrowser.getSessionToken());

            // If session token is null - throw exception
            //if (mMediaBrowser.getSessionToken() == null) {
            //    throw new IllegalArgumentException("No Session token");
            //}

            // Initialize Media Controller
            try {
                activity.mMediaController = new MediaControllerCompat(
                        activity.getApplicationContext(),
                        activity.mMediaBrowser.getSessionToken()
                );
            } catch (final RemoteException e) {
                AppLogger.e(CLASS_NAME + " Can not init Media Controller:\n" + Log.getStackTraceString(e));
                CrashlyticsUtils.logException(e);
                return;
            }

            // Initialize Transport Controls
            activity.mTransportControls = activity.mMediaController.getTransportControls();
            // Register callbacks
            activity.mMediaController.registerCallback(activity.mMediaSessionCallback);

            // Set actual media controller
            MediaControllerCompat.setMediaController(activity, activity.mMediaController);

            // Get actual playback state
            activity.mPlaybackState = activity.mMediaController.getPlaybackState();

            // Update queue
            final List<MediaSessionCompat.QueueItem> queue = activity.mMediaController.getQueue();

            if (queue != null) {

                // If the is no first visible position restored, try to get selected id from the
                // bundles of the Intent.

                if (activity.mListFirstVisiblePosition == 0) {
                    final int queueSize = queue.size();
                    final String selectedMediaId = getSelectedMediaId(activity.getIntent());
                    MediaSessionCompat.QueueItem item;
                    String mediaId;
                    for (int i = 0; i < queueSize; i++) {
                        item = queue.get(i);
                        if (item == null) {
                            continue;
                        }
                        mediaId = item.getDescription().getMediaId();
                        if (mediaId == null) {
                            continue;
                        }
                        if (mediaId.equals(selectedMediaId)) {
                            activity.mListFirstVisiblePosition = i;
                            AppLogger.d(CLASS_NAME + " 1st visible pos (after connected) is " + i);
                            break;
                        }
                    }
                }

                activity.mQueueAdapter.clear();
                activity.mQueueAdapter.notifyDataSetInvalidated();
                activity.mQueueAdapter.addAll(queue);
                activity.mQueueAdapter.notifyDataSetChanged();
            }

            // Change play state
            activity.onPlaybackStateChanged(activity.mPlaybackState);
        }

        @Override
        public void onConnectionFailed() {
            AppLogger.w(CLASS_NAME + " On Connection Failed");
        }

        @Override
        public void onConnectionSuspended() {
            AppLogger.w(CLASS_NAME + " On Connection Suspended");
            final QueueActivity activity = mReference.get();
            if (activity == null) {
                return;
            }
            activity.mMediaController.unregisterCallback(activity.mMediaSessionCallback);
            activity.mTransportControls = null;
            activity.mMediaController = null;
            MediaControllerCompat.setMediaController(activity, null);
        }
    }

    /**
     * Listener for the media controls (previous, play|pause, next).
     */
    private static final class ControlsClickListener implements View.OnClickListener {

        /**
         * Weak reference to the outer activity.
         */
        private final WeakReference<QueueActivity> mReference;

        /**
         * Constructor
         *
         * @param reference Reference to the Activity.
         */
        private ControlsClickListener(final QueueActivity reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onClick(final View view) {
            final QueueActivity activity = mReference.get();
            if (activity == null) {
                return;
            }

            final int state
                    = activity.mPlaybackState == null
                    ? PlaybackStateCompat.STATE_NONE : activity.mPlaybackState.getState();

            switch (view.getId()) {
                case R.id.play_pause:
                    AppLogger.d(CLASS_NAME + " Play|Pause button pressed, state " + state);
                    if (state == PlaybackStateCompat.STATE_PAUSED
                            || state == PlaybackStateCompat.STATE_STOPPED
                            || state == PlaybackStateCompat.STATE_NONE) {
                        activity.playMedia();
                    } else if (state == PlaybackStateCompat.STATE_PLAYING) {
                        activity.pauseMedia();
                    }
                    break;
                case R.id.skip_previous:
                    AppLogger.d(CLASS_NAME + " Previous button pressed, state " + state);
                    activity.skipToPrevious();
                    break;
                case R.id.skip_next:
                    AppLogger.d(CLASS_NAME + " Next button pressed, state " + state);
                    activity.skipToNext();
                    break;
            }
        }
    }
}
