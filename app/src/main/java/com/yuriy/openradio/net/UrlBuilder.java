package com.yuriy.openradio.net;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */

import android.content.Context;
import android.net.Uri;

import com.yuriy.openradio.utils.ApiKeyLoader;

/**
 * {@link com.yuriy.openradio.net.UrlBuilder} is a helper class which performs
 * build URL of the different API calls.
 */
public class UrlBuilder {

    /**
     * Base URL for the API requests
     */
    protected static final String BASE_URL = "http://api.dirble.com/v1/";

    /**
     * Private constructor.
     * Disallow instantiation of this helper class.
     */
    private UrlBuilder() { }

    /**
     * Get Uri for the All Categories list.
     *
     * @param context Context of the application.
     * @return {@link android.net.Uri}
     */
    public static Uri getAllCategoriesUrl(final Context context) {
        return Uri.parse(BASE_URL + "categories/apikey/" + ApiKeyLoader.getApiKey(context));
    }

    /**
     * Get Uri for the All Categories list.
     *
     * @param context   Context of the application.
     * @param primaryId Id of the primary Menu Item
     * @return {@link android.net.Uri}
     */
    public static Uri getChildCategoriesUrl(final Context context, final String primaryId) {
        return Uri.parse(BASE_URL + "childCategories/apikey/" + ApiKeyLoader.getApiKey(context)
                + "/primaryid/" + primaryId);
    }
}
