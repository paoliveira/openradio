package com.yuriy.openradio.view.list;

import android.app.Activity;
import android.media.browse.MediaBrowser;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.yuriy.openradio.R;
import com.yuriy.openradio.utils.ImageFetcher;
import com.yuriy.openradio.utils.MediaIDHelper;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/18/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class MediaItemsAdapter extends BaseAdapter {

    private static final String CLASS_NAME = MediaItemsAdapter.class.getSimpleName();

    private ListAdapterViewHolder mViewHolder;
    private Activity mCurrentActivity;
    //private ImageFetcher mImageFetcher;
    private final ListAdapterData<MediaBrowser.MediaItem> mAdapterData = new ListAdapterData<>(null);

    /**
     * Constructor.
     *
     * @param activity     current {@link android.app.Activity}
     * @param imageFetcher {@link ImageFetcher} instance
     */
    public MediaItemsAdapter(final FragmentActivity activity, final ImageFetcher imageFetcher) {
        mCurrentActivity = activity;
        //mImageFetcher = imageFetcher;
    }

    @Override
    public int getCount() {
        return mAdapterData.getItemsCount();
    }

    @Override
    public Object getItem(final int position) {
        return mAdapterData.getItem(position);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final MediaBrowser.MediaItem mediaItem = (MediaBrowser.MediaItem) getItem(position);
        convertView = prepareViewAndHolder(convertView, R.layout.category_list_item);

        mViewHolder.mNameView.setText(mediaItem.getDescription().getTitle());
        mViewHolder.mDescriptionView.setText(mediaItem.getDescription().getSubtitle());
        if (mediaItem.getMediaId().equals(MediaIDHelper.MEDIA_ID_ALL_CATEGORIES)) {
            mViewHolder.mImageView.setImageDrawable(
                    mCurrentActivity.getDrawable(R.drawable.ic_all_categories));
        } else {
            if (mediaItem.isBrowsable()) {
                mViewHolder.mImageView.setImageDrawable(
                        mCurrentActivity.getDrawable(R.drawable.ic_child_categories));
            } else if (mediaItem.isPlayable()) {
                mViewHolder.mImageView.setImageDrawable(
                        mCurrentActivity.getDrawable(R.drawable.ic_radio_station));
            }
        }

        return convertView;
    }

    /**
     * Add {@link com.yuriy.openradio.api.CategoryVO} into the collection.
     * @param value {@link com.yuriy.openradio.api.CategoryVO}
     */
    public void addItem(final MediaBrowser.MediaItem value) {
        mAdapterData.addItem(value);
    }

    /**
     * Add {@link com.yuriy.openradio.api.CategoryVO}s into the collection.
     * @param items Collection of the {@link com.yuriy.openradio.api.CategoryVO}
     */
    public void addItems(final List<MediaBrowser.MediaItem> items) {
        for (MediaBrowser.MediaItem item : items) {
            addItem(item);
        }
    }

    /**
     * Clear adapter data.
     */
    public void clear() {
        mAdapterData.clear();
    }

    /**
     * Prepare view holder for the item rendering.
     *
     * @param convertView      {@link android.view.View} associated with List Item
     * @param inflateViewResId Id of the View layout
     * @return View
     */
    private View prepareViewAndHolder(View convertView, final int inflateViewResId) {
        // If there is no View created - create it here and set it's Tag
        if (convertView == null) {
            convertView = LayoutInflater.from(mCurrentActivity).inflate(inflateViewResId, null);
            mViewHolder = createViewHolder(convertView);
            convertView.setTag(mViewHolder);
        } else {
            // Get View by provided Tag
            mViewHolder = (ListAdapterViewHolder) convertView.getTag();
        }
        return convertView;
    }

    /**
     * Create View holder to keep reference to the layout items
     * @param view {@link android.view.View}
     * @return {@link ListAdapterViewHolder} object
     */
    private ListAdapterViewHolder createViewHolder(final View view) {
        final ListAdapterViewHolder viewHolder = new ListAdapterViewHolder();
        viewHolder.mNameView = (TextView) view.findViewById(R.id.name_view);
        viewHolder.mDescriptionView = (TextView) view.findViewById(R.id.description_view);
        viewHolder.mImageView = (ImageView) view.findViewById(R.id.img_view);
        return viewHolder;
    }
}