package com.yuriy.openradio.view;

import android.content.ComponentName;
import android.content.Context;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.yuriy.openradio.R;
import com.yuriy.openradio.service.OpenRadioService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity {

    private static final String CLASS_NAME = MainActivity.class.getSimpleName();

    /**
     * The mediaId to be used for subscribing for children using the MediaBrowser.
     */
    private String mMediaId;

    private MediaBrowser mMediaBrowser;

    private BrowseAdapter mBrowserAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final View controls = findViewById(R.id.controls);
        controls.setVisibility(View.GONE);

        mBrowserAdapter = new BrowseAdapter(this);

        final ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(mBrowserAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final MediaBrowser.MediaItem item = mBrowserAdapter.getItem(position);
                Log.i(CLASS_NAME, "Item selected:" + item);

                mBrowserAdapter.clear();

                mMediaId = item.getMediaId();
                mMediaBrowser.subscribe(mMediaId, mSubscriptionCallback);

                try {
                    //FragmentDataHelper listener = (FragmentDataHelper) getActivity();
                    //listener.onMediaItemSelected(item);
                } catch (ClassCastException ex) {
                    Log.e(CLASS_NAME, "Exception trying to cast to FragmentDataHelper", ex);
                }
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

    private MediaBrowser.SubscriptionCallback mSubscriptionCallback
            = new MediaBrowser.SubscriptionCallback() {

        @Override
        public void onChildrenLoaded(final String parentId,
                                     final List<MediaBrowser.MediaItem> children) {
            mBrowserAdapter.clear();
            mBrowserAdapter.notifyDataSetInvalidated();
            for (MediaBrowser.MediaItem item : children) {
                mBrowserAdapter.add(item);
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

            Log.i(CLASS_NAME, "MediaBrowser connected");

            if (mMediaId == null) {
                mMediaId = mMediaBrowser.getRoot();
            }
            mMediaBrowser.subscribe(mMediaId, mSubscriptionCallback);
            if (mMediaBrowser.getSessionToken() == null) {
                throw new IllegalArgumentException("No Session token");
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

    /**
     * An adapter for showing the list of browsed MediaItem's
     */
    private static class BrowseAdapter extends ArrayAdapter<MediaBrowser.MediaItem> {

        public BrowseAdapter(final Context context) {
            super(context, R.layout.category_list_item, new ArrayList<MediaBrowser.MediaItem>());
        }

        static class ViewHolder {
            private ImageView mImageView;
            private TextView mNameView;
            private TextView mDescriptionView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.category_list_item, parent, false);
                holder = new ViewHolder();
                holder.mImageView = (ImageView) convertView.findViewById(R.id.img_view);
                holder.mImageView.setVisibility(View.GONE);
                holder.mNameView = (TextView) convertView.findViewById(R.id.name_view);
                holder.mDescriptionView = (TextView) convertView.findViewById(R.id.description_view);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final MediaBrowser.MediaItem item = getItem(position);
            holder.mNameView.setText(item.getDescription().getTitle());
            holder.mDescriptionView.setText(item.getDescription().getDescription());
            if (item.isPlayable()) {
                holder.mImageView.setImageDrawable(
                        getContext().getDrawable(R.drawable.ic_play_arrow_white_24dp));
                holder.mImageView.setVisibility(View.VISIBLE);
            }
            return convertView;
        }
    }
}
