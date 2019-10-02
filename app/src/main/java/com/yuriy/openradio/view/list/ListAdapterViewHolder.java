package com.yuriy.openradio.view.list;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 11/29/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link ListAdapterViewHolder} is a helper class to keep references for
 * the View elements of the single row in the List
 */
final class ListAdapterViewHolder {

    /**
     * Default constructor.
     */
    ListAdapterViewHolder() {
        super();
    }

    /**
     * Title text view.
     */
    TextView mNameView;

    /**
     * Description text view.
     */
    TextView mDescriptionView;

    /**
     * Category image view.
     */
    ImageView mImageView;

    /**
     * Check box vew for the "Favorites" option.
     */
    CheckBox mFavoriteCheckView;

    /**
     * Root view of the layout.
     */
    RelativeLayout mRootView;
}
