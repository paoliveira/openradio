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

package com.yuriy.openradio.view.list;

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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.model.net.UrlBuilder;
import com.yuriy.openradio.shared.service.OpenRadioService;
import com.yuriy.openradio.shared.utils.MediaIdHelper;
import com.yuriy.openradio.shared.utils.MediaItemHelper;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/18/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class TvMediaItemsAdapter extends RecyclerView.Adapter<TvMediaItemsAdapter.ViewHolder> {

    public interface Listener {

        void onItemSelected(MediaBrowserCompat.MediaItem item, int position);
    }

    private Context mContext;
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
     * @param context current {@link android.app.Activity}
     */
    public TvMediaItemsAdapter(final Context context) {
        super();
        mParentId = MediaIdHelper.MEDIA_ID_ROOT;
        mAdapterData = new ListAdapterData<>();
        mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.tv_category_list_item, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final MediaBrowserCompat.MediaItem mediaItem = getItem(position);
        if (mediaItem == null) {
            return;
        }
        if (mContext == null) {
            return;
        }

        final MediaDescriptionCompat description = mediaItem.getDescription();
        final boolean isPlayable = mediaItem.isPlayable();

        holder.mRoot.setOnClickListener(
                v -> {
                    if (mListener == null) {
                        return;
                    }
                    mListener.onItemSelected(mediaItem, position);
                }
        );

        handleNameAndDescriptionView(holder.mNameView, holder.mDescriptionView, description, getParentId());
        updateImage(description, holder.mImageView);
        updateBitrateView(
                MediaItemHelper.getBitrateField(mediaItem), holder.mBitrateView, isPlayable
        );

        holder.mFavoriteCheckView.setButtonDrawable(
                AppCompatResources.getDrawable(mContext, R.drawable.src_favorite)
        );

        if (isPlayable) {
            handleFavoriteAction(
                    holder.mFavoriteCheckView, description, mediaItem, mContext
            );
        } else {
            holder.mFavoriteCheckView.setVisibility(View.GONE);
        }

        boolean selected = false;
        if (position == getActiveItemId()) {
            selected = true;
        }
        holder.mRoot.setSelected(selected);
    }

    @Override
    public int getItemCount() {
        return mAdapterData.getItemsCount();
    }

    public void setListener(@NonNull final Listener value) {
        mListener = value;
    }

    public void removeListener() {
        mListener = null;
    }

    @Nullable
    public MediaBrowserCompat.MediaItem getItem(final int position) {
        return mAdapterData.getItem(position);
    }

    @Override
    public void onViewRecycled(@NonNull final ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.mImageView.setImageResource(android.R.color.transparent);
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
        mContext = null;
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
        private RelativeLayout mRoot;


        public ViewHolder(final View view) {
            super(view);
            mRoot = view.findViewById(R.id.tv_root_view);
            mNameView = view.findViewById(R.id.tv_name_view);
            mDescriptionView = view.findViewById(R.id.tv_description_view);
            mImageView = view.findViewById(R.id.tv_img_view);
            mFavoriteCheckView = view.findViewById(R.id.tv_favorite_btn_view);
            mBitrateView = view.findViewById(R.id.tv_bitrate_view);
        }
    }
}
