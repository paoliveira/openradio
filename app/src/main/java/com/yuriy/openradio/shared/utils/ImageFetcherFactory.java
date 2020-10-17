package com.yuriy.openradio.shared.utils;

import androidx.fragment.app.FragmentActivity;

import com.yuriy.openradio.R;

/**
 * Created with Android Studio.
 * User: Chernyshov Yuriy
 * Date: 18.05.14
 * Time: 21:14
 * <p>
 * This class provides factory methods to create
 * {@link ImageFetcher} instance
 */
public class ImageFetcherFactory {

    private static final String SMALL_IMAGE_CACHE_DIR = "thumbs_small";
    private static final String LARGE_IMAGE_CACHE_DIR = "thumbs_large";
    private static final String TV_PLAYER_IMAGE_CACHE_DIR = "thumbs_tv_player";

    /**
     * Create {@link ImageFetcher} instance to fetch
     * small images for the List View
     *
     * @param context {@link android.content.Context}
     * @return {@link ImageFetcher} instance
     */
    public static ImageWorker getSmallImageFetcher(final FragmentActivity context) {
        int imageThumbSize = context.getResources().getDimensionPixelSize(R.dimen.small_image_fetcher);
        return getImageFetcher(context, imageThumbSize, SMALL_IMAGE_CACHE_DIR, false);
    }

    /**
     * Create {@link ImageFetcher} instance to fetch
     * small images for the Details View
     *
     * @param context {@link android.content.Context}
     * @return {@link ImageFetcher} instance
     */
    public static ImageWorker getLargeImageFetcher(final FragmentActivity context) {
        return getImageFetcher(
                context, AppUtils.getLongestScreenSize(context), LARGE_IMAGE_CACHE_DIR, false
        );
    }

    /**
     *
     * @param context
     * @return
     */
    public static ImageWorker getTvPlayerImageFetcher(final FragmentActivity context) {
        return getImageFetcher(
                context,
                context.getResources().getDimensionPixelSize(R.dimen.list_item_tv_img_width),
                TV_PLAYER_IMAGE_CACHE_DIR,
                true
        );
    }

    /**
     * Create {@link ImageFetcher} instance
     *
     * @param context        {@link android.content.Context}
     * @param imageThumbSize desired size of the fetched image
     * @param imageDir       directory to addToLocals images at
     * @return {@link ImageFetcher} instance
     */
    private static ImageWorker getImageFetcher(final FragmentActivity context, final int imageThumbSize,
                                               final String imageDir, final boolean isTvPlayer) {

        final ImageCache.ImageCacheParams params =
                new ImageCache.ImageCacheParams(context, imageDir);

        params.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        final ImageWorker worker = new ImageFetcher(context, imageThumbSize);
        worker.setLoadingImage(R.drawable.ic_radio_station_empty);
        worker.addImageCache(context.getSupportFragmentManager(), params);
        worker.setImageFadeIn(false);
        worker.setTvPlayer(isTvPlayer);

        return worker;
    }
}
