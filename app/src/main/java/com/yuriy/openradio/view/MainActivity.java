package com.yuriy.openradio.view;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
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

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created with Android Studio.
 * Author: Chernyshov Yuriy - Mobile Development
 * Date: 19.12.14
 * Time: 15:13
 */

/**
 * Main Activity class with represents the list of the categories: All, By Genre, Favorites, etc ...
 */
public class MainActivity extends FragmentActivity {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = MainActivity.class.getSimpleName();

    /**
     * Browses media content offered by a link MediaBrowserService.
     * This object is not thread-safe.
     * All calls should happen on the thread on which the browser was constructed.
     */
    private MediaBrowser mMediaBrowser;

    /**
     * Adapter for the representing media items in the list.
     */
    private MediaItemsAdapter mBrowserAdapter;

    /**
     * Stack of the media items.
     * It is used when navigating back and forth via list.
     */
    private final List<String> mediaItemsStack = new LinkedList<>();

    /**
     * Map of the last used list position for the given list of the media items.
     */
    private final Map<String, Integer> listPositionMap = new Hashtable<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set content.
        setContentView(R.layout.activity_main);

        // Instantiate adapter
        mBrowserAdapter = new MediaItemsAdapter(this, null);

        // Get list view reference from the inflated xml
        final ListView listView = (ListView) findViewById(R.id.list_view);
        // Set adapter
        listView.setAdapter(mBrowserAdapter);
        // Set click listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(final AdapterView<?> parent, final View view,
                                    final int position, final long id) {

                // Keep last selected position for the given category.
                // We will use it when back to this category
                listPositionMap.put(mediaItemsStack.get(mediaItemsStack.size() - 1), position);

                // Current selected media item
                final MediaBrowser.MediaItem item
                        = (MediaBrowser.MediaItem) mBrowserAdapter.getItem(position);

                // If it is browsable - then we navigate to the next category
                if (item.isBrowsable()) {
                    addMediaItemToStack(item.getMediaId());
                } else if (item.isPlayable()) {
                    // Else - we play an item
                    getMediaController().getTransportControls()
                            .playFromMediaId(item.getMediaId(), null);

                    // Call appropriate activity for the items playing
                    startActivity(QueueActivity.makeIntent(MainActivity.this));
                }
            }
        });

        // Instantiate media browser
        mMediaBrowser = new MediaBrowser(
                this,
                new ComponentName(this, OpenRadioService.class),
                connectionCallback, null
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
            // DialogFragment.show() will take care of adding the fragment
            // in a transaction.  We also want to remove any currently showing
            // dialog, so make our own transaction and take care of that here.
            final FragmentTransaction fragmentTransaction = getFragmentManager()
                    .beginTransaction();
            final Fragment fragmentByTag = getFragmentManager()
                    .findFragmentByTag(AboutDialog.DIALOG_TAG);
            if (fragmentByTag != null) {
                fragmentTransaction.remove(fragmentByTag);
            }
            fragmentTransaction.addToBackStack(null);

            // Show About Dialog
            final DialogFragment aboutDialog = AboutDialog.newInstance();
            aboutDialog.show(fragmentTransaction, AboutDialog.DIALOG_TAG);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        // If there is root category - close activity
        if (mediaItemsStack.size() == 1) {

            // Un-subscribe from item
            mMediaBrowser.unsubscribe(mediaItemsStack.remove(mediaItemsStack.size() - 1));
            // Clear stack
            mediaItemsStack.clear();

            // perform android framework lifecycle
            super.onBackPressed();
            return;
        }

        // Pop up current media item
        final String currentMediaId = mediaItemsStack.remove(mediaItemsStack.size() - 1);

        // Un-subscribe from all items
        mMediaBrowser.unsubscribe(currentMediaId);
        for (String mediaItemId : mediaItemsStack) {
            mMediaBrowser.unsubscribe(mediaItemId);
        }

        // Disconnect and connect back to media browser
        mMediaBrowser.disconnect();
        mMediaBrowser.connect();
    }

    /**
     * Add {@link android.media.browse.MediaBrowser.MediaItem} to stack.
     *
     * @param mediaId Id of the {@link android.view.MenuItem}
     */
    private void addMediaItemToStack(final String mediaId) {
        Log.i(CLASS_NAME, "MediaItem Id added:" + mediaId);

        mediaItemsStack.add(mediaId);

        mMediaBrowser.subscribe(mediaId, subscriptionCallback);
    }

    /**
     * Callback object for the subscription events
     */
    private final MediaBrowser.SubscriptionCallback subscriptionCallback
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

            // Get list view reference from the inflated xml
            final ListView listView = (ListView) findViewById(R.id.list_view);
            if (listView == null) {
                return;
            }
            if (!listPositionMap.containsKey(parentId)) {
                return;
            }
            // Restore previous position for the given category
            listView.setSelection(listPositionMap.get(parentId));
        }

        @Override
        public void onError(final String id) {
            Toast.makeText(
                    MainActivity.this, R.string.error_loading_media, Toast.LENGTH_LONG
            ).show();
        }
    };

    /**
     * Callback object for the Media Browser connection events
     */
    private final MediaBrowser.ConnectionCallback connectionCallback
            = new MediaBrowser.ConnectionCallback() {

        @Override
        public void onConnected() {
            super.onConnected();

            Log.i(CLASS_NAME, "MediaBrowser connected, stack empty:" + mediaItemsStack.isEmpty());

            // If stack is empty - assume that this is a start point
            if (mediaItemsStack.isEmpty()) {
                addMediaItemToStack(mMediaBrowser.getRoot());
            }

            // If session token is null - throw exception
            if (mMediaBrowser.getSessionToken() == null) {
                throw new IllegalArgumentException("No Session token");
            }

            // Subscribe to the media item
            mMediaBrowser.subscribe(
                    mediaItemsStack.get(mediaItemsStack.size() - 1),
                    subscriptionCallback
            );

            if (getMediaController() != null) {
                return;
            }

            // Initialize media controller
            final MediaController mediaController = new MediaController(
                    MainActivity.this,
                    mMediaBrowser.getSessionToken()
            );

            // Set actual controller
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
