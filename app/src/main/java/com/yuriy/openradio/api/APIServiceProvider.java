package com.yuriy.openradio.api;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/14/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */

import android.net.Uri;

import com.yuriy.openradio.net.Downloader;

import java.util.List;

/**
 * {@link com.yuriy.openradio.api.APIServiceProvider} is an interface which provide
 * various methods of the Dirble's API.
 */
public interface APIServiceProvider {

    /**
     * Get a list of all categories.
     *
     * @param downloader Implementation of the {@link com.yuriy.openradio.net.Downloader} interface.
     * @param uri        {@link android.net.Uri} of the request.
     *
     * @return Collection of the {@link com.yuriy.openradio.api.CategoryVO}s
     */
    public List<CategoryVO> getAllCategories(final Downloader downloader, final Uri uri);

    /**
     * Get a list of child in category.
     *
     * @param downloader Implementation of the {@link com.yuriy.openradio.net.Downloader} interface.
     * @param uri        {@link android.net.Uri} of the request.
     * @return Collection of the {@link com.yuriy.openradio.api.CategoryVO}s
     */
    public List<CategoryVO> getChildCategories(final Downloader downloader, final Uri uri);

    /**
     * Get a list of Radio Stations in the Category.
     *
     * @param downloader Implementation of the {@link com.yuriy.openradio.net.Downloader} interface.
     * @param uri        {@link android.net.Uri} of the request.
     * @return collection of the Radio Stations.
     */
    public List<RadioStationVO> getStationsInCategory(final Downloader downloader, final Uri uri);

    /**
     * Get a Radio Station.
     *
     * @param downloader Implementation of the {@link com.yuriy.openradio.net.Downloader} interface.
     * @param uri        {@link android.net.Uri} of the request.
     * @return Radio Station.
     */
    public RadioStationVO getStation(final Downloader downloader, final Uri uri);
}
