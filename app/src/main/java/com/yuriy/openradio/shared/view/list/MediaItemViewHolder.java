package com.yuriy.openradio.shared.view.list;

import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.xenione.libs.swipemaker.SwipeLayout;
import com.yuriy.openradio.mobile.view.list.BothSideCoordinatorLayout;

public final class MediaItemViewHolder extends RecyclerView.ViewHolder {

    /**
     * Title text view.
     */
    public final TextView mNameView;

    public final TextView mBitrateView;

    /**
     * Description text view.
     */
    public final TextView mDescriptionView;

    /**
     * Category image view.
     */
    public final ImageView mImageView;

    /**
     * Check box vew for the "Favorites" option.
     */
    public final CheckBox mFavoriteCheckView;

    public final SwipeLayout mForegroundView;

    public final ViewGroup mRoot;

    public final ImageButton mSettingsView;

    public MediaItemViewHolder(final View view, final int root_view_id, final int name_view_id,
                               final int description_view_id, final int img_view_id, final int favorite_view_id,
                               final int bitrate_view_id, final int settings_btn_view_id,
                               final int foreground_view_id) {
        super(view);
        mRoot = view.findViewById(root_view_id);
        mNameView = view.findViewById(name_view_id);
        mDescriptionView = view.findViewById(description_view_id);
        mImageView = view.findViewById(img_view_id);
        mFavoriteCheckView = view.findViewById(favorite_view_id);
        mBitrateView = view.findViewById(bitrate_view_id);
        mSettingsView = view.findViewById(settings_btn_view_id);
        mForegroundView = view.findViewById(foreground_view_id);
    }
}