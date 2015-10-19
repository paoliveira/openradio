/*
 * Copyright 2015 The "Open Radio" Project. Author: Chernyshov Yuriy
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
     * Base URL for the API requests.
     */
    private static final String BASE_URL = "http://api.dirble.com/v2/";

    /**
     * URL of the Geo Names service to obtain country's flag.
     */
    private static final String GEO_NAMES_FLAGS = "http://www.geonames.org/flags/";

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
        return Uri.parse(BASE_URL + "categories?token=" + ApiKeyLoader.getApiKey(context));
    }

    /**
     * Get Uri for the All Countries list.
     *
     * @param context Context of the application.
     * @return {@link android.net.Uri}
     */
    public static Uri getAllCountriesUrl(final Context context) {
        return Uri.parse(BASE_URL + "countries?token=" + ApiKeyLoader.getApiKey(context));
    }

    /**
     * Get Uri for the Child Category's list (list of the categories in the main menu item).
     *
     * @param context   Context of the application.
     * @param primaryId Id of the primary Menu Item
     * @return {@link android.net.Uri}
     */
    public static Uri getChildCategoriesUrl(final Context context, final String primaryId) {
        return Uri.parse(
                BASE_URL + "category/" + primaryId + "/childs"
                        + "?token=" + ApiKeyLoader.getApiKey(context)
        );
    }

    /**
     * Get Uri for the list of the Radio Stations in concrete Category.
     *
     * @param context    Context of the application.
     * @param categoryId Id of the Category.
     * @return {@link android.net.Uri}
     */
    public static Uri getStationsInCategory(final Context context, final String categoryId) {
        return Uri.parse(
                BASE_URL + "category/" + categoryId + "/stations"
                        + "?token=" + ApiKeyLoader.getApiKey(context)
        );
    }

    /**
     * Get Uri for the list of the Radio Stations in concrete Category.
     *
     * @param context     Context of the application.
     * @param countryCode Country Code.
     * @return {@link android.net.Uri}
     */
    public static Uri getStationsInCountry(final Context context, final String countryCode) {
        return Uri.parse(
                BASE_URL + "countries/" + countryCode + "/stations"
                        + "?token=" + ApiKeyLoader.getApiKey(context)
        );
    }

    /**
     * Get Uri for the concrete Radio Station details.
     *
     * @param context    Context of the application.
     * @param stationId  Id of the Radio Station.
     * @return {@link android.net.Uri}
     */
    public static Uri getStation(final Context context, final String stationId) {
        return Uri.parse(
                BASE_URL + "station/" + stationId + "?token=" + ApiKeyLoader.getApiKey(context)
        );
    }

    /**
     * Get Uri for the search by provided query.
     *
     * @param context Context of the application.
     * @param query   Search query.
     * @return {@link android.net.Uri}
     */
    public static Uri getSearchQuery(final Context context, final String query) {
        return Uri.parse(
                BASE_URL + "search/" + query + "?token=" + ApiKeyLoader.getApiKey(context)
        );
    }

    /**
     * Get Uri for the provided country flag of the small size for the usage in Geo Names service.
     *
     * @param countryCode Country code.
     * @return {@link android.net.Uri}
     */
    public static Uri getCountryFlagSmall(final String countryCode) {
        return getCountryFlag(countryCode.toLowerCase(), "l");
    }

    /**
     * Get Uri for list of all Radio Stations.
     *
     * @param context Context of the application.
     * @return {@link android.net.Uri}
     */
    public static Uri getAllStations(final Context context, final int pageNumber,
                                     final int numberPerPage) {
        return Uri.parse(BASE_URL + "stations/?token=" + ApiKeyLoader.getApiKey(context)
                + "&page=" + String.valueOf(pageNumber)
                + "&per_page=" + String.valueOf(numberPerPage));
    }

    /**
     * Get Uri for the provided country flag and flag size for the usage in Geo Names service.
     *
     * @param countryCode Country code.
     * @param size        Size of the flag's image. Could be "l" for the small size and
     *                    "x" for the big size.
     * @return {@link android.net.Uri}
     */
    private static Uri getCountryFlag(final String countryCode, final String size) {
        return Uri.parse(GEO_NAMES_FLAGS + size + "/" + countryCode.toLowerCase() + ".gif");
    }
}
