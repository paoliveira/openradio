package com.yuriy.openradio.presenter;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import com.yuriy.openradio.R;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class CardPresenter extends Presenter {

    private static final String CLASS_NAME = CardPresenter.class.getSimpleName() + " ";

    private int mSelectedBackgroundColor = -1;
    private int mDefaultBackgroundColor = -1;
    private Drawable mDefaultCardImage;

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent) {
        mDefaultBackgroundColor =
            ContextCompat.getColor(parent.getContext(), R.color.primary_color);
        mSelectedBackgroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.blue_light_color);
        mDefaultCardImage = parent.getResources().getDrawable(R.drawable.ic_launcher, null);

        final ImageCardView cardView = new ImageCardView(parent.getContext()) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
            }
        };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        updateCardBackgroundColor(cardView, false);
        return new ViewHolder(cardView);
    }

    private void updateCardBackgroundColor(ImageCardView view, boolean selected) {
        int color = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;

        // Both background colors should be set because the view's
        // background is temporarily visible during animations.
        view.setBackgroundColor(color);
        view.findViewById(R.id.info_field).setBackgroundColor(color);
    }

    @Override
    public void onBindViewHolder(final Presenter.ViewHolder viewHolder, final Object item) {
        final ImageCardView cardView = (ImageCardView) viewHolder.view;
        cardView.setTitleText("video.title");
        cardView.setContentText("video.studio");

//        if (video.cardImageUrl != null) {
            // Set card size from dimension resources.
            Resources res = cardView.getResources();
            int width = res.getDimensionPixelSize(R.dimen.card_width);
            int height = res.getDimensionPixelSize(R.dimen.card_height);
            cardView.setMainImageDimensions(width, height);
//
//            Glide.with(cardView.getContext())
//                    .load(video.cardImageUrl)
//                    .apply(RequestOptions.errorOf(mDefaultCardImage))
//                    .into(cardView.getMainImageView());
//        }
    }

    @Override
    public void onUnbindViewHolder(final Presenter.ViewHolder viewHolder) {
        final ImageCardView cardView = (ImageCardView) viewHolder.view;

        // Remove references to images so that the garbage collector can free up memory.
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}
