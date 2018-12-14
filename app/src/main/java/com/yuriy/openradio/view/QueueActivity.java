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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yuriy.openradio.R;
import com.yuriy.openradio.business.MediaResourceManagerListener;
import com.yuriy.openradio.business.MediaResourcesManager;
import com.yuriy.openradio.utils.AppLogger;
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
public final class QueueActivity extends AppCompatActivity {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = QueueActivity.class.getSimpleName();

    /**
     * Play - Pause button
     */
    private ImageButton mPlayPause;

    /**
     * Playback state
     */
    private PlaybackStateCompat mPlaybackState;

    /**
     * adapter to manage list items in the queue
     */
    private QueueAdapter mQueueAdapter;

    /**
     * Key value for the first visible ID in the List for the store Bundle
     */
    private static final String BUNDLE_ARG_LIST_1_VISIBLE_ID = "BUNDLE_ARG_LIST_1_VISIBLE_ID";

    /**
     * Progress Bar to indicate that Radio Station is going to play.
     */
    private ProgressBar mProgressBar;

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

    /**
     * Manager object that acts as interface between Media Resources and current Activity.
     */
    private final MediaResourcesManager mMediaResourcesManager = new MediaResourcesManager(
            this,
            new MediaResourceManagerListenerImpl(this)
    );

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
                    return Integer.compare(sortId1, sortId2);
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

        mMediaResourcesManager.create(savedInstanceState);

        updateBufferedTime(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMediaResourcesManager.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMediaResourcesManager.disconnect();
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
     *
     * @return {@link android.content.Intent}
     */
    public static Intent makeIntent(final Context context) {
        return new Intent(context, QueueActivity.class);
    }

    private void skipToQueueItem(final int position) {
        final MediaSessionCompat.QueueItem item = mQueueAdapter.getItem(position);
        if (item == null) {
            AppLogger.w(CLASS_NAME + " clicked item is null");
            return;
        }

        mMediaResourcesManager.transportControlsSkipToQueueItem(item.getQueueId());
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
            mMediaResourcesManager.transportControlsStop();
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

        statusBuilder.append(" -- At position: ").append(state.getPosition());
        AppLogger.d(CLASS_NAME + " " + statusBuilder.toString());

        if (enablePlay) {
            mPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
        } else {
            mPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_white_24dp));
        }
    }

    /**
     * Skip to previous handler
     */
    private void skipToPrevious() {
        final int position = mQueueAdapter.getActivePosition();
        if (position > 0) {
            skipToQueueItem(position - 1);
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
                        activity.mMediaResourcesManager.transportControlsPlay();
                    } else if (state == PlaybackStateCompat.STATE_PLAYING) {
                        activity.mMediaResourcesManager.transportControlsPause();
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

    /**
     * Listener for the Media Resources related events.
     */
    private static final class MediaResourceManagerListenerImpl implements MediaResourceManagerListener {

        /**
         * Weak reference to the outer activity.
         */
        private final WeakReference<QueueActivity> mReference;

        /**
         * Constructor
         *
         * @param reference Reference to the Activity.
         */
        private MediaResourceManagerListenerImpl(final QueueActivity reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onConnected(final List<MediaSessionCompat.QueueItem> queue) {
            final QueueActivity activity = mReference.get();
            if (activity == null) {
                return;
            }

            // Get actual playback state
            activity.mPlaybackState = activity.mMediaResourcesManager.getPlaybackState();

            if (queue != null) {
                activity.mQueueAdapter.clear();
                activity.mQueueAdapter.notifyDataSetInvalidated();
                activity.mQueueAdapter.addAll(queue);
                activity.mQueueAdapter.notifyDataSetChanged();
            }

            // Change play state
            activity.onPlaybackStateChanged(activity.mPlaybackState);
        }

        @Override
        public void onPlaybackStateChanged(@NonNull final PlaybackStateCompat state) {
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
        public void onMetadataChanged(final MediaMetadataCompat metadata,
                                      final List<MediaSessionCompat.QueueItem> queue) {
            final QueueActivity activity = mReference.get();
            if (activity == null) {
                return;
            }
            if (queue == null) {
                return;
            }

            final long activeQueueItemId = activity.mQueueAdapter.getActiveQueueItemId();

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
}
