package com.yuriy.openradio.presenter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.yuriy.openradio.R;
import com.yuriy.openradio.model.net.UrlBuilder;
import com.yuriy.openradio.utils.ImageFetcher;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class CardPresenter extends Presenter {

    private static final String CLASS_NAME = CardPresenter.class.getSimpleName() + " ";

    private int mSelectedBackgroundColor = -1;
    private int mDefaultBackgroundColor = -1;
    private Drawable mDefaultCardImage;
    private final Context mContext;
    private final ImageFetcher mImageFetcher;

    public CardPresenter(final Context context, final ImageFetcher imageFetcher) {
        super();
        mContext = context;
        mImageFetcher = imageFetcher;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent) {
        mDefaultBackgroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.primary_light_color);
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

    private void updateCardBackgroundColor(final ImageCardView view, final boolean selected) {
        int color = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;

        // Both background colors should be set because the view's
        // background is temporarily visible during animations.
        view.setBackgroundColor(color);
        view.findViewById(R.id.info_field).setBackgroundColor(color);
    }

    @Override
    public void onBindViewHolder(final Presenter.ViewHolder viewHolder, final Object item) {
        final MediaBrowserCompat.MediaItem mediaItem = (MediaBrowserCompat.MediaItem) item;
        if (mediaItem == null) {
            return;
        }
        final MediaDescriptionCompat description = mediaItem.getDescription();
        final ImageCardView cardView = (ImageCardView) viewHolder.view;
        cardView.setTitleText(description.getTitle());
        cardView.setContentText(description.getSubtitle());

        final Resources res = cardView.getResources();
        final int width = res.getDimensionPixelSize(R.dimen.card_width);
        final int height = res.getDimensionPixelSize(R.dimen.card_height);
        cardView.setMainImageDimensions(width, height);

        if (description.getIconBitmap() != null) {
            cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE);
            cardView.setMainImage(new BitmapDrawable(mContext.getResources(), description.getIconBitmap()));
        } else {
            final Uri iconUri = UrlBuilder.preProcessIconUri(description.getIconUri());
            if (mediaItem.isPlayable()) {
                if (iconUri != null && iconUri.toString().startsWith("android")) {
                    cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    Glide.with(cardView.getContext())
                            .load(iconUri)
                            .apply(RequestOptions.errorOf(mDefaultCardImage))
                            .into(cardView.getMainImageView());
                } else {
                    // Load the image asynchronously into the ImageView, this also takes care of
                    // setting a placeholder image while the background thread runs
                    mImageFetcher.loadImage(iconUri, cardView.getMainImageView());
                }
            } else {
                cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE);
                Glide.with(cardView.getContext())
                    .load(iconUri)
                    .apply(RequestOptions.errorOf(mDefaultCardImage))
                    .into(cardView.getMainImageView());
            }
        }
    }

    @Override
    public void onUnbindViewHolder(final Presenter.ViewHolder viewHolder) {
        final ImageCardView cardView = (ImageCardView) viewHolder.view;

        // Remove references to images so that the garbage collector can free up memory.
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}
