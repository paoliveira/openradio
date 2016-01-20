/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.yuriy.openradio.view.list;

import android.app.Activity;
import android.media.session.MediaSession;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.yuriy.openradio.R;

import java.util.ArrayList;

/**
 * A list adapter for items in a queue
 */
public class QueueAdapter extends ArrayAdapter<MediaSession.QueueItem> {

    @SuppressWarnings("unused")
    private static final String CLASS_NAME = QueueAdapter.class.getSimpleName();

    /**
     * The currently selected/active queue item Id.
     */
    private long mActiveQueueItemId = MediaSession.QueueItem.UNKNOWN_ID;

    /**
     * Position of the Active element.
     */
    private int mActivePosition = 0;

    /**
     * Constructor.
     * @param context Context.
     */
    public QueueAdapter(final Activity context) {
        super(context, R.layout.media_list_item, new ArrayList<MediaSession.QueueItem>());
    }

    /**
     * Set active ID from the items queue.
     * @param id Id of the Item.
     */
    public void setActiveQueueItemId(final long id) {
        mActiveQueueItemId = id;
    }

    /**
     * Returns the currently active queue Id.
     *
     * @return The currently active queue Id.
     */
    public long getActiveQueueItemId() {
        return mActiveQueueItemId;
    }

    /**
     * @return Position of the current active element.
     */
    public int getActivePosition() {
        return mActivePosition;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.media_list_item, parent, false);
            holder = new ViewHolder();
            holder.mImageView = (ImageView) convertView.findViewById(R.id.play_eq);
            holder.mTitleView = (TextView) convertView.findViewById(R.id.title);
            holder.mDescriptionView = (TextView) convertView.findViewById(R.id.description);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final MediaSession.QueueItem item = getItem(position);
        Log.d(CLASS_NAME, "Queue Item:" + item);

        holder.mTitleView.setText(item.getDescription().getTitle());
        holder.mDescriptionView.setText(item.getDescription().getDescription());

        // If the itemId matches the active Id then use a different icon
        if (mActiveQueueItemId == item.getQueueId()) {
            mActivePosition = position;
            holder.mImageView.setImageDrawable(
                    getContext().getDrawable(R.drawable.ic_equalizer_white_24dp));
            convertView.setBackgroundColor(
                    getContext().getResources().getColor(R.color.queue_item_selected_bg_color)
            );
        } else {
            holder.mImageView.setImageDrawable(
                    getContext().getDrawable(R.drawable.ic_play_arrow_white_24dp));
            convertView.setBackgroundColor(
                    getContext().getResources().getColor(R.color.transparent_color)
            );
        }

        return convertView;
    }

    /**
     * Static class to hold UI controls references.
     */
    private static class ViewHolder {

        private ViewHolder() {
            super();
        }

        private ImageView mImageView;
        private TextView mTitleView;
        private TextView mDescriptionView;
    }
}
