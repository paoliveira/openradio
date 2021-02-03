/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.gabor.view.list;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.xenione.libs.swipemaker.SwipeLayout;
import com.yuriy.openradio.gabor.R;
import com.yuriy.openradio.gabor.shared.model.net.UrlBuilder;
import com.yuriy.openradio.gabor.shared.service.OpenRadioService;
import com.yuriy.openradio.gabor.shared.utils.MediaIdHelper;
import com.yuriy.openradio.gabor.shared.utils.MediaItemHelper;
import com.yuriy.openradio.gabor.view.activity.MainActivity;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/18/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class MediaItemsAdapter extends RecyclerView.Adapter<MediaItemsAdapter.ViewHolder> {

    public interface Listener {
        void onItemSettings(MediaBrowserCompat.MediaItem item, final int position);

        void onItemTap(MediaBrowserCompat.MediaItem item, final int position);
    }

    private MainActivity mActivity;
    private final ListAdapterData<MediaBrowserCompat.MediaItem> mAdapterData;
    private String mParentId;
    private Listener mListener;

    /**
     * The currently selected / active Item Id.
     */
    private int mActiveItemId = MediaSessionCompat.QueueItem.UNKNOWN_ID;

    /**
     * Main constructor.
     *
     * @param activity current {@link android.app.Activity}
     */
    public MediaItemsAdapter(final MainActivity activity) {
        super();
        mParentId = MediaIdHelper.MEDIA_ID_ROOT;
        mAdapterData = new ListAdapterData<>();
        mActivity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_both_side_swipe, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final MediaBrowserCompat.MediaItem mediaItem = getItem(position);
        if (mediaItem == null) {
            return;
        }
        if (mActivity == null) {
            return;
        }

        final MediaDescriptionCompat description = mediaItem.getDescription();
        final boolean isPlayable = mediaItem.isPlayable();

        handleNameAndDescriptionView(holder.mNameView, holder.mDescriptionView, description, getParentId());
        updateImage(description, holder.mImageView);
        updateBitrateView(
                MediaItemHelper.getBitrateField(mediaItem), holder.mBitrateView, isPlayable
        );

        holder.mFavoriteCheckView.setButtonDrawable(
                AppCompatResources.getDrawable(mActivity, R.drawable.src_favorite)
        );

        if (isPlayable) {
            handleFavoriteAction(
                    holder.mFavoriteCheckView, description, mediaItem, mActivity
            );
        } else {
            holder.mFavoriteCheckView.setVisibility(View.GONE);
        }

        holder.mSettingsView.setOnClickListener(new OnSettingsListener(mediaItem, position));

        holder.mForegroundView.isDragDisabled(!isPlayable);
        holder.mForegroundView.setOnClickListener(new OnItemTapListener(mediaItem, position));
        int color = R.color.or_color_list_item_bg;
        if (position == getActiveItemId()) {
            color = R.color.or_color_list_item_bg_selected;
        }
        holder.mForegroundView.setBackgroundColor(mActivity.getResources().getColor(color));
    }

    @Override
    public int getItemCount() {
        return mAdapterData.getItemsCount();
    }

    @Nullable
    public MediaBrowserCompat.MediaItem getItem(final int position) {
        return mAdapterData.getItem(position);
    }

    @Override
    public void onViewRecycled(@NonNull final ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.mImageView.setImageResource(android.R.color.transparent);
        holder.mRoot.sync();
    }

    public void setListener(final Listener listener) {
        mListener = listener;
    }

    /**
     * get index of the Item by provided Media Id.
     *
     * @param mediaId Media Id of the Radio Station.
     * @return Index of the Radio Station in the adapter, or -1 if nothing founded.
     */
    public int getIndexForMediaId(final String mediaId) {
        final int count = mAdapterData.getItemsCount();
        MediaBrowserCompat.MediaItem item;
        for (int i = 0; i < count; i++) {
            item = mAdapterData.getItem(i);
            if (item == null) {
                continue;
            }
            if (TextUtils.equals(item.getDescription().getMediaId(), mediaId)) {
                return i;
            }
        }
        return -1;
    }

    private String getParentId() {
        return mParentId;
    }

    public void setParentId(final String value) {
        mParentId = value;
    }

    /**
     * Set active Id from the items list.
     *
     * @param id Id of the Item.
     */
    public void setActiveItemId(final int id) {
        mActiveItemId = id;
    }

    /**
     * Returns the currently active Item Id.
     *
     * @return The currently active Item Id.
     */
    public int getActiveItemId() {
        return mActiveItemId;
    }

    /**
     * Add {@link MediaBrowserCompat.MediaItem}s into the collection.
     *
     * @param value {@link MediaBrowserCompat.MediaItem}s.
     */
    public final void addAll(final List<MediaBrowserCompat.MediaItem> value) {
        mAdapterData.addAll(value);
    }

    /**
     * Add {@link MediaBrowserCompat.MediaItem} into the collection
     * at specified position.
     *
     * @param position  Position to add.
     * @param mediaItem {@link MediaBrowserCompat.MediaItem} to add.
     */
    public final void addAt(final int position, final MediaBrowserCompat.MediaItem mediaItem) {
        mAdapterData.addAt(position, mediaItem);
    }

    /**
     * Removes Media Item from the adapter.
     *
     * @param mediaItem Media Item to remove.
     */
    public final void remove(final MediaBrowserCompat.MediaItem mediaItem) {
        mAdapterData.remove(mediaItem);
    }

    /**
     * Clear adapter data.
     */
    public final void clearData() {
        mAdapterData.clear();
    }

    public void clear() {
        clearData();
        mActivity = null;
    }

    public static void updateBitrateView(final int bitrate,
                                         @Nullable final TextView view,
                                         final boolean isPlayable) {
        if (view == null) {
            return;
        }
        if (isPlayable) {
            if (bitrate != 0) {
                view.setVisibility(View.VISIBLE);
                final String bitrateStr = bitrate + "kb/s";
                view.setText(bitrateStr);
            } else {
                view.setVisibility(View.GONE);
            }
        } else {
            view.setVisibility(View.GONE);
        }
    }

    /**
     * Handle view of list item responsible to display Title and Description.<p>
     * Different categories requires different handle approaches.
     *
     * @param nameView
     * @param descriptionView
     * @param description
     * @param parentId
     */
    private static void handleNameAndDescriptionView(@NonNull final TextView nameView,
                                                     @NonNull final TextView descriptionView,
                                                     final MediaDescriptionCompat description,
                                                     @NonNull final String parentId) {
        nameView.setText(description.getTitle());
        descriptionView.setText(description.getSubtitle());

        final RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) nameView.getLayoutParams();

        if (MediaIdHelper.MEDIA_ID_ROOT.equals(parentId)) {
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            descriptionView.setVisibility(View.GONE);
        } else {
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.LEFT_OF);
            descriptionView.setVisibility(View.VISIBLE);
        }
        nameView.setLayoutParams(layoutParams);
    }

    /**
     * Updates an image view of the Media Item.
     *
     * @param description Media Description of the Media Item.
     * @param view        Image View to apply image to.
     */
    public static void updateImage(@NonNull final MediaDescriptionCompat description,
                                   @Nullable final ImageView view) {
        if (view == null) {
            return;
        }
        if (description.getIconBitmap() != null) {
            view.setImageBitmap(description.getIconBitmap());
        } else {
            final Uri iconUri = UrlBuilder.preProcessIconUri(description.getIconUri());
            final int iconId = MediaItemHelper.getDrawableId(description.getExtras());
            if (MediaItemHelper.isDrawableIdValid(iconId)) {
                view.setImageResource(iconId);
            }
            if (iconUri != null) {
                Picasso.get().load(iconUri).noPlaceholder().into(view);
            }
        }
    }

    /**
     * Handle "Add | Remove to | from Favorites".
     *
     * @param checkBox    Favorite check box view.
     * @param description Media aItem description.
     * @param mediaItem   Media Item.
     * @param context     Current context.
     */
    public static void handleFavoriteAction(final CheckBox checkBox, final MediaDescriptionCompat description,
                                            final MediaBrowserCompat.MediaItem mediaItem, final Context context) {
        checkBox.setChecked(MediaItemHelper.isFavoriteField(mediaItem));
        checkBox.setVisibility(View.VISIBLE);
        checkBox.setOnClickListener(

                view -> {
                    final boolean isChecked = ((CheckBox) view).isChecked();

                    MediaItemHelper.updateFavoriteField(mediaItem, isChecked);

                    // Make Intent to update Favorite RadioStation object associated with
                    // the Media Description
                    final Intent intent = OpenRadioService.makeUpdateIsFavoriteIntent(
                            context,
                            description,
                            isChecked
                    );
                    // Send Intent to the OpenRadioService.
                    context.startService(intent);
                }
        );
    }

    private final class OnSettingsListener implements View.OnClickListener {

        private final int mPosition;
        @NonNull
        private final MediaBrowserCompat.MediaItem mItem;

        public OnSettingsListener(@NonNull final MediaBrowserCompat.MediaItem item, final int position) {
            super();
            mPosition = position;
            mItem = new MediaBrowserCompat.MediaItem(item.getDescription(), item.getFlags());
        }

        @Override
        public void onClick(final View view) {
            mListener.onItemSettings(
                    new MediaBrowserCompat.MediaItem(mItem.getDescription(), mItem.getFlags()), mPosition
            );
        }
    }

    private final class OnItemTapListener implements View.OnClickListener {

        private final int mPosition;
        private final MediaBrowserCompat.MediaItem mItem;

        public OnItemTapListener(final MediaBrowserCompat.MediaItem item, final int position) {
            super();
            mPosition = position;
            mItem = item;
        }

        @Override
        public void onClick(final View view) {
            mListener.onItemTap(mItem, mPosition);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        /**
         * Title text view.
         */
        private final TextView mNameView;

        private final TextView mBitrateView;

        /**
         * Description text view.
         */
        private final TextView mDescriptionView;

        /**
         * Category image view.
         */
        private final ImageView mImageView;

        /**
         * Check box vew for the "Favorites" option.
         */
        private final CheckBox mFavoriteCheckView;

        private final SwipeLayout mForegroundView;

        private final BothSideCoordinatorLayout mRoot;

        private final ImageButton mSettingsView;

        public ViewHolder(final View view) {
            super(view);
            mNameView = view.findViewById(R.id.name_view);
            mDescriptionView = view.findViewById(R.id.description_view);
            mImageView = view.findViewById(R.id.img_view);
            mFavoriteCheckView = view.findViewById(R.id.favorite_btn_view);
            mBitrateView = view.findViewById(R.id.bitrate_view);
            mSettingsView = view.findViewById(R.id.settings_btn_view);
            mForegroundView = view.findViewById(R.id.foreground_view);
            mRoot = view.findViewById(R.id.item_root);
        }
    }
}
