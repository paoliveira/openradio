package com.yuriy.openradio.view;

import android.content.ComponentName;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.yuriy.openradio.R;
import com.yuriy.openradio.service.OpenRadioService;
import com.yuriy.openradio.view.list.MediaItemsAdapter;

import java.util.LinkedList;
import java.util.List;

public class MainActivity extends FragmentActivity {

    private static final String CLASS_NAME = MainActivity.class.getSimpleName();

    private MediaBrowser mMediaBrowser;

    private MediaItemsAdapter mBrowserAdapter;

    private final List<String> mediaItemsStack = new LinkedList<>();

    private boolean isInit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final View controls = findViewById(R.id.controls);
        controls.setVisibility(View.GONE);

        mBrowserAdapter = new MediaItemsAdapter(this, null);

        final ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(mBrowserAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(final AdapterView<?> parent, final View view,
                                    final int position, final long id) {

                final MediaBrowser.MediaItem item
                        = (MediaBrowser.MediaItem) mBrowserAdapter.getItem(position);

                addMediaItemToQueue(item.getMediaId());
            }
        });

        mMediaBrowser = new MediaBrowser(
                this,
                new ComponentName(this, OpenRadioService.class),
                mConnectionCallback, null
        );
    }

    @Override
    protected void onStart() {
        super.onStart();

        mMediaBrowser.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mMediaBrowser.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        //Log.i(CLASS_NAME, "Back pressed");

        if (mediaItemsStack.size() == 1) {

            mMediaBrowser.unsubscribe(mediaItemsStack.remove(mediaItemsStack.size() - 1));
            mediaItemsStack.clear();

            super.onBackPressed();
            return;
        }

        final String currentMediaId = mediaItemsStack.remove(mediaItemsStack.size() - 1);
        //Log.i(CLASS_NAME, "Item Id remove: " + currentMediaId);

        mMediaBrowser.unsubscribe(currentMediaId);
        for (String mediaItemId : mediaItemsStack) {
            mMediaBrowser.unsubscribe(mediaItemId);
        }

        mMediaBrowser.disconnect();
        mMediaBrowser.connect();
    }

    private void addMediaItemToQueue(final String mediaId) {
        Log.i(CLASS_NAME, "MediaItem Id added:" + mediaId);

        mediaItemsStack.add(mediaId);

        mMediaBrowser.subscribe(mediaId, mSubscriptionCallback);
    }

    private MediaBrowser.SubscriptionCallback mSubscriptionCallback
            = new MediaBrowser.SubscriptionCallback() {

        @Override
        public void onChildrenLoaded(final String parentId,
                                     final List<MediaBrowser.MediaItem> children) {
            Log.i(CLASS_NAME, "On children loaded:" + children.size() + " " + parentId);

            mBrowserAdapter.clear();
            mBrowserAdapter.notifyDataSetInvalidated();
            for (MediaBrowser.MediaItem item : children) {
                mBrowserAdapter.addItem(item);
            }
            mBrowserAdapter.notifyDataSetChanged();
        }

        @Override
        public void onError(final String id) {
            Toast.makeText(
                    MainActivity.this, R.string.error_loading_media, Toast.LENGTH_LONG
            ).show();
        }
    };

    private MediaBrowser.ConnectionCallback mConnectionCallback
            = new MediaBrowser.ConnectionCallback() {

        @Override
        public void onConnected() {
            super.onConnected();

            Log.i(CLASS_NAME, "MediaBrowser connected, stack empty:" + mediaItemsStack.isEmpty());

            if (mediaItemsStack.isEmpty()) {
                addMediaItemToQueue(mMediaBrowser.getRoot());
            }

            if (mMediaBrowser.getSessionToken() == null) {
                throw new IllegalArgumentException("No Session token");
            }

            mMediaBrowser.subscribe(mediaItemsStack.get(mediaItemsStack.size() - 1), mSubscriptionCallback);

            if (getMediaController() != null) {
                return;
            }

            final MediaController mediaController = new MediaController(
                    MainActivity.this,
                    mMediaBrowser.getSessionToken()
            );

            setMediaController(mediaController);
        }

        @Override
        public void onConnectionSuspended() {
            super.onConnectionSuspended();

            Log.w(CLASS_NAME, "MediaBrowser connection suspended");
        }

        @Override
        public void onConnectionFailed() {
            super.onConnectionFailed();

            Log.w(CLASS_NAME, "MediaBrowser connection failed");

            setMediaController(null);
        }
    };
}
