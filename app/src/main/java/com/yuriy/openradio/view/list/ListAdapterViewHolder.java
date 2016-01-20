package com.yuriy.openradio.view.list;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 11/29/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */

import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * {@link ListAdapterViewHolder} is a helper class to keep references for
 * the View elements of the single row in the List
 */
class ListAdapterViewHolder {

    /**
     * Title text view.
     */
    public TextView mNameView;

    /**
     * Description text view.
     */
    public TextView mDescriptionView;

    /**
     * Category image view.
     */
    public ImageView mImageView;

    /**
     *
     */
    public CheckBox mFavoriteCheckView;

    /**
     *
     */
    public RelativeLayout mRootView;
}
