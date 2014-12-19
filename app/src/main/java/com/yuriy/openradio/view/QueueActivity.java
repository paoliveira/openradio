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
public class QueueActivity extends FragmentActivity {

    private static final String CLASS_NAME = QueueActivity.class.getSimpleName();

    private ImageButton mSkipNext;
    private ImageButton mSkipPrevious;
    private ImageButton mPlayPause;

    private MediaBrowser mMediaBrowser;
    private MediaController.TransportControls mTransportControls;
    private MediaController mMediaController;
    private PlaybackState mPlaybackState;

    private QueueAdapter mQueueAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_queue);

        mSkipPrevious = (ImageButton) findViewById(R.id.skip_previous);
        mSkipPrevious.setEnabled(false);
        mSkipPrevious.setOnClickListener(mButtonListener);

        mSkipNext = (ImageButton) findViewById(R.id.skip_next);
        mSkipNext.setEnabled(false);
        mSkipNext.setOnClickListener(mButtonListener);

        mPlayPause = (ImageButton) findViewById(R.id.play_pause);
        mPlayPause.setEnabled(true);
        mPlayPause.setOnClickListener(mButtonListener);

        mQueueAdapter = new QueueAdapter(this);

        final ListView mListView = (ListView) findViewById(R.id.queue_list_view);
        mListView.setAdapter(mQueueAdapter);
        mListView.setFocusable(true);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MediaSession.QueueItem item = mQueueAdapter.getItem(position);
                mTransportControls.skipToQueueItem(item.getQueueId());
            }
        });

        mMediaBrowser = new MediaBrowser(
                this, new ComponentName(this, OpenRadioService.class), mConnectionCallback, null
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
            mMediaController.unregisterCallback(mSessionCallback);
        }
        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }
    }

    public static Intent makeIntent(final Context context) {
        return new Intent(context, QueueActivity.class);
    }

    private MediaBrowser.ConnectionCallback mConnectionCallback
            = new MediaBrowser.ConnectionCallback() {

        @Override
        public void onConnected() {
            Log.d(CLASS_NAME, "onConnected: session token "
                    + mMediaBrowser.getSessionToken());

            if (mMediaBrowser.getSessionToken() == null) {
                throw new IllegalArgumentException("No Session token");
            }

            mMediaController = new MediaController(QueueActivity.this,
                    mMediaBrowser.getSessionToken());
            mTransportControls = mMediaController.getTransportControls();
            mMediaController.registerCallback(mSessionCallback);

            setMediaController(mMediaController);
            mPlaybackState = mMediaController.getPlaybackState();

            final List<MediaSession.QueueItem> queue = mMediaController.getQueue();
            if (queue != null) {
                mQueueAdapter.clear();
                mQueueAdapter.notifyDataSetInvalidated();
                mQueueAdapter.addAll(queue);
                mQueueAdapter.notifyDataSetChanged();
            }
            onPlaybackStateChanged(mPlaybackState);
        }

        @Override
        public void onConnectionFailed() {
            Log.w(CLASS_NAME, "On Connection Failed");
        }

        @Override
        public void onConnectionSuspended() {
            Log.w(CLASS_NAME, "On Connection Suspended");
            mMediaController.unregisterCallback(mSessionCallback);
            mTransportControls = null;
            mMediaController = null;
            setMediaController(null);
        }
    };

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private MediaController.Callback mSessionCallback = new MediaController.Callback() {

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

    private View.OnClickListener mButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int state = mPlaybackState == null ?
                    PlaybackState.STATE_NONE : mPlaybackState.getState();
            switch (v.getId()) {
                case R.id.play_pause:
                    Log.d(CLASS_NAME, "Play button pressed, in state " + state);
                    if (state == PlaybackState.STATE_PAUSED ||
                            state == PlaybackState.STATE_STOPPED ||
                            state == PlaybackState.STATE_NONE) {
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

    private void playMedia() {
        if (mTransportControls != null) {
            mTransportControls.play();
        }
    }

    private void pauseMedia() {
        if (mTransportControls != null) {
            mTransportControls.pause();
        }
    }

    private void skipToPrevious() {
        if (mTransportControls != null) {
            mTransportControls.skipToPrevious();
        }
    }

    private void skipToNext() {
        if (mTransportControls != null) {
            mTransportControls.skipToNext();
        }
    }
}
